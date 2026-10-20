package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.API;
import li.cil.oc.api.fs.FileSystem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/** World definitions never replace permanent addon registrations. */
public final class WorldLootDisks {
    private static final Map<MinecraftServer, State> states = new ConcurrentHashMap<>();

    public static void registerServerEvents() {
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) -> load(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> unload(event.getServer()));
    }

    static void load(MinecraftServer server) {
        if (API.items instanceof ItemRegistry registry) states.put(server, read(server.getWorldPath(LevelResource.ROOT), registry));
    }

    static void unload(MinecraftServer server) { states.remove(server); }

    static State read(Path worldRoot, ItemRegistry owner) {
        final var properties = new Properties();
        final var descriptorFile = worldRoot.resolve("opencomputers/loot/loot.properties");
        if (Files.isRegularFile(descriptorFile)) {
            try (var input = Files.newInputStream(descriptorFile)) { properties.load(input); }
            catch (IOException | IllegalArgumentException failure) {
                NeoOpenComputers.LOGGER.warn("Cannot read world loot descriptor {}", descriptorFile, failure);
                properties.clear();
            }
        }
        final var definitions = new LinkedHashMap<String, ItemStack>();
        final var factories = new LinkedHashMap<String, Callable<FileSystem>>();
        final var sampling = new ArrayList<LootDiskPool.Entry>();
        final var fileSystems = new FileSystemRegistry(() -> worldRoot);
        for (final var descriptor : ModLootDisks.descriptors(properties)) {
            final var stack = ModLootDisks.descriptorStack(descriptor, owner);
            if (stack.isEmpty()) continue;
            final var factory = ModLootDisks.factoryId(descriptor.path());
            definitions.put(factory, stack.copy());
            factories.put(factory, () -> fileSystems.asReadOnly(fileSystems.fromSaveDirectory("loot/" + descriptor.path(), 0, false)));
            sampling.add(new LootDiskPool.Entry(stack, descriptor.weight()));
        }
        // Upstream appends the bundled sampling entries after world entries.
        for (final var descriptor : ModLootDisks.bundledDescriptors()) {
            sampling.add(new LootDiskPool.Entry(ModLootDisks.descriptorStack(descriptor, owner), descriptor.weight()));
        }
        return new State(owner, Map.copyOf(factories), List.copyOf(definitions.values()), new LootDiskPool(sampling));
    }

    static Callable<FileSystem> factory(ItemRegistry owner, String id) {
        final var state = current(owner);
        return state == null ? null : state.factories.get(id);
    }

    static List<ItemStack> cycling(ItemRegistry owner) {
        final var state = current(owner);
        return state == null ? owner.disksForCycling() : state.merge(owner.disksForCycling());
    }

    public static List<ItemStack> registeredDisks(MinecraftServer server, ItemRegistry owner) {
        if (server == null) return owner.registeredFloppies();
        final var state = states.get(server);
        return state == null || state.owner != owner ? owner.registeredFloppies() : state.merge(owner.registeredFloppies());
    }

    static ItemStack sample(RandomSource random) {
        final var state = API.items instanceof ItemRegistry registry ? current(registry) : null;
        return state == null ? ItemStack.EMPTY : state.pool.sample(random);
    }

    private static State current(ItemRegistry owner) {
        final var server = ServerLifecycleHooks.getCurrentServer();
        final var state = server == null ? null : states.get(server);
        return state != null && state.owner == owner ? state : null;
    }

    static final class State {
        private final ItemRegistry owner;
        private final Map<String, Callable<FileSystem>> factories;
        private final List<ItemStack> ordered;
        private final LootDiskPool pool;

        private State(ItemRegistry owner, Map<String, Callable<FileSystem>> factories, List<ItemStack> ordered, LootDiskPool pool) {
            this.owner = owner; this.factories = factories; this.ordered = ordered; this.pool = pool;
        }

        List<ItemStack> merge(List<ItemStack> base) {
            final var merged = new LinkedHashMap<String, ItemStack>();
            for (final var stack : base) merged.put(factory(stack), stack.copy());
            for (final var stack : ordered) merged.put(factory(stack), stack.copy());
            return List.copyOf(merged.values());
        }

        Callable<FileSystem> factory(String id) { return factories.get(id); }

        private static String factory(ItemStack stack) {
            return stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY)
                .copyTag().getString(ItemRegistry.FLOPPY_FACTORY_ID_TAG);
        }
    }

    private WorldLootDisks() {}
}
