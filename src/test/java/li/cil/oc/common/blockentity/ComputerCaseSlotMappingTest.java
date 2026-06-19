package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.item.Slot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ComputerCaseSlotMappingTest {
    @Test
    void tierOneSlotTypesMatchExpectedComponents() {
        assertEquals(Slot.CPU, ComputerCaseBlockEntity.slotType(ComputerCaseBlockEntity.SLOT_CPU));
        assertEquals(Slot.Memory, ComputerCaseBlockEntity.slotType(ComputerCaseBlockEntity.SLOT_MEMORY_0));
        assertEquals(Slot.Memory, ComputerCaseBlockEntity.slotType(ComputerCaseBlockEntity.SLOT_MEMORY_1));
        assertEquals(Slot.None, ComputerCaseBlockEntity.slotType(-1));
        assertEquals(Slot.None, ComputerCaseBlockEntity.slotType(ComputerCaseBlockEntity.CONTAINER_SIZE));
    }
}
