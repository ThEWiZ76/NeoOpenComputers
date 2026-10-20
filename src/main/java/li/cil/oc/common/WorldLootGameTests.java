package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.common.network.LootDiskNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** Opt-in; the isolated world is preseeded before server startup. */
@net.neoforged.neoforge.gametest.GameTestHolder("neoopencomputers")
public final class WorldLootGameTests {
    @GameTestGenerator public static Collection<TestFunction> tests() {
        if (!Boolean.getBoolean("neoopencomputers.testWorldLoot")) return List.of();
        return List.of(
            new TestFunction("worldLoot", "world_loot_descriptors_reach_factories_recipes_and_metadata", "neoopencomputers:empty", 100, 0, true, WorldLootGameTests::definitions),
            new TestFunction("worldLoot", "world_loot_sampling_uses_descriptor_weights", "neoopencomputers:empty", 100, 0, true, WorldLootGameTests::sampling),
            new TestFunction("worldLoot", "world_loot_large_weights_and_copies", "neoopencomputers:empty", 100, 0, true, WorldLootGameTests::pool),
            new TestFunction("worldLoot", "world_loot_unload_and_other_world_isolation", "neoopencomputers:empty", 100, 0, true, WorldLootGameTests::isolation));
    }

    private static void definitions(GameTestHelper helper) {
        final var registry = (ItemRegistry) API.items;
        final var cycling = ModLootDisks.stacksForCycling();
        helper.assertTrue(cycling.stream().anyMatch(stack -> stack.getHoverName().getString().equals("World fixture")), "World loot.properties definitions did not reach the actual cycling recipe outputs");
        helper.assertTrue(cycling.stream().anyMatch(stack -> stack.getHoverName().getString().equals("Zero fixture")), "Zero sampling weight incorrectly disables cycling");
        helper.assertTrue(cycling.stream().noneMatch(stack -> stack.getHoverName().getString().equals("Invalid count") || stack.getHoverName().getString().equals("Escaping fixture")), "Invalid descriptor became a disk");
        final var packets = LootDiskNetworking.snapshot().stream().map(packet -> packet.stack()).filter(stack -> !stack.isEmpty()).toList();
        helper.assertTrue(packets.stream().anyMatch(stack -> stack.getHoverName().getString().equals("World fixture")) && packets.stream().anyMatch(stack -> stack.getHoverName().getString().equals("Zero fixture")), "Login metadata omits world disks or zero-weight disks");
        helper.assertTrue(packets.stream().filter(stack -> ModLootDisks.factoryId("network").equals(factory(stack))).count() == 1 &&
            packets.stream().anyMatch(stack -> ModLootDisks.factoryId("network").equals(factory(stack)) && stack.getHoverName().getString().equals("World network")), "World factory collision duplicated or lost its metadata");
        helper.assertTrue(registry.registeredFloppies().stream().noneMatch(stack -> stack.getHoverName().getString().equals("World fixture")), "World initialization permanently mutated the addon registry");
        try {
            final var fs = registry.floppyFactory(ModLootDisks.factoryId("addon")).call();
            try {
                helper.assertTrue(fs.isReadOnly(), "World loot factory is writable");
                final var handle = fs.getHandle(fs.open("probe.bin", Mode.Read));
                final byte[] bytes = new byte[5];
                helper.assertTrue(handle.read(bytes) == 5 && Arrays.equals(bytes, new byte[]{'a', 0, (byte) 128, (byte) 255, 42}), "World loot factory read the wrong world or damaged binary bytes");
            } finally { fs.close(); }
            final var network = registry.floppyFactory(ModLootDisks.factoryId("network")).call();
            try { helper.assertTrue(network.exists("network-fixture.bin"), "Bundled factory collision did not select the world's files"); }
            finally { network.close(); }
        } catch (Exception failure) { helper.assertTrue(false, "World loot factory failed: " + failure); }
        helper.succeed();
    }

