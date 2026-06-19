package li.cil.oc.common;

import li.cil.oc.common.block.ScreenBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
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
        assertArrayEquals(new Class<?>[]{BlockBehaviour.Properties.class}, constructor.getParameterTypes());
    }
}
