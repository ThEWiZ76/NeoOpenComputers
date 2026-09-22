package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.Component;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class TankControlGameTests {
    @GameTest(template = "empty")
    public static void robotTransfersAndSwapsInternalFluids(GameTestHelper helper) throws Exception {
        final var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(pos);
        robot.setTier(2);
        RobotMovementPersistenceGameTests.installHardware(helper, robot, List.of(
            new ItemStack(ModItems.TANK_UPGRADE.get()), new ItemStack(ModItems.TANK_UPGRADE.get())));
        robot.onLoad();
        verify(helper, robot);
    }

    @GameTest(template = "empty")
    public static void droneTransfersAndSwapsInternalFluids(GameTestHelper helper) throws Exception {
        final var drone = new DroneEntity(helper.getLevel());
        drone.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER2.get()), new ItemStack(ModItems.TANK_UPGRADE.get()),
            new ItemStack(ModItems.TANK_UPGRADE.get())), null);
        verify(helper, drone);
        drone.discard();
    }

    private static void verify(GameTestHelper helper, Agent agent) throws Exception {
        final Component component = (Component) ((li.cil.oc.api.network.Environment) agent).node();
        helper.assertTrue(((Number) component.invoke("tankCount", agent.machine())[0]).intValue() == 2, "Two installed tanks were not exposed");
        final var first = agent.tank().getFluidTank(0);
        final var second = agent.tank().getFluidTank(1);
        first.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
        second.fill(new FluidStack(Fluids.LAVA, 500), FluidAction.EXECUTE);
        helper.assertTrue(((Number) component.invoke("selectTank", agent.machine(), 1)[0]).intValue() == 1, "Tank selection failed");
        helper.assertTrue(((Number) component.invoke("tankLevel", agent.machine())[0]).intValue() == 1000, "Selected tank level is wrong");
        helper.assertTrue(((Number) component.invoke("tankSpace", agent.machine(), 2)[0]).intValue() == second.getCapacity() - 500, "Explicit tank space is wrong");
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("compareFluidTo", agent.machine(), 2)[0]), "Water and lava compared equal");
        final var rejected = component.invoke("transferFluidTo", agent.machine(), 2, 250);
        helper.assertTrue(rejected[0] == null && "incompatible or no fluid".equals(rejected[1]), "Partial incompatible transfer was accepted");
        helper.assertTrue(first.getFluidAmount() == 1000 && second.getFluidAmount() == 500, "Rejected transfer changed amounts");
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("transferFluidTo", agent.machine(), 2)[0]), "Full incompatible tanks did not swap");
        helper.assertTrue(first.getFluid().is(Fluids.LAVA) && first.getFluidAmount() == 500
            && second.getFluid().is(Fluids.WATER) && second.getFluidAmount() == 1000, "Fluid swap lost type or amount");
        component.invoke("transferFluidTo", agent.machine(), 2, 1000);
        second.drain(500, FluidAction.EXECUTE);
        component.invoke("transferFluidTo", agent.machine(), 2, 250);
        helper.assertTrue(first.getFluidAmount() == 750 && second.getFluidAmount() == 250, "Partial water transfer was not conserved");
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("compareFluidTo", agent.machine(), 2)[0]), "Matching fluids compared unequal");
        component.invoke("transferFluidTo", agent.machine(), 2, 0);
        helper.assertTrue(first.getFluidAmount() == 750 && second.getFluidAmount() == 250, "Zero transfer changed tanks");
        component.invoke("transferFluidTo", agent.machine(), 2);
        component.invoke("selectTank", agent.machine(), 2);
        helper.assertTrue(first.getFluid().isEmpty() && ((Number) component.invoke("tankLevel", agent.machine())[0]).intValue() == 1000, "Default transfer lost water");
        try {
            component.invoke("selectTank", agent.machine(), 0);
            throw new AssertionError("Invalid tank index accepted");
        } catch (IllegalArgumentException expected) {
            helper.assertTrue("invalid tank index".equals(expected.getMessage()), "Wrong invalid-index error");
        }
        helper.succeed();
    }
}
