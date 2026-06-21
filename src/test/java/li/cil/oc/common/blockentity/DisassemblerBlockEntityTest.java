package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
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

    private static DisassemblerBlockEntity allocateDisassembler() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (DisassemblerBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(DisassemblerBlockEntity.class);
    }
}
