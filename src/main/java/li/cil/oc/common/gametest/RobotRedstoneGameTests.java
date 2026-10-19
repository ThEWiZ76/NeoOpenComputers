package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.block.RobotBlock;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class RobotRedstoneGameTests {
    @GameTest(template = "empty", timeoutTicks = 220)
    public static void robotAdvancedRedstoneUsesActualComputerCraftAndPersistsLocalBundledOutput(GameTestHelper helper) throws Exception {
        if (!net.neoforged.fml.ModList.get().isLoaded("computercraft")) { helper.succeed(); return; }
        final var world = helper.getLevel();
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        world.setBlockAndUpdate(pos, ModBlocks.ROBOT.get().defaultBlockState());
        final var robot = (RobotBlockEntity) world.getBlockEntity(pos);
        robot.setTier(1);
        RobotMovementPersistenceGameTests.installHardware(helper, robot, java.util.List.of(new ItemStack(ModItems.REDSTONE_CARD_TIER2.get())));
        robot.onLoad();
        ((Connector) robot.machine().node()).changeBuffer(10000);
        final var card = redstone(helper, robot);
        card.invoke("setBundledOutput", null, 1, 15, 255);
        final var cleanup = RedstoneCardGameTests.bootComputerCraft(helper, pos.above(),
            "redstone.setBundledOutput('bottom', 5); redstone.setBundledOutput('top', redstone.getBundledInput('bottom'))");
        helper.succeedWhen(() -> {
            helper.assertTrue(dan200.computercraft.api.ComputerCraftAPI.getBundledRedstoneOutput(world, pos.above(), Direction.UP) == 32768,
                "Actual CC computer did not receive robot bundled output");
            helper.assertTrue(robot.bundledRedstoneInput(Direction.UP, 0) == 15 && robot.bundledRedstoneInput(Direction.UP, 2) == 15,
                "Robot has not sampled actual CC bundled output");
            try {
                helper.assertTrue(((Number) card.invoke("getBundledInput", null, 1, 0)[0]).intValue() == 15
                    && ((Number) card.invoke("getBundledInput", null, 1, 2)[0]).intValue() == 15, "Robot did not receive actual CC bundled output");
                card.invoke("setBundledOutput", null, Direction.WEST.get3DDataValue(), 7, 42);
                final var oldSide = robot.toGlobal(Direction.WEST);
                helper.assertTrue(Boolean.TRUE.equals(((Component) robot.node()).invoke("turn", null, true)[0]), "Bundled robot could not turn");
                helper.assertTrue(RedstoneCardGameTests.ccProviderOutput(world, pos, robot.toGlobal(Direction.WEST)) == 128
                    && RedstoneCardGameTests.ccProviderOutput(world, pos, oldSide) == 0, "Bundled output did not rotate with the robot");
                final var saved = robot.saveWithFullMetadata(world.registryAccess());
                card.invoke("setBundledOutput", null, Direction.WEST.get3DDataValue(), 7, 0);
                robot.loadWithComponents(saved, world.registryAccess());
                robot.onLoad();
                helper.assertTrue(((Number) redstone(helper, robot).invoke("getBundledOutput", null, Direction.WEST.get3DDataValue(), 7)[0]).intValue() == 42,
                    "Robot reload lost local bundled output strength");
            } catch (Exception exception) { throw new RuntimeException(exception); }
            finally { cleanup.run(); }
        });
    }

    @GameTest(template = "empty")
    public static void robotRedstoneDrivesWorldAndKeepsLocalOutputsAcrossTurnMoveAndReload(GameTestHelper helper) throws Exception {
        final var world = helper.getLevel();
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        world.setBlockAndUpdate(pos, ModBlocks.ROBOT.get().defaultBlockState().setValue(RobotBlock.FACING, Direction.EAST));
        final var robot = (RobotBlockEntity) world.getBlockEntity(pos);
        RobotMovementPersistenceGameTests.installHardware(helper, robot, java.util.List.of(new ItemStack(ModItems.REDSTONE_CARD.get())));
        robot.onLoad();
        ((Connector) robot.machine().node()).changeBuffer(10000);
        final var redstone = redstone(helper, robot);
        for (Direction local : Direction.values()) redstone.invoke("setOutput", null, local.get3DDataValue(), local.get3DDataValue() + 1);
        helper.assertTrue(world.getSignal(pos, Direction.WEST) == 4, "East-facing robot front did not emit eastward");
        verifyOutputs(helper, robot);
        redstone.invoke("setWakeThreshold", null, 7);
        final var robotComponent = (Component) robot.node();
        helper.assertTrue(Boolean.TRUE.equals(robotComponent.invoke("turn", null, true)[0]), "Robot could not turn");
        helper.assertTrue(world.getSignal(pos, Direction.NORTH) == 4, "Turned robot front did not emit southward");
        verifyOutputs(helper, robot);
        helper.assertTrue(Boolean.TRUE.equals(robotComponent.invoke("move", null, 1)[0]), "Robot could not move up");
        helper.assertTrue(world.getBlockEntity(pos.above()) == robot, "Movement did not retain robot host");
        verifyOutputs(helper, robot);
        helper.assertTrue(world.getSignal(pos, Direction.DOWN) == 0, "Old robot position still emitted power");
        final var saved = robot.saveWithFullMetadata(world.registryAccess());
        for (Direction local : Direction.values()) redstone.invoke("setOutput", null, local.get3DDataValue(), 0);
        redstone.invoke("setWakeThreshold", null, 0);
        robot.loadWithComponents(saved, world.registryAccess());
        robot.onLoad();
        verifyOutputs(helper, robot);
        helper.assertTrue(((Number) redstone(helper, robot).invoke("getWakeThreshold", null)[0]).intValue() == 7, "Wake threshold did not survive reload");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void robotLuaReceivesRedstoneInputAndRuntimeCardRemovalClearsOutput(GameTestHelper helper) {
        final var world = helper.getLevel();
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        world.setBlockAndUpdate(pos, ModBlocks.ROBOT.get().defaultBlockState());
        final var robot = (RobotBlockEntity) world.getBlockEntity(pos);
        RobotMovementPersistenceGameTests.installHardware(helper, robot, java.util.List.of(
            new ItemStack(ModItems.CARD_CONTAINER_TIER1.get()), new ItemStack(ModItems.CPU_TIER1.get()), new ItemStack(ModItems.MEMORY_TIER1.get()),
            RobotMovementPersistenceGameTests.eeprom("""
                local r = component.proxy(component.list('redstone')())
                local bot = component.proxy(component.list('robot')())
                r.setWakeThreshold(1)
                r.setOutput(3, 11)
                bot.setLightColor(0x113311)
                while true do
                  local event, address, side, old, value = computer.pullSignal()
                  if event == 'redstone_changed' and side == 1 and old == 0 and value == 15 and r.getInput(1) == 15 then
                    bot.setLightColor(0x226622)
                  end
                end
                """)));
        robot.setItem(1, new ItemStack(ModItems.REDSTONE_CARD.get()));
        robot.onLoad();
        ((Connector) robot.machine().node()).changeBuffer(10000);
        helper.assertTrue(robot.machine().start(), "Robot redstone Lua fixture could not boot");
        final var powered = new java.util.concurrent.atomic.AtomicBoolean();
        helper.succeedWhen(() -> {
            if (!powered.get()) {
                helper.assertTrue(robot.lightColor() == 0x113311, "Robot Lua did not discover the runtime redstone card: " + robot.machine().lastError());
                world.setBlockAndUpdate(pos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                powered.set(true);
            }
            helper.assertTrue(robot.lightColor() == 0x226622, "Robot Lua did not receive side/value-correct input event");
            helper.assertTrue(world.getSignal(pos, robot.toGlobal(Direction.SOUTH).getOpposite()) == 11, "Runtime card never powered the robot output");
            robot.machine().stop();
            helper.assertTrue(world.getSignal(pos, robot.toGlobal(Direction.SOUTH).getOpposite()) == 11, "Stopping a robot incorrectly cleared its installed card output");
            world.removeBlock(pos.above(), false);
            RobotBlockEntity.serverTick(world, pos, robot.getBlockState(), robot);
            helper.assertTrue(!robot.machine().isRunning(), "Falling edge spuriously woke the robot");
            world.setBlockAndUpdate(pos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            RobotBlockEntity.serverTick(world, pos, robot.getBlockState(), robot);
            helper.assertTrue(robot.machine().isRunning(), "Configured rising input did not wake the stopped robot");
            robot.setItem(1, ItemStack.EMPTY);
            RobotBlockEntity.serverTick(world, pos, robot.getBlockState(), robot);
            for (Direction side : Direction.values()) helper.assertTrue(world.getSignal(pos, side) == 0, "Removed redstone card left powered robot output");
            robot.machine().stop();
        });
    }

    private static Component redstone(GameTestHelper helper, RobotBlockEntity robot) {
        for (final var node : robot.machine().node().neighbors()) {
            if (node instanceof Component component && component.name().equals("redstone")) return component;
        }
        helper.fail("Installed robot redstone card did not create a component");
        throw new AssertionError();
    }

    private static void verifyOutputs(GameTestHelper helper, RobotBlockEntity robot) {
        for (Direction local : Direction.values()) {
            final var query = robot.toGlobal(local).getOpposite();
            final int expected = local.get3DDataValue() + 1;
            helper.assertTrue(robot.getLevel().getSignal(robot.getBlockPos(), query) == expected, "Robot weak output lost local side " + local);
            helper.assertTrue(robot.getLevel().getDirectSignal(robot.getBlockPos(), query) == expected, "Robot strong output lost local side " + local);
        }
    }
}
