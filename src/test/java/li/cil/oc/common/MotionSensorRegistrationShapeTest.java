package li.cil.oc.common;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.network.Environment;
import li.cil.oc.common.block.MotionSensorBlock;
import li.cil.oc.common.blockentity.MotionSensorBlockEntity;
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

final class MotionSensorRegistrationShapeTest {
    @Test
    void motionSensorBlockIsEntityBlock() throws NoSuchMethodException {
        final Constructor<MotionSensorBlock> constructor = MotionSensorBlock.class.getConstructor(BlockBehaviour.Properties.class);

        assertTrue(Block.class.isAssignableFrom(MotionSensorBlock.class));
        assertTrue(EntityBlock.class.isAssignableFrom(MotionSensorBlock.class));
        assertArrayEquals(new Class<?>[]{BlockBehaviour.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void motionSensorBlockEntityExposesComponentShape() throws NoSuchMethodException {
        final Constructor<MotionSensorBlockEntity> constructor = MotionSensorBlockEntity.class.getConstructor(BlockPos.class, BlockState.class);

        assertTrue(BlockEntity.class.isAssignableFrom(MotionSensorBlockEntity.class));
        assertTrue(Environment.class.isAssignableFrom(MotionSensorBlockEntity.class));
        assertTrue(DeviceInfo.class.isAssignableFrom(MotionSensorBlockEntity.class));
        assertArrayEquals(new Class<?>[]{BlockPos.class, BlockState.class}, constructor.getParameterTypes());
        assertCallback("getSensitivity");
        assertCallback("setSensitivity");
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = MotionSensorBlockEntity.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }
}
