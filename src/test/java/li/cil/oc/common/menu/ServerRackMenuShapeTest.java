package li.cil.oc.common.menu;

import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.common.component.ServerRackMountableEnvironment;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

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
        assertEquals(38, ServerRackMenu.SERVER_DATA_COUNT);
        assertEquals(0, ServerRackMenu.STATE_EMPTY);
        assertEquals(1, ServerRackMenu.STATE_READY);
        assertEquals(2, ServerRackMenu.STATE_RUNNING);
        assertEquals(3, ServerRackMenu.STATE_INCOMPLETE);
        assertEquals(ServerRackMountableEnvironment.MISSING_CPU, ServerRackMenu.MISSING_CPU);
        assertEquals(ServerRackMountableEnvironment.MISSING_MEMORY, ServerRackMenu.MISSING_MEMORY);
        assertEquals(ServerRackMountableEnvironment.MISSING_EEPROM, ServerRackMenu.MISSING_EEPROM);
    }

    @Test
    void serverRackMenuExposesServerInventoryTarget() throws NoSuchMethodException {
        assertEquals(Container.class, ServerRackMenu.class.getMethod("serverInventory").getReturnType());
    }

    @Test
    void serverRackMenuExposesSlotKindAccess() throws NoSuchMethodException {
        final Method kind = ServerRackMenu.class.getMethod("slotKind", int.class);
        final Method tier = ServerRackMenu.class.getMethod("slotTierLimit", int.class);
        final Method state = ServerRackMenu.class.getMethod("serverState");
        final Method missing = ServerRackMenu.class.getMethod("missingRequirements");
        final Method componentCount = ServerRackMenu.class.getMethod("componentCount");
        final Method maxComponents = ServerRackMenu.class.getMethod("maxComponents");
        final Method stateFor = ServerRackMenu.class.getMethod("serverStateFor", Container.class);
        final Method missingFor = ServerRackMenu.class.getMethod("missingRequirementsFor", Container.class);
        final Method componentCountFor = ServerRackMenu.class.getMethod("componentCountFor", Container.class);
        final Method maxComponentsFor = ServerRackMenu.class.getMethod("maxComponentsFor", Container.class);

        assertEquals(int.class, kind.getReturnType());
        assertEquals(int.class, tier.getReturnType());
        assertEquals(int.class, state.getReturnType());
        assertEquals(int.class, missing.getReturnType());
        assertEquals(int.class, componentCount.getReturnType());
        assertEquals(int.class, maxComponents.getReturnType());
        assertEquals(int.class, stateFor.getReturnType());
        assertEquals(int.class, missingFor.getReturnType());
        assertEquals(int.class, componentCountFor.getReturnType());
        assertEquals(int.class, maxComponentsFor.getReturnType());
    }

    @Test
    void nonServerInventoryReportsEmptyStatus() {
        assertEquals(ServerRackMenu.STATE_EMPTY, ServerRackMenu.serverStateFor(null));
        assertEquals(0, ServerRackMenu.missingRequirementsFor(null));
        assertEquals(0, ServerRackMenu.componentCountFor(null));
        assertEquals(0, ServerRackMenu.maxComponentsFor(null));
    }

    @Test
    void serverInventoryReportsMachineComponentCapacity() throws ReflectiveOperationException {
        final Container server = fakeServerWithMachineCapacity(5, 12);

        assertEquals(5, ServerRackMenu.componentCountFor(server));
        assertEquals(12, ServerRackMenu.maxComponentsFor(server));
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

    private static Container fakeServerWithMachineCapacity(final int componentCount, final int maxComponents) throws ReflectiveOperationException {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final Unsafe unsafe = (Unsafe) unsafeField.get(null);
        final ServerRackMountableEnvironment server = (ServerRackMountableEnvironment) unsafe.allocateInstance(ServerRackMountableEnvironment.class);
        final Machine machine = (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> {
                if ("componentCount".equals(method.getName())) {
                    return componentCount;
                }
                if ("maxComponents".equals(method.getName())) {
                    return maxComponents;
                }
                final Class<?> returnType = method.getReturnType();
                if (returnType == boolean.class) {
                    return false;
                }
                if (returnType == int.class || returnType == long.class || returnType == short.class || returnType == byte.class) {
                    return 0;
                }
                if (returnType == double.class || returnType == float.class) {
                    return 0D;
                }
                return null;
            });
        final Field machineField = ServerRackMountableEnvironment.class.getDeclaredField("machine");
        unsafe.putObject(server, unsafe.objectFieldOffset(machineField), machine);
        return server;
    }
}
