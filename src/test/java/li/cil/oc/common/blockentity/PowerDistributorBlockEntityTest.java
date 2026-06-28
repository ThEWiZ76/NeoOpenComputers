package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.common.ModSettings;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PowerDistributorBlockEntityTest {
    @Test
    void doesNotExposeDeviceInfoMetadataLikeUpstream() {
        assertFalse(DeviceInfo.class.isAssignableFrom(PowerDistributorBlockEntity.class));
    }

    @Test
    void usesConfiguredConnectorBufferLikeUpstream() throws Exception {
        withCachedConfig(ModSettings.POWER_DISTRIBUTOR_BUFFER, 42D, () -> {
            assertEquals(42D, PowerDistributorBlockEntity.connectorBufferSize(), 0.000_001D);
        });
    }

    @Test
    void syncsVisualBufferRatioToClientRenderer() throws Exception {
        final Method updateTag = PowerDistributorBlockEntity.class.getDeclaredMethod("getUpdateTag", net.minecraft.core.HolderLookup.Provider.class);
        final Method updatePacket = PowerDistributorBlockEntity.class.getDeclaredMethod("getUpdatePacket");
        final Method visualBufferRatio = PowerDistributorBlockEntity.class.getDeclaredMethod("visualBufferRatio");
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/PowerDistributorBlockEntity.java"));

        assertEquals(net.minecraft.nbt.CompoundTag.class, updateTag.getReturnType());
        assertEquals(net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.class, updatePacket.getReturnType());
        assertEquals(double.class, visualBufferRatio.getReturnType());
        assertTrue(source.contains("ClientboundBlockEntityDataPacket.create(this)"));
        assertTrue(source.contains("TAG_VISUAL_BUFFER_RATIO"));
        assertTrue(source.contains("sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3)"));
    }

    private static <T> void withCachedConfig(final ModConfigSpec.ConfigValue<T> value, final T override, final ThrowingRunnable action) throws Exception {
        final Field cachedValue = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cachedValue.setAccessible(true);
        final Object previous = cachedValue.get(value);
        cachedValue.set(value, override);
        try {
            action.run();
        } finally {
            cachedValue.set(value, previous);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
