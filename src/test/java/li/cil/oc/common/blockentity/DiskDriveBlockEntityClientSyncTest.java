package li.cil.oc.common.blockentity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DiskDriveBlockEntityClientSyncTest {
    @Test
    void declaresClientItemSyncHooks() throws NoSuchMethodException {
        assertEquals(DiskDriveBlockEntity.class, DiskDriveBlockEntity.class.getDeclaredMethod("getUpdatePacket").getDeclaringClass());
        assertEquals(DiskDriveBlockEntity.class, DiskDriveBlockEntity.class.getDeclaredMethod("getUpdateTag", HolderLookup.Provider.class).getDeclaringClass());
        assertEquals(DiskDriveBlockEntity.class, DiskDriveBlockEntity.class.getDeclaredMethod("handleUpdateTag", CompoundTag.class, HolderLookup.Provider.class).getDeclaringClass());
    }
}
