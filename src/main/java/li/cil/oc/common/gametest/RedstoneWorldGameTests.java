package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.RedstoneIoBlockEntity;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.component.RedstoneControllerHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class RedstoneWorldGameTests {
    @GameTest(template = "empty")
    public static void caseEmitsOnAllSixPhysicalSides(GameTestHelper helper) { axes(helper, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState()); }

    @GameTest(template = "empty")
    public static void microcontrollerEmitsOnAllSixPhysicalSides(GameTestHelper helper) { axes(helper, ModBlocks.MICROCONTROLLER_TIER1.get().defaultBlockState()); }

    @GameTest(template = "empty")
    public static void redstoneIoEmitsOnAllSixPhysicalSides(GameTestHelper helper) { axes(helper, ModBlocks.REDSTONE_IO.get().defaultBlockState()); }

    @GameTest(template = "empty")
    public static void casePowersAdjacentLampAndLampThroughSolidBlock(GameTestHelper helper) { consumers(helper, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState()); }

    @GameTest(template = "empty")
    public static void microcontrollerPowersAdjacentLampAndLampThroughSolidBlock(GameTestHelper helper) { consumers(helper, ModBlocks.MICROCONTROLLER_TIER1.get().defaultBlockState()); }

    @GameTest(template = "empty")
    public static void redstoneIoPowersAdjacentLampAndLampThroughSolidBlock(GameTestHelper helper) { consumers(helper, ModBlocks.REDSTONE_IO.get().defaultBlockState()); }

    @GameTest(template = "empty")
    public static void redstoneIoReadsDirectionalNeighborTowardItsOwnFace(GameTestHelper helper) { directionalInput(helper, ModBlocks.REDSTONE_IO.get().defaultBlockState()); }

    @GameTest(template = "empty")
    public static void caseReadsDirectionalNeighborTowardItsOwnFace(GameTestHelper helper) { directionalInput(helper, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState()); }

    @GameTest(template = "empty")
    public static void microcontrollerReadsDirectionalNeighborTowardItsOwnFace(GameTestHelper helper) { directionalInput(helper, ModBlocks.MICROCONTROLLER_TIER1.get().defaultBlockState()); }

    @GameTest(template = "empty")
    public static void robotReadsDirectionalNeighborTowardItsOwnFace(GameTestHelper helper) { directionalInput(helper, ModBlocks.ROBOT.get().defaultBlockState()); }

    private static void directionalInput(GameTestHelper helper, BlockState state) {
        final var world = helper.getLevel();
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        world.setBlockAndUpdate(pos, state);
        world.setBlockAndUpdate(pos.east(), ModBlocks.ROBOT.get().defaultBlockState());
        final var robot = (RobotBlockEntity) world.getBlockEntity(pos.east());
        RobotMovementPersistenceGameTests.installHardware(helper, robot, java.util.List.of(new ItemStack(ModItems.REDSTONE_CARD.get())));
        robot.onLoad();
        robot.setRedstoneOutput(Direction.WEST, 9);
        helper.assertTrue(world.getSignal(pos.east(), Direction.EAST) == 9 && world.getSignal(pos.east(), Direction.WEST) == 0,
            "Directional fixture did not point only toward the IO block");
        final var blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof RedstoneIoBlockEntity io) io.updateRedstoneInputs();
        else if (blockEntity instanceof li.cil.oc.common.blockentity.ComputerCaseBlockEntity computer) computer.serverTick(world, pos, state, computer);
        else if (blockEntity instanceof li.cil.oc.common.blockentity.MicrocontrollerBlockEntity micro) micro.serverTick(world, pos, state, micro);
        else if (blockEntity instanceof RobotBlockEntity target) RobotBlockEntity.serverTick(world, pos, state, target);
        final var host = (RedstoneControllerHost) blockEntity;
        helper.assertTrue(host.redstoneInput(Direction.EAST) == 9 && host.redstoneInput(Direction.WEST) == 0,
            "Host read the outward face of its directional neighbor");
        helper.succeed();
    }

    private static void axes(GameTestHelper helper, BlockState state) {
        final var world = helper.getLevel();
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        world.setBlockAndUpdate(pos, state);
        final var host = (RedstoneControllerHost) world.getBlockEntity(pos);
        for (Direction outward : Direction.values()) host.setRedstoneOutput(outward, outward.get3DDataValue() + 1);
        for (Direction outward : Direction.values()) {
            final int expected = outward.get3DDataValue() + 1;
            helper.assertTrue(world.getSignal(pos, outward.getOpposite()) == expected, "Weak output points away from physical " + outward);
            helper.assertTrue(world.getDirectSignal(pos, outward.getOpposite()) == expected, "Strong output missing on physical " + outward);
        }
        helper.succeed();
    }

    private static void consumers(GameTestHelper helper, BlockState state) {
        final var world = helper.getLevel();
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        world.setBlockAndUpdate(pos, state);
        final var host = (RedstoneControllerHost) world.getBlockEntity(pos);
        world.setBlockAndUpdate(pos.east(), Blocks.REDSTONE_LAMP.defaultBlockState());
        host.setRedstoneOutput(Direction.EAST, 15);
        helper.assertTrue(world.getBlockState(pos.east()).getValue(RedstoneLampBlock.LIT), "Physical east lamp did not receive output");
        world.setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
        world.setBlockAndUpdate(pos.above(2), Blocks.REDSTONE_LAMP.defaultBlockState());
        host.setRedstoneOutput(Direction.UP, 15);
        helper.assertTrue(world.getBlockState(pos.above(2)).getValue(RedstoneLampBlock.LIT), "Output did not strongly power a solid block and the lamp above it");
        host.setRedstoneOutput(Direction.EAST, 0);
        host.setRedstoneOutput(Direction.UP, 0);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!world.getBlockState(pos.east()).getValue(RedstoneLampBlock.LIT)
                && !world.getBlockState(pos.above(2)).getValue(RedstoneLampBlock.LIT), "Output removal left a real receiver powered");
            helper.succeed();
        });
    }
}
