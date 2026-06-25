package li.cil.oc.api.prefab;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BlockEntityEnvironmentPrefabTest {
    @Test
    void tileEntityEnvironmentIsModernBlockEntityEnvironment() throws NoSuchMethodException {
        Constructor<TileEntityEnvironment> constructor = TileEntityEnvironment.class.getDeclaredConstructor(
                BlockEntityType.class, BlockPos.class, BlockState.class);

        assertTrue(BlockEntity.class.isAssignableFrom(TileEntityEnvironment.class));
        assertTrue(Environment.class.isAssignableFrom(TileEntityEnvironment.class));
        assertArrayEquals(new Class<?>[]{BlockEntityType.class, BlockPos.class, BlockState.class}, constructor.getParameterTypes());
    }

    @Test
    void sidedEnvironmentIsModernBlockEntitySidedEnvironment() throws NoSuchMethodException {
        Constructor<TileEntitySidedEnvironment> constructor = TileEntitySidedEnvironment.class.getDeclaredConstructor(
                BlockEntityType.class, BlockPos.class, BlockState.class, Node[].class);
        Method sidedNode = TileEntitySidedEnvironment.class.getMethod("sidedNode", Direction.class);

        assertTrue(BlockEntity.class.isAssignableFrom(TileEntitySidedEnvironment.class));
        assertTrue(SidedEnvironment.class.isAssignableFrom(TileEntitySidedEnvironment.class));
        assertArrayEquals(new Class<?>[]{BlockEntityType.class, BlockPos.class, BlockState.class, Node[].class}, constructor.getParameterTypes());
        assertEquals(Node.class, sidedNode.getReturnType());
    }

    @Test
    void keepsLegacyPrefabLifecycleAndNbtBridgeMethodNames() throws NoSuchMethodException {
        assertLegacyLifecycleAndNbtMethods(TileEntityEnvironment.class);
        assertLegacyLifecycleAndNbtMethods(TileEntitySidedEnvironment.class);
    }

    private static void assertLegacyLifecycleAndNbtMethods(final Class<?> prefabClass) throws NoSuchMethodException {
        Method onChunkUnload = prefabClass.getMethod("onChunkUnload");
        Method invalidate = prefabClass.getMethod("invalidate");
        Method readFromNbt = prefabClass.getMethod("readFromNBT", CompoundTag.class);
        Method writeToNbt = prefabClass.getMethod("writeToNBT", CompoundTag.class);

        assertEquals(void.class, onChunkUnload.getReturnType());
        assertEquals(void.class, invalidate.getReturnType());
        assertEquals(void.class, readFromNbt.getReturnType());
        assertEquals(CompoundTag.class, writeToNbt.getReturnType());
    }
}
