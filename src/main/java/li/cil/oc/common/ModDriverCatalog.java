package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.common.blockentity.ScreenItemEnvironment;
import li.cil.oc.common.component.AngelUpgradeEnvironment;
import li.cil.oc.common.component.BarcodeReaderUpgradeEnvironment;
import li.cil.oc.common.component.BatteryUpgradeEnvironment;
import li.cil.oc.common.component.ChunkloaderUpgradeEnvironment;
import li.cil.oc.common.component.CraftingUpgradeEnvironment;
import li.cil.oc.common.component.DatabaseEnvironment;
import li.cil.oc.common.component.DataCardEnvironment;
import li.cil.oc.common.component.DebugCardEnvironment;
import li.cil.oc.common.component.EepromEnvironment;
import li.cil.oc.common.component.ExperienceUpgradeEnvironment;
import li.cil.oc.common.component.GeneratorUpgradeEnvironment;
import li.cil.oc.common.component.GeolyzerEnvironment;
import li.cil.oc.common.component.GraphicsCardEnvironment;
import li.cil.oc.common.component.InternetCardEnvironment;
import li.cil.oc.common.component.InventoryControllerEnvironment;
import li.cil.oc.common.component.KeyboardItemEnvironment;
import li.cil.oc.common.component.LinkedCardEnvironment;
import li.cil.oc.common.component.MfuEnvironment;
import li.cil.oc.common.component.MotionSensorEnvironment;
import li.cil.oc.common.component.NavigationUpgradeEnvironment;
import li.cil.oc.common.component.NetworkCardEnvironment;
import li.cil.oc.common.component.PistonUpgradeEnvironment;
import li.cil.oc.common.component.RedstoneCardEnvironment;
import li.cil.oc.common.component.SignUpgradeEnvironment;
import li.cil.oc.common.component.SolarGeneratorUpgradeEnvironment;
import li.cil.oc.common.component.StickyPistonUpgradeEnvironment;
import li.cil.oc.common.component.TankControllerEnvironment;
import li.cil.oc.common.component.TankUpgradeEnvironment;
import li.cil.oc.common.component.TractorBeamUpgradeEnvironment;
import li.cil.oc.common.component.TradingUpgradeEnvironment;
import li.cil.oc.common.component.TransposerEnvironment;
import li.cil.oc.common.component.WirelessNetworkCardEnvironment;
import li.cil.oc.common.driver.ComputerCaseBlockDriver;
import li.cil.oc.common.driver.DiskDriveBlockDriver;
import li.cil.oc.common.driver.DiskDriveContainerDriver;
import li.cil.oc.common.driver.GeolyzerItemDriver;
import li.cil.oc.common.driver.InventoryBlockDriver;
import li.cil.oc.common.driver.KeyboardItemDriver;
import li.cil.oc.common.driver.MinecraftConverters;
import li.cil.oc.common.driver.MotionSensorItemDriver;
import li.cil.oc.common.driver.ScreenBlockDriver;
import li.cil.oc.common.driver.ScreenItemDriver;
import li.cil.oc.common.driver.TransposerItemDriver;
import net.minecraft.world.item.ItemStack;

