package li.cil.oc.common;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.block.GeolyzerBlock;
import li.cil.oc.common.blockentity.GeolyzerBlockEntity;
import li.cil.oc.common.component.GeolyzerEnvironment;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GeolyzerRegistrationShapeTest {
    @Test
    void geolyzerBlockIsEntityBlock() throws NoSuchMethodException {
        final Constructor<GeolyzerBlock> constructor = GeolyzerBlock.class.getConstructor(BlockBehaviour.Properties.class);

        assertTrue(Block.class.isAssignableFrom(GeolyzerBlock.class));
        assertTrue(EntityBlock.class.isAssignableFrom(GeolyzerBlock.class));
        assertArrayEquals(new Class<?>[]{BlockBehaviour.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void geolyzerBlockEntityExposesComponentShape() throws NoSuchMethodException {
        final Constructor<GeolyzerBlockEntity> constructor = GeolyzerBlockEntity.class.getConstructor(BlockPos.class, BlockState.class);

        assertTrue(BlockEntity.class.isAssignableFrom(GeolyzerBlockEntity.class));
        assertTrue(Environment.class.isAssignableFrom(GeolyzerBlockEntity.class));
        assertTrue(EnvironmentHost.class.isAssignableFrom(GeolyzerBlockEntity.class));
        assertTrue(DeviceInfo.class.isAssignableFrom(GeolyzerBlockEntity.class));
        assertArrayEquals(new Class<?>[]{BlockPos.class, BlockState.class}, constructor.getParameterTypes());
        assertCallback("canSeeSky");
        assertCallback("isSunVisible");
        assertCallback("scan");
        assertCallback("analyze");
        assertCallback("store");
    }

    @Test
    void geolyzerExposesUpstreamDeviceInfoMetadata() throws Exception {
        OpenComputersApi.initialize();

        assertUpstreamDeviceInfo(new GeolyzerEnvironment(null).getDeviceInfo());
        assertUpstreamDeviceInfo(allocateGeolyzer().getDeviceInfo());
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = GeolyzerBlockEntity.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }

    private static void assertUpstreamDeviceInfo(final Map<String, String> metadata) {
        assertEquals(DeviceInfo.DeviceClass.Generic, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Geolyzer", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("Terrain Analyzer MkII", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("32", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
    }

    private static GeolyzerBlockEntity allocateGeolyzer() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (GeolyzerBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(GeolyzerBlockEntity.class);
    }
}
