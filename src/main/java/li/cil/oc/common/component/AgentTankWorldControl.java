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

    public static Object[] drain(Agent agent, Arguments args) {
        final var side = side(agent, args);
        final int count = Math.max(0, args.optInteger(1, 1000));
        final var tank = agent.tank().getFluidTank(agent.selectedTank());
        if (tank == null) return new Object[]{null, "no tank selected"};
        final int amount = Math.min(count, tank.getCapacity() - tank.getFluidAmount());
        if (count > 0 && amount <= 0) return new Object[]{null, "tank is full"};
        final var pos = BlockPos.containing(agent.xPosition(), agent.yPosition(), agent.zPosition()).relative(side);
        if (agent.world() == null || !agent.world().isLoaded(pos)) return new Object[]{null, "incompatible or no fluid"};
        final boolean empty = tank.getFluid().isEmpty();
        if (count == 0) return new Object[]{!empty, 0};
        var handler = agent.world().getCapability(Capabilities.FluidHandler.BLOCK, pos, side.getOpposite());
        if (handler == null && agent.world().getFluidState(pos).isSource()
            && agent.world().getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.BucketPickup pickup) {
            handler = new net.neoforged.neoforge.fluids.capability.wrappers.BucketPickupHandlerWrapper(agent.player(), pickup, agent.world(), pos);
        }
        if (handler == null) return empty ? new Object[]{false, 0} : new Object[]{null, "incompatible or no fluid"};
        final var simulated = empty ? handler.drain(amount, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE)
            : handler.drain(tank.getFluid().copyWithAmount(amount), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
        final int accepted = tank.fill(simulated, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return empty ? new Object[]{false, 0} : new Object[]{null, "incompatible or no fluid"};
        final var extracted = handler.drain(simulated.copyWithAmount(accepted), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        final int transferred = tank.fill(extracted, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        return new Object[]{transferred > 0, transferred};
    }

    public static Object[] fill(Agent agent, Arguments args) {
        final var side = side(agent, args);
        final int count = Math.max(0, args.optInteger(1, 1000));
        final var tank = agent.tank().getFluidTank(agent.selectedTank());
        if (tank == null) return new Object[]{null, "no tank selected"};
        if (tank.getFluid().isEmpty()) return new Object[]{null, "tank is empty"};
        final int amount = Math.min(count, tank.getFluidAmount());
        final var pos = BlockPos.containing(agent.xPosition(), agent.yPosition(), agent.zPosition()).relative(side);
        if (agent.world() == null || !agent.world().isLoaded(pos)) return new Object[]{null, "no space"};
        if (count == 0) return new Object[]{true, 0};
        final var handler = agent.world().getCapability(Capabilities.FluidHandler.BLOCK, pos, side.getOpposite());
        if (handler != null) {
            final int transferred = handler.fill(tank.getFluid().copyWithAmount(amount), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            if (transferred <= 0) return new Object[]{null, "incompatible or no fluid"};
            tank.drain(transferred, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            return new Object[]{true, transferred};
        }
        if (amount < 1000 || agent.world().getFluidState(pos).isSource()) return new Object[]{null, "incompatible or no fluid"};
        // A temporary bucket prevents a failed world placement from consuming the internal tank.
        final var bucket = new net.neoforged.neoforge.fluids.capability.templates.FluidTank(1000);
        bucket.setFluid(tank.getFluid().copyWithAmount(1000));
        if (!net.neoforged.neoforge.fluids.FluidUtil.tryPlaceFluid(agent.player(), agent.world(),
            net.minecraft.world.InteractionHand.MAIN_HAND, pos, bucket, bucket.getFluid().copy())) {
            return new Object[]{null, "incompatible or no fluid"};
        }
        tank.drain(1000, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        return new Object[]{true, 1000};
    }
}
