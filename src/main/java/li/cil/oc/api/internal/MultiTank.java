package li.cil.oc.api.internal;

import net.neoforged.neoforge.fluids.IFluidTank;

public interface MultiTank {
    int tankCount();

    IFluidTank getFluidTank(int index);
}
