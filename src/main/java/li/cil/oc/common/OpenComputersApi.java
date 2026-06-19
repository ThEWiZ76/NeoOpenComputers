package li.cil.oc.common;

import li.cil.oc.api.API;

public final class OpenComputersApi {
    public static void initialize() {
        if (API.driver == null) {
            API.driver = new DriverRegistry();
        }
        if (API.machine == null) {
            API.machine = new MachineRegistry();
        }
        if (API.manual == null) {
            API.manual = new ManualRegistry();
        }
        if (API.nanomachines == null) {
            API.nanomachines = new NanomachinesRegistry();
        }
        if (API.fileSystem == null) {
            API.fileSystem = new FileSystemRegistry();
        }
        if (API.network == null) {
            API.network = new NetworkRegistry();
        }
    }

    private OpenComputersApi() {
    }
}
