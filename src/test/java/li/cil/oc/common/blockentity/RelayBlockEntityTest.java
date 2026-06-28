package li.cil.oc.common.blockentity;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RelayBlockEntityTest {
    @Test
    void relaySyncsVisualActivityToClient() throws NoSuchMethodException {
        final Method activity = RelayBlockEntity.class.getDeclaredMethod("visualActivity");
        final Method updatePacket = RelayBlockEntity.class.getDeclaredMethod("getUpdatePacket");
        final Method updateTag = RelayBlockEntity.class.getDeclaredMethod("getUpdateTag", HolderLookup.Provider.class);
        final Method handleUpdateTag = RelayBlockEntity.class.getDeclaredMethod("handleUpdateTag", net.minecraft.nbt.CompoundTag.class, HolderLookup.Provider.class);
        final Method onDataPacket = RelayBlockEntity.class.getDeclaredMethod("onDataPacket", Connection.class, ClientboundBlockEntityDataPacket.class, HolderLookup.Provider.class);

        assertEquals(RelayBlockEntity.class, activity.getDeclaringClass());
        assertEquals(RelayBlockEntity.class, updatePacket.getDeclaringClass());
        assertEquals(RelayBlockEntity.class, updateTag.getDeclaringClass());
        assertEquals(RelayBlockEntity.class, handleUpdateTag.getDeclaringClass());
        assertEquals(RelayBlockEntity.class, onDataPacket.getDeclaringClass());
    }

    @Test
    void relayMarksVisualActivityWhenPacketIsRelayed() throws IOException {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/RelayBlockEntity.java"));

        assertTrue(source.contains("TAG_VISUAL_ACTIVITY_SEQUENCE"));
        assertTrue(source.contains("markVisualActivity();"));
        assertTrue(source.contains("clientVisualActivityUntilMillis = Util.getMillis() + 1000L"));
        assertTrue(source.contains("level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3)"));
    }
}
