package li.cil.oc.common;

import li.cil.oc.common.block.KeyboardBlock;
import li.cil.oc.common.blockentity.KeyboardBlockEntity;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Keyboard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

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
}
