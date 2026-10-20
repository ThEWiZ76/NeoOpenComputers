package li.cil.oc.common.menu;

import li.cil.oc.common.item.HardDiskDriveItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DriveMenuShapeTest {
    @Test
    void driveMenuHasClientAndHeldStackConstructors() throws NoSuchMethodException {
        final Constructor<DriveMenu> client = DriveMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<DriveMenu> server = DriveMenu.class.getConstructor(int.class, Inventory.class, ItemStack.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(DriveMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, client.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, ItemStack.class}, server.getParameterTypes());
    }

    @Test
    void driveMenuButtonAndDataIndexesAreStable() {
        assertEquals(0, DriveMenu.BUTTON_MANAGED);
        assertEquals(1, DriveMenu.BUTTON_UNMANAGED);
        assertEquals(2, DriveMenu.BUTTON_LOCK);
        assertEquals(0, DriveMenu.DATA_UNMANAGED);
        assertEquals(1, DriveMenu.DATA_LOCKED);
        assertEquals(2, DriveMenu.DATA_COUNT);
    }

    @Test
    void driveDataHelpersMirrorUpstreamTags() {
        final CompoundTag data = new CompoundTag();

        assertFalse(HardDiskDriveItem.isUnmanaged(data));
        assertFalse(HardDiskDriveItem.isLocked(data));

        HardDiskDriveItem.setUnmanaged(data, true);
        HardDiskDriveItem.lock(data, "tester");

        assertTrue(HardDiskDriveItem.isUnmanaged(data));
        assertTrue(HardDiskDriveItem.isLocked(data));
        assertEquals("tester", HardDiskDriveItem.lockInfo(data));
    }

    @Test
    void driveModeChangesClearReadOnlyLockLikeUpstream() {
        final CompoundTag data = new CompoundTag();
        data.putString("oc:lock", "tester");

        HardDiskDriveItem.setUnmanaged(data, true);

        assertTrue(HardDiskDriveItem.isUnmanaged(data));
        assertFalse(HardDiskDriveItem.isLocked(data));
    }

    @Test
    void modeChangeDropsOnlyOldStorageAndIdentity() {
        final var data = new CompoundTag();
        final var node = new CompoundTag(); node.putString("address", "old-volume"); node.putString("custom", "keep");
        data.put("node", node); data.put("fs", new CompoundTag());
        data.put("owners", new net.minecraft.nbt.ListTag()); data.putByteArray("data", new byte[]{0, (byte) 255}); data.putInt("headPos", 3);
        data.putString(li.cil.oc.common.NativeVolumeStore.RAW_STORAGE_TAG, "11111111-2222-3333-4444-555555555555.bin");
        data.putString("oc:fs.label", "Keep label"); data.putString("addon:marker", "keep"); data.putString("oc:lock", "owner");
        HardDiskDriveItem.setUnmanaged(data, true);
        assertFalse(data.getCompound("node").contains("address"));
        for (final var key : java.util.List.of("fs", "owners", "data", "headPos", "oc:lock", li.cil.oc.common.NativeVolumeStore.RAW_STORAGE_TAG)) assertFalse(data.contains(key), key);
        assertEquals("keep", data.getCompound("node").getString("custom"));
        assertEquals("Keep label", data.getString("oc:fs.label")); assertEquals("keep", data.getString("addon:marker"));
        assertTrue(HardDiskDriveItem.isUnmanaged(data));
    }

    @Test
    void sameModePreservesStorageIdentityAndLock() {
        final var data = new CompoundTag(); data.putBoolean("oc:unmanaged", true);
        data.putString("oc:lock", "owner"); data.putByteArray("data", new byte[]{0, (byte) 255});
        data.putString(li.cil.oc.common.NativeVolumeStore.RAW_STORAGE_TAG, "11111111-2222-3333-4444-555555555555.bin");
        final var node = new CompoundTag(); node.putString("address", "same-volume"); data.put("node", node);
        final var before = data.copy(); HardDiskDriveItem.setUnmanaged(data, true);
        assertEquals(before, data);
    }
}
