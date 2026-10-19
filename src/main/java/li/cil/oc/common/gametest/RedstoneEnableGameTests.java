package li.cil.oc.common.gametest;

import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.item.ItemDriverData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class RedstoneEnableGameTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void caseStopClearsOutputs(GameTestHelper h) { lifecycle(h, ModBlocks.COMPUTER_CASE_TIER2.get(), "stop"); }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void microcontrollerStopClearsOutputs(GameTestHelper h) { lifecycle(h, ModBlocks.MICROCONTROLLER_TIER2.get(), "stop"); }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void caseCardRemovalClearsOutputs(GameTestHelper h) { lifecycle(h, ModBlocks.COMPUTER_CASE_TIER2.get(), "remove"); }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void microcontrollerCardRemovalClearsOutputs(GameTestHelper h) { lifecycle(h, ModBlocks.MICROCONTROLLER_TIER2.get(), "remove"); }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void caseCrashClearsOutputs(GameTestHelper h) { lifecycle(h, ModBlocks.COMPUTER_CASE_TIER2.get(), "crash"); }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void microcontrollerCrashClearsOutputs(GameTestHelper h) { lifecycle(h, ModBlocks.MICROCONTROLLER_TIER2.get(), "crash"); }
    @GameTest(template = "empty")
    public static void caseWithoutCardCannotConnectDust(GameTestHelper h) { disabled(h, ModBlocks.COMPUTER_CASE_TIER2.get()); }
    @GameTest(template = "empty")
    public static void microcontrollerWithoutCardCannotConnectDust(GameTestHelper h) { disabled(h, ModBlocks.MICROCONTROLLER_TIER2.get()); }

    private static void disabled(GameTestHelper helper, Block block) {
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        helper.assertTrue(!block.canConnectRedstone(block.defaultBlockState(), helper.getLevel(), pos, Direction.WEST),
            "Host without a running card advertised a dust connection");
        helper.succeed();
    }

    private static void lifecycle(GameTestHelper helper, Block block, String action) {
        final var world = helper.getLevel();
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        world.setBlockAndUpdate(pos, block.defaultBlockState());
        world.setBlockAndUpdate(pos.above(), net.minecraft.world.level.block.Blocks.REDSTONE_LAMP.defaultBlockState());
        final var fixture = RedstoneMachineFixture.boot(helper, world.getBlockEntity(pos), """
            local r = component.proxy(component.list('redstone')())
            r.setOutput(1, 11)
            if r.setBundledOutput then r.setBundledOutput(1, 0, 255) end
            component.proxy(component.list('eeprom')()).setData('ready')
            while true do computer.pullSignal() end
            """, true);
        helper.startSequence().thenWaitUntil(() -> {
            final var data = ItemDriverData.dataTag(fixture.inventory().getItem(fixture.eepromSlot()));
            helper.assertTrue(new String(data.getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8).equals("ready"),
                "Native redstone program did not reach checkpoint: " + fixture.machine().lastError());
            helper.assertTrue(world.getSignal(pos, Direction.DOWN) == 11, "Running native card did not power its physical output");
            helper.assertTrue(world.getBlockState(pos.above()).getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT),
                "Running native card did not light the adjacent lamp");
            helper.assertTrue(fixture.host().isRedstoneOutputEnabled()
                && block.canConnectRedstone(world.getBlockState(pos), world, pos, Direction.EAST)
                && world.getBlockEntity(pos).getUpdateTag(world.registryAccess()).getBoolean("oc:redstoneOutputEnabled"),
                "Running card did not advertise server/client redstone connectivity");
        }).thenExecute(() -> {
            final var entity = world.getBlockEntity(pos);
            fixture.host().setBundledRedstoneOutput(Direction.UP, 0, 255);
            final var saved = entity.saveWithFullMetadata(world.registryAccess());
            entity.loadWithComponents(saved, world.registryAccess());
            helper.assertTrue(fixture.host().redstoneOutput(Direction.UP) == 11 && fixture.host().bundledRedstoneOutput(Direction.UP, 0) == 255,
                "Reinitializing hardware while loading a running snapshot erased saved outputs");
            fixture.machine().pause(1);
            helper.assertTrue(fixture.host().isRedstoneOutputEnabled() && fixture.host().redstoneOutput(Direction.UP) == 11,
                "Pausing incorrectly disabled the installed card");
            // Also clear legacy bundled state when its provider has since been removed.
            fixture.host().setBundledRedstoneOutput(Direction.UP, 0, 255);
            if (action.equals("remove")) MicrocontrollerFixture.setItem(fixture.inventory(), fixture.cardSlot(), ItemStack.EMPTY);
            else if (action.equals("crash")) fixture.machine().crash("redstone lifecycle probe");
            else fixture.machine().stop();
            helper.assertTrue(fixture.host().redstoneOutput(Direction.UP) == 0 && fixture.host().bundledRedstoneOutput(Direction.UP, 0) == 0,
                "Lifecycle action did not immediately clear cached output");
        }).thenIdle(1).thenExecute(() -> {
            helper.assertTrue(!fixture.host().isRedstoneOutputEnabled()
                && !block.canConnectRedstone(world.getBlockState(pos), world, pos, Direction.EAST)
                && !world.getBlockEntity(pos).getUpdateTag(world.registryAccess()).getBoolean("oc:redstoneOutputEnabled"),
                "Disabled host retained server/client redstone connectivity");
            for (Direction side : Direction.values()) {
                helper.assertTrue(fixture.host().redstoneOutput(side) == 0 && world.getSignal(pos, side) == 0,
                    "Disabled " + action + " retained vanilla output");
                for (int color = 0; color < 16; color++) helper.assertTrue(fixture.host().bundledRedstoneOutput(side, color) == 0,
                    "Disabled " + action + " retained bundled output");
            }
        }).thenIdle(5).thenExecute(() -> helper.assertTrue(
            !world.getBlockState(pos.above()).getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT),
            "Disabled card left its adjacent lamp lit after the scheduled shutoff")).thenSucceed();
    }

    private RedstoneEnableGameTests() {}
}
