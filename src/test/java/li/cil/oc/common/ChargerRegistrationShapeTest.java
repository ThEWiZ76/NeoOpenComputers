package li.cil.oc.common;

import li.cil.oc.common.block.ChargerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChargerRegistrationShapeTest {
    @Test
    void chargerBlockIsFacingGuiEntityBlockLikeUpstream() throws Exception {
        final Constructor<ChargerBlock> constructor = ChargerBlock.class.getConstructor(BlockBehaviour.Properties.class);
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/block/ChargerBlock.java"));

        assertTrue(Block.class.isAssignableFrom(ChargerBlock.class));
        assertTrue(HorizontalDirectionalBlock.class.isAssignableFrom(ChargerBlock.class));
        assertTrue(EntityBlock.class.isAssignableFrom(ChargerBlock.class));
        assertArrayEquals(new Class<?>[]{BlockBehaviour.Properties.class}, constructor.getParameterTypes());
        assertTrue(source.contains("builder.add(FACING)"));
        assertTrue(source.contains("getStateForPlacement"));
    }

    @Test
    void chargerBlockstateHasHorizontalFacingVariants() throws Exception {
        final String blockstate = Files.readString(Path.of("src/main/resources/assets/neoopencomputers/blockstates/charger.json"));

        assertTrue(blockstate.contains("facing=north"));
        assertTrue(blockstate.contains("facing=east"));
        assertTrue(blockstate.contains("facing=south"));
        assertTrue(blockstate.contains("facing=west"));
    }
}