public final class ModDriverCatalog {
    public static void registerDefaults() {
        if (API.driver instanceof DriverRegistry registry) {
            registerBlocks(registry, new ComputerCaseBlockDriver(), new DiskDriveBlockDriver(), new InventoryBlockDriver(), new ScreenBlockDriver());
            registry.add(MinecraftConverters.ITEM_STACK);
            registry.add(MinecraftConverters.FLUID_CONTAINER_ITEM);
            registry.add(MinecraftConverters.BLOCK);
            registry.add(MinecraftConverters.BLOCK_STATE);
            registry.add(MinecraftConverters.NBT);
            registry.add(MinecraftConverters.FLUID_STACK);
            registry.add(MinecraftConverters.FLUID_TANK);
            registry.add(MinecraftConverters.LEVEL);
            register(
                registry,
                ModItems.CPU_TIER1.get(), ModItems.CPU_TIER2.get(), ModItems.CPU_TIER3.get(), ModItems.APU_TIER1.get(), ModItems.APU_TIER2.get(),
                ModItems.CARD_CONTAINER_TIER1.get(), ModItems.CARD_CONTAINER_TIER2.get(), ModItems.CARD_CONTAINER_TIER3.get(),
                ModItems.TABLET.get(),
                ModItems.SERVER_TIER1.get(), ModItems.SERVER_TIER2.get(), ModItems.SERVER_TIER3.get(), ModItems.TERMINAL_SERVER.get(),
                ModItems.BATTERY_UPGRADE_TIER1.get(), ModItems.BATTERY_UPGRADE_TIER2.get(), ModItems.BATTERY_UPGRADE_TIER3.get(), ModItems.BARCODE_READER_UPGRADE.get(),
                ModItems.DATA_CARD_TIER1.get(), ModItems.DATA_CARD_TIER2.get(), ModItems.DATA_CARD_TIER3.get(), ModItems.DEBUG_CARD.get(),
                ModItems.DATABASE_UPGRADE_TIER1.get(), ModItems.DATABASE_UPGRADE_TIER2.get(), ModItems.DATABASE_UPGRADE_TIER3.get(),
                new DiskDriveContainerDriver(),
                ModItems.MEMORY_TIER1.get(), ModItems.MEMORY_TIER2.get(), ModItems.MEMORY_TIER3.get(),
                ModItems.COMPONENT_BUS_TIER1.get(), ModItems.COMPONENT_BUS_TIER2.get(), ModItems.COMPONENT_BUS_TIER3.get(),
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
                ModItems.PISTON_UPGRADE.get(), ModItems.STICKY_PISTON_UPGRADE.get(), ModItems.SIGN_UPGRADE.get(), ModItems.TRADING_UPGRADE.get(), ModItems.TRACTOR_BEAM_UPGRADE.get(), ModItems.LEASH_UPGRADE.get(), ModItems.ANGEL_UPGRADE.get(), ModItems.CHUNKLOADER_UPGRADE.get(), ModItems.MFU.get(),
                ModItems.GENERATOR_UPGRADE.get(), ModItems.SOLAR_GENERATOR_UPGRADE.get(), ModItems.TANK_UPGRADE.get(), ModItems.TANK_CONTROLLER_UPGRADE.get(),
                ModItems.UPGRADE_CONTAINER_TIER1.get(), ModItems.UPGRADE_CONTAINER_TIER2.get(), ModItems.UPGRADE_CONTAINER_TIER3.get());
            registerEnvironmentProviders(
                registry,
                providerFor(ModItems.APU_TIER1.get(), GraphicsCardEnvironment.class),
                providerFor(ModItems.APU_TIER2.get(), GraphicsCardEnvironment.class),
                providerFor(ModItems.BATTERY_UPGRADE_TIER1.get(), BatteryUpgradeEnvironment.class),
                providerFor(ModItems.BATTERY_UPGRADE_TIER2.get(), BatteryUpgradeEnvironment.class),
                providerFor(ModItems.BATTERY_UPGRADE_TIER3.get(), BatteryUpgradeEnvironment.class),
                providerFor(ModItems.BARCODE_READER_UPGRADE.get(), BarcodeReaderUpgradeEnvironment.class),
                providerFor(ModItems.DATA_CARD_TIER1.get(), DataCardEnvironment.class),
                providerFor(ModItems.DATA_CARD_TIER2.get(), DataCardEnvironment.class),
                providerFor(ModItems.DATA_CARD_TIER3.get(), DataCardEnvironment.class),
                providerFor(ModItems.DEBUG_CARD.get(), DebugCardEnvironment.class),
                providerFor(ModItems.DATABASE_UPGRADE_TIER1.get(), DatabaseEnvironment.class),
                providerFor(ModItems.DATABASE_UPGRADE_TIER2.get(), DatabaseEnvironment.class),
                providerFor(ModItems.DATABASE_UPGRADE_TIER3.get(), DatabaseEnvironment.class),
                providerFor(ModItems.EEPROM.get(), EepromEnvironment.class),
                providerFor(ModItems.EXPERIENCE_UPGRADE.get(), ExperienceUpgradeEnvironment.class),
                providerFor(new GeolyzerItemDriver(), GeolyzerEnvironment.class),
                providerFor(ModItems.GRAPHICS_CARD_TIER1.get(), GraphicsCardEnvironment.class),
                providerFor(ModItems.GRAPHICS_CARD_TIER2.get(), GraphicsCardEnvironment.class),
                providerFor(ModItems.GRAPHICS_CARD_TIER3.get(), GraphicsCardEnvironment.class),
                providerFor(ModItems.INVENTORY_CONTROLLER_UPGRADE.get(), InventoryControllerEnvironment.class),
                providerFor(ModItems.CRAFTING_UPGRADE.get(), CraftingUpgradeEnvironment.class),
                providerFor(ModItems.INTERNET_CARD.get(), InternetCardEnvironment.class),
                providerFor(ModItems.LINKED_CARD.get(), LinkedCardEnvironment.class),
                providerFor(ModItems.NAVIGATION_UPGRADE.get(), NavigationUpgradeEnvironment.class),
                providerFor(ModItems.NETWORK_CARD.get(), NetworkCardEnvironment.class),
                providerFor(ModItems.PISTON_UPGRADE.get(), PistonUpgradeEnvironment.class),
                providerFor(ModItems.STICKY_PISTON_UPGRADE.get(), StickyPistonUpgradeEnvironment.class),
                providerFor(ModItems.SIGN_UPGRADE.get(), SignUpgradeEnvironment.class),
                providerFor(ModItems.TRADING_UPGRADE.get(), TradingUpgradeEnvironment.class),
                providerFor(ModItems.TRACTOR_BEAM_UPGRADE.get(), TractorBeamUpgradeEnvironment.class),
                providerFor(ModItems.ANGEL_UPGRADE.get(), AngelUpgradeEnvironment.class),
                providerFor(ModItems.CHUNKLOADER_UPGRADE.get(), ChunkloaderUpgradeEnvironment.class),
                providerFor(ModItems.MFU.get(), MfuEnvironment.class),
                providerFor(ModItems.GENERATOR_UPGRADE.get(), GeneratorUpgradeEnvironment.class),
                providerFor(ModItems.SOLAR_GENERATOR_UPGRADE.get(), SolarGeneratorUpgradeEnvironment.class),
                providerFor(ModItems.TANK_UPGRADE.get(), TankUpgradeEnvironment.class),
                providerFor(ModItems.TANK_CONTROLLER_UPGRADE.get(), TankControllerEnvironment.class),
                providerFor(ModItems.WIRELESS_NETWORK_CARD_TIER1.get(), WirelessNetworkCardEnvironment.class),
                providerFor(ModItems.WIRELESS_NETWORK_CARD_TIER2.get(), WirelessNetworkCardEnvironment.class),
                providerFor(ModItems.REDSTONE_CARD.get(), RedstoneCardEnvironment.class),
                providerFor(new ScreenItemDriver(ModItems.SCREEN_TIER1.get(), 0), ScreenItemEnvironment.class),
                providerFor(new ScreenItemDriver(ModItems.SCREEN_TIER2.get(), 1), ScreenItemEnvironment.class),
                providerFor(new ScreenItemDriver(ModItems.SCREEN_TIER3.get(), 2), ScreenItemEnvironment.class),
                providerFor(new KeyboardItemDriver(), KeyboardItemEnvironment.class),
                providerFor(new MotionSensorItemDriver(), MotionSensorEnvironment.class),
                providerFor(new TransposerItemDriver(), TransposerEnvironment.class));
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

    static void registerEnvironmentProviders(final DriverRegistry registry, final EnvironmentProvider... providers) {
        for (final EnvironmentProvider provider : providers) {
            registry.add(provider);
        }
    }

    private static EnvironmentProvider providerFor(final DriverItem driver, final Class<?> environment) {
        return (final ItemStack stack) -> driver.worksWith(stack) ? environment : null;
    }

    private ModDriverCatalog() {
    }
}
