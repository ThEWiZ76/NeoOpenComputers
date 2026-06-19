package li.cil.oc.api.detail;

import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;

import java.util.Collection;

public interface MachineAPI {
    void add(Class<? extends Architecture> architecture);

    Collection<Class<? extends Architecture>> architectures();

    String getArchitectureName(Class<? extends Architecture> architecture);

    Machine create(MachineHost host);
}
