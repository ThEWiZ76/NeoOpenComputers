package li.cil.oc.api.driver.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SlotTest {
    @Test
    void exposesOpenComputersSlotNames() {
        assertEquals("any", Slot.Any);
        assertEquals("card", Slot.Card);
        assertEquals("component_bus", Slot.ComponentBus);
        assertEquals("rack_mountable", Slot.RackMountable);
    }
}
