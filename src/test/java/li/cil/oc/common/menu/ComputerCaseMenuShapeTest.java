package li.cil.oc.common.menu;

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

final class ComputerCaseMenuShapeTest {
    @Test
    void computerCaseMenuHasClientAndServerConstructors() throws NoSuchMethodException {
        final Constructor<ComputerCaseMenu> clientConstructor = ComputerCaseMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<ComputerCaseMenu> serverConstructor = ComputerCaseMenu.class.getConstructor(int.class, Inventory.class, Container.class);
        final Constructor<ComputerCaseMenu> dataConstructor = ComputerCaseMenu.class.getConstructor(int.class, Inventory.class, Container.class, ContainerData.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(ComputerCaseMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class, ContainerData.class}, dataConstructor.getParameterTypes());
    }

    @Test
    void computerCaseMenuSlotCountsAreStable() {
        assertEquals(7, ComputerCaseMenu.MIN_COMPUTER_SLOT_COUNT);
        assertEquals(10, ComputerCaseMenu.COMPUTER_SLOT_COUNT);
        assertEquals(10, ComputerCaseMenu.MAX_COMPUTER_SLOT_COUNT);
        assertEquals(36, ComputerCaseMenu.PLAYER_SLOT_COUNT);
        assertEquals(46, ComputerCaseMenu.TOTAL_SLOT_COUNT);
        assertEquals(46, ComputerCaseMenu.MAX_TOTAL_SLOT_COUNT);
        assertEquals(5, ComputerCaseMenu.COMPUTER_DATA_COUNT);
        assertEquals(0, ComputerCaseMenu.STATE_EMPTY);
        assertEquals(1, ComputerCaseMenu.STATE_READY);
        assertEquals(2, ComputerCaseMenu.STATE_RUNNING);
        assertEquals(3, ComputerCaseMenu.STATE_INCOMPLETE);
    }

    @Test
    void computerCaseMenuUsesUpstreamTieredSlotPositions() {
        assertEquals(7, ComputerCaseMenu.computerSlotCountForTier(0));
        assertEquals(8, ComputerCaseMenu.computerSlotCountForTier(1));
        assertEquals(10, ComputerCaseMenu.computerSlotCountForTier(2));

        assertEquals(98, ComputerCaseMenu.computerSlotX(0, 0));
        assertEquals(16, ComputerCaseMenu.computerSlotY(0, 0));
        assertEquals(48, ComputerCaseMenu.computerSlotX(0, 6));
        assertEquals(34, ComputerCaseMenu.computerSlotY(0, 6));
        assertEquals(-1, ComputerCaseMenu.computerSlotX(0, 7));
        assertEquals(-1, ComputerCaseMenu.computerSlotY(0, 7));

        assertEquals(142, ComputerCaseMenu.computerSlotX(2, 7));
        assertEquals(52, ComputerCaseMenu.computerSlotY(2, 7));
        assertEquals(48, ComputerCaseMenu.computerSlotX(2, 9));
        assertEquals(34, ComputerCaseMenu.computerSlotY(2, 9));
    }

    @Test
    void computerCaseMenuExposesStatusAccessors() throws NoSuchMethodException {
        final Method state = ComputerCaseMenu.class.getMethod("computerState");
        final Method missing = ComputerCaseMenu.class.getMethod("missingRequirements");
        final Method componentCount = ComputerCaseMenu.class.getMethod("componentCount");
        final Method maxComponents = ComputerCaseMenu.class.getMethod("maxComponents");
        final Method tier = ComputerCaseMenu.class.getMethod("computerTier");
        final Method stateFor = ComputerCaseMenu.class.getMethod("computerStateFor", Container.class);
        final Method missingFor = ComputerCaseMenu.class.getMethod("missingRequirementsFor", Container.class);
        final Method componentCountFor = ComputerCaseMenu.class.getMethod("componentCountFor", Container.class);
        final Method maxComponentsFor = ComputerCaseMenu.class.getMethod("maxComponentsFor", Container.class);
        final Method tierFor = ComputerCaseMenu.class.getMethod("computerTierFor", Container.class);

        assertEquals(int.class, state.getReturnType());
        assertEquals(int.class, missing.getReturnType());
        assertEquals(int.class, componentCount.getReturnType());
        assertEquals(int.class, maxComponents.getReturnType());
        assertEquals(int.class, tier.getReturnType());
        assertEquals(int.class, stateFor.getReturnType());
        assertEquals(int.class, missingFor.getReturnType());
        assertEquals(int.class, componentCountFor.getReturnType());
        assertEquals(int.class, maxComponentsFor.getReturnType());
        assertEquals(int.class, tierFor.getReturnType());
    }

    @Test
    void nonComputerInventoryReportsEmptyStatus() {
        assertEquals(ComputerCaseMenu.STATE_EMPTY, ComputerCaseMenu.computerStateFor(null));
        assertEquals(0, ComputerCaseMenu.missingRequirementsFor(null));
        assertEquals(0, ComputerCaseMenu.componentCountFor(null));
        assertEquals(0, ComputerCaseMenu.maxComponentsFor(null));
        assertEquals(0, ComputerCaseMenu.computerTierFor(null));
    }
}
