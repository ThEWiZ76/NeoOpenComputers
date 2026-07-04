package li.cil.oc.common;

import li.cil.oc.api.internal.Tiered;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.util.StateAware;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.client.extensions.IMenuProviderExtension;
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
        assertEquals("robot", ModContentIds.class.getField("ROBOT_MENU").get(null));
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
    void robotBlockEntityIsRunnableRobotHost() throws ReflectiveOperationException {
        final Class<?> blockEntityClass = Class.forName(
            "li.cil.oc.common.blockentity.RobotBlockEntity",
            false,
            Thread.currentThread().getContextClassLoader());

        assertTrue(Container.class.isAssignableFrom(blockEntityClass));
        assertTrue(WorldlyContainer.class.isAssignableFrom(blockEntityClass));
        assertTrue(Tiered.class.isAssignableFrom(blockEntityClass));
        assertTrue(Robot.class.isAssignableFrom(blockEntityClass));
        assertTrue(MachineHost.class.isAssignableFrom(blockEntityClass));
        assertTrue(MenuProvider.class.isAssignableFrom(blockEntityClass));
        assertTrue(IMenuProviderExtension.class.isAssignableFrom(blockEntityClass));
        assertTrue(DeviceInfo.class.isAssignableFrom(blockEntityClass));
        assertTrue(StateAware.class.isAssignableFrom(blockEntityClass));
        assertTrue(Analyzable.class.isAssignableFrom(blockEntityClass));
    }

    @Test
    void robotRegistriesIncludeBlockItemAndBlockEntity() throws Exception {
        final String blocks = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModBlocks.java"));
        final String items = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModItems.java"));
        final String blockEntities = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModBlockEntities.java"));
        final String menus = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModMenus.java"));

        assertTrue(blocks.contains("ROBOT"));
        assertTrue(items.contains("ROBOT"));
        assertTrue(blockEntities.contains("ROBOT"));
        assertTrue(menus.contains("ROBOT"));
    }

    @Test
    void robotBlockHasRuntimeHooks() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/block/RobotBlock.java"));

        assertTrue(source.contains("serverTick"));
        assertTrue(source.contains("BlockNetworkConnector.joinIfServer"));
        assertTrue(source.contains("useWithoutItem"));
        assertTrue(source.contains("openMenu"));
    }

    @Test
    void robotBlockEntityPersistsAssemblyData() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/RobotBlockEntity.java"));

        assertTrue(source.contains("TAG_TIER"));
        assertTrue(source.contains("ContainerHelper.loadAllItems"));
        assertTrue(source.contains("ContainerHelper.saveAllItems"));
        assertTrue(source.contains("machine.load"));
        assertTrue(source.contains("machine.save"));
        assertTrue(source.contains("slotCount"));
    }
}
