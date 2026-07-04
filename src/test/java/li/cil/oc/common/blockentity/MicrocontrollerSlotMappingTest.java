package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.item.Slot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MicrocontrollerSlotMappingTest {
    private static final String EEPROM = "eeprom";

    @Test
    void tierOneSlotsMatchPinnedAlphaLayout() {
        assertEquals(6, MicrocontrollerBlockEntity.slotCount(0));
        assertEquals(Slot.CPU, MicrocontrollerBlockEntity.slotType(0, 0));
        assertEquals(0, MicrocontrollerBlockEntity.slotTier(0, 0));
        assertEquals(Slot.Memory, MicrocontrollerBlockEntity.slotType(0, 1));
        assertEquals(0, MicrocontrollerBlockEntity.slotTier(0, 1));
        assertEquals(EEPROM, MicrocontrollerBlockEntity.slotType(0, 2));
        assertEquals(Integer.MAX_VALUE, MicrocontrollerBlockEntity.slotTier(0, 2));
        assertEquals(Slot.Card, MicrocontrollerBlockEntity.slotType(0, 3));
        assertEquals(0, MicrocontrollerBlockEntity.slotTier(0, 3));
        assertEquals(Slot.Card, MicrocontrollerBlockEntity.slotType(0, 4));
        assertEquals(0, MicrocontrollerBlockEntity.slotTier(0, 4));
        assertEquals(Slot.Upgrade, MicrocontrollerBlockEntity.slotType(0, 5));
        assertEquals(1, MicrocontrollerBlockEntity.slotTier(0, 5));
        assertEquals(Slot.None, MicrocontrollerBlockEntity.slotType(0, 6));
    }

    @Test
    void tierTwoSlotsMatchPinnedAlphaLayout() {
        assertEquals(7, MicrocontrollerBlockEntity.slotCount(1));
        assertEquals(Slot.CPU, MicrocontrollerBlockEntity.slotType(1, 0));
        assertEquals(0, MicrocontrollerBlockEntity.slotTier(1, 0));
        assertEquals(Slot.Memory, MicrocontrollerBlockEntity.slotType(1, 1));
        assertEquals(0, MicrocontrollerBlockEntity.slotTier(1, 1));
        assertEquals(Slot.Memory, MicrocontrollerBlockEntity.slotType(1, 2));
        assertEquals(0, MicrocontrollerBlockEntity.slotTier(1, 2));
        assertEquals(EEPROM, MicrocontrollerBlockEntity.slotType(1, 3));
        assertEquals(Integer.MAX_VALUE, MicrocontrollerBlockEntity.slotTier(1, 3));
        assertEquals(Slot.Card, MicrocontrollerBlockEntity.slotType(1, 4));
        assertEquals(1, MicrocontrollerBlockEntity.slotTier(1, 4));
        assertEquals(Slot.Card, MicrocontrollerBlockEntity.slotType(1, 5));
        assertEquals(0, MicrocontrollerBlockEntity.slotTier(1, 5));
        assertEquals(Slot.Upgrade, MicrocontrollerBlockEntity.slotType(1, 6));
        assertEquals(2, MicrocontrollerBlockEntity.slotTier(1, 6));
        assertEquals(Slot.None, MicrocontrollerBlockEntity.slotType(1, 7));
    }

    @Test
    void creativeSlotsMatchPinnedAlphaLayout() {
        assertEquals(16, MicrocontrollerBlockEntity.slotCount(3));
        assertEquals(Slot.CPU, MicrocontrollerBlockEntity.slotType(3, 0));
        assertEquals(2, MicrocontrollerBlockEntity.slotTier(3, 0));
        assertEquals(Slot.Memory, MicrocontrollerBlockEntity.slotType(3, 1));
        assertEquals(2, MicrocontrollerBlockEntity.slotTier(3, 1));
        assertEquals(Slot.Memory, MicrocontrollerBlockEntity.slotType(3, 2));
        assertEquals(2, MicrocontrollerBlockEntity.slotTier(3, 2));
        assertEquals(EEPROM, MicrocontrollerBlockEntity.slotType(3, 3));
        assertEquals(Integer.MAX_VALUE, MicrocontrollerBlockEntity.slotTier(3, 3));

        for (int slot = 4; slot <= 6; slot++) {
            assertEquals(Slot.Card, MicrocontrollerBlockEntity.slotType(3, slot));
            assertEquals(2, MicrocontrollerBlockEntity.slotTier(3, slot));
        }
        for (int slot = 7; slot <= 15; slot++) {
            assertEquals(Slot.Upgrade, MicrocontrollerBlockEntity.slotType(3, slot));
            assertEquals(2, MicrocontrollerBlockEntity.slotTier(3, slot));
        }
        assertEquals(Slot.None, MicrocontrollerBlockEntity.slotType(3, 16));
    }

    @Test
    void invalidSlotsReturnNoSlot() {
        assertEquals(Slot.None, MicrocontrollerBlockEntity.slotType(0, -1));
        assertEquals(-1, MicrocontrollerBlockEntity.slotTier(0, -1));
    }
}
