package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.common.ModSettings;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
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
        assertEquals("MightyPirates", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("Break.3R-100", metadata.get(DeviceInfo.DeviceAttribute.Product));
    }

    @Test
    void usesConfiguredBreakChanceLikeUpstream() throws Exception {
        withCachedConfig(ModSettings.DISASSEMBLER_BREAK_CHANCE, 0.25D, () ->
            assertEquals(0.25D, DisassemblerBlockEntity.defaultBreakChance(), 0.000_001D));
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
