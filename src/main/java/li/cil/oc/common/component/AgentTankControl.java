package li.cil.oc.common.component;

import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.machine.Arguments;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Internal tank operations shared by robots and drones, matching upstream TankControl. */
public final class AgentTankControl {
    private AgentTankControl() { }

    private static int index(Agent agent, Arguments args, boolean optional) {
        if (optional && (args.count() == 0 || args.checkAny(0) == null)) return agent.selectedTank();
        final int index = args.checkInteger(0) - 1;
        if (index < 0 || index >= agent.tank().tankCount()) throw new IllegalArgumentException("invalid tank index");
        return index;
    }

    private static IFluidTank tank(Agent agent, int index) {
        return index >= 0 && index < agent.tank().tankCount() ? agent.tank().getFluidTank(index) : null;
    }

    public static Object[] select(Agent agent, Arguments args) {
        if (args.count() > 0 && args.checkAny(0) != null) agent.setSelectedTank(index(agent, args, false));
        return new Object[]{agent.selectedTank() + 1};
    }

    public static Object[] level(Agent agent, Arguments args) {
        final var tank = tank(agent, index(agent, args, true));
        return new Object[]{tank == null ? 0 : tank.getFluidAmount()};
    }

    public static Object[] space(Agent agent, Arguments args) {
        final var tank = tank(agent, index(agent, args, true));
        return new Object[]{tank == null ? 0 : tank.getCapacity() - tank.getFluidAmount()};
    }

    public static Object[] compare(Agent agent, Arguments args) {
        final var from = tank(agent, agent.selectedTank());
        final var to = tank(agent, index(agent, args, false));
        final var first = from == null ? FluidStack.EMPTY : from.getFluid();
        final var second = to == null ? FluidStack.EMPTY : to.getFluid();
        return new Object[]{first.isEmpty() && second.isEmpty()
            || !first.isEmpty() && !second.isEmpty() && FluidStack.isSameFluidSameComponents(first, second)};
    }

    public static Object[] transfer(Agent agent, Arguments args) {
        final int target = index(agent, args, false);
        final int count = Math.max(0, args.optInteger(1, 1000));
        if (target == agent.selectedTank() || count == 0) return new Object[]{true};
        final var from = tank(agent, agent.selectedTank());
        final var to = tank(agent, target);
        if (from == null || to == null) return new Object[]{null, "invalid index"};
        final int moved = to.fill(from.drain(count, FluidAction.SIMULATE), FluidAction.EXECUTE);
        if (moved > 0) {
            from.drain(moved, FluidAction.EXECUTE);
            return new Object[]{true};
        }
        if (count >= from.getFluidAmount() && to.getCapacity() >= from.getFluidAmount()
            && from.getCapacity() >= to.getFluidAmount()) {
            final var other = to.drain(to.getFluidAmount(), FluidAction.EXECUTE);
            to.fill(from.drain(from.getFluidAmount(), FluidAction.EXECUTE), FluidAction.EXECUTE);
            from.fill(other, FluidAction.EXECUTE);
            return new Object[]{true};
        }
        return new Object[]{null, "incompatible or no fluid"};
    }
}
