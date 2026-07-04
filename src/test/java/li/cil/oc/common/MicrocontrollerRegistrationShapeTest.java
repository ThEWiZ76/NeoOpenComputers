package li.cil.oc.common;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MicrocontrollerRegistrationShapeTest {
    @Test
    void microcontrollerContentIdsAreStable() throws ReflectiveOperationException {
        assertEquals("microcontroller_tier1", ModContentIds.class.getField("MICROCONTROLLER_TIER1").get(null));
        assertEquals("microcontroller_tier2", ModContentIds.class.getField("MICROCONTROLLER_TIER2").get(null));
        assertEquals("microcontroller_creative", ModContentIds.class.getField("MICROCONTROLLER_CREATIVE").get(null));
        assertEquals("microcontroller", ModContentIds.class.getField("MICROCONTROLLER_BLOCK_ENTITY").get(null));
        assertEquals("microcontroller", ModContentIds.class.getField("MICROCONTROLLER_MENU").get(null));
    }

    @Test
    void microcontrollerBlockProvidesBlockEntity() throws ReflectiveOperationException {
        final Class<?> blockClass = Class.forName(
            "li.cil.oc.common.block.MicrocontrollerBlock",
            false,
            Thread.currentThread().getContextClassLoader());

        assertTrue(Block.class.isAssignableFrom(blockClass));
        assertTrue(HorizontalDirectionalBlock.class.isAssignableFrom(blockClass));
        assertTrue(EntityBlock.class.isAssignableFrom(blockClass));
    }

    @Test
    void microcontrollerRegistriesIncludeAllTiers() throws Exception {
        final String blocks = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModBlocks.java"));
        final String items = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModItems.java"));
        final String blockEntities = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModBlockEntities.java"));
        final String menus = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModMenus.java"));

        assertTrue(blocks.contains("MICROCONTROLLER_TIER1"));
        assertTrue(blocks.contains("MICROCONTROLLER_TIER2"));
        assertTrue(blocks.contains("MICROCONTROLLER_CREATIVE"));
        assertTrue(items.contains("MICROCONTROLLER_TIER1"));
        assertTrue(items.contains("MICROCONTROLLER_TIER2"));
        assertTrue(items.contains("MICROCONTROLLER_CREATIVE"));
        assertTrue(blockEntities.contains("MICROCONTROLLER"));
        assertTrue(menus.contains("MICROCONTROLLER"));
    }
}
