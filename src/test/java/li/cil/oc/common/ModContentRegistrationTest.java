package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ModContentRegistrationTest {
    @Test
    void manualItemIdIsStable() {
        assertEquals("manual", ModContentIds.MANUAL);
    }

    @Test
    void creativeTabIdIsStable() {
        assertEquals("main", ModContentIds.MAIN_CREATIVE_TAB);
    }

    @Test
    void computerCaseBlockIdIsStable() {
        assertEquals("computer_case_tier1", ModContentIds.COMPUTER_CASE_TIER1);
    }

    @Test
    void computerCaseBlockEntityIdIsStable() {
        assertEquals("computer_case", ModContentIds.COMPUTER_CASE_BLOCK_ENTITY);
    }

    @Test
    void cpuTier1ItemIdIsStable() {
        assertEquals("cpu_tier1", ModContentIds.CPU_TIER1);
    }

    @Test
    void memoryTier1ItemIdIsStable() {
        assertEquals("memory_tier1", ModContentIds.MEMORY_TIER1);
    }

    @Test
    void hardDiskDriveTier1ItemIdIsStable() {
        assertEquals("hdd_tier1", ModContentIds.HDD_TIER1);
    }

    @Test
    void eepromItemIdIsStable() {
        assertEquals("eeprom", ModContentIds.EEPROM);
    }

    @Test
    void graphicsCardTier1ItemIdIsStable() {
        assertEquals("graphics_card_tier1", ModContentIds.GRAPHICS_CARD_TIER1);
    }

    @Test
    void networkCardItemIdIsStable() {
        assertEquals("network_card", ModContentIds.NETWORK_CARD);
    }

    @Test
    void screenTier1BlockIdIsStable() {
        assertEquals("screen_tier1", ModContentIds.SCREEN_TIER1);
    }

    @Test
    void keyboardBlockIdIsStable() {
        assertEquals("keyboard", ModContentIds.KEYBOARD);
    }

    @Test
    void computerCaseMenuIdIsStable() {
        assertEquals("computer_case", ModContentIds.COMPUTER_CASE_MENU);
    }
}
