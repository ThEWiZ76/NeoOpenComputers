package li.cil.oc.common.menu;

import li.cil.oc.common.blockentity.AssemblerBlockEntity;
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

final class AssemblerMenuShapeTest {
    @Test
    void assemblerMenuHasClientAndServerConstructors() throws NoSuchMethodException {
        final Constructor<AssemblerMenu> clientConstructor = AssemblerMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<AssemblerMenu> serverConstructor = AssemblerMenu.class.getConstructor(int.class, Inventory.class, Container.class);
        final Constructor<AssemblerMenu> dataConstructor = AssemblerMenu.class.getConstructor(int.class, Inventory.class, Container.class, ContainerData.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(AssemblerMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class, ContainerData.class}, dataConstructor.getParameterTypes());
    }

    @Test
    void assemblerMenuSlotCountsAreStable() {
        assertEquals(AssemblerBlockEntity.CONTAINER_SIZE, AssemblerMenu.ASSEMBLER_SLOT_COUNT);
        assertEquals(36, AssemblerMenu.PLAYER_SLOT_COUNT);
        assertEquals(58, AssemblerMenu.TOTAL_SLOT_COUNT);
        assertEquals(2, AssemblerMenu.ASSEMBLER_DATA_COUNT);
        assertEquals(0, AssemblerMenu.STATE_IDLE);
        assertEquals(1, AssemblerMenu.STATE_READY);
        assertEquals(2, AssemblerMenu.STATE_BUSY);
    }

    @Test
    void assemblerMenuExposesStatusData() throws NoSuchMethodException {
        final Method assemblyState = AssemblerMenu.class.getMethod("assemblyState");
        final Method assemblyProgress = AssemblerMenu.class.getMethod("assemblyProgress");
        final Method stateFor = AssemblerMenu.class.getMethod("stateFor", Container.class);
        final Method progressFor = AssemblerMenu.class.getMethod("progressFor", Container.class);

        assertEquals(int.class, assemblyState.getReturnType());
        assertEquals(int.class, assemblyProgress.getReturnType());
        assertEquals(int.class, stateFor.getReturnType());
        assertEquals(int.class, progressFor.getReturnType());
    }

    @Test
    void nonAssemblerInventoryReportsIdleState() {
        assertEquals(AssemblerMenu.STATE_IDLE, AssemblerMenu.stateFor(null));
        assertEquals(0, AssemblerMenu.progressFor(null));
    }
}
