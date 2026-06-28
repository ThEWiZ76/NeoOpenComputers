package li.cil.oc.common;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Case;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.common.block.ComputerCaseBlock;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComputerCaseRegistrationShapeTest {
    @Test
    void computerCaseBlockProvidesBlockEntity() {
        assertTrue(Block.class.isAssignableFrom(ComputerCaseBlock.class));
        assertTrue(HorizontalDirectionalBlock.class.isAssignableFrom(ComputerCaseBlock.class));
        assertTrue(EntityBlock.class.isAssignableFrom(ComputerCaseBlock.class));
    }

    @Test
    void computerCaseBlockHandlesEmptyHandInteraction() throws NoSuchMethodException {
        final Method useWithoutItem = ComputerCaseBlock.class.getDeclaredMethod(
            "useWithoutItem",
            BlockState.class,
            Level.class,
            BlockPos.class,
            Player.class,
            BlockHitResult.class);

        assertEquals(InteractionResult.class, useWithoutItem.getReturnType());
    }

    @Test
    void computerCaseBlockEntityHostsMachine() throws NoSuchMethodException {
        final Constructor<ComputerCaseBlockEntity> constructor = ComputerCaseBlockEntity.class.getDeclaredConstructor(
            BlockPos.class,
            BlockState.class);

        assertTrue(BlockEntity.class.isAssignableFrom(ComputerCaseBlockEntity.class));
        assertTrue(Case.class.isAssignableFrom(ComputerCaseBlockEntity.class));
        assertTrue(Container.class.isAssignableFrom(ComputerCaseBlockEntity.class));
        assertTrue(MachineHost.class.isAssignableFrom(ComputerCaseBlockEntity.class));
        assertTrue(Analyzable.class.isAssignableFrom(ComputerCaseBlockEntity.class));
        assertTrue(DeviceInfo.class.isAssignableFrom(ComputerCaseBlockEntity.class));
        assertArrayEquals(new Class<?>[]{BlockPos.class, BlockState.class}, constructor.getParameterTypes());
    }

    @Test
    void computerCaseExposesVisualFilesystemAccessStateLikeUpstream() throws NoSuchMethodException {
        final Method activity = ComputerCaseBlockEntity.class.getMethod("visualFileSystemActivity");
        final Method access = ComputerCaseBlockEntity.class.getMethod("recordFileSystemAccess", li.cil.oc.api.network.Node.class, long.class);

        assertEquals(double.class, activity.getReturnType());
        assertEquals(boolean.class, access.getReturnType());
    }

    @Test
    void tierOneComputerCaseHasInitialComponentSlots() {
        assertEquals(7, ComputerCaseBlockEntity.CONTAINER_SIZE);
        assertEquals(0, ComputerCaseBlockEntity.SLOT_CARD_0);
        assertEquals(1, ComputerCaseBlockEntity.SLOT_CARD_1);
        assertEquals(2, ComputerCaseBlockEntity.SLOT_MEMORY_0);
        assertEquals(3, ComputerCaseBlockEntity.SLOT_HDD);
        assertEquals(4, ComputerCaseBlockEntity.SLOT_CPU);
        assertEquals(5, ComputerCaseBlockEntity.SLOT_MEMORY_1);
        assertEquals(6, ComputerCaseBlockEntity.SLOT_EEPROM);
    }

    @Test
    void computerCaseMenuProviderUsesShortUpstreamContainerTitle() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/ComputerCaseBlockEntity.java"));

        assertTrue(source.contains("gui.neoopencomputers.computer_case.title"));
        assertTrue(!source.contains("return Component.translatable(\"block.neoopencomputers.computer_case_tier\""));
    }

    @Test
    void filesystemAccessHandlerRecordsComputerCaseAccess() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/component/FileSystemAccessHandler.java"));

        assertTrue(source.contains("ComputerCaseBlockEntity"));
        assertTrue(source.contains("recordComputerCaseAccess"));
    }
}
