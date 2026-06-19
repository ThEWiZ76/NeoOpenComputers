package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.item.Slot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ComputerCaseSlotMappingTest {
    private static final String EEPROM = "eeprom";

    @Test
    void tierOneSlotTypesMatchExpectedComponents() {
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
}
