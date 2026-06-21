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

final class RelayMenuShapeTest {
    @Test
    void relayMenuHasClientAndServerConstructors() throws NoSuchMethodException {
        final Constructor<RelayMenu> clientConstructor = RelayMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<RelayMenu> serverConstructor = RelayMenu.class.getConstructor(int.class, Inventory.class, Container.class);
        final Constructor<RelayMenu> dataConstructor = RelayMenu.class.getConstructor(int.class, Inventory.class, Container.class, ContainerData.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(RelayMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class, ContainerData.class}, dataConstructor.getParameterTypes());
    }

    @Test
    void relayMenuSlotCountsAreStable() {
        assertEquals(4, RelayMenu.RELAY_SLOT_COUNT);
        assertEquals(36, RelayMenu.PLAYER_SLOT_COUNT);
        assertEquals(40, RelayMenu.TOTAL_SLOT_COUNT);
        assertEquals(0, RelayMenu.RELAY_MODE_INDEX);
        assertEquals(1, RelayMenu.RELAY_DELAY_INDEX);
        assertEquals(2, RelayMenu.RELAY_QUEUE_SIZE_INDEX);
        assertEquals(3, RelayMenu.RELAY_MAX_QUEUE_SIZE_INDEX);
        assertEquals(4, RelayMenu.RELAY_STRENGTH_INDEX);
        assertEquals(5, RelayMenu.RELAY_REPEATER_INDEX);
        assertEquals(6, RelayMenu.RELAY_DATA_COUNT);
        assertEquals(0, RelayMenu.MODE_WIRED);
        assertEquals(1, RelayMenu.MODE_WIRELESS);
        assertEquals(2, RelayMenu.MODE_LINKED);
    }

    @Test
    void relayMenuExposesStatusAccess() throws NoSuchMethodException {
        final Method mode = RelayMenu.class.getMethod("relayMode");
        final Method delay = RelayMenu.class.getMethod("relayDelay");
        final Method queueSize = RelayMenu.class.getMethod("relayQueueSize");
        final Method maxQueueSize = RelayMenu.class.getMethod("relayMaxQueueSize");
        final Method strength = RelayMenu.class.getMethod("relayStrength");
        final Method repeater = RelayMenu.class.getMethod("relayRepeater");
        final Method modeFor = RelayMenu.class.getMethod("relayModeFor", Container.class);
        final Method delayFor = RelayMenu.class.getMethod("relayDelayFor", Container.class);
        final Method queueSizeFor = RelayMenu.class.getMethod("relayQueueSizeFor", Container.class);
        final Method maxQueueSizeFor = RelayMenu.class.getMethod("relayMaxQueueSizeFor", Container.class);
        final Method strengthFor = RelayMenu.class.getMethod("relayStrengthFor", Container.class);
        final Method repeaterFor = RelayMenu.class.getMethod("relayRepeaterFor", Container.class);

        assertEquals(int.class, mode.getReturnType());
        assertEquals(int.class, delay.getReturnType());
        assertEquals(int.class, queueSize.getReturnType());
        assertEquals(int.class, maxQueueSize.getReturnType());
        assertEquals(int.class, strength.getReturnType());
        assertEquals(int.class, repeater.getReturnType());
        assertEquals(int.class, modeFor.getReturnType());
        assertEquals(int.class, delayFor.getReturnType());
        assertEquals(int.class, queueSizeFor.getReturnType());
        assertEquals(int.class, maxQueueSizeFor.getReturnType());
        assertEquals(int.class, strengthFor.getReturnType());
        assertEquals(int.class, repeaterFor.getReturnType());
    }

    @Test
    void nonRelayInventoryReportsEmptyStatus() {
        assertEquals(RelayMenu.MODE_WIRED, RelayMenu.relayModeFor(null));
        assertEquals(0, RelayMenu.relayDelayFor(null));
        assertEquals(0, RelayMenu.relayQueueSizeFor(null));
        assertEquals(0, RelayMenu.relayMaxQueueSizeFor(null));
        assertEquals(0, RelayMenu.relayStrengthFor(null));
        assertEquals(0, RelayMenu.relayRepeaterFor(null));
    }
}
