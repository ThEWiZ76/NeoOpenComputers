package li.cil.oc.common;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.block.GeolyzerBlock;
import li.cil.oc.common.blockentity.GeolyzerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = GeolyzerBlockEntity.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }
}
