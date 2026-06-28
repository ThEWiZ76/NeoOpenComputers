package li.cil.oc.common.network;

import li.cil.oc.api.machine.Machine;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.menu.ComputerCaseMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComputerCaseNetworkingTest {
    @Test
    void applyComputerCaseStartTogglesStoppedComputerLikeUpstreamButton() throws Exception {
        final AtomicBoolean running = new AtomicBoolean(false);
        final AtomicInteger toggles = new AtomicInteger();
        final ComputerCaseMenu menu = allocateMenu(17, computer(running, toggles));

        assertTrue(ComputerCaseNetworking.applyComputerCaseControl(menu, new ComputerCaseControlPayload(17, RackControlPayload.START)));

        assertTrue(running.get());
        assertEquals(1, toggles.get());
    }

    @Test
    void applyComputerCaseStopTogglesRunningComputerLikeUpstreamButton() throws Exception {
        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicInteger toggles = new AtomicInteger();
        final ComputerCaseMenu menu = allocateMenu(17, computer(running, toggles));

        assertTrue(ComputerCaseNetworking.applyComputerCaseControl(menu, new ComputerCaseControlPayload(17, RackControlPayload.STOP)));

        assertFalse(running.get());
        assertEquals(1, toggles.get());
    }

    @Test
    void applyComputerCaseControlRejectsWrongContainer() throws Exception {
        final AtomicBoolean running = new AtomicBoolean(false);
        final AtomicInteger toggles = new AtomicInteger();
        final ComputerCaseMenu menu = allocateMenu(17, computer(running, toggles));

        assertFalse(ComputerCaseNetworking.applyComputerCaseControl(menu, new ComputerCaseControlPayload(18, RackControlPayload.START)));

        assertFalse(running.get());
        assertEquals(0, toggles.get());
    }

    @Test
    void computerCaseStartErrorMessageShowsLastErrorLikeUpstreamAnalyzerFeedback() {
        assertEquals("Last error: no bios found", ComputerCaseNetworking.startErrorMessage(machine(new AtomicBoolean(false), "no bios found")).getString());
        assertNull(ComputerCaseNetworking.startErrorMessage(machine(new AtomicBoolean(false), null)));
        assertNull(ComputerCaseNetworking.startErrorMessage(machine(new AtomicBoolean(false), "")));
    }

    @Test
    void failedComputerCaseStartStillReportsLastError() {
        assertEquals(
            "Last error: missing required components",
            ComputerCaseNetworking.startFailureMessage(
                machine(new AtomicBoolean(false), "missing required components"),
                false,
                RackControlPayload.START).getString());
    }

    @Test
    void runningComputerCaseStartDoesNotRepeatOldErrorMessage() {
        assertNull(ComputerCaseNetworking.startFailureMessage(
            machine(new AtomicBoolean(true), "old error"),
            true,
            RackControlPayload.START));
    }

    private static ComputerCaseMenu allocateMenu(final int containerId, final ComputerCaseBlockEntity computer) throws Exception {
        final ComputerCaseMenu menu = (ComputerCaseMenu) unsafe().allocateInstance(ComputerCaseMenu.class);
        setField(menu, AbstractContainerMenu.class, "containerId", containerId);
        setField(menu, ComputerCaseMenu.class, "computerInventory", computer);
        return menu;
    }

    private static TestComputerCaseBlockEntity computer(final AtomicBoolean running, final AtomicInteger toggles) throws Exception {
        final TestComputerCaseBlockEntity computer = (TestComputerCaseBlockEntity) unsafe().allocateInstance(TestComputerCaseBlockEntity.class);
        computer.running = running;
        computer.toggles = toggles;
        computer.machine = machine(running);
        return computer;
    }

    private static Machine machine(final AtomicBoolean running) {
        return machine(running, null);
    }

    private static Machine machine(final AtomicBoolean running, final String lastError) {
        return (Machine) Proxy.newProxyInstance(ComputerCaseNetworkingTest.class.getClassLoader(), new Class<?>[]{Machine.class}, (proxy, method, args) -> switch (method.getName()) {
            case "isRunning" -> running.get();
            case "isPaused" -> false;
            case "start" -> running.compareAndSet(false, true);
            case "stop" -> running.compareAndSet(true, false);
            case "pause", "crash", "canInteract", "signal" -> false;
            case "componentCount", "maxComponents" -> 0;
            case "getCostPerTick", "upTime", "cpuTime", "worldTime" -> 0;
            case "components", "methods" -> Map.of();
            case "users" -> new String[0];
            case "lastError" -> lastError;
            case "tmpAddress", "node" -> null;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static Object defaultValue(final Class<?> type) {
        if (type == Void.TYPE) {
            return null;
        }
        if (type == Boolean.TYPE) {
            return false;
        }
        if (type == Byte.TYPE) {
            return (byte) 0;
        }
        if (type == Short.TYPE) {
            return (short) 0;
        }
        if (type == Integer.TYPE) {
            return 0;
        }
        if (type == Long.TYPE) {
            return 0L;
        }
        if (type == Float.TYPE) {
            return 0F;
        }
        if (type == Double.TYPE) {
            return 0D;
        }
        if (type == Character.TYPE) {
            return (char) 0;
        }
        return null;
    }

    private static Unsafe unsafe() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (Unsafe) unsafeField.get(null);
    }

    private static void setField(final Object target, final Class<?> owner, final String name, final Object value) throws Exception {
        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setField(final Object target, final Class<?> owner, final String name, final int value) throws Exception {
        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.setInt(target, value);
    }

    private static final class TestComputerCaseBlockEntity extends ComputerCaseBlockEntity {
        private AtomicBoolean running;
        private AtomicInteger toggles;
        private Machine machine;

        private TestComputerCaseBlockEntity() {
            super(null, null);
        }

        @Override
        public Machine machine() {
            return machine;
        }

        @Override
        public boolean toggleMachine() {
            toggles.incrementAndGet();
            return running.get() ? machine.stop() : machine.start();
        }
    }
}
