package li.cil.oc.common;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.block.TransposerBlock;
import li.cil.oc.common.blockentity.TransposerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TransposerRegistrationShapeTest {
    @Test
    void transposerBlockIsEntityBlock() throws NoSuchMethodException {
        final Constructor<TransposerBlock> constructor = TransposerBlock.class.getConstructor(BlockBehaviour.Properties.class);

        assertTrue(Block.class.isAssignableFrom(TransposerBlock.class));
        assertTrue(EntityBlock.class.isAssignableFrom(TransposerBlock.class));
        assertArrayEquals(new Class<?>[]{BlockBehaviour.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void transposerBlockEntityExposesComponentShape() throws NoSuchMethodException {
        final Constructor<TransposerBlockEntity> constructor = TransposerBlockEntity.class.getConstructor(BlockPos.class, BlockState.class);

        assertTrue(BlockEntity.class.isAssignableFrom(TransposerBlockEntity.class));
        assertTrue(Environment.class.isAssignableFrom(TransposerBlockEntity.class));
        assertTrue(EnvironmentHost.class.isAssignableFrom(TransposerBlockEntity.class));
        assertTrue(DeviceInfo.class.isAssignableFrom(TransposerBlockEntity.class));
        assertArrayEquals(new Class<?>[]{BlockPos.class, BlockState.class}, constructor.getParameterTypes());
        assertCallback("getInventorySize");
        assertCallback("getSlotStackSize");
        assertCallback("getSlotMaxStackSize");
        assertCallback("compareStacks");
        assertCallback("areStacksEquivalent");
        assertCallback("getStackInSlot");
        assertCallback("getAllStacks");
        assertCallback("getInventoryName");
        assertCallback("transferItem");
    }

    @Test
    void transposerBlockEntityExposesUpstreamDeviceInfoMetadata() throws Exception {
        final TransposerBlockEntity transposer = allocateTransposer();

        Map<String, String> metadata = transposer.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Generic, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Transposer", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("TP4k-iX", metadata.get(DeviceInfo.DeviceAttribute.Product));
    }

    @Test
    void transposerBlockEntitySyncsActivityForRenderer() throws Exception {
        final Method updateTag = TransposerBlockEntity.class.getDeclaredMethod("getUpdateTag", net.minecraft.core.HolderLookup.Provider.class);
        final Method updatePacket = TransposerBlockEntity.class.getDeclaredMethod("getUpdatePacket");
        final Method visualActivity = TransposerBlockEntity.class.getDeclaredMethod("visualActivity");
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/TransposerBlockEntity.java"));

        assertEquals(net.minecraft.nbt.CompoundTag.class, updateTag.getReturnType());
        assertEquals(net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.class, updatePacket.getReturnType());
        assertEquals(double.class, visualActivity.getReturnType());
        assertTrue(source.contains("TAG_VISUAL_ACTIVITY_SEQUENCE"));
        assertTrue(source.contains("ClientboundBlockEntityDataPacket.create(this)"));
        assertTrue(source.contains("sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3)"));
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = TransposerBlockEntity.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }

    private static TransposerBlockEntity allocateTransposer() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (TransposerBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(TransposerBlockEntity.class);
    }
}
