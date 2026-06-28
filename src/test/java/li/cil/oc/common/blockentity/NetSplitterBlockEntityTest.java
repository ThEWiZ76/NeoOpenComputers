package li.cil.oc.common.blockentity;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NetSplitterBlockEntityTest {
    @Test
    void netSplitterSyncsVisualStateToClient() throws NoSuchMethodException {
        final Method updatePacket = NetSplitterBlockEntity.class.getDeclaredMethod("getUpdatePacket");
        final Method updateTag = NetSplitterBlockEntity.class.getDeclaredMethod("getUpdateTag", HolderLookup.Provider.class);
        final Method handleUpdateTag = NetSplitterBlockEntity.class.getDeclaredMethod("handleUpdateTag", net.minecraft.nbt.CompoundTag.class, HolderLookup.Provider.class);
        final Method onDataPacket = NetSplitterBlockEntity.class.getDeclaredMethod("onDataPacket", Connection.class, ClientboundBlockEntityDataPacket.class, HolderLookup.Provider.class);

        assertEquals(NetSplitterBlockEntity.class, updatePacket.getDeclaringClass());
        assertEquals(NetSplitterBlockEntity.class, updateTag.getDeclaringClass());
        assertEquals(NetSplitterBlockEntity.class, handleUpdateTag.getDeclaringClass());
        assertEquals(NetSplitterBlockEntity.class, onDataPacket.getDeclaringClass());
    }

    @Test
    void netSplitterSendsClientUpdatesWhenSideStateChanges() throws IOException {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/NetSplitterBlockEntity.java"));

        assertTrue(source.contains("TAG_OPEN_SIDES"));
        assertTrue(source.contains("TAG_INVERTED"));
        assertTrue(source.contains("level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3)"));
    }

    @Test
    void netSplitterUsesUpstreamRandomPistonSoundPitch() throws IOException {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/NetSplitterBlockEntity.java"));
        final float pitch = NetSplitterBlockEntity.pistonSoundPitch(RandomSource.create(0L));

        assertTrue(source.contains("pistonSoundPitch(level.random)"));
        assertTrue(source.contains("0.5F"));
        assertTrue(pitch >= 0.7F);
        assertTrue(pitch <= 0.95F);
    }
}
