package li.cil.oc.common.network;

import li.cil.oc.api.machine.Machine;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import li.cil.oc.common.menu.MicrocontrollerMenu;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MicrocontrollerNetworkingTest {
    @Test
    void applyMicrocontrollerStartTogglesStoppedMachineFromGui() throws Exception {
        final AtomicBoolean running = new AtomicBoolean(false);
        final AtomicInteger toggles = new AtomicInteger();
        final MicrocontrollerMenu menu = allocateMenu(23, microcontroller(running, toggles));

        assertTrue(MicrocontrollerNetworking.applyMicrocontrollerControl(menu, new MicrocontrollerControlPayload(23, RackControlPayload.START)));

        assertTrue(running.get());
        assertEquals(1, toggles.get());
    }

    @Test
    void applyMicrocontrollerStopTogglesRunningMachineFromGui() throws Exception {
        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicInteger toggles = new AtomicInteger();
        final MicrocontrollerMenu menu = allocateMenu(23, microcontroller(running, toggles));

        assertTrue(MicrocontrollerNetworking.applyMicrocontrollerControl(menu, new MicrocontrollerControlPayload(23, RackControlPayload.STOP)));

        assertFalse(running.get());
        assertEquals(1, toggles.get());
    }

    @Test
    void applyMicrocontrollerControlRejectsWrongContainer() throws Exception {
        final AtomicBoolean running = new AtomicBoolean(false);
        final AtomicInteger toggles = new AtomicInteger();
        final MicrocontrollerMenu menu = allocateMenu(23, microcontroller(running, toggles));

        assertFalse(MicrocontrollerNetworking.applyMicrocontrollerControl(menu, new MicrocontrollerControlPayload(24, RackControlPayload.START)));

        assertFalse(running.get());
        assertEquals(0, toggles.get());
    }

    private static MicrocontrollerMenu allocateMenu(final int containerId, final MicrocontrollerBlockEntity microcontroller) throws Exception {
        final MicrocontrollerMenu menu = (MicrocontrollerMenu) unsafe().allocateInstance(MicrocontrollerMenu.class);
        setField(menu, AbstractContainerMenu.class, "containerId", containerId);
        setField(menu, MicrocontrollerMenu.class, "microcontrollerInventory", microcontroller);
        return menu;
    }

    private static TestMicrocontrollerBlockEntity microcontroller(final AtomicBoolean running, final AtomicInteger toggles) throws Exception {
        final TestMicrocontrollerBlockEntity microcontroller = (TestMicrocontrollerBlockEntity) unsafe().allocateInstance(TestMicrocontrollerBlockEntity.class);
        microcontroller.running = running;
        microcontroller.toggles = toggles;
        microcontroller.machine = machine(running);
        return microcontroller;
    }

    private static Machine machine(final AtomicBoolean running) {
        return (Machine) Proxy.newProxyInstance(MicrocontrollerNetworkingTest.class.getClassLoader(), new Class<?>[]{Machine.class}, (proxy, method, args) -> switch (method.getName()) {
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

    private static final class TestMicrocontrollerBlockEntity extends MicrocontrollerBlockEntity {
        private AtomicBoolean running;
        private AtomicInteger toggles;
        private Machine machine;

        private TestMicrocontrollerBlockEntity() {
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
