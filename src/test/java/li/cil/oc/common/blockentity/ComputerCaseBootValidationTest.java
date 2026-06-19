package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Machine;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComputerCaseBootValidationTest {
    private static final String EEPROM = "eeprom";

    @Test
    void tierOneCaseCanBootWithCpuTwoMemorySticksHardDriveAndEeprom() {
        assertTrue(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.Memory, Slot.Memory, Slot.HDD, EEPROM));
    }

    @Test
    void tierOneCaseCannotBootWithoutCpu() {
        assertFalse(ComputerCaseBlockEntity.hasRequiredComponents(Slot.None, Slot.Memory, Slot.Memory, Slot.HDD, EEPROM));
    }

    @Test
    void tierOneCaseCannotBootWithoutBothMemorySticks() {
        assertFalse(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.Memory, Slot.None, Slot.HDD, EEPROM));
        assertFalse(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.None, Slot.Memory, Slot.HDD, EEPROM));
    }

    @Test
    void tierOneCaseCannotBootWithoutHardDiskDrive() {
        assertFalse(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.Memory, Slot.Memory, Slot.None, EEPROM));
    }

    @Test
    void tierOneCaseCannotBootWithoutEeprom() {
        assertFalse(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.Memory, Slot.Memory, Slot.HDD, Slot.None));
    }

    @Test
    void tickUpdatesHostedMachineWhenItCanUpdate() {
        final int[] updates = {0};
        ComputerCaseBlockEntity.tickHostedMachine(machine(true, updates));

        assertEquals(1, updates[0]);
    }

    @Test
    void tickSkipsHostedMachineWhenItCannotUpdate() {
        final int[] updates = {0};
        ComputerCaseBlockEntity.tickHostedMachine(machine(false, updates));

        assertEquals(0, updates[0]);
    }

    @Test
    void inventoryChangeNotifiesHostedMachineAboutHardwareChanges() {
        final int[] refreshes = {0};
        ComputerCaseBlockEntity.notifyHardwareChanged(machine(true, new int[1], refreshes));

        assertEquals(1, refreshes[0]);
    }

    @Test
    void clearingInventorySlotsKeepsFixedContainerSize() {
        final List<String> items = new ArrayList<>(List.of("cpu", "memory", "disk"));

        ComputerCaseBlockEntity.fillExistingSlots(items, "empty");

        assertEquals(List.of("empty", "empty", "empty"), items);
        assertEquals(3, items.size());
    }

    private static Machine machine(final boolean canUpdate, final int[] updates) {
        return machine(canUpdate, updates, new int[1]);
    }

    private static Machine machine(final boolean canUpdate, final int[] updates, final int[] refreshes) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "canUpdate" -> canUpdate;
                case "update" -> {
                    updates[0]++;
                    yield null;
                }
                case "onHostChanged" -> {
                    refreshes[0]++;
                    yield null;
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Object defaultValue(final Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0D;
        }
        if (type == void.class) {
            return null;
        }
        return null;
    }
}
