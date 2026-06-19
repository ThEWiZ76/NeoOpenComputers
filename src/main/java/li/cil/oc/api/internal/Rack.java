package li.cil.oc.api.internal;

import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;

public interface Rack extends SidedEnvironment, EnvironmentHost, Rotatable, Container {
    int indexOfMountable(RackMountable mountable);

    RackMountable getMountable(int slot);

    CompoundTag getMountableData(int slot);

    void markChanged(int slot);
}
