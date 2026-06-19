package li.cil.oc.common;

import li.cil.oc.api.API;

public final class OpenComputersApi {
    public static void initialize() {
        if (API.driver == null) {
            API.driver = new DriverRegistry();
        }
    }

    private OpenComputersApi() {
    }
}
