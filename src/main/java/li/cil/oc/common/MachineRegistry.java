package li.cil.oc.common;

import li.cil.oc.api.detail.MachineAPI;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class MachineRegistry implements MachineAPI {
    private final List<Class<? extends Architecture>> architectures = new ArrayList<>();

    @Override
    public void add(final Class<? extends Architecture> architecture) {
        architectures.add(architecture);
    }

    @Override
    public Collection<Class<? extends Architecture>> architectures() {
        return List.copyOf(architectures);
    }

    @Override
    public String getArchitectureName(final Class<? extends Architecture> architecture) {
        final Architecture.Name name = architecture.getAnnotation(Architecture.Name.class);
        return name == null ? architecture.getSimpleName() : name.value();
    }

    @Override
    public Machine create(final MachineHost host) {
        return null;
    }
}
