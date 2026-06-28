package li.cil.oc.common;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AssemblerRegistrationShapeTest {
    @Test
    void assemblerInventoryMatchesUpstreamSlotLayout() {
        assertEquals(0, AssemblerBlockEntity.SLOT_TEMPLATE);
        assertEquals(1, AssemblerBlockEntity.SLOT_CONTAINER_START);
        assertEquals(3, AssemblerBlockEntity.CONTAINER_SLOT_COUNT);
        assertEquals(4, AssemblerBlockEntity.SLOT_UPGRADE_START);
        assertEquals(9, AssemblerBlockEntity.UPGRADE_SLOT_COUNT);
        assertEquals(13, AssemblerBlockEntity.SLOT_COMPONENT_START);
        assertEquals(9, AssemblerBlockEntity.COMPONENT_SLOT_COUNT);
        assertEquals(22, AssemblerBlockEntity.CONTAINER_SIZE);
    }

    @Test
    void assemblerExposesUpstreamDeviceInfoMetadata() throws Exception {
        assertTrue(DeviceInfo.class.isAssignableFrom(AssemblerBlockEntity.class));

        final AssemblerBlockEntity assembler = allocateAssembler();
        final Map<String, String> metadata = ((DeviceInfo) assembler).getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Generic, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Assembler", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("Factorizer R1D1", metadata.get(DeviceInfo.DeviceAttribute.Product));
    }

    @Test
    void assemblerSyncsVisualStateToClientRenderer() throws Exception {
        final Method updateTag = AssemblerBlockEntity.class.getDeclaredMethod("getUpdateTag", net.minecraft.core.HolderLookup.Provider.class);
        final Method updatePacket = AssemblerBlockEntity.class.getDeclaredMethod("getUpdatePacket");
        final Method visuallyAssembling = AssemblerBlockEntity.class.getDeclaredMethod("isVisuallyAssembling");
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/AssemblerBlockEntity.java"));

        assertEquals(net.minecraft.nbt.CompoundTag.class, updateTag.getReturnType());
        assertEquals(net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.class, updatePacket.getReturnType());
        assertEquals(boolean.class, visuallyAssembling.getReturnType());
        assertTrue(source.contains("ClientboundBlockEntityDataPacket.create(this)"));
        assertTrue(source.contains("TAG_VISUAL_ASSEMBLING"));
        assertTrue(source.contains("sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3)"));
    }

    private static AssemblerBlockEntity allocateAssembler() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (AssemblerBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(AssemblerBlockEntity.class);
    }
}
