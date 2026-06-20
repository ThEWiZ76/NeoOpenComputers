package li.cil.oc.common.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class FluidDescriptions {
    private FluidDescriptions() {
    }

    public static Object[] describe(final IFluidHandler handler, final int tank) {
        final FluidStack stack = handler.getFluidInTank(tank);
        return new Object[]{
            stack.isEmpty() ? "" : BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString(),
            stack.isEmpty() ? 0 : stack.getAmount(),
            handler.getTankCapacity(tank)
        };
    }

    public static Object[] describeAll(final IFluidHandler handler) {
        final Object[] tanks = new Object[handler.getTanks()];
        for (int tank = 0; tank < tanks.length; tank++) {
            tanks[tank] = describe(handler, tank);
        }
        return tanks;
    }
}
