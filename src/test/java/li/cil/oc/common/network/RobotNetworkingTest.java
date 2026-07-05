package li.cil.oc.common.network;

import li.cil.oc.api.machine.Machine;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.menu.RobotMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RobotNetworkingTest {
    @Test
    void applyRobotStartTogglesStoppedMachineFromGui() throws Exception {
        final AtomicBoolean running = new AtomicBoolean(false);
        final AtomicInteger toggles = new AtomicInteger();
        final RobotMenu menu = allocateMenu(23, robot(running, toggles));

        assertTrue(RobotNetworking.applyRobotControl(menu, new RobotControlPayload(23, RackControlPayload.START)));

        assertTrue(running.get());
        assertEquals(1, toggles.get());
    }

    @Test
    void applyRobotStopTogglesRunningMachineFromGui() throws Exception {
        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicInteger toggles = new AtomicInteger();
        final RobotMenu menu = allocateMenu(23, robot(running, toggles));

        assertTrue(RobotNetworking.applyRobotControl(menu, new RobotControlPayload(23, RackControlPayload.STOP)));

        assertFalse(running.get());
        assertEquals(1, toggles.get());
    }

    @Test
    void applyRobotControlRejectsWrongContainer() throws Exception {
        final AtomicBoolean running = new AtomicBoolean(false);
        final AtomicInteger toggles = new AtomicInteger();
        final RobotMenu menu = allocateMenu(23, robot(running, toggles));

        assertFalse(RobotNetworking.applyRobotControl(menu, new RobotControlPayload(24, RackControlPayload.START)));

        assertFalse(running.get());
        assertEquals(0, toggles.get());
    }

    @Test
    void robotStartFailureReportsMachineErrorLikeComputerCase() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/network/RobotNetworking.java"));

        assertTrue(source.contains("ComputerCaseNetworking.startFailureMessage"),
            "Robot GUI start failure must report machine.lastError to the player like computer cases do.");
        assertTrue(source.contains("player.displayClientMessage(message, false)"),
            "Robot GUI start failure must be visible immediately when the power button does nothing.");
    }

    private static RobotMenu allocateMenu(final int containerId, final RobotBlockEntity robot) throws Exception {
        final RobotMenu menu = (RobotMenu) unsafe().allocateInstance(RobotMenu.class);
        setField(menu, AbstractContainerMenu.class, "containerId", containerId);
        setField(menu, RobotMenu.class, "robotInventory", robot);
        return menu;
    }

    private static TestRobotBlockEntity robot(final AtomicBoolean running, final AtomicInteger toggles) throws Exception {
        final TestRobotBlockEntity robot = (TestRobotBlockEntity) unsafe().allocateInstance(TestRobotBlockEntity.class);
        robot.running = running;
        robot.toggles = toggles;
        robot.machine = machine(running);
        return robot;
    }

    private static Machine machine(final AtomicBoolean running) {
        return (Machine) Proxy.newProxyInstance(RobotNetworkingTest.class.getClassLoader(), new Class<?>[]{Machine.class}, (proxy, method, args) -> switch (method.getName()) {
            case "isRunning" -> running.get();
            case "isPaused" -> false;
            case "start" -> running.compareAndSet(false, true);
            case "stop" -> running.compareAndSet(true, false);
            case "pause", "crash", "canInteract", "signal" -> false;
            case "componentCount", "maxComponents" -> 0;
            case "getCostPerTick", "upTime", "cpuTime", "worldTime" -> 0;
            case "components", "methods" -> Map.of();
            case "users" -> new String[0];
            case "lastError", "tmpAddress", "node" -> null;
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

    private static final class TestRobotBlockEntity extends RobotBlockEntity {
        private AtomicBoolean running;
        private AtomicInteger toggles;
        private Machine machine;

        private TestRobotBlockEntity() {
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
