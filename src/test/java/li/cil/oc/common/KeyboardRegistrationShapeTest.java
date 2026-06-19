package li.cil.oc.common;

import li.cil.oc.common.block.KeyboardBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class KeyboardRegistrationShapeTest {
    @Test
    void keyboardBlockIsFacingBlock() throws NoSuchMethodException {
        final Constructor<KeyboardBlock> constructor = KeyboardBlock.class.getConstructor(BlockBehaviour.Properties.class);

        assertTrue(Block.class.isAssignableFrom(KeyboardBlock.class));
        assertTrue(HorizontalDirectionalBlock.class.isAssignableFrom(KeyboardBlock.class));
        assertArrayEquals(new Class<?>[]{BlockBehaviour.Properties.class}, constructor.getParameterTypes());
    }
}
