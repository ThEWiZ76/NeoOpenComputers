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
    void screenPlacementAndNeighborChangesNotifyConnectedScreensForClientRerender() throws Exception {
        final String blockSource = Files.readString(Path.of("src/main/java/li/cil/oc/common/block/ScreenBlock.java"));
        final String entitySource = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/ScreenBlockEntity.java"));

        assertTrue(blockSource.contains("notifyConnectedScreensForClientUpdate"));
        assertTrue(blockSource.contains("Block.UPDATE_CLIENTS"));
        assertTrue(entitySource.contains("lastLayoutOrigin"));
        assertTrue(entitySource.contains("markChanged()"));
    }

    @Test
    void screenBlockTicksClientForDelayedMultiblockChecksLikeUpstream() throws Exception {
        final String blockSource = Files.readString(Path.of("src/main/java/li/cil/oc/common/block/ScreenBlock.java"));
        final String entitySource = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/ScreenBlockEntity.java"));

        assertTrue(blockSource.contains("if (level.isClientSide)"),
            "Client screens need a ticker so the upstream-style delayed multiblock check can count down");
        assertTrue(blockSource.contains("screen.updateClient()"),
            "Client screens need a ticker so the upstream-style delayed multiblock check can count down");
        assertTrue(!blockSource.contains("level.isClientSide || type != ModBlockEntities.SCREEN.get()"),
            "Client screens must not be excluded from ticking");
        assertTrue(entitySource.contains("public void updateClient()"),
            "Client ticking should use a render/layout-only path, not the server energy/network update");
    }

    @Test
    void screenPropertiesUseOcclusionSoStaticCaseCullsInternalMultiblockFaces() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModBlocks.java"));
        final String method = source.substring(
            source.indexOf("private static BlockBehaviour.Properties screenProperties()"),
            source.indexOf("private static BlockBehaviour.Properties diskDriveProperties()"));

        assertTrue(!method.contains(".noOcclusion()"), "Screen full-cube static body must occlude so cullface hides internal multiblock parts");
    }

    @Test
    void screenStaticModelDoesNotDrawFixedFrontOverDynamicConnectedFaces() throws Exception {
        final String model = Files.readString(Path.of("src/main/resources/assets/neoopencomputers/models/block/screen_panel.json"));

        assertTrue(!model.contains("block/screen/"),
            "Static screen model must not draw fixed screen panel textures; the block-entity renderer owns upstream connected screen faces");
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

    @Test
    void screenBlockHandlesDyeUseLikeUpstreamColoredBlocks() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/block/ScreenBlock.java"));

        assertTrue(source.contains("useItemOn"));
        assertTrue(source.contains("DyeItem"));
        assertTrue(source.contains("setRenderColor"));
        assertTrue(source.contains("ItemInteractionResult.sidedSuccess"));
        assertTrue(!source.contains("stack.shrink(1)"), "Screen dye use should not consume dye, matching upstream Colored.consumesDye=false");
    }

    @Test
    void screenBlockRefreshesConnectedLayoutsAfterDyeAndRemovalLikeUpstream() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/block/ScreenBlock.java"));

        final int dyeUse = source.indexOf("protected ItemInteractionResult useItemOn");
        final int dyeColor = source.indexOf("screen.setRenderColor", dyeUse);
        final int dyeRefresh = source.indexOf("notifyConnectedScreensForClientUpdate", dyeColor);
        final int afterDyeUse = source.indexOf("public static InteractionResult openPhysicalTerminal", dyeColor);
        assertTrue(dyeUse >= 0 && dyeColor > dyeUse && dyeRefresh > dyeColor && dyeRefresh < afterDyeUse,
            "Dyeing a screen should refresh connected screen layouts like upstream onColorChanged()");

        final int removal = source.indexOf("protected void onRemove");
        final int removalRefresh = source.indexOf("notifyConnectedScreensForClientUpdate", removal);
        final int afterRemoval = source.indexOf("protected void onPlace", removal);
        assertTrue(removal >= 0 && removalRefresh > removal && removalRefresh < afterRemoval,
            "Removing a screen should refresh connected screen layouts like upstream dispose()");
    }

    @Test
    void screenBlockUpdatesRedstoneInputLikeUpstreamScreenTile() throws Exception {
        final String block = Files.readString(Path.of("src/main/java/li/cil/oc/common/block/ScreenBlock.java"));
        final String entity = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/ScreenBlockEntity.java"));

        assertTrue(block.contains("screen.updateRedstoneInput()"),
            "Screen neighbor changes should update redstone input like upstream RedstoneAware Screen");
        assertTrue(entity.contains("hadRedstoneInput"),
            "Screen should persist prior redstone input state like upstream HadRedstoneInputTag");
        assertTrue(entity.contains("setPowerState(!origin.getPowerState())"),
            "Rising redstone input should toggle the origin screen power state");
    }
}
