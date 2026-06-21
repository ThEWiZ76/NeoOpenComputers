package li.cil.oc.common.component;

import li.cil.oc.api.driver.item.Slot;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ServerRackMountableEnvironmentShapeTest {
    @Test
    void exposesRackControlPowerEntryPoint() throws NoSuchMethodException {
        final Method method = ServerRackMountableEnvironment.class.getMethod("controlPower", int.class);

        assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    void exposesTierAwareSlotMetadata() {
        assertEquals(17, ServerRackMountableEnvironment.maxSlotCount());
        assertEquals(9, ServerRackMountableEnvironment.slotCountForTier(0));
        assertEquals(13, ServerRackMountableEnvironment.slotCountForTier(1));
        assertEquals(17, ServerRackMountableEnvironment.slotCountForTier(2));

        assertEquals(Slot.CPU, ServerRackMountableEnvironment.slotTypeName(1, 2));
        assertEquals(Slot.ComponentBus, ServerRackMountableEnvironment.slotTypeName(1, 3));
        assertEquals(Slot.Memory, ServerRackMountableEnvironment.slotTypeName(1, 5));
        assertEquals(Slot.HDD, ServerRackMountableEnvironment.slotTypeName(1, 8));
        assertEquals(ServerRackMountableEnvironment.SLOT_TYPE_EEPROM, ServerRackMountableEnvironment.slotTypeName(1, 12));
        assertEquals(Slot.None, ServerRackMountableEnvironment.slotTypeName(1, 13));
    }
}
