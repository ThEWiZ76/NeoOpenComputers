package li.cil.oc.common;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.mixin.LootTableAccessor;
import li.cil.oc.mixin.LootPoolAccessor;
import li.cil.oc.mixin.LootItemAccessor;
import li.cil.oc.mixin.CompositeLootEntryAccessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ChestLoot {
    private static final Set<ResourceLocation> TABLES = Set.of(
        ResourceLocation.withDefaultNamespace("chests/simple_dungeon"),
        ResourceLocation.withDefaultNamespace("chests/desert_pyramid"),
        ResourceLocation.withDefaultNamespace("chests/jungle_temple"),
        ResourceLocation.withDefaultNamespace("chests/stronghold_library"));
    private static final DeferredRegister<LootPoolEntryType> TYPES = DeferredRegister.create(Registries.LOOT_POOL_ENTRY_TYPE, NeoOpenComputers.MODID);
    private static final Supplier<LootPoolEntryType> DISK = TYPES.register("loot_disk", () -> new LootPoolEntryType(DiskEntry.CODEC));

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        NeoForge.EVENT_BUS.addListener(ChestLoot::load);
    }

    static void load(LootTableLoadEvent event) {
        final int weight = ModSettings.LOOT_PROBABILITY.get();
        if (weight <= 0 || !TABLES.contains(event.getName())) return;
        // A post-generation GLM cannot make this entry compete with the original pool's weights.
        final var table = event.getTable();
        final var pools = ((LootTableAccessor) table).neoopencomputers$getPools();
        if (pools.isEmpty() || pools.stream().flatMap(pool -> entries(pool).stream()).anyMatch(ChestLoot::containsDisk)) return;
        LootPool selected = null;
        final String anchor = event.getName().getPath().equals("chests/stronghold_library") ? "minecraft:book" : "minecraft:iron_ingot";
        for (final var pool : pools) {
            if (entries(pool).stream().anyMatch(entry -> containsItem(entry, anchor))) {
                selected = pool;
                break;
            }
        }
        // A datapack may replace the vanilla entries; keep its pool count and roll settings.
        if (selected == null) selected = pools.getFirst();
        if (table.isFrozen() || selected.isFrozen()) throw new IllegalStateException("Loot disk injection must precede loot table freezing");
        final var entries = new ArrayList<>(entries(selected));
        entries.add(new DiskEntry(weight, 0, List.of(), List.of()));
        ((LootPoolAccessor) selected).neoopencomputers$setEntries(List.copyOf(entries));
    }

    private static List<LootPoolEntryContainer> entries(LootPool pool) {
        return ((LootPoolAccessor) pool).neoopencomputers$getEntries();
    }

    private static boolean containsDisk(LootPoolEntryContainer entry) {
        return entry instanceof DiskEntry || entry instanceof CompositeEntryBase && ((CompositeLootEntryAccessor) entry).neoopencomputers$getChildren().stream().anyMatch(ChestLoot::containsDisk);
    }

    private static boolean containsItem(LootPoolEntryContainer entry, String name) {
        if (entry instanceof LootItem) return ((LootItemAccessor) entry).neoopencomputers$getItem().is(ResourceLocation.parse(name));
        return entry instanceof CompositeEntryBase && ((CompositeLootEntryAccessor) entry).neoopencomputers$getChildren().stream().anyMatch(child -> containsItem(child, name));
    }

    private static final class DiskEntry extends LootPoolSingletonContainer {
        static final MapCodec<DiskEntry> CODEC = RecordCodecBuilder.mapCodec(instance -> singletonFields(instance).apply(instance, DiskEntry::new));

        DiskEntry(int weight, int quality, List<LootItemCondition> conditions, List<LootItemFunction> functions) {
            super(weight, quality, conditions, functions);
        }

        @Override public LootPoolEntryType getType() { return DISK.get(); }

        @Override protected void createItemStack(Consumer<ItemStack> output, LootContext context) {
            final var disk = ModLootDisks.randomDisk(context.getRandom());
            if (!disk.isEmpty()) output.accept(disk);
        }
    }
}
