package li.cil.oc.common;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.block.HologramBlock;
import li.cil.oc.common.blockentity.HologramBlockEntity;
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

final class HologramRegistrationShapeTest {
    @Test
    void hologramBlockIsEntityBlock() throws NoSuchMethodException {
        final Constructor<HologramBlock> constructor = HologramBlock.class.getConstructor(BlockBehaviour.Properties.class, int.class);

        assertTrue(Block.class.isAssignableFrom(HologramBlock.class));
        assertTrue(EntityBlock.class.isAssignableFrom(HologramBlock.class));
        assertArrayEquals(new Class<?>[]{BlockBehaviour.Properties.class, int.class}, constructor.getParameterTypes());
    }

    @Test
    void hologramBlockEntityExposesComponentShape() throws NoSuchMethodException {
        final Constructor<HologramBlockEntity> constructor = HologramBlockEntity.class.getConstructor(BlockPos.class, BlockState.class);

        assertTrue(BlockEntity.class.isAssignableFrom(HologramBlockEntity.class));
        assertTrue(Environment.class.isAssignableFrom(HologramBlockEntity.class));
        assertTrue(EnvironmentHost.class.isAssignableFrom(HologramBlockEntity.class));
        assertTrue(DeviceInfo.class.isAssignableFrom(HologramBlockEntity.class));
        assertArrayEquals(new Class<?>[]{BlockPos.class, BlockState.class}, constructor.getParameterTypes());
        assertCallback("clear");
        assertCallback("get");
        assertCallback("set");
        assertCallback("fill");
        assertCallback("setRaw");
        assertCallback("copy");
        assertCallback("getScale");
        assertCallback("setScale");
        assertCallback("getTranslation");
        assertCallback("setTranslation");
        assertCallback("maxDepth");
        assertCallback("getPaletteColor");
        assertCallback("setPaletteColor");
        assertCallback("setRotation");
        assertCallback("setRotationSpeed");
        assertCallback("getDimensions");
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = HologramBlockEntity.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }
}
