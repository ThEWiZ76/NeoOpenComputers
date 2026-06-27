package li.cil.oc.common.blockentity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DiskDriveBlockEntityClientSyncTest {
    @Test
    void declaresClientItemSyncHooks() throws NoSuchMethodException {
        assertEquals(DiskDriveBlockEntity.class, DiskDriveBlockEntity.class.getDeclaredMethod("getUpdatePacket").getDeclaringClass());
        assertEquals(DiskDriveBlockEntity.class, DiskDriveBlockEntity.class.getDeclaredMethod("getUpdateTag", HolderLookup.Provider.class).getDeclaringClass());
        assertEquals(DiskDriveBlockEntity.class, DiskDriveBlockEntity.class.getDeclaredMethod("handleUpdateTag", CompoundTag.class, HolderLookup.Provider.class).getDeclaringClass());
    }

    @Test
    void clientItemSyncClearsRemovedMediaBeforeLoadingPacketItems() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/DiskDriveBlockEntity.java"));
        final String method = source.substring(
            source.indexOf("private void loadClientData"),
            source.indexOf("private void connectDiskEnvironment"));

        assertTrue(method.indexOf("items.replaceAll") >= 0);
        assertTrue(method.indexOf("items.replaceAll") < method.indexOf("ContainerHelper.loadAllItems"));
    }
}
