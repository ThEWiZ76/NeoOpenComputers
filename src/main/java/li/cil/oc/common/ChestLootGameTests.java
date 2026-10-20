package li.cil.oc.common;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.event.LootTableLoadEvent;

import java.util.List;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class ChestLootGameTests {
    private static final List<String> TARGETS = List.of("simple_dungeon", "desert_pyramid", "jungle_temple", "stronghold_library");
    private static final String ENTRY = "neoopencomputers:loot_disk";

    @GameTest(template = "empty")
    public static void loadedTreasureTablesIncludeWeightedDisks(GameTestHelper helper) {
        final var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        for (final var name : TARGETS) {
            final var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(key(name));
            final var json = LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow().getAsJsonObject();
            int disks = 0;
            for (final var pool : json.getAsJsonArray("pools")) {
                for (final var entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
                    final var object = entry.getAsJsonObject();
                    if (object.get("type").getAsString().equals(ENTRY)) {
                        disks++;
                        helper.assertTrue(object.get("weight").getAsInt() == 5, "Loot probability must remain an entry weight of five");
                    }
                }
            }
            helper.assertTrue(disks == 1, "Missing or duplicated weighted loot disk in " + name);
            if (name.equals("simple_dungeon") && net.neoforged.fml.ModList.get().isLoaded("computercraft")) {
                helper.assertTrue(json.toString().contains("computercraft:treasure_disk"), "OC injection lost ComputerCraft's actual treasure-disk pool");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void openingRealTreasureChestsGeneratesUsableDisks(GameTestHelper helper) {
        final var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.CHEST);
        final var chest = (ChestBlockEntity) helper.getBlockEntity(pos);
        final var definitions = new java.util.ArrayList<>(ModLootDisks.stacksForCycling());
        definitions.addAll(ModLootDisks.bundledStacksForCycling());
        for (final var name : TARGETS) {
            boolean foundDisk = false;
            boolean foundVanilla = false;
            boolean foundWorldDisk = false;
            for (long seed = 1; seed <= 128; seed++) {
                chest.clearContent();
                chest.setLootTable(key(name));
                chest.setLootTableSeed(seed);
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    final var stack = chest.getItem(slot);
                    if (stack.is(ModItems.FLOPPY.get())) {
                        foundDisk = true;
                        if (stack.getHoverName().getString().equals("World fixture")) foundWorldDisk = true;
                        helper.assertTrue(stack.getCount() == 1 && definitions.stream().anyMatch(disk -> ItemStack.isSameItemSameComponents(disk, stack)), "Chest disk lost its registered factory/label/color");
                    } else if (!stack.isEmpty()) foundVanilla = true;
                }
                helper.assertTrue(chest.getLootTable() == null, "Chest did not actually unpack its loot table");
            }
            helper.assertTrue(foundDisk && foundVanilla, "Chest generation lost disks or vanilla loot in " + name);
            if (Boolean.getBoolean("neoopencomputers.testWorldLoot")) helper.assertTrue(foundWorldDisk, "Real chests did not sample the active world's custom disks in " + name);
        }
        helper.succeed();
    }

    private static ResourceKey<LootTable> key(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/" + name));
    }

    @GameTest(template = "empty")
    public static void resourceReloadPreservesPoolsAndHonorsWeightAndDisable(GameTestHelper helper) {
        final var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        final int previous = ModSettings.LOOT_PROBABILITY.get();
        final var original = LootTable.DIRECT_CODEC.parse(ops, JsonParser.parseString("""
            {"type":"minecraft:chest", "random_sequence":"neoopencomputers:chest_fixture",
             "functions":[{"function":"minecraft:set_count","count":1}],
             "pools":[
              {"name":"other_mod:bonus", "rolls":1, "entries":[{"type":"minecraft:item","name":"minecraft:diamond"}]},
              {"name":"pack:main", "rolls":3,"bonus_rolls":2,
               "conditions":[{"condition":"minecraft:random_chance","chance":0.5}],
               "functions":[{"function":"minecraft:set_count","count":1}],
               "entries":[{"type":"minecraft:item","name":"minecraft:iron_ingot","weight":10},
                          {"type":"minecraft:item","name":"minecraft:gold_ingot","weight":5}]}]}
            """)).getOrThrow();
        original.setLootTableId(key("simple_dungeon").location());
        final var before = LootTable.DIRECT_CODEC.encodeStart(ops, original).getOrThrow();
        try {
            ModSettings.LOOT_PROBABILITY.set(7);
            final var event = new LootTableLoadEvent(helper.getLevel().registryAccess(), key("simple_dungeon").location(), original);
            ChestLoot.load(event);
            helper.assertTrue(event.getTable() == original, "Injection must preserve the existing table and registry-owned values");
            final var after = LootTable.DIRECT_CODEC.encodeStart(ops, event.getTable()).getOrThrow().getAsJsonObject();
            final var entries = after.getAsJsonArray("pools").get(1).getAsJsonObject().getAsJsonArray("entries");
            helper.assertTrue(entries.size() == 3 && entries.get(2).getAsJsonObject().get("weight").getAsInt() == 7, "Configured weight did not join the original iron/gold pool");
            entries.remove(2);
            helper.assertTrue(before.equals(after), "Injection altered another pool, entry, condition, function, roll count or random sequence");
            final var injected = event.getTable();
            ChestLoot.load(event);
            helper.assertTrue(event.getTable() == injected, "Repeated load hook duplicated the disk entry");
            ModSettings.LOOT_PROBABILITY.set(0);
            final var fresh = LootTable.DIRECT_CODEC.parse(ops, before).getOrThrow();
            final var disabled = new LootTableLoadEvent(helper.getLevel().registryAccess(), key("simple_dungeon").location(), fresh);
            ChestLoot.load(disabled);
            helper.assertTrue(before.equals(LootTable.DIRECT_CODEC.encodeStart(ops, disabled.getTable()).getOrThrow()), "Zero weight did not disable injection on fresh reload");
            ModSettings.LOOT_PROBABILITY.set(7);
            final var unrelated = new LootTableLoadEvent(helper.getLevel().registryAccess(), key("village/village_weaponsmith").location(), fresh);
            ChestLoot.load(unrelated);
            helper.assertTrue(before.equals(LootTable.DIRECT_CODEC.encodeStart(ops, unrelated.getTable()).getOrThrow()), "Injection affected an unrelated chest table");
            final var empty = new LootTableLoadEvent(helper.getLevel().registryAccess(), key("simple_dungeon").location(), LootTable.EMPTY);
            ChestLoot.load(empty);
            helper.assertTrue(empty.getTable() == LootTable.EMPTY, "An intentionally empty datapack table was populated");
        } finally { ModSettings.LOOT_PROBABILITY.set(previous); }
        helper.succeed();
    }
}
