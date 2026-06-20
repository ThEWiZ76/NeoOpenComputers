package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.common.driver.ComputerCaseBlockDriver;
import li.cil.oc.common.driver.DiskDriveBlockDriver;
import li.cil.oc.common.driver.InventoryBlockDriver;
import li.cil.oc.common.driver.ScreenBlockDriver;

public final class ModDriverCatalog {
    public static void registerDefaults() {
        if (API.driver instanceof DriverRegistry registry) {
            registerBlocks(registry, new ComputerCaseBlockDriver(), new DiskDriveBlockDriver(), new InventoryBlockDriver(), new ScreenBlockDriver());
            register(
                registry,
                ModItems.CPU_TIER1.get(), ModItems.CPU_TIER2.get(), ModItems.CPU_TIER3.get(),
                ModItems.BATTERY_UPGRADE_TIER1.get(), ModItems.BATTERY_UPGRADE_TIER2.get(), ModItems.BATTERY_UPGRADE_TIER3.get(),
                ModItems.DATA_CARD_TIER1.get(), ModItems.DATA_CARD_TIER2.get(), ModItems.DATA_CARD_TIER3.get(),
                ModItems.DATABASE_UPGRADE_TIER1.get(), ModItems.DATABASE_UPGRADE_TIER2.get(), ModItems.DATABASE_UPGRADE_TIER3.get(),
                ModItems.MEMORY_TIER1.get(), ModItems.MEMORY_TIER2.get(), ModItems.MEMORY_TIER3.get(),
                ModItems.HDD_TIER1.get(), ModItems.HDD_TIER2.get(), ModItems.HDD_TIER3.get(),
                ModItems.INVENTORY_CONTROLLER_UPGRADE.get(), ModItems.INVENTORY_UPGRADE.get(), ModItems.INTERNET_CARD.get(), ModItems.LINKED_CARD.get(), ModItems.NAVIGATION_UPGRADE.get(),
                ModItems.EEPROM.get(), ModItems.FLOPPY.get(),
                ModItems.GRAPHICS_CARD_TIER1.get(), ModItems.GRAPHICS_CARD_TIER2.get(), ModItems.GRAPHICS_CARD_TIER3.get(),
                ModItems.NETWORK_CARD.get(), ModItems.WIRELESS_NETWORK_CARD_TIER1.get(), ModItems.WIRELESS_NETWORK_CARD_TIER2.get(), ModItems.REDSTONE_CARD.get());
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
