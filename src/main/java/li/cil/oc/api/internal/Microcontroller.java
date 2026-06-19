package li.cil.oc.api.internal;

import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;

public interface Microcontroller extends Environment, EnvironmentHost, MachineHost, Rotatable, Tiered {
}
