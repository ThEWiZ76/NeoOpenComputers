package li.cil.oc.api.internal;

import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.world.Container;

public interface Case extends Environment, EnvironmentHost, MachineHost, Colored, Rotatable, Tiered, Container {
}
