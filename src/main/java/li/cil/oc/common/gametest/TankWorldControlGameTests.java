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
        final var tank = agent.tank().getFluidTank(0);
        helper.assertTrue(component.invoke("fill", agent.machine(), side)[0] == null, "Fluid placement replaced solid block");
        helper.assertTrue(tank.getFluidAmount() == 1000, "Rejected placement consumed fluid");
        helper.getLevel().setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        helper.assertTrue(component.invoke("fill", agent.machine(), side, 999)[0] == null, "Partial bucket placed a source");
        helper.assertTrue(tank.getFluidAmount() == 1000 && helper.getLevel().isEmptyBlock(target), "Partial placement changed tank/world");
        final var filled = component.invoke("fill", agent.machine(), side);
        helper.assertTrue(Boolean.TRUE.equals(filled[0]) && ((Number) filled[1]).intValue() == 1000, "Bucket placement failed");
        helper.assertTrue(tank.getFluidAmount() == 0 && helper.getLevel().getFluidState(target).isSource(), "Placed source did not conserve water");
        final var partial = component.invoke("drain", agent.machine(), side, 999);
        helper.assertTrue(!Boolean.TRUE.equals(partial[0]) && tank.getFluidAmount() == 0
            && helper.getLevel().getFluidState(target).isSource(), "Partial drain destroyed world source");
        final var drained = component.invoke("drain", agent.machine(), side);
        helper.assertTrue(Boolean.TRUE.equals(drained[0]) && ((Number) drained[1]).intValue() == 1000, "Source pickup failed");
        helper.assertTrue(tank.getFluidAmount() == 1000 && helper.getLevel().isEmptyBlock(target), "Source pickup did not conserve water");
        helper.getLevel().setBlockAndUpdate(target, Blocks.LAVA.defaultBlockState());
        helper.assertTrue(component.invoke("drain", agent.machine(), side)[0] == null, "Incompatible lava entered water tank");
        helper.assertTrue(tank.getFluidAmount() == 1000 && helper.getLevel().getFluidState(target).is(Fluids.LAVA), "Rejected drain changed fluid");
        final var lava = new net.neoforged.neoforge.fluids.capability.templates.FluidTank(1000);
        lava.setFluid(new FluidStack(Fluids.LAVA, 1000));
        final var water = new net.neoforged.neoforge.fluids.capability.templates.FluidTank(2000);
        water.setFluid(new FluidStack(Fluids.WATER, 1200));
        try (var fixture = TestFluidCapabilities.attach(helper.getLevel(), target, direction.getOpposite(), lava, water)) {
            helper.assertTrue(Boolean.TRUE.equals(component.invoke("compareFluid", agent.machine(), side)[0]), "Multi-tank search missed matching fluid");
            helper.assertTrue(Boolean.FALSE.equals(component.invoke("compareFluid", agent.machine(), side, 1)[0])
                && Boolean.TRUE.equals(component.invoke("compareFluid", agent.machine(), side, 2)[0]), "Explicit external tank selection failed");
            final var partialFill = component.invoke("fill", agent.machine(), side, 1000);
            helper.assertTrue(Boolean.TRUE.equals(partialFill[0]) && ((Number) partialFill[1]).intValue() == 800
                && tank.getFluidAmount() == 200 && water.getFluidAmount() == 2000, "Partial capability fill lost fluid");
            helper.assertTrue(component.invoke("fill", agent.machine(), side)[0] == null
                && tank.getFluidAmount() == 200, "Full external tank consumed fluid");
            final var partialDrain = component.invoke("drain", agent.machine(), side, 600);
            helper.assertTrue(Boolean.TRUE.equals(partialDrain[0]) && ((Number) partialDrain[1]).intValue() == 600
                && tank.getFluidAmount() == 800 && water.getFluidAmount() == 1400, "Typed drain chose wrong tank or lost fluid");
            helper.assertTrue(lava.getFluidAmount() == 1000, "Water transfer changed neighboring lava tank");
            tank.fill(new FluidStack(Fluids.WATER, tank.getCapacity()), FluidAction.EXECUTE);
            final var full = component.invoke("drain", agent.machine(), side);
            helper.assertTrue(full[0] == null && "tank is full".equals(full[1]) && water.getFluidAmount() == 1400, "Full internal tank consumed external fluid");
        }
    }
}
