package li.cil.oc.common;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.block.WaypointBlock;
import li.cil.oc.common.blockentity.WaypointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WaypointRegistrationShapeTest {
    @Test
    void waypointBlockIsEntityBlock() throws NoSuchMethodException {
        final Constructor<WaypointBlock> constructor = WaypointBlock.class.getConstructor(BlockBehaviour.Properties.class);

        assertTrue(Block.class.isAssignableFrom(WaypointBlock.class));
        assertTrue(DirectionalBlock.class.isAssignableFrom(WaypointBlock.class));
        assertTrue(EntityBlock.class.isAssignableFrom(WaypointBlock.class));
        assertArrayEquals(new Class<?>[]{BlockBehaviour.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void waypointBlockKeepsUpstreamSixWayFacing() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/block/WaypointBlock.java"));

        assertTrue(source.contains("extends DirectionalBlock"));
        assertTrue(source.contains("builder.add(FACING)"));
        assertTrue(source.contains("context.getNearestLookingDirection().getOpposite()"));
    }

    @Test
    void waypointBlockstateDefinesVerticalFacingsLikeUpstream() throws Exception {
        final String blockstate = Files.readString(Path.of("src/main/resources/assets/neoopencomputers/blockstates/waypoint.json"));

        assertTrue(blockstate.contains("\"facing=up\""));
        assertTrue(blockstate.contains("\"facing=down\""));
    }

    @Test
    void waypointBlockEntityExposesComponentShape() throws NoSuchMethodException {
        final Constructor<WaypointBlockEntity> constructor = WaypointBlockEntity.class.getConstructor(BlockPos.class, BlockState.class);

        assertTrue(BlockEntity.class.isAssignableFrom(WaypointBlockEntity.class));
        assertTrue(Environment.class.isAssignableFrom(WaypointBlockEntity.class));
        assertTrue(EnvironmentHost.class.isAssignableFrom(WaypointBlockEntity.class));
        assertArrayEquals(new Class<?>[]{BlockPos.class, BlockState.class}, constructor.getParameterTypes());
        assertCallback("getLabel");
        assertCallback("setLabel");
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = WaypointBlockEntity.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }
}
