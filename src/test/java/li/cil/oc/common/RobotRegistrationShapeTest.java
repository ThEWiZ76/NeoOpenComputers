package li.cil.oc.common;

import li.cil.oc.api.internal.Tiered;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RobotRegistrationShapeTest {
    @Test
    void robotContentIdsAreStable() throws ReflectiveOperationException {
        assertEquals("robot", ModContentIds.class.getField("ROBOT").get(null));
        assertEquals("robot", ModContentIds.class.getField("ROBOT_BLOCK_ENTITY").get(null));
    }

    @Test
    void robotBlockProvidesBlockEntity() throws ReflectiveOperationException {
        final Class<?> blockClass = Class.forName(
            "li.cil.oc.common.block.RobotBlock",
            false,
            Thread.currentThread().getContextClassLoader());

        assertTrue(Block.class.isAssignableFrom(blockClass));
        assertTrue(HorizontalDirectionalBlock.class.isAssignableFrom(blockClass));
        assertTrue(EntityBlock.class.isAssignableFrom(blockClass));
    }

    @Test
    void robotBlockEntityStoresTierAndInventoryWithoutClaimingRuntimeYet() throws ReflectiveOperationException {
        final Class<?> blockEntityClass = Class.forName(
            "li.cil.oc.common.blockentity.RobotBlockEntity",
            false,
            Thread.currentThread().getContextClassLoader());

        assertTrue(Container.class.isAssignableFrom(blockEntityClass));
        assertTrue(Tiered.class.isAssignableFrom(blockEntityClass));
    }

    @Test
    void robotRegistriesIncludeBlockItemAndBlockEntity() throws Exception {
        final String blocks = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModBlocks.java"));
        final String items = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModItems.java"));
        final String blockEntities = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModBlockEntities.java"));

        assertTrue(blocks.contains("ROBOT"));
        assertTrue(items.contains("ROBOT"));
        assertTrue(blockEntities.contains("ROBOT"));
    }

    @Test
    void robotBlockEntityPersistsAssemblyData() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/RobotBlockEntity.java"));

        assertTrue(source.contains("TAG_TIER"));
        assertTrue(source.contains("ContainerHelper.loadAllItems"));
        assertTrue(source.contains("ContainerHelper.saveAllItems"));
        assertTrue(source.contains("slotCount"));
    }
}
