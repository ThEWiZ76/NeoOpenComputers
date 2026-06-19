package li.cil.oc.common;

import li.cil.oc.api.internal.Case;
import li.cil.oc.api.machine.MachineHost;
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
        assertArrayEquals(new Class<?>[]{BlockPos.class, BlockState.class}, constructor.getParameterTypes());
    }

    @Test
    void tierOneComputerCaseHasInitialComponentSlots() {
        assertEquals(3, ComputerCaseBlockEntity.CONTAINER_SIZE);
        assertEquals(0, ComputerCaseBlockEntity.SLOT_CPU);
        assertEquals(1, ComputerCaseBlockEntity.SLOT_MEMORY_0);
        assertEquals(2, ComputerCaseBlockEntity.SLOT_MEMORY_1);
    }
}
