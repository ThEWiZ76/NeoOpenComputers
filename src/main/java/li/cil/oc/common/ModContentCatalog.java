package li.cil.oc.common;

import li.cil.oc.api.API;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModContentCatalog {
    public static final String COMPAT_COMPUTER_CASE_TIER1 = "case1";
    public static final String COMPAT_COMPUTER_CASE_TIER2 = "case2";
    public static final String COMPAT_COMPUTER_CASE_TIER3 = "case3";
    public static final String COMPAT_CPU_TIER1 = "cpu1";
    public static final String COMPAT_CPU_TIER2 = "cpu2";
    public static final String COMPAT_CPU_TIER3 = "cpu3";
    public static final String COMPAT_DATABASE_UPGRADE_TIER1 = "databaseUpgrade1";
    public static final String COMPAT_DATABASE_UPGRADE_TIER2 = "databaseUpgrade2";
    public static final String COMPAT_DATABASE_UPGRADE_TIER3 = "databaseUpgrade3";
    public static final String COMPAT_GRAPHICS_CARD_TIER1 = "graphicscard1";
    public static final String COMPAT_GRAPHICS_CARD_TIER2 = "graphicscard2";
    public static final String COMPAT_GRAPHICS_CARD_TIER3 = "graphicscard3";
    public static final String COMPAT_HDD_TIER1 = "hdd1";
    public static final String COMPAT_HDD_TIER2 = "hdd2";
    public static final String COMPAT_HDD_TIER3 = "hdd3";
    public static final String COMPAT_MEMORY_TIER1 = "ram1";
    public static final String COMPAT_MEMORY_TIER2 = "ram2";
    public static final String COMPAT_MEMORY_TIER3 = "ram3";
    public static final String COMPAT_NETWORK_CARD = "lancard";
    public static final String COMPAT_REDSTONE_CARD = "redstone";
    public static final String COMPAT_WIRELESS_NETWORK_CARD_TIER1 = "wlancard1";
    public static final String COMPAT_WIRELESS_NETWORK_CARD_TIER2 = "wlancard2";
    public static final String COMPAT_WIRELESS_NETWORK_CARD = "wlancard";
    public static final String COMPAT_SCREEN_TIER1 = "screen1";
    public static final String COMPAT_SCREEN_TIER2 = "screen2";
    public static final String COMPAT_SCREEN_TIER3 = "screen3";

    public static void registerDefaults() {
        if (API.items instanceof ItemRegistry registry) {
            register(
                registry,
                ModBlocks.ADAPTER.get(),
                ModItems.ADAPTER.get(),
                ModBlocks.CABLE.get(),
                ModItems.CABLE.get(),
                ModBlocks.COMPUTER_CASE_TIER1.get(),
                ModItems.COMPUTER_CASE_TIER1.get(),
                ModBlocks.COMPUTER_CASE_TIER2.get(),
                ModItems.COMPUTER_CASE_TIER2.get(),
                ModBlocks.COMPUTER_CASE_TIER3.get(),
                ModItems.COMPUTER_CASE_TIER3.get(),
                ModBlocks.DISK_DRIVE.get(),
                ModItems.DISK_DRIVE.get(),
                ModBlocks.SCREEN_TIER1.get(),
                ModItems.SCREEN_TIER1.get(),
                ModBlocks.SCREEN_TIER2.get(),
                ModItems.SCREEN_TIER2.get(),
                ModBlocks.SCREEN_TIER3.get(),
                ModItems.SCREEN_TIER3.get(),
                ModBlocks.KEYBOARD.get(),
                ModItems.KEYBOARD.get(),
                ModItems.MANUAL.get(),
                ModItems.CPU_TIER1.get(),
                ModItems.CPU_TIER2.get(),
                ModItems.CPU_TIER3.get(),
                ModItems.DATABASE_UPGRADE_TIER1.get(),
                ModItems.DATABASE_UPGRADE_TIER2.get(),
                ModItems.DATABASE_UPGRADE_TIER3.get(),
                ModItems.MEMORY_TIER1.get(),
                ModItems.MEMORY_TIER2.get(),
                ModItems.MEMORY_TIER3.get(),
                ModItems.HDD_TIER1.get(),
                ModItems.HDD_TIER2.get(),
                ModItems.HDD_TIER3.get(),
                ModItems.INVENTORY_CONTROLLER_UPGRADE.get(),
                ModItems.EEPROM.get(),
                ModItems.FLOPPY.get(),
                ModItems.GRAPHICS_CARD_TIER1.get(),
                ModItems.GRAPHICS_CARD_TIER2.get(),
                ModItems.GRAPHICS_CARD_TIER3.get(),
                ModItems.NETWORK_CARD.get(),
                ModItems.WIRELESS_NETWORK_CARD_TIER1.get(),
                ModItems.WIRELESS_NETWORK_CARD_TIER2.get(),
                ModItems.REDSTONE_CARD.get());
        }
    }

    static void register(
        final ItemRegistry registry,
        final Block adapter,
        final Item adapterItem,
        final Block cable,
        final Item cableItem,
        final Block computerCaseTier1,
        final Item computerCaseTier1Item,
        final Block computerCaseTier2,
        final Item computerCaseTier2Item,
        final Block computerCaseTier3,
        final Item computerCaseTier3Item,
        final Block diskDrive,
        final Item diskDriveItem,
        final Block screenTier1,
        final Item screenTier1Item,
        final Block screenTier2,
        final Item screenTier2Item,
        final Block screenTier3,
        final Item screenTier3Item,
        final Block keyboard,
        final Item keyboardItem,
        final Item manualItem,
        final Item cpuTier1Item,
        final Item cpuTier2Item,
        final Item cpuTier3Item,
        final Item databaseUpgradeTier1Item,
        final Item databaseUpgradeTier2Item,
        final Item databaseUpgradeTier3Item,
        final Item memoryTier1Item,
        final Item memoryTier2Item,
        final Item memoryTier3Item,
        final Item hddTier1Item,
        final Item hddTier2Item,
        final Item hddTier3Item,
        final Item inventoryControllerUpgradeItem,
        final Item eepromItem,
        final Item floppyItem,
        final Item graphicsCardTier1Item,
        final Item graphicsCardTier2Item,
        final Item graphicsCardTier3Item,
        final Item networkCardItem,
        final Item wirelessNetworkCardTier1Item,
        final Item wirelessNetworkCardTier2Item,
        final Item redstoneCardItem) {
        registry.register(ModContentIds.MANUAL, null, manualItem);
        registry.register(ModContentIds.ADAPTER, adapter, adapterItem);
        registry.register(ModContentIds.CABLE, cable, cableItem);
        registry.register(ModContentIds.COMPUTER_CASE_TIER1, computerCaseTier1, computerCaseTier1Item);
        registry.register(ModContentIds.COMPUTER_CASE_TIER2, computerCaseTier2, computerCaseTier2Item);
        registry.register(ModContentIds.COMPUTER_CASE_TIER3, computerCaseTier3, computerCaseTier3Item);
        registry.register(COMPAT_COMPUTER_CASE_TIER1, computerCaseTier1, computerCaseTier1Item);
        registry.register(COMPAT_COMPUTER_CASE_TIER2, computerCaseTier2, computerCaseTier2Item);
        registry.register(COMPAT_COMPUTER_CASE_TIER3, computerCaseTier3, computerCaseTier3Item);
        registry.register(ModContentIds.DISK_DRIVE, diskDrive, diskDriveItem);
        registry.register(ModContentIds.SCREEN_TIER1, screenTier1, screenTier1Item);
        registry.register(ModContentIds.SCREEN_TIER2, screenTier2, screenTier2Item);
        registry.register(ModContentIds.SCREEN_TIER3, screenTier3, screenTier3Item);
        registry.register(COMPAT_SCREEN_TIER1, screenTier1, screenTier1Item);
        registry.register(COMPAT_SCREEN_TIER2, screenTier2, screenTier2Item);
        registry.register(COMPAT_SCREEN_TIER3, screenTier3, screenTier3Item);
        registry.register(ModContentIds.KEYBOARD, keyboard, keyboardItem);
        registry.register(ModContentIds.CPU_TIER1, null, cpuTier1Item);
        registry.register(ModContentIds.CPU_TIER2, null, cpuTier2Item);
        registry.register(ModContentIds.CPU_TIER3, null, cpuTier3Item);
        registry.register(ModContentIds.DATABASE_UPGRADE_TIER1, null, databaseUpgradeTier1Item);
        registry.register(ModContentIds.DATABASE_UPGRADE_TIER2, null, databaseUpgradeTier2Item);
        registry.register(ModContentIds.DATABASE_UPGRADE_TIER3, null, databaseUpgradeTier3Item);
        registry.register(ModContentIds.MEMORY_TIER1, null, memoryTier1Item);
        registry.register(ModContentIds.MEMORY_TIER2, null, memoryTier2Item);
        registry.register(ModContentIds.MEMORY_TIER3, null, memoryTier3Item);
        registry.register(ModContentIds.HDD_TIER1, null, hddTier1Item);
        registry.register(ModContentIds.HDD_TIER2, null, hddTier2Item);
        registry.register(ModContentIds.HDD_TIER3, null, hddTier3Item);
        registry.register(ModContentIds.INVENTORY_CONTROLLER_UPGRADE, null, inventoryControllerUpgradeItem);
        registry.register(ModContentIds.EEPROM, null, eepromItem);
        registry.register(ModContentIds.FLOPPY, null, floppyItem);
        registry.register(ModContentIds.GRAPHICS_CARD_TIER1, null, graphicsCardTier1Item);
        registry.register(ModContentIds.GRAPHICS_CARD_TIER2, null, graphicsCardTier2Item);
        registry.register(ModContentIds.GRAPHICS_CARD_TIER3, null, graphicsCardTier3Item);
        registry.register(ModContentIds.NETWORK_CARD, null, networkCardItem);
        registry.register(ModContentIds.WIRELESS_NETWORK_CARD_TIER1, null, wirelessNetworkCardTier1Item);
        registry.register(ModContentIds.WIRELESS_NETWORK_CARD_TIER2, null, wirelessNetworkCardTier2Item);
        registry.register(ModContentIds.REDSTONE_CARD, null, redstoneCardItem);
        registry.register(COMPAT_CPU_TIER1, null, cpuTier1Item);
        registry.register(COMPAT_CPU_TIER2, null, cpuTier2Item);
        registry.register(COMPAT_CPU_TIER3, null, cpuTier3Item);
        registry.register(COMPAT_DATABASE_UPGRADE_TIER1, null, databaseUpgradeTier1Item);
        registry.register(COMPAT_DATABASE_UPGRADE_TIER2, null, databaseUpgradeTier2Item);
        registry.register(COMPAT_DATABASE_UPGRADE_TIER3, null, databaseUpgradeTier3Item);
        registry.register(COMPAT_MEMORY_TIER1, null, memoryTier1Item);
        registry.register(COMPAT_MEMORY_TIER2, null, memoryTier2Item);
        registry.register(COMPAT_MEMORY_TIER3, null, memoryTier3Item);
        registry.register(COMPAT_HDD_TIER1, null, hddTier1Item);
        registry.register(COMPAT_HDD_TIER2, null, hddTier2Item);
        registry.register(COMPAT_HDD_TIER3, null, hddTier3Item);
        registry.register(COMPAT_GRAPHICS_CARD_TIER1, null, graphicsCardTier1Item);
        registry.register(COMPAT_GRAPHICS_CARD_TIER2, null, graphicsCardTier2Item);
        registry.register(COMPAT_GRAPHICS_CARD_TIER3, null, graphicsCardTier3Item);
        registry.register(COMPAT_NETWORK_CARD, null, networkCardItem);
        registry.register(COMPAT_WIRELESS_NETWORK_CARD_TIER1, null, wirelessNetworkCardTier1Item);
        registry.register(COMPAT_WIRELESS_NETWORK_CARD_TIER2, null, wirelessNetworkCardTier2Item);
        registry.register(COMPAT_WIRELESS_NETWORK_CARD, null, wirelessNetworkCardTier2Item);
        registry.register(COMPAT_REDSTONE_CARD, null, redstoneCardItem);
    }

    private ModContentCatalog() {
    }
}
