package li.cil.oc.api.internal;

import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.EnvironmentHost;

public interface Server extends EnvironmentHost, MachineHost, Tiered, RackMountable {
    Rack rack();

    int slot();
}
