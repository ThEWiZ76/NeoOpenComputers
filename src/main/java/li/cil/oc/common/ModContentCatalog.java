package li.cil.oc.common;

import li.cil.oc.api.API;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModContentCatalog {
    public static final String COMPAT_COMPUTER_CASE_TIER1 = "case1";
    public static final String COMPAT_CPU_TIER1 = "cpu1";
    public static final String COMPAT_CPU_TIER2 = "cpu2";
    public static final String COMPAT_CPU_TIER3 = "cpu3";
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
    public static final String COMPAT_SCREEN_TIER1 = "screen1";

    public static void registerDefaults() {
        if (API.items instanceof ItemRegistry registry) {
            register(
                registry,
                ModBlocks.COMPUTER_CASE_TIER1.get(),
                ModItems.COMPUTER_CASE_TIER1.get(),
                ModBlocks.DISK_DRIVE.get(),
                ModItems.DISK_DRIVE.get(),
                ModBlocks.SCREEN_TIER1.get(),
                ModItems.SCREEN_TIER1.get(),
                ModBlocks.KEYBOARD.get(),
                ModItems.KEYBOARD.get(),
                ModItems.MANUAL.get(),
                ModItems.CPU_TIER1.get(),
                ModItems.CPU_TIER2.get(),
                ModItems.CPU_TIER3.get(),
                ModItems.MEMORY_TIER1.get(),
                ModItems.MEMORY_TIER2.get(),
                ModItems.MEMORY_TIER3.get(),
                ModItems.HDD_TIER1.get(),
                ModItems.HDD_TIER2.get(),
                ModItems.HDD_TIER3.get(),
                ModItems.EEPROM.get(),
                ModItems.FLOPPY.get(),
                ModItems.GRAPHICS_CARD_TIER1.get(),
                ModItems.GRAPHICS_CARD_TIER2.get(),
                ModItems.GRAPHICS_CARD_TIER3.get(),
                ModItems.NETWORK_CARD.get());
        }
    }

    static void register(
        final ItemRegistry registry,
        final Block computerCaseTier1,
        final Item computerCaseTier1Item,
        final Block diskDrive,
        final Item diskDriveItem,
        final Block screenTier1,
        final Item screenTier1Item,
        final Block keyboard,
        final Item keyboardItem,
        final Item manualItem,
        final Item cpuTier1Item,
        final Item cpuTier2Item,
        final Item cpuTier3Item,
        final Item memoryTier1Item,
        final Item memoryTier2Item,
        final Item memoryTier3Item,
        final Item hddTier1Item,
        final Item hddTier2Item,
        final Item hddTier3Item,
        final Item eepromItem,
        final Item floppyItem,
        final Item graphicsCardTier1Item,
        final Item graphicsCardTier2Item,
        final Item graphicsCardTier3Item,
        final Item networkCardItem) {
        registry.register(ModContentIds.MANUAL, null, manualItem);
        registry.register(ModContentIds.COMPUTER_CASE_TIER1, computerCaseTier1, computerCaseTier1Item);
        registry.register(COMPAT_COMPUTER_CASE_TIER1, computerCaseTier1, computerCaseTier1Item);
        registry.register(ModContentIds.DISK_DRIVE, diskDrive, diskDriveItem);
        registry.register(ModContentIds.SCREEN_TIER1, screenTier1, screenTier1Item);
        registry.register(COMPAT_SCREEN_TIER1, screenTier1, screenTier1Item);
        registry.register(ModContentIds.KEYBOARD, keyboard, keyboardItem);
        registry.register(ModContentIds.CPU_TIER1, null, cpuTier1Item);
        registry.register(ModContentIds.CPU_TIER2, null, cpuTier2Item);
        registry.register(ModContentIds.CPU_TIER3, null, cpuTier3Item);
        registry.register(ModContentIds.MEMORY_TIER1, null, memoryTier1Item);
        registry.register(ModContentIds.MEMORY_TIER2, null, memoryTier2Item);
        registry.register(ModContentIds.MEMORY_TIER3, null, memoryTier3Item);
        registry.register(ModContentIds.HDD_TIER1, null, hddTier1Item);
        registry.register(ModContentIds.HDD_TIER2, null, hddTier2Item);
        registry.register(ModContentIds.HDD_TIER3, null, hddTier3Item);
        registry.register(ModContentIds.EEPROM, null, eepromItem);
        registry.register(ModContentIds.FLOPPY, null, floppyItem);
        registry.register(ModContentIds.GRAPHICS_CARD_TIER1, null, graphicsCardTier1Item);
        registry.register(ModContentIds.GRAPHICS_CARD_TIER2, null, graphicsCardTier2Item);
        registry.register(ModContentIds.GRAPHICS_CARD_TIER3, null, graphicsCardTier3Item);
        registry.register(ModContentIds.NETWORK_CARD, null, networkCardItem);
        registry.register(COMPAT_CPU_TIER1, null, cpuTier1Item);
        registry.register(COMPAT_CPU_TIER2, null, cpuTier2Item);
        registry.register(COMPAT_CPU_TIER3, null, cpuTier3Item);
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
    }

    private ModContentCatalog() {
    }
}
