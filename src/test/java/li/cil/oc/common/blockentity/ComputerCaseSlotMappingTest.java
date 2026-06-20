package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.item.Slot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ComputerCaseSlotMappingTest {
    private static final String EEPROM = "eeprom";

    @Test
    void tierOneSlotTypesMatchExpectedComponents() {
        assertEquals(7, ComputerCaseBlockEntity.slotCount(0));
        assertEquals(Slot.Card, ComputerCaseBlockEntity.slotType(ComputerCaseBlockEntity.SLOT_CARD_0));
        assertEquals(Slot.Card, ComputerCaseBlockEntity.slotType(ComputerCaseBlockEntity.SLOT_CARD_1));
        assertEquals(Slot.CPU, ComputerCaseBlockEntity.slotType(ComputerCaseBlockEntity.SLOT_CPU));
        assertEquals(Slot.Memory, ComputerCaseBlockEntity.slotType(ComputerCaseBlockEntity.SLOT_MEMORY_0));
        assertEquals(Slot.Memory, ComputerCaseBlockEntity.slotType(ComputerCaseBlockEntity.SLOT_MEMORY_1));
        assertEquals(Slot.HDD, ComputerCaseBlockEntity.slotType(ComputerCaseBlockEntity.SLOT_HDD));
        assertEquals(EEPROM, ComputerCaseBlockEntity.slotType(ComputerCaseBlockEntity.SLOT_EEPROM));
        assertEquals(Slot.None, ComputerCaseBlockEntity.slotType(-1));
        assertEquals(Slot.None, ComputerCaseBlockEntity.slotType(ComputerCaseBlockEntity.CONTAINER_SIZE));
    }

    @Test
    void tierTwoSlotTypesMatchUpstreamComponents() {
        assertEquals(8, ComputerCaseBlockEntity.slotCount(1));
        assertEquals(Slot.Card, ComputerCaseBlockEntity.slotType(1, 0));
        assertEquals(1, ComputerCaseBlockEntity.slotTier(1, 0));
        assertEquals(Slot.Card, ComputerCaseBlockEntity.slotType(1, 1));
        assertEquals(0, ComputerCaseBlockEntity.slotTier(1, 1));
        assertEquals(Slot.Memory, ComputerCaseBlockEntity.slotType(1, 2));
        assertEquals(1, ComputerCaseBlockEntity.slotTier(1, 2));
        assertEquals(Slot.Memory, ComputerCaseBlockEntity.slotType(1, 3));
        assertEquals(1, ComputerCaseBlockEntity.slotTier(1, 3));
        assertEquals(Slot.HDD, ComputerCaseBlockEntity.slotType(1, 4));
        assertEquals(1, ComputerCaseBlockEntity.slotTier(1, 4));
        assertEquals(Slot.HDD, ComputerCaseBlockEntity.slotType(1, 5));
        assertEquals(0, ComputerCaseBlockEntity.slotTier(1, 5));
        assertEquals(Slot.CPU, ComputerCaseBlockEntity.slotType(1, 6));
        assertEquals(1, ComputerCaseBlockEntity.slotTier(1, 6));
        assertEquals(EEPROM, ComputerCaseBlockEntity.slotType(1, 7));
        assertEquals(Integer.MAX_VALUE, ComputerCaseBlockEntity.slotTier(1, 7));
        assertEquals(Slot.None, ComputerCaseBlockEntity.slotType(1, 8));
    }

    @Test
    void tierThreeSlotTypesMatchUpstreamComponents() {
        assertEquals(10, ComputerCaseBlockEntity.slotCount(2));
        assertEquals(Slot.Card, ComputerCaseBlockEntity.slotType(2, 0));
        assertEquals(2, ComputerCaseBlockEntity.slotTier(2, 0));
        assertEquals(Slot.Card, ComputerCaseBlockEntity.slotType(2, 1));
        assertEquals(1, ComputerCaseBlockEntity.slotTier(2, 1));
        assertEquals(Slot.Card, ComputerCaseBlockEntity.slotType(2, 2));
        assertEquals(1, ComputerCaseBlockEntity.slotTier(2, 2));
        assertEquals(Slot.Memory, ComputerCaseBlockEntity.slotType(2, 3));
        assertEquals(2, ComputerCaseBlockEntity.slotTier(2, 3));
        assertEquals(Slot.Memory, ComputerCaseBlockEntity.slotType(2, 4));
        assertEquals(2, ComputerCaseBlockEntity.slotTier(2, 4));
        assertEquals(Slot.HDD, ComputerCaseBlockEntity.slotType(2, 5));
        assertEquals(2, ComputerCaseBlockEntity.slotTier(2, 5));
        assertEquals(Slot.HDD, ComputerCaseBlockEntity.slotType(2, 6));
        assertEquals(1, ComputerCaseBlockEntity.slotTier(2, 6));
        assertEquals(Slot.Floppy, ComputerCaseBlockEntity.slotType(2, 7));
        assertEquals(0, ComputerCaseBlockEntity.slotTier(2, 7));
        assertEquals(Slot.CPU, ComputerCaseBlockEntity.slotType(2, 8));
        assertEquals(2, ComputerCaseBlockEntity.slotTier(2, 8));
        assertEquals(EEPROM, ComputerCaseBlockEntity.slotType(2, 9));
        assertEquals(Integer.MAX_VALUE, ComputerCaseBlockEntity.slotTier(2, 9));
        assertEquals(Slot.None, ComputerCaseBlockEntity.slotType(2, 10));
    }
}