    private static void sampling(GameTestHelper helper) {
        final var expected = new ArrayList<String>(List.of("World fixture", "World fixture", "World network"));
        final var descriptors = ModLootDisks.bundledDescriptors();
        for (final var descriptor : descriptors) for (int count = 0; count < descriptor.weight(); count++) expected.add(descriptor.label());
        final var actualRandom = RandomSource.create(731);
        final var expectedRandom = RandomSource.create(731);
        for (int count = 0; count < 1000; count++) {
            final var disk = ModLootDisks.randomDisk(actualRandom);
            helper.assertTrue(disk.getCount() == 1 && disk.getHoverName().getString().equals(expected.get(expectedRandom.nextInt(expected.size()))), "Weighted disk selection differs from the upstream expanded-weight model");
            disk.setCount(0);
        }
        helper.succeed();
    }

    private static void pool(GameTestHelper helper) {
        final var first = new ItemStack(ModItems.FLOPPY.get());
        first.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("First"));
        final var second = first.copy();
        second.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Second"));
        final var pool = new LootDiskPool(List.of(new LootDiskPool.Entry(first, Integer.MAX_VALUE), new LootDiskPool.Entry(second, Integer.MAX_VALUE), new LootDiskPool.Entry(second, 0)));
        first.setCount(0);
        helper.assertTrue(pool.weight() == 4294967294L, "Large weights overflowed or were expanded into an integer-sized pool");
        helper.assertTrue(pool.at(0).getHoverName().getString().equals("First") && pool.at(Integer.MAX_VALUE - 1L).getHoverName().getString().equals("First")
            && pool.at(Integer.MAX_VALUE).getHoverName().getString().equals("Second") && pool.at(4294967293L).getHoverName().getString().equals("Second"), "Weighted boundaries select the wrong disk");
        final var random = RandomSource.create(731);
        for (int index = 0; index < 100; index++) helper.assertTrue(!pool.sample(random).isEmpty(), "Large weighted selection lost an entry");
        pool.at(0).setCount(0);
        helper.assertTrue(pool.at(0).getCount() == 1 && !second.isEmpty(), "Sampling shares private or caller stacks");
        helper.assertTrue(new LootDiskPool(List.of(new LootDiskPool.Entry(second, 0))).sample(random).isEmpty(), "An empty weighted pool selected a zero-weight disk");
        helper.succeed();
    }

    private static void isolation(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var owner = (ItemRegistry) API.items;
        final var other = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("other-world-loot-fixture");
        helper.assertTrue(!Files.exists(other), "Preserve an existing second-world fixture directory");
        final var loot = other.resolve("opencomputers/loot");
        try {
            Files.createDirectories(loot.resolve("addon"));
            Files.writeString(loot.resolve("loot.properties"), "addon=Other world:1:dyeRed\n");
            Files.write(loot.resolve("addon/probe.bin"), new byte[]{9});
            final var state = WorldLootDisks.read(other, owner);
            final var fs = state.factory(ModLootDisks.factoryId("addon")).call();
            try {
                final byte[] data = new byte[1];
                helper.assertTrue(fs.getHandle(fs.open("probe.bin", Mode.Read)).read(data) == 1 && data[0] == 9, "Other-world factory read the active world's bytes");
            } finally { fs.close(); }
            helper.assertTrue(ModLootDisks.stacksForCycling().stream().anyMatch(stack -> stack.getHoverName().getString().equals("World fixture")), "Reading another world's descriptors changed the active world");
            WorldLootDisks.unload(server);
            helper.assertTrue(owner.floppyFactory(ModLootDisks.factoryId("addon")) == null && ModLootDisks.randomDisk(RandomSource.create(731)).isEmpty(), "Unloaded world factories or sampling state leaked");
            final var bundled = owner.floppyFactory(ModLootDisks.factoryId("network")).call();
            try { helper.assertTrue(!bundled.exists("network-fixture.bin"), "World override permanently replaced the bundled factory"); }
            finally { bundled.close(); }
        } catch (Exception failure) { helper.assertTrue(false, "World isolation fixture failed: " + failure); }
        finally {
            WorldLootDisks.load(server);
            try {
                for (final var path : List.of(loot.resolve("addon/probe.bin"), loot.resolve("loot.properties"), loot.resolve("addon"), loot, other.resolve("opencomputers"), other)) Files.deleteIfExists(path);
            } catch (java.io.IOException failure) { helper.assertTrue(false, "Cannot clean owned world fixture: " + failure); }
        }
        helper.succeed();
    }

    private static String factory(ItemStack stack) {
        final var data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getString(ItemRegistry.FLOPPY_FACTORY_ID_TAG);
    }
}
