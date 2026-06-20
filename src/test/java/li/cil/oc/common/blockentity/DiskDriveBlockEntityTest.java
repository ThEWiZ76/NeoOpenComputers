package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.driver.DiskDriveBlockDriver;
import net.minecraft.world.Container;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DiskDriveBlockEntityTest {
    @Test
    void diskDriveExposesOneFloppySlot() {
        assertEquals(0, DiskDriveBlockEntity.SLOT_FLOPPY);
        assertEquals(1, DiskDriveBlockEntity.CONTAINER_SIZE);
        assertTrue(DiskDriveBlockEntity.acceptsDriverSlot(Slot.Floppy));
        assertFalse(DiskDriveBlockEntity.acceptsDriverSlot(Slot.HDD));
        assertFalse(DiskDriveBlockEntity.acceptsDriverSlot(Slot.Card));
    }

    @Test
    void diskDriveIsNetworkedContainerHost() {
        assertTrue(ManagedEnvironment.class.isAssignableFrom(DiskDriveBlockEntity.class));
        assertTrue(EnvironmentHost.class.isAssignableFrom(DiskDriveBlockEntity.class));
        assertTrue(Container.class.isAssignableFrom(DiskDriveBlockEntity.class));
        assertTrue(DeviceInfo.class.isAssignableFrom(DiskDriveBlockEntity.class));
    }

    @Test
    void diskDriveBlockDriverIsRegisteredShape() throws NoSuchMethodException {
        Constructor<DiskDriveBlockDriver> constructor = DiskDriveBlockDriver.class.getConstructor();

        assertTrue(DriverBlock.class.isAssignableFrom(DiskDriveBlockDriver.class));
        assertEquals(0, constructor.getParameterCount());
    }
}
