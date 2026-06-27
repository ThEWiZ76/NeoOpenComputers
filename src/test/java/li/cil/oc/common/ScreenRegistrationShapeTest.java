package li.cil.oc.common;

import li.cil.oc.common.block.ScreenBlock;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import li.cil.oc.api.driver.DeviceInfo;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.core.BlockPos;
import li.cil.oc.api.internal.TextBuffer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenRegistrationShapeTest {
    @Test
    void screenBlockIsFacingBlock() throws NoSuchMethodException {
        final Constructor<ScreenBlock> constructor = ScreenBlock.class.getConstructor(BlockBehaviour.Properties.class);

        assertTrue(Block.class.isAssignableFrom(ScreenBlock.class));
        assertTrue(EntityBlock.class.isAssignableFrom(ScreenBlock.class));
        assertArrayEquals(new Class<?>[]{BlockBehaviour.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void screenBlockTracksUpstreamPitchAndYaw() throws ReflectiveOperationException {
        final Field pitch = ScreenBlock.class.getDeclaredField("PITCH");
        final Field yaw = ScreenBlock.class.getDeclaredField("YAW");

        assertEquals(DirectionProperty.class, pitch.getType());
        assertEquals(DirectionProperty.class, yaw.getType());
    }

    @Test
    void screenBlockstatesIncludeWallFloorAndCeilingPlacements() throws Exception {
        for (final String tier : new String[]{"screen_tier1", "screen_tier2", "screen_tier3"}) {
            final String blockstate = Files.readString(Path.of("src/main/resources/assets/neoopencomputers/blockstates/" + tier + ".json"));

            assertTrue(blockstate.contains("pitch=north,yaw=north"), tier);
            assertTrue(blockstate.contains("pitch=up,yaw=north"), tier);
            assertTrue(blockstate.contains("pitch=down,yaw=north"), tier);
        }
    }

    @Test
    void screenPlacementUsesPlayerLookDirectionLikeUpstreamRotatableScreens() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/block/ScreenBlock.java"));
        final String method = source.substring(
            source.indexOf("public BlockState getStateForPlacement"),
            source.indexOf("protected BlockState rotate"));

        assertTrue(method.contains("context.getNearestLookingDirection()"));
    }

    @Test
    void screenPlacementInvertsPlayerLookLikeUpstreamItemBlock() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/block/ScreenBlock.java"));
        final String method = source.substring(
            source.indexOf("public BlockState getStateForPlacement"),
            source.indexOf("protected BlockState rotate"));

        assertTrue(method.contains("lookDirection.getOpposite()"));
        assertTrue(method.contains("final Direction yaw = context.getHorizontalDirection().getOpposite();"));
        assertTrue(!method.contains("final Direction yaw = context.getHorizontalDirection();"));
    }

    @Test
    void screenPlacementInheritsAdjacentScreenOrientationForMultiblockBuilding() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/block/ScreenBlock.java"));
        final String method = source.substring(
            source.indexOf("public BlockState getStateForPlacement"),
            source.indexOf("protected BlockState rotate"));
        final String helper = source.substring(
            source.indexOf("public static BlockState inheritConnectedScreenState"),
            source.indexOf("public static Direction localRight"));

        assertTrue(method.contains("inheritConnectedScreenState(context, fallback)"));
        assertTrue(source.contains("context.getClickedFace().getOpposite()"));
        assertTrue(helper.contains("screenBlock.tier() != neighborScreenBlock.tier()"));
        assertTrue(helper.contains("direction == right || direction == right.getOpposite() || direction == up || direction == up.getOpposite()"));
        assertTrue(helper.contains(".setValue(PITCH, pitch(neighborState))"));
        assertTrue(helper.contains(".setValue(YAW, yaw(neighborState))"));
    }

    @Test
    void screenLocalEastMatchesUpstreamRotationHelper() {
        assertEquals(Direction.WEST, ScreenBlock.localRight(Direction.NORTH));
        assertEquals(Direction.NORTH, ScreenBlock.localRight(Direction.EAST));
        assertEquals(Direction.EAST, ScreenBlock.localRight(Direction.SOUTH));
        assertEquals(Direction.SOUTH, ScreenBlock.localRight(Direction.WEST));
    }

    @Test
    void screenBlockEntityIsTextBuffer() throws NoSuchMethodException {
        final Constructor<ScreenBlockEntity> constructor = ScreenBlockEntity.class.getConstructor(BlockPos.class, BlockState.class);

        assertTrue(BlockEntity.class.isAssignableFrom(ScreenBlockEntity.class));
        assertTrue(TextBuffer.class.isAssignableFrom(ScreenBlockEntity.class));
        assertTrue(DeviceInfo.class.isAssignableFrom(ScreenBlockEntity.class));
        assertArrayEquals(new Class<?>[]{BlockPos.class, BlockState.class}, constructor.getParameterTypes());
    }

    @Test
    void screenBlockOpensPhysicalTerminalGuiWhenKeyboardIsAttached() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/block/ScreenBlock.java"));

        assertTrue(source.contains("openPhysicalTerminal"));
        assertTrue(source.contains("new TerminalMenu(containerId, playerInventory, screen.terminalSnapshot(), screen)"));
    }
}
