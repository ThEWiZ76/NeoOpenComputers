package li.cil.oc.api;

import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.MachineHost;

import java.util.Collection;
import java.util.Collections;

public final class Machine {
    public static Class<? extends Architecture> LuaArchitecture = null;

    public static void add(final Class<? extends Architecture> architecture) {
        if (API.machine != null) {
            API.machine.add(architecture);
        }
    }

    public static Collection<Class<? extends Architecture>> architectures() {
        if (API.machine != null) {
            return API.machine.architectures();
        }
        return Collections.emptyList();
    }

    public static String getArchitectureName(final Class<? extends Architecture> architecture) {
        if (API.machine != null) {
            return API.machine.getArchitectureName(architecture);
        }
        return null;
    }

    public static li.cil.oc.api.machine.Machine create(final MachineHost host) {
        if (API.machine != null) {
            return API.machine.create(host);
        }
        return null;
    }

    private Machine() {
    }
}
