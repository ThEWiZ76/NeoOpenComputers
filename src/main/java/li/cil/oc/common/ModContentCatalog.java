package li.cil.oc.common;

import li.cil.oc.api.API;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModContentCatalog {
    public static final String COMPAT_COMPUTER_CASE_TIER1 = "case1";
    public static final String COMPAT_CPU_TIER1 = "cpu1";
    public static final String COMPAT_HDD_TIER1 = "hdd1";
    public static final String COMPAT_MEMORY_TIER1 = "ram1";

    public static void registerDefaults() {
        if (API.items instanceof ItemRegistry registry) {
            register(
                registry,
                ModBlocks.COMPUTER_CASE_TIER1.get(),
                ModItems.COMPUTER_CASE_TIER1.get(),
                ModItems.MANUAL.get(),
                ModItems.CPU_TIER1.get(),
                ModItems.MEMORY_TIER1.get(),
                ModItems.HDD_TIER1.get(),
                ModItems.EEPROM.get());
        }
    }

    static void register(
        final ItemRegistry registry,
        final Block computerCaseTier1,
        final Item computerCaseTier1Item,
        final Item manualItem,
        final Item cpuTier1Item,
        final Item memoryTier1Item,
        final Item hddTier1Item,
        final Item eepromItem) {
        registry.register(ModContentIds.MANUAL, null, manualItem);
        registry.register(ModContentIds.COMPUTER_CASE_TIER1, computerCaseTier1, computerCaseTier1Item);
        registry.register(COMPAT_COMPUTER_CASE_TIER1, computerCaseTier1, computerCaseTier1Item);
        registry.register(ModContentIds.CPU_TIER1, null, cpuTier1Item);
        registry.register(ModContentIds.MEMORY_TIER1, null, memoryTier1Item);
        registry.register(ModContentIds.HDD_TIER1, null, hddTier1Item);
        registry.register(ModContentIds.EEPROM, null, eepromItem);
        registry.register(COMPAT_CPU_TIER1, null, cpuTier1Item);
        registry.register(COMPAT_MEMORY_TIER1, null, memoryTier1Item);
        registry.register(COMPAT_HDD_TIER1, null, hddTier1Item);
    }

    private ModContentCatalog() {
    }
}
