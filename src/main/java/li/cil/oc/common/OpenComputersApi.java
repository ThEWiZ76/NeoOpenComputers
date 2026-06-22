package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.common.machine.LuaArchitecture;
import li.cil.oc.common.nanomachines.provider.NanomachineHungryProvider;
import li.cil.oc.common.nanomachines.provider.NanomachineParticleProvider;

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
            registry.addProvider(new NanomachineParticleProvider());
            registry.addProvider(new NanomachineHungryProvider());
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

    private OpenComputersApi() {
    }
}
