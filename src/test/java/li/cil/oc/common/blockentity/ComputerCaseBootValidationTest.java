package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Node;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    void tierOneCaseCanBootWithoutHardDiskForExternalBootMedia() {
        assertTrue(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.Memory, Slot.Memory, Slot.None, EEPROM));
    }

    @Test
    void tierOneCaseCanBootWithOneMemoryStick() {
        assertTrue(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.Memory, Slot.None, Slot.HDD, EEPROM));
        assertTrue(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.None, Slot.Memory, Slot.HDD, EEPROM));
    }

    @Test
    void tierOneCaseCannotBootWithoutCpu() {
        assertFalse(ComputerCaseBlockEntity.hasRequiredComponents(Slot.None, Slot.Memory, Slot.Memory, Slot.HDD, EEPROM));
    }

    @Test
    void tierOneCaseCannotBootWithoutAnyMemoryStick() {
        assertFalse(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.None, Slot.None, Slot.HDD, EEPROM));
    }

    @Test
    void tierOneCaseDoesNotRequireInternalHardDiskDrive() {
        assertTrue(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.Memory, Slot.Memory, Slot.None, EEPROM));
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
    void cpuRemovalStopsHostedMachineLikeUpstream() {
        final int[] stops = {0};

        ComputerCaseBlockEntity.notifyItemRemoved(machine(true, new int[1], new int[1], stops), 0, ComputerCaseBlockEntity.SLOT_CPU);

        assertEquals(1, stops[0]);
    }

    @Test
    void nonCpuRemovalDoesNotStopHostedMachine() {
        final int[] stops = {0};

        ComputerCaseBlockEntity.notifyItemRemoved(machine(true, new int[1], new int[1], stops), 0, ComputerCaseBlockEntity.SLOT_MEMORY_0);

        assertEquals(0, stops[0]);
    }

    @Test
    void shiftUseStartsStoppedComputerCaseLikeUpstream() {
        final int[] starts = {0};
        final int[] stops = {0};

        ComputerCaseBlockEntity.activateMachineFromBlockUse(machine(false, new int[1], new int[1], stops, starts));

        assertEquals(1, starts[0]);
        assertEquals(0, stops[0]);
    }

    @Test
    void shiftUseRunningComputerCaseDoesNotStopLikeUpstream() {
        final int[] starts = {0};
        final int[] stops = {0};

        ComputerCaseBlockEntity.activateMachineFromBlockUse(machine(true, new int[1], new int[1], stops, starts));

        assertEquals(0, starts[0]);
        assertEquals(0, stops[0]);
    }

    @Test
    void reportsDelayedBootCrashAfterMachineWasRunning() {
        assertTrue(ComputerCaseBlockEntity.shouldReportMachineError(true, false, "boot:60 no bootable medium found"));
        assertEquals(
            "Computer error: boot:60 no bootable medium found",
            ComputerCaseBlockEntity.machineErrorMessage("boot:60 no bootable medium found").getString());
    }

    @Test
    void reportsImmediateBlockUseStartFailure() {
        assertEquals(
            "Computer error: missing required components",
            ComputerCaseBlockEntity.blockUseStartFailureMessage(machine(false, "missing required components")).getString());
        assertEquals(
            "Computer error: boot failed",
            ComputerCaseBlockEntity.blockUseStartFailureMessage(machine(false, "boot failed\ntrace")).getString());
    }

    @Test
    void doesNotReportBlockUseStartFailureWithoutError() {
        assertEquals(null, ComputerCaseBlockEntity.blockUseStartFailureMessage(machine(false, (String) null)));
        assertEquals(null, ComputerCaseBlockEntity.blockUseStartFailureMessage(machine(false, "")));
    }

    @Test
    void doesNotReportMachineErrorWithoutNewStoppedError() {
        assertFalse(ComputerCaseBlockEntity.shouldReportMachineError(false, false, "boot failed"));
        assertFalse(ComputerCaseBlockEntity.shouldReportMachineError(true, true, "boot failed"));
        assertFalse(ComputerCaseBlockEntity.shouldReportMachineError(true, false, null));
        assertFalse(ComputerCaseBlockEntity.shouldReportMachineError(true, false, ""));
    }

    @Test
    void clearingInventorySlotsKeepsFixedContainerSize() {
        final List<String> items = new ArrayList<>(List.of("cpu", "memory", "disk"));

        ComputerCaseBlockEntity.fillExistingSlots(items, "empty");

        assertEquals(List.of("empty", "empty", "empty"), items);
        assertEquals(3, items.size());
    }

    @Test
    void componentSlotMapTracksConnectedNodeAddresses() {
        final Map<String, Integer> slots = new HashMap<>();

        ComputerCaseBlockEntity.recordComponentSlot(slots, node("component-address"), 3);

        assertEquals(3, ComputerCaseBlockEntity.componentSlot(slots, "component-address"));
        assertEquals(-1, ComputerCaseBlockEntity.componentSlot(slots, "missing"));

        ComputerCaseBlockEntity.removeComponentSlot(slots, node("component-address"));

        assertEquals(-1, ComputerCaseBlockEntity.componentSlot(slots, "component-address"));
    }

    @Test
    void exposesDeviceInfoMetadata() {
        Map<String, String> metadata = ComputerCaseBlockEntity.deviceInfo(7);

        assertEquals(DeviceInfo.DeviceClass.System, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Computer", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("Blocker", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("7", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
    }

    private static Machine machine(final boolean canUpdate, final int[] updates) {
        return machine(canUpdate, updates, new int[1]);
    }

    private static Machine machine(final boolean canUpdate, final int[] updates, final int[] refreshes) {
        return machine(canUpdate, updates, refreshes, new int[1]);
    }

    private static Machine machine(final boolean canUpdate, final int[] updates, final int[] refreshes, final int[] stops) {
        return machine(canUpdate, updates, refreshes, stops, new int[1]);
    }

    private static Machine machine(final boolean canUpdate, final int[] updates, final int[] refreshes, final int[] stops, final int[] starts) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "canUpdate" -> canUpdate;
                case "isRunning" -> canUpdate;
                case "start" -> {
                    starts[0]++;
                    yield true;
                }
                case "update" -> {
                    updates[0]++;
                    yield null;
                }
                case "onHostChanged" -> {
                    refreshes[0]++;
                    yield null;
                }
                case "stop" -> {
                    stops[0]++;
                    yield true;
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machine(final boolean running, final String lastError) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "isRunning", "isPaused" -> running;
                case "lastError" -> lastError;
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

    private static Node node(final String address) {
        return (Node) Proxy.newProxyInstance(
            Node.class.getClassLoader(),
            new Class<?>[]{Node.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "address" -> address;
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-node";
                default -> defaultValue(method.getReturnType());
            });
    }
}
