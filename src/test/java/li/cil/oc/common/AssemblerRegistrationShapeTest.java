package li.cil.oc.common;

import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AssemblerRegistrationShapeTest {
    @Test
    void assemblerInventoryMatchesUpstreamSlotLayout() {
        assertEquals(0, AssemblerBlockEntity.SLOT_TEMPLATE);
        assertEquals(1, AssemblerBlockEntity.SLOT_CONTAINER_START);
        assertEquals(3, AssemblerBlockEntity.CONTAINER_SLOT_COUNT);
        assertEquals(4, AssemblerBlockEntity.SLOT_UPGRADE_START);
        assertEquals(9, AssemblerBlockEntity.UPGRADE_SLOT_COUNT);
        assertEquals(13, AssemblerBlockEntity.SLOT_COMPONENT_START);
        assertEquals(9, AssemblerBlockEntity.COMPONENT_SLOT_COUNT);
        assertEquals(22, AssemblerBlockEntity.CONTAINER_SIZE);
    }
}
