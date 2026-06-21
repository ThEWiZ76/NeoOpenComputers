package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.Rack;
import net.minecraft.world.Container;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RackBlockEntityTest {
    @Test
    void rackExposesFourRackMountableSlots() {
        assertEquals(4, RackBlockEntity.CONTAINER_SIZE);
        assertTrue(RackBlockEntity.acceptsDriverSlot(Slot.RackMountable));
        assertFalse(RackBlockEntity.acceptsDriverSlot(Slot.HDD));
        assertFalse(RackBlockEntity.acceptsDriverSlot(Slot.Upgrade));
    }

    @Test
    void rackIsContainerAndApiRack() {
        assertTrue(Container.class.isAssignableFrom(RackBlockEntity.class));
        assertTrue(Rack.class.isAssignableFrom(RackBlockEntity.class));
    }
}
