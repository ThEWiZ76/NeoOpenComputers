package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.network.Component;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class TankWorldControlGameTests {
    @GameTest(template = "empty")
    public static void robotComparesWorldFluidUsingLocalFront(GameTestHelper helper) throws Exception {
        final var pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(pos);
        robot.setTier(2);
        RobotMovementPersistenceGameTests.installHardware(helper, robot, java.util.List.of(new ItemStack(ModItems.TANK_UPGRADE.get())));
        robot.onLoad();
        verify(helper, robot, 3, robot.toGlobal(Direction.SOUTH));
        try {
            ((Component) robot.node()).invoke("compareFluid", robot.machine(), 2);
            throw new AssertionError("Robot accepted backward action side");
        } catch (IllegalArgumentException expected) { }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void droneComparesWorldFluidUsingGlobalSide(GameTestHelper helper) throws Exception {
        final var drone = new DroneEntity(helper.getLevel());
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        drone.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        drone.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER1.get()), new ItemStack(ModItems.TANK_UPGRADE.get())), null);
        verify(helper, drone, 5, Direction.EAST);
        drone.discard();
        helper.succeed();
    }

    private static void verify(GameTestHelper helper, Agent agent, int side, Direction direction) throws Exception {
        final var component = (Component) ((li.cil.oc.api.network.Environment) agent).node();
        final var target = BlockPos.containing(agent.xPosition(), agent.yPosition(), agent.zPosition()).relative(direction);
        helper.getLevel().setBlockAndUpdate(target, Blocks.WATER.defaultBlockState());
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("compareFluid", agent.machine(), side)[0]), "Empty tank compared equal to water");
        agent.tank().getFluidTank(0).fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("compareFluid", agent.machine(), side)[0]), "Matching world water did not compare equal");
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("compareFluid", agent.machine(), side, 1)[0]), "Explicit source tank index failed");
        try {
            component.invoke("compareFluid", agent.machine(), side, 2);
            throw new AssertionError("Invalid external tank index accepted");
        } catch (IllegalArgumentException expected) { }
        helper.getLevel().setBlockAndUpdate(target, Blocks.LAVA.defaultBlockState());
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("compareFluid", agent.machine(), side)[0]), "Water compared equal to lava");
        helper.getLevel().setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("compareFluid", agent.machine(), side)[0]), "Solid block compared equal to fluid");
        helper.assertTrue(agent.tank().getFluidTank(0).getFluidAmount() == 1000, "Comparison changed internal fluid");
    }
}
