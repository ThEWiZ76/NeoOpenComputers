package li.cil.oc.common.menu;

import li.cil.oc.common.blockentity.DisassemblerBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DisassemblerMenuShapeTest {
    @Test
    void disassemblerMenuHasClientAndServerConstructors() throws NoSuchMethodException {
        final Constructor<DisassemblerMenu> clientConstructor = DisassemblerMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<DisassemblerMenu> serverConstructor = DisassemblerMenu.class.getConstructor(int.class, Inventory.class, Container.class);
        final Constructor<DisassemblerMenu> dataConstructor = DisassemblerMenu.class.getConstructor(int.class, Inventory.class, Container.class, ContainerData.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(DisassemblerMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class, ContainerData.class}, dataConstructor.getParameterTypes());
    }

    @Test
    void disassemblerMenuSlotCountsAreStable() {
        assertEquals(DisassemblerBlockEntity.CONTAINER_SIZE, DisassemblerMenu.DISASSEMBLER_SLOT_COUNT);
        assertEquals(36, DisassemblerMenu.PLAYER_SLOT_COUNT);
        assertEquals(46, DisassemblerMenu.TOTAL_SLOT_COUNT);
        assertEquals(1, DisassemblerMenu.DISASSEMBLER_DATA_COUNT);
        assertEquals(0, DisassemblerMenu.STATE_EMPTY);
        assertEquals(1, DisassemblerMenu.STATE_READY);
        assertEquals(2, DisassemblerMenu.STATE_BLOCKED);
    }

    @Test
    void disassemblerMenuExposesStatusData() throws NoSuchMethodException {
        final Method disassemblyState = DisassemblerMenu.class.getMethod("disassemblyState");
        final Method stateFor = DisassemblerMenu.class.getMethod("stateFor", Container.class);

        assertEquals(int.class, disassemblyState.getReturnType());
        assertEquals(int.class, stateFor.getReturnType());
    }

    @Test
    void nonDisassemblerInventoryReportsEmptyState() {
        assertEquals(DisassemblerMenu.STATE_EMPTY, DisassemblerMenu.stateFor(null));
    }
}
