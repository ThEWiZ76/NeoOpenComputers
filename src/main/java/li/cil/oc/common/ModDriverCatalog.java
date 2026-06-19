package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.driver.DriverItem;

public final class ModDriverCatalog {
    public static void registerDefaults() {
        if (API.driver instanceof DriverRegistry registry) {
            register(registry, ModItems.CPU_TIER1.get(), ModItems.MEMORY_TIER1.get());
        }
    }

    static void register(final DriverRegistry registry, final DriverItem... drivers) {
        for (final DriverItem driver : drivers) {
            registry.add(driver);
        }
    }

    private ModDriverCatalog() {
    }
}
