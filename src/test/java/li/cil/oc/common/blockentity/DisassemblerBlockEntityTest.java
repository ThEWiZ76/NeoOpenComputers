package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DisassemblerBlockEntityTest {
    @Test
    void exposesUpstreamDeviceInfoMetadata() throws Exception {
        assertTrue(DeviceInfo.class.isAssignableFrom(DisassemblerBlockEntity.class));

        final DisassemblerBlockEntity disassembler = allocateDisassembler();
        final Map<String, String> metadata = ((DeviceInfo) disassembler).getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Generic, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Disassembler", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("Break.3R-100", metadata.get(DeviceInfo.DeviceAttribute.Product));
    }

    @Test
    void exposesPoweredEnvironmentShapeLikeUpstream() throws NoSuchMethodException {
        assertTrue(Environment.class.isAssignableFrom(DisassemblerBlockEntity.class));
        assertTrue(SidedEnvironment.class.isAssignableFrom(DisassemblerBlockEntity.class));
        assertEquals(
            void.class,
            DisassemblerBlockEntity.class.getMethod("serverTick", Level.class, BlockPos.class, BlockState.class, DisassemblerBlockEntity.class).getReturnType());
    }

    @Test
    void usesConfiguredBreakChanceLikeUpstream() throws Exception {
        withCachedConfig(ModSettings.DISASSEMBLER_BREAK_CHANCE, 0.25D, () ->
            assertEquals(0.25D, DisassemblerBlockEntity.defaultBreakChance(), 0.000_001D));
    }

    @Test
    void syncsActiveStateToClientRenderer() throws Exception {
        final Method updateTag = DisassemblerBlockEntity.class.getDeclaredMethod("getUpdateTag", net.minecraft.core.HolderLookup.Provider.class);
        final Method updatePacket = DisassemblerBlockEntity.class.getDeclaredMethod("getUpdatePacket");
        final Method visuallyActive = DisassemblerBlockEntity.class.getDeclaredMethod("isVisuallyActive");
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/DisassemblerBlockEntity.java"));

        assertEquals(net.minecraft.nbt.CompoundTag.class, updateTag.getReturnType());
        assertEquals(net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.class, updatePacket.getReturnType());
        assertEquals(boolean.class, visuallyActive.getReturnType());
        assertTrue(source.contains("ClientboundBlockEntityDataPacket.create(this)"));
        assertTrue(source.contains("TAG_VISUAL_ACTIVE"));
        assertTrue(source.contains("sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3)"));
    }

    private static DisassemblerBlockEntity allocateDisassembler() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (DisassemblerBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(DisassemblerBlockEntity.class);
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

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
