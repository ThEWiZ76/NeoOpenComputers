package li.cil.oc.common.menu;

import li.cil.oc.api.driver.item.Slot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MicrocontrollerMenuShapeTest {
    private static final String EEPROM = "eeprom";

    @Test
    void slotCountsMatchMicrocontrollerTiers() {
        assertEquals(6, MicrocontrollerMenu.microcontrollerSlotCountForTier(0));
        assertEquals(7, MicrocontrollerMenu.microcontrollerSlotCountForTier(1));
        assertEquals(16, MicrocontrollerMenu.microcontrollerSlotCountForTier(3));
    }

    @Test
    void slotMetadataMatchesBlockEntityLayout() {
        assertEquals(Slot.CPU, MicrocontrollerMenu.microcontrollerSlotKind(0, 0));
        assertEquals(Slot.Memory, MicrocontrollerMenu.microcontrollerSlotKind(0, 1));
        assertEquals(EEPROM, MicrocontrollerMenu.microcontrollerSlotKind(0, 2));
        assertEquals(Slot.Card, MicrocontrollerMenu.microcontrollerSlotKind(1, 4));
        assertEquals(1, MicrocontrollerMenu.microcontrollerSlotTierLimit(1, 4));
        assertEquals(Slot.Upgrade, MicrocontrollerMenu.microcontrollerSlotKind(3, 15));
        assertEquals(2, MicrocontrollerMenu.microcontrollerSlotTierLimit(3, 15));
        assertEquals(Slot.None, MicrocontrollerMenu.microcontrollerSlotKind(3, 16));
    }

    @Test
    void slotPositionsAreStableForScreens() {
        assertEquals(48, MicrocontrollerMenu.microcontrollerSlotX(0, 0));
        assertEquals(16, MicrocontrollerMenu.microcontrollerSlotY(0, 0));
        assertEquals(48, MicrocontrollerMenu.microcontrollerSlotX(1, 3));
        assertEquals(34, MicrocontrollerMenu.microcontrollerSlotY(1, 3));
        assertEquals(156, MicrocontrollerMenu.microcontrollerSlotX(3, 15));
        assertEquals(52, MicrocontrollerMenu.microcontrollerSlotY(3, 15));
        assertEquals(-1, MicrocontrollerMenu.microcontrollerSlotX(3, 16));
        assertEquals(-1, MicrocontrollerMenu.microcontrollerSlotY(3, 16));
    }
}
