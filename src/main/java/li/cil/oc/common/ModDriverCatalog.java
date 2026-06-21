package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.common.driver.ComputerCaseBlockDriver;
import li.cil.oc.common.driver.DiskDriveBlockDriver;
import li.cil.oc.common.driver.DiskDriveContainerDriver;
import li.cil.oc.common.driver.GeolyzerItemDriver;
import li.cil.oc.common.driver.InventoryBlockDriver;
import li.cil.oc.common.driver.KeyboardItemDriver;
import li.cil.oc.common.driver.MotionSensorItemDriver;
import li.cil.oc.common.driver.ScreenBlockDriver;
import li.cil.oc.common.driver.ScreenItemDriver;
import li.cil.oc.common.driver.TransposerItemDriver;

public final class ModDriverCatalog {
    public static void registerDefaults() {
        if (API.driver instanceof DriverRegistry registry) {
            registerBlocks(registry, new ComputerCaseBlockDriver(), new DiskDriveBlockDriver(), new InventoryBlockDriver(), new ScreenBlockDriver());
            register(
                registry,
                ModItems.CPU_TIER1.get(), ModItems.CPU_TIER2.get(), ModItems.CPU_TIER3.get(),
                ModItems.CARD_CONTAINER_TIER1.get(), ModItems.CARD_CONTAINER_TIER2.get(), ModItems.CARD_CONTAINER_TIER3.get(),
                ModItems.TABLET.get(),
                ModItems.BATTERY_UPGRADE_TIER1.get(), ModItems.BATTERY_UPGRADE_TIER2.get(), ModItems.BATTERY_UPGRADE_TIER3.get(),
                ModItems.DATA_CARD_TIER1.get(), ModItems.DATA_CARD_TIER2.get(), ModItems.DATA_CARD_TIER3.get(),
                ModItems.DATABASE_UPGRADE_TIER1.get(), ModItems.DATABASE_UPGRADE_TIER2.get(), ModItems.DATABASE_UPGRADE_TIER3.get(),
                new DiskDriveContainerDriver(),
                ModItems.MEMORY_TIER1.get(), ModItems.MEMORY_TIER2.get(), ModItems.MEMORY_TIER3.get(),
                ModItems.HDD_TIER1.get(), ModItems.HDD_TIER2.get(), ModItems.HDD_TIER3.get(),
                ModItems.HOVER_UPGRADE_TIER1.get(), ModItems.HOVER_UPGRADE_TIER2.get(),
                ModItems.INVENTORY_CONTROLLER_UPGRADE.get(), ModItems.INVENTORY_UPGRADE.get(), ModItems.CRAFTING_UPGRADE.get(), ModItems.EXPERIENCE_UPGRADE.get(),
                ModItems.INTERNET_CARD.get(), ModItems.LINKED_CARD.get(), ModItems.NAVIGATION_UPGRADE.get(),
                ModItems.EEPROM.get(), ModItems.FLOPPY.get(),
                new GeolyzerItemDriver(),
                new ScreenItemDriver(ModItems.SCREEN_TIER1.get(), 0),
                new ScreenItemDriver(ModItems.SCREEN_TIER2.get(), 1),
                new ScreenItemDriver(ModItems.SCREEN_TIER3.get(), 2),
                new KeyboardItemDriver(),
                new MotionSensorItemDriver(),
                new TransposerItemDriver(),
                ModItems.GRAPHICS_CARD_TIER1.get(), ModItems.GRAPHICS_CARD_TIER2.get(), ModItems.GRAPHICS_CARD_TIER3.get(),
                ModItems.NETWORK_CARD.get(), ModItems.WIRELESS_NETWORK_CARD_TIER1.get(), ModItems.WIRELESS_NETWORK_CARD_TIER2.get(), ModItems.REDSTONE_CARD.get(),
                ModItems.PISTON_UPGRADE.get(), ModItems.STICKY_PISTON_UPGRADE.get(), ModItems.SIGN_UPGRADE.get(), ModItems.TRADING_UPGRADE.get(), ModItems.TRACTOR_BEAM_UPGRADE.get(),
                ModItems.GENERATOR_UPGRADE.get(), ModItems.SOLAR_GENERATOR_UPGRADE.get(), ModItems.TANK_UPGRADE.get(), ModItems.TANK_CONTROLLER_UPGRADE.get(),
                ModItems.UPGRADE_CONTAINER_TIER1.get(), ModItems.UPGRADE_CONTAINER_TIER2.get(), ModItems.UPGRADE_CONTAINER_TIER3.get());
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
