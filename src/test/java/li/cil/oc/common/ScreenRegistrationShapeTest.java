package li.cil.oc.common;

import li.cil.oc.common.block.ScreenBlock;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import li.cil.oc.api.driver.DeviceInfo;
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
    void screenBlockEntityIsTextBuffer() throws NoSuchMethodException {
        final Constructor<ScreenBlockEntity> constructor = ScreenBlockEntity.class.getConstructor(BlockPos.class, BlockState.class);

        assertTrue(BlockEntity.class.isAssignableFrom(ScreenBlockEntity.class));
        assertTrue(TextBuffer.class.isAssignableFrom(ScreenBlockEntity.class));
        assertTrue(DeviceInfo.class.isAssignableFrom(ScreenBlockEntity.class));
        assertArrayEquals(new Class<?>[]{BlockPos.class, BlockState.class}, constructor.getParameterTypes());
    }
}
