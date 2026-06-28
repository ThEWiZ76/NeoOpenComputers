package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdapterBlockEntityTest {
    @Test
    void exposesUpstreamDeviceInfoMetadata() throws Exception {
        assertTrue(DeviceInfo.class.isAssignableFrom(AdapterBlockEntity.class));

        final AdapterBlockEntity adapter = allocateAdapter();
        final Map<String, String> metadata = ((DeviceInfo) adapter).getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Bus, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Adapter", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("Multiplug Ext.1", metadata.get(DeviceInfo.DeviceAttribute.Product));
    }

    @Test
    void syncsOpenSidesToClientForRenderer() throws Exception {
        Method updateTag = AdapterBlockEntity.class.getDeclaredMethod("getUpdateTag", net.minecraft.core.HolderLookup.Provider.class);
        Method updatePacket = AdapterBlockEntity.class.getDeclaredMethod("getUpdatePacket");

        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/AdapterBlockEntity.java"));

        assertEquals(net.minecraft.nbt.CompoundTag.class, updateTag.getReturnType());
        assertEquals(net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.class, updatePacket.getReturnType());
        assertTrue(source.contains("ClientboundBlockEntityDataPacket.create(this)"));
        assertTrue(source.contains("sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3)"));
        assertTrue(source.contains("TAG_OPEN_SIDES"));
    }

    @Test
    void playsUpstreamPistonSoundWhenTogglingOpenSides() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/AdapterBlockEntity.java"));
        final float pitch = AdapterBlockEntity.sideTogglePitch(RandomSource.create(0L));

        assertTrue(source.contains("SoundEvents.PISTON_EXTEND"));
        assertTrue(source.contains("SoundSource.BLOCKS"));
        assertTrue(source.contains("level.playSound(null, worldPosition"));
        assertTrue(source.contains("0.5F"));
        assertTrue(pitch >= 0.7F);
        assertTrue(pitch <= 0.95F);
    }

    private static AdapterBlockEntity allocateAdapter() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (AdapterBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(AdapterBlockEntity.class);
    }
}
