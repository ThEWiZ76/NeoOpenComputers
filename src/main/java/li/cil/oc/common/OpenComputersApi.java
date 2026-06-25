package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.common.machine.LuaArchitecture;
import li.cil.oc.common.nanomachines.provider.NanomachineDisintegrationProvider;
import li.cil.oc.common.nanomachines.provider.NanomachineHungryProvider;
import li.cil.oc.common.nanomachines.provider.NanomachineMagnetProvider;
import li.cil.oc.common.nanomachines.provider.NanomachineParticleProvider;
import li.cil.oc.common.nanomachines.provider.NanomachinePotionProvider;

public final class OpenComputersApi {
    public static void initialize() {
        if (API.driver == null) {
            API.driver = new DriverRegistry();
        }
        if (API.machine == null) {
            API.machine = new MachineRegistry();
        }
        li.cil.oc.api.Machine.LuaArchitecture = LuaArchitecture.class;
        if (!API.machine.architectures().contains(LuaArchitecture.class)) {
            API.machine.add(LuaArchitecture.class);
        }
        if (API.manual == null) {
            API.manual = new ManualRegistry();
        }
        if (API.nanomachines == null) {
            final NanomachinesRegistry registry = new NanomachinesRegistry();
            registry.addProvider(new NanomachineDisintegrationProvider());
            registry.addProvider(new NanomachineHungryProvider());
            registry.addProvider(new NanomachineParticleProvider());
            registry.addProvider(new NanomachinePotionProvider());
            registry.addProvider(new NanomachineMagnetProvider());
            API.nanomachines = registry;
        }
        if (API.fileSystem == null) {
            API.fileSystem = new FileSystemRegistry();
        }
        if (API.network == null) {
            API.network = new NetworkRegistry();
        }
        if (API.items == null) {
            API.items = new ItemRegistry();
        }
    }

    public static void lockDriverRegistry() {
        if (API.driver instanceof DriverRegistry registry) {
            registry.lockRegistrations();
        }
    }

    static Object[] convert(final Object[] values) {
        if (API.driver instanceof DriverRegistry registry) {
            return registry.convert(values);
        }
        return values;
    }

    private OpenComputersApi() {
    }
}
