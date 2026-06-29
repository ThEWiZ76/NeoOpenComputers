package li.cil.oc.common;

import li.cil.oc.common.block.KeyboardBlock;
import li.cil.oc.common.blockentity.KeyboardBlockEntity;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Keyboard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class KeyboardRegistrationShapeTest {
    @Test
    void keyboardBlockIsFacingBlock() throws NoSuchMethodException {
        final Constructor<KeyboardBlock> constructor = KeyboardBlock.class.getConstructor(BlockBehaviour.Properties.class);

        assertTrue(Block.class.isAssignableFrom(KeyboardBlock.class));
        assertTrue(HorizontalDirectionalBlock.class.isAssignableFrom(KeyboardBlock.class));
        assertTrue(EntityBlock.class.isAssignableFrom(KeyboardBlock.class));
        assertArrayEquals(new Class<?>[]{BlockBehaviour.Properties.class}, constructor.getParameterTypes());
    }

    @Test
    void keyboardBlockTracksAttachmentSideLikeUpstream() throws ReflectiveOperationException {
        final Field attachFace = KeyboardBlock.class.getDeclaredField("ATTACH_FACE");

        assertEquals(DirectionProperty.class, attachFace.getType());
    }

    @Test
    void keyboardBlockEntityIsKeyboardEnvironment() throws NoSuchMethodException {
        final Constructor<KeyboardBlockEntity> constructor = KeyboardBlockEntity.class.getConstructor(BlockPos.class, BlockState.class);

        assertTrue(BlockEntity.class.isAssignableFrom(KeyboardBlockEntity.class));
        assertTrue(Keyboard.class.isAssignableFrom(KeyboardBlockEntity.class));
        assertTrue(DeviceInfo.class.isAssignableFrom(KeyboardBlockEntity.class));
        assertArrayEquals(new Class<?>[]{BlockPos.class, BlockState.class}, constructor.getParameterTypes());
    }

    @Test
    void keyboardBlockEntityUsesModernNbtHooks() throws NoSuchMethodException {
        assertEquals(KeyboardBlockEntity.class, KeyboardBlockEntity.class.getDeclaredMethod("loadAdditional", CompoundTag.class, HolderLookup.Provider.class).getDeclaringClass());
        assertEquals(KeyboardBlockEntity.class, KeyboardBlockEntity.class.getDeclaredMethod("saveAdditional", CompoundTag.class, HolderLookup.Provider.class).getDeclaringClass());
    }

    @Test
    void keyboardNodeSidesFollowUpstreamOrientationRule() throws ReflectiveOperationException {
        final Method method = KeyboardBlockEntity.class.getDeclaredMethod(
            "hasNodeOnSide",
            Direction.class,
            Direction.class,
            Direction.class);
        method.setAccessible(true);

        assertEquals(false, method.invoke(null, Direction.UP, Direction.NORTH, Direction.UP));
        assertEquals(false, method.invoke(null, Direction.UP, Direction.NORTH, Direction.SOUTH));
        assertEquals(true, method.invoke(null, Direction.UP, Direction.NORTH, Direction.NORTH));
        assertEquals(true, method.invoke(null, Direction.UP, Direction.NORTH, Direction.EAST));

        assertEquals(false, method.invoke(null, Direction.NORTH, Direction.EAST, Direction.NORTH));
        assertEquals(true, method.invoke(null, Direction.NORTH, Direction.EAST, Direction.DOWN));
        assertEquals(true, method.invoke(null, Direction.NORTH, Direction.EAST, Direction.UP));
    }

    @Test
    void keyboardBlockDoesNotOccludeNeighborFaces() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModBlocks.java"));
        final int methodStart = source.indexOf("private static BlockBehaviour.Properties keyboardProperties()");
        final int methodEnd = source.indexOf("private static BlockBehaviour.Properties hologramProperties()", methodStart);

        assertTrue(methodStart >= 0);
        assertTrue(methodEnd > methodStart);
        assertTrue(source.substring(methodStart, methodEnd).contains(".noOcclusion()"));
    }

    @Test
    void keyboardBlockstateIncludesWallFloorAndCeilingAttachments() throws Exception {
        final String blockstate = Files.readString(Path.of("src/main/resources/assets/neoopencomputers/blockstates/keyboard.json"));

        assertTrue(blockstate.contains("attach_face=north,facing=north"));
        assertTrue(blockstate.contains("attach_face=up,facing=north"));
        assertTrue(blockstate.contains("attach_face=down,facing=north"));
    }

    @Test
    void keyboardBlockForwardsActivationToAttachedScreenTerminal() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/block/KeyboardBlock.java"));

        assertTrue(source.contains("useWithoutItem"));
        assertTrue(source.contains("findAdjacentScreen"));
        assertTrue(source.contains("ScreenBlock.openPhysicalTerminal"));
    }
}
