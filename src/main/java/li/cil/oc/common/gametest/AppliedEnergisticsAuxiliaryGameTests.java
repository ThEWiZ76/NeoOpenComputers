package li.cil.oc.common.gametest;

import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.WrenchTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@GameTestHolder("neoopencomputers")
public final class AppliedEnergisticsAuxiliaryGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> auxiliaryTests() {
        return net.neoforged.fml.ModList.get().isLoaded("ae2") ? Loaded.tests() : List.of();
    }

    private static final class Loaded {
        static Collection<TestFunction> tests() {
            return List.of(
                test("ae2_item_cell_descriptions", helper -> cells(helper, false)),
                test("ae2_fluid_cell_descriptions", helper -> cells(helper, true)),
                test("ae2_wrench_oc_interaction", Loaded::wrench),
                test("ae2_print_move_strategy", Loaded::printMove));
        }

        static TestFunction test(final String name, final java.util.function.Consumer<GameTestHelper> body) {
            return new TestFunction("ae2Auxiliary", name, "neoopencomputers:ae2_export", 200, 0, true, body);
        }

        static void cells(final GameTestHelper helper, final boolean fluid) {
            final var stack = new ItemStack(item(fluid ? "fluid_storage_cell_1k" : "item_storage_cell_1k"));
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Fixture Cell"));
            final var cellItem = (appeng.api.storage.cells.IBasicCellItem) stack.getItem();
            cellItem.setFuzzyMode(stack, appeng.api.config.FuzzyMode.PERCENT_75);
            final var cell = appeng.api.storage.StorageCells.getCellInventory(stack, null);
            final var first = fluid ? appeng.api.stacks.AEFluidKey.of(net.minecraft.world.level.material.Fluids.WATER) : appeng.api.stacks.AEItemKey.of(Items.STONE);
            final var second = fluid ? appeng.api.stacks.AEFluidKey.of(net.minecraft.world.level.material.Fluids.LAVA) : appeng.api.stacks.AEItemKey.of(Items.DIRT);
            final var source = appeng.api.networking.security.IActionSource.empty();
            final var empty = describe(stack);
            number(helper, empty, "totalBytes", 1024);
            number(helper, empty, "freeBytes", 1024);
            number(helper, empty, "storedItemCount", 0);
            helper.assertTrue(Boolean.TRUE.equals(empty.get("canHoldNewItem")), "Empty cell cannot hold a new type");
            final long a = fluid ? 2000 : 64, b = fluid ? 1000 : 3;
            helper.assertTrue(cell != null && cell.insert(first, a, appeng.api.config.Actionable.MODULATE, source) == a
                && cell.insert(second, b, appeng.api.config.Actionable.MODULATE, source) == b, "Cannot fill real description cell");
            final var original = stack.copy();
            final var result = describe(stack);
            number(helper, result, "storedItemCount", a + b);
            number(helper, result, "storedItemTypes", 2);
            number(helper, result, "getTotalItemTypes", fluid ? 18 : 63);
            number(helper, result, "remainingItemTypes", fluid ? 16 : 61);
            number(helper, result, "usedBytes", fluid ? 17 : 25);
            number(helper, result, "freeBytes", fluid ? 1007 : 999);
            number(helper, result, "unusedItemCount", fluid ? 5000 : 5);
            number(helper, result, "remainingItemCount", fluid ? 8061000 : 7997);
            helper.assertTrue(result.get("name").equals("Fixture Cell") && result.get("fuzzyMode").equals("PERCENT_75"), "Cell name/fuzzy setting missing: " + result.get("name") + ", " + result.get("fuzzyMode"));
            helper.assertTrue(ItemStack.matches(stack, original), "Description conversion mutated the cell item");
            final var contents = (Object[]) result.get("getAvailableItems");
            helper.assertTrue(contents.length == 2, "Cell descriptions lost a type");
            long total = 0;
            for (final var value : contents) {
                final var entry = (Map<?, ?>) value;
                final String name = (String) entry.get("name");
                helper.assertTrue(name.equals(fluid ? "minecraft:water" : "minecraft:stone") || name.equals(fluid ? "minecraft:lava" : "minecraft:dirt"), "Wrong cell content description");
                total += ((Number) entry.get(fluid ? "amount" : "size")).longValue();
            }
            helper.assertTrue(total == a + b, "Cell contents quantities changed units or lost items");
            number(helper, describe(cell), "storedItemCount", a + b);
            number(helper, describe(new appeng.me.storage.DriveWatcher(cell, () -> { })), "storedItemCount", a + b);
            cell.insert(first, Long.MAX_VALUE, appeng.api.config.Actionable.MODULATE, source);
            final var full = describe(stack);
            number(helper, full, "freeBytes", 0);
            number(helper, full, "remainingItemCount", 0);
            helper.assertTrue(Boolean.FALSE.equals(full.get("canHoldNewItem")), "Full cell advertises new type capacity");
            helper.assertTrue(!describe(new ItemStack(Items.DIAMOND)).containsKey("totalBytes"), "Non-cell fabricated metrics");
            helper.succeed();
        }

        static void wrench(final GameTestHelper helper) {
            final var player = helper.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            final var pos = helper.absolutePos(new BlockPos(2, 2, 2));
            helper.getLevel().setBlockAndUpdate(pos, ModBlocks.ADAPTER.get().defaultBlockState());
            final var adapter = (li.cil.oc.common.blockentity.AdapterBlockEntity) helper.getLevel().getBlockEntity(pos);
            for (final String name : List.of("certus_quartz_wrench", "nether_quartz_wrench", "network_tool")) {
                final var tool = new ItemStack(item(name));
                player.setItemInHand(InteractionHand.MAIN_HAND, tool);
                final var original = tool.copy();
                helper.assertTrue(WrenchTools.isWrench(tool) && WrenchTools.holdsApplicableWrench(player, pos), "AE2 tool not usable as OC wrench: " + name);
                final boolean before = adapter.isSideOpen(Direction.EAST);
                final var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.EAST, pos, false);
                helper.assertTrue(adapter.getBlockState().useItemOn(tool, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit).consumesAction()
                    && adapter.isSideOpen(Direction.EAST) != before && ItemStack.matches(tool, original), "Real AE2 tool did not toggle OC adapter without consumption");
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
            helper.assertTrue(!WrenchTools.holdsApplicableWrench(player, pos), "Non-wrench accepted by AE2 integration");
            if (net.neoforged.fml.ModList.get().isLoaded("projectred_core")) {
                final var screwdriver = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("projectred_core", "screwdriver")));
                helper.assertTrue(!li.cil.oc.common.integration.AppliedEnergisticsIntegration.isWrench(screwdriver), "AE2 bypasses another mod's wrench eligibility/consumption contract");
            }
            helper.succeed();
        }

        static void printMove(final GameTestHelper helper) {
            final var from = helper.absolutePos(new BlockPos(2, 2, 2));
            final var to = helper.absolutePos(new BlockPos(5, 2, 2));
            final var state = ModBlocks.PRINT.get().defaultBlockState().setValue(li.cil.oc.common.block.PrintBlock.FACING, Direction.WEST);
            helper.getLevel().setBlockAndUpdate(from, state);
            final var original = (li.cil.oc.common.blockentity.PrintBlockEntity) helper.getLevel().getBlockEntity(from);
            final var data = original.data();
            data.setLabel("Spatial Fixture");
            data.addStateOff(new li.cil.oc.common.item.data.PrintData.Shape(new net.minecraft.world.phys.AABB(0, 0, 0, 1, 0.5, 1), "minecraft:block/stone", 0x123456));
            final var before = original.saveWithFullMetadata(helper.getLevel().registryAccess());
            final var strategy = appeng.api.movable.BlockEntityMoveStrategies.get(original);
            helper.assertTrue(!state.is(appeng.api.ids.AETags.SPATIAL_BLACKLIST), "OC print is blacklisted from spatial moves");
            final var saved = strategy.beginMove(original, helper.getLevel().registryAccess());
            helper.assertTrue(saved != null, "AE2 refuses OC print movement");
            helper.getLevel().setBlockAndUpdate(from, Blocks.AIR.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(to, state);
            helper.assertTrue(strategy.completeMove(original, state, saved, helper.getLevel(), to), "AE2 print reconstruction failed");
            final var moved = (li.cil.oc.common.blockentity.PrintBlockEntity) helper.getLevel().getBlockEntity(to);
            final var after = moved.saveWithFullMetadata(helper.getLevel().registryAccess());
            before.remove("x"); before.remove("y"); before.remove("z");
            after.remove("x"); after.remove("y"); after.remove("z");
            helper.assertTrue(before.equals(after) && moved.getBlockPos().equals(to) && moved.getBlockState().equals(state), "AE2 spatial strategy changed print data/rotation");
            helper.succeed();
        }

        static net.minecraft.world.item.Item item(final String path) { return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("ae2", path)).orElseThrow(); }
        static Map<?, ?> describe(final Object value) { return (Map<?, ?>) OpenComputersApi.convert(new Object[]{value})[0]; }
        static void number(final GameTestHelper helper, final Map<?, ?> map, final String key, final long expected) {
            helper.assertTrue(map.get(key) instanceof Number number && number.longValue() == expected, "Wrong " + key + ": expected=" + expected + ", actual=" + map.get(key));
        }
    }
}
