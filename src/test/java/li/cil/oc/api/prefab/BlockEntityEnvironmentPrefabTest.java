package li.cil.oc.api.prefab;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
}
