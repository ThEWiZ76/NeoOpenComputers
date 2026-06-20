package li.cil.oc.common;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.block.RedstoneIoBlock;
import li.cil.oc.common.blockentity.RedstoneIoBlockEntity;
import li.cil.oc.common.component.RedstoneControllerHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RedstoneIoRegistrationShapeTest {
    @Test
    void redstoneIoBlockIsEntityBlockAndSignalSource() throws NoSuchMethodException {
        final Constructor<RedstoneIoBlock> constructor = RedstoneIoBlock.class.getConstructor(BlockBehaviour.Properties.class);
        final Method getSignal = RedstoneIoBlock.class.getDeclaredMethod("getSignal", BlockState.class, net.minecraft.world.level.BlockGetter.class, BlockPos.class, Direction.class);

        assertTrue(Block.class.isAssignableFrom(RedstoneIoBlock.class));
        assertTrue(EntityBlock.class.isAssignableFrom(RedstoneIoBlock.class));
        assertArrayEquals(new Class<?>[]{BlockBehaviour.Properties.class}, constructor.getParameterTypes());
        assertEquals(int.class, getSignal.getReturnType());
    }

    @Test
    void redstoneIoBlockEntityExposesComponentShape() throws NoSuchMethodException {
        final Constructor<RedstoneIoBlockEntity> constructor = RedstoneIoBlockEntity.class.getConstructor(BlockPos.class, BlockState.class);

        assertTrue(BlockEntity.class.isAssignableFrom(RedstoneIoBlockEntity.class));
        assertTrue(Environment.class.isAssignableFrom(RedstoneIoBlockEntity.class));
        assertTrue(EnvironmentHost.class.isAssignableFrom(RedstoneIoBlockEntity.class));
        assertTrue(RedstoneControllerHost.class.isAssignableFrom(RedstoneIoBlockEntity.class));
        assertTrue(DeviceInfo.class.isAssignableFrom(RedstoneIoBlockEntity.class));
        assertArrayEquals(new Class<?>[]{BlockPos.class, BlockState.class}, constructor.getParameterTypes());
        assertCallback("getInput");
        assertCallback("getOutput");
        assertCallback("setOutput");
        assertCallback("getComparatorInput");
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = RedstoneIoBlockEntity.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }
}
