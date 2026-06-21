package li.cil.oc.common.menu;

import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.common.component.ServerRackMountableEnvironment;
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

final class ServerRackMenuShapeTest {
    @Test
    void serverRackMenuHasClientAndServerConstructors() throws NoSuchMethodException {
        final Constructor<ServerRackMenu> clientConstructor = ServerRackMenu.class.getConstructor(int.class, Inventory.class);
        final Constructor<ServerRackMenu> serverConstructor = ServerRackMenu.class.getConstructor(int.class, Inventory.class, Container.class);
        final Constructor<ServerRackMenu> dataConstructor = ServerRackMenu.class.getConstructor(int.class, Inventory.class, Container.class, ContainerData.class);

        assertTrue(AbstractContainerMenu.class.isAssignableFrom(ServerRackMenu.class));
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class}, clientConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class}, serverConstructor.getParameterTypes());
        assertArrayEquals(new Class<?>[]{int.class, Inventory.class, Container.class, ContainerData.class}, dataConstructor.getParameterTypes());
    }

    @Test
    void serverRackMenuSlotCountsAreStable() {
        assertEquals(17, ServerRackMenu.SERVER_SLOT_COUNT);
        assertEquals(36, ServerRackMenu.PLAYER_SLOT_COUNT);
        assertEquals(53, ServerRackMenu.TOTAL_SLOT_COUNT);
        assertEquals(34, ServerRackMenu.SERVER_DATA_COUNT);
    }

    @Test
    void serverRackMenuExposesServerInventoryTarget() throws NoSuchMethodException {
        assertEquals(Container.class, ServerRackMenu.class.getMethod("serverInventory").getReturnType());
    }

    @Test
    void serverRackMenuExposesSlotKindAccess() throws NoSuchMethodException {
        final Method kind = ServerRackMenu.class.getMethod("slotKind", int.class);
        final Method tier = ServerRackMenu.class.getMethod("slotTierLimit", int.class);

        assertEquals(int.class, kind.getReturnType());
        assertEquals(int.class, tier.getReturnType());
    }

    @Test
    void slotKindCodeMapsServerComponentTypes() {
        assertEquals(ServerRackMenu.SLOT_KIND_NONE, ServerRackMenu.slotKindCode(Slot.None));
        assertEquals(ServerRackMenu.SLOT_KIND_CARD, ServerRackMenu.slotKindCode(Slot.Card));
        assertEquals(ServerRackMenu.SLOT_KIND_CPU, ServerRackMenu.slotKindCode(Slot.CPU));
        assertEquals(ServerRackMenu.SLOT_KIND_COMPONENT_BUS, ServerRackMenu.slotKindCode(Slot.ComponentBus));
        assertEquals(ServerRackMenu.SLOT_KIND_MEMORY, ServerRackMenu.slotKindCode(Slot.Memory));
        assertEquals(ServerRackMenu.SLOT_KIND_HDD, ServerRackMenu.slotKindCode(Slot.HDD));
        assertEquals(ServerRackMenu.SLOT_KIND_EEPROM, ServerRackMenu.slotKindCode(ServerRackMountableEnvironment.SLOT_TYPE_EEPROM));
    }

    @Test
    void slotKindForTierUsesServerTierLayout() {
        assertEquals(ServerRackMenu.SLOT_KIND_CPU, ServerRackMenu.slotKindForTier(1, 2));
        assertEquals(ServerRackMenu.SLOT_KIND_EEPROM, ServerRackMenu.slotKindForTier(1, 12));
        assertEquals(ServerRackMenu.SLOT_KIND_NONE, ServerRackMenu.slotKindForTier(1, 13));
    }

    @Test
    void slotTierLimitForTierUsesServerTierLayout() {
        assertEquals(2, ServerRackMenu.slotTierLimitForTier(1, 2));
        assertEquals(Integer.MAX_VALUE, ServerRackMenu.slotTierLimitForTier(1, 12));
        assertEquals(-1, ServerRackMenu.slotTierLimitForTier(1, 13));
    }
}
