package li.cil.oc.common;

import li.cil.oc.common.block.ScreenBlock;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import li.cil.oc.api.driver.DeviceInfo;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import li.cil.oc.api.internal.TextBuffer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenRegistrationShapeTest {
    @Test
    void screenBlockIsFacingBlock() throws NoSuchMethodException {
        final Constructor<ScreenBlock> constructor = ScreenBlock.class.getConstructor(BlockBehaviour.Properties.class);

        assertTrue(Block.class.isAssignableFrom(ScreenBlock.class));
        assertTrue(HorizontalDirectionalBlock.class.isAssignableFrom(ScreenBlock.class));
        assertTrue(EntityBlock.class.isAssignableFrom(ScreenBlock.class));
        assertArrayEquals(new Class<?>[]{BlockBehaviour.Properties.class}, constructor.getParameterTypes());
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
