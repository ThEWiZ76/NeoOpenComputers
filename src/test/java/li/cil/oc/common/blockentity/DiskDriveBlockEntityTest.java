package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.driver.DiskDriveBlockDriver;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

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
        assertTrue(MenuProvider.class.isAssignableFrom(DiskDriveBlockEntity.class));
    }

    @Test
    void diskDriveExposesUpstreamDeviceInfoMetadata() throws Exception {
        final DiskDriveBlockEntity diskDrive = allocateDiskDrive();
        final Map<String, String> metadata = ((DeviceInfo) diskDrive).getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Disk, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Floppy disk drive", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("Spinner 520p1", metadata.get(DeviceInfo.DeviceAttribute.Product));
    }

    @Test
    void diskDriveBlockDriverIsRegisteredShape() throws NoSuchMethodException {
        Constructor<DiskDriveBlockDriver> constructor = DiskDriveBlockDriver.class.getConstructor();

        assertTrue(DriverBlock.class.isAssignableFrom(DiskDriveBlockDriver.class));
        assertEquals(0, constructor.getParameterCount());
    }

    private static DiskDriveBlockEntity allocateDiskDrive() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (DiskDriveBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(DiskDriveBlockEntity.class);
    }
}
