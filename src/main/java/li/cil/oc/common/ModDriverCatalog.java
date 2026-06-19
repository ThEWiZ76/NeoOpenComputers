package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.common.driver.ComputerCaseBlockDriver;

public final class ModDriverCatalog {
    public static void registerDefaults() {
        if (API.driver instanceof DriverRegistry registry) {
            registerBlocks(registry, new ComputerCaseBlockDriver());
            register(registry, ModItems.CPU_TIER1.get(), ModItems.MEMORY_TIER1.get(), ModItems.HDD_TIER1.get());
        }
    }

    static void registerBlocks(final DriverRegistry registry, final DriverBlock... drivers) {
        for (final DriverBlock driver : drivers) {
            registry.add(driver);
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
