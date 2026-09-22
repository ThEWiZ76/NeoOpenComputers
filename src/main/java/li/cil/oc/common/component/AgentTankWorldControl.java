package li.cil.oc.common.component;

import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.machine.Arguments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/** World tank operations shared by robots and drones. */
public final class AgentTankWorldControl {
    private AgentTankWorldControl() { }

    private static Direction side(Agent agent, Arguments args) {
        final int side = args.checkInteger(0);
        if (side < 0 || side > 5 || agent instanceof Robot && side != 0 && side != 1 && side != 3) {
            throw new IllegalArgumentException("invalid side");
        }
        final var direction = Direction.from3DDataValue(side);
        return agent instanceof Robot robot ? robot.toGlobal(direction) : direction;
    }

    public static Object[] compare(Agent agent, Arguments args) {
        final var side = side(agent, args);
        final var tank = agent.tank().getFluidTank(agent.selectedTank());
        if (tank == null || tank.getFluid().isEmpty() || agent.world() == null) return new Object[]{false};
        final var pos = BlockPos.containing(agent.xPosition(), agent.yPosition(), agent.zPosition()).relative(side);
        if (!agent.world().isLoaded(pos)) return new Object[]{false};
        final var handler = agent.world().getCapability(Capabilities.FluidHandler.BLOCK, pos, side.getOpposite());
        final var fluid = agent.world().getFluidState(pos);
        final int tanks = handler != null ? handler.getTanks() : !fluid.isEmpty() && fluid.isSource() ? 1 : 0;
        final boolean specified = args.count() > 1 && args.checkAny(1) != null;
        final int first = specified ? args.checkInteger(1) - 1 : 0;
        if (specified && (first < 0 || first >= tanks)) throw new IllegalArgumentException("invalid tank index");
        for (int index = first; index < (specified ? first + 1 : tanks); index++) {
            final var other = handler != null ? handler.getFluidInTank(index) : new FluidStack(fluid.getType(), 1000);
            if (FluidStack.isSameFluidSameComponents(tank.getFluid(), other)) return new Object[]{true};
        }
        return new Object[]{false};
    }
}
