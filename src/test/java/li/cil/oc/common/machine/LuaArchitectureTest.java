package li.cil.oc.common.machine;

import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.common.ItemRegistry;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LuaArchitectureTest {
    @Test
    void executesConfiguredLuaChunkOnce() {
        LuaArchitecture architecture = new LuaArchitecture("counter = (counter or 0) + 1");

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(1, architecture.globalInteger("counter"));
    }

    @Test
    void reportsLuaRuntimeErrorsAsExecutionErrors() {
        LuaArchitecture architecture = new LuaArchitecture("error('boot failed')");

        assertTrue(architecture.initialize());
        ExecutionResult result = architecture.runThreaded(false);

        ExecutionResult.Error error = assertInstanceOf(ExecutionResult.Error.class, result);
        assertTrue(error.message.contains("boot failed"));
    }

    @Test
    void readsBootSourceFromEepromDataTag() {
        CompoundTag data = new CompoundTag();
        data.putByteArray(ItemRegistry.EEPROM_CODE_TAG, "counter = 7".getBytes(StandardCharsets.UTF_8));
        LuaArchitecture architecture = new LuaArchitecture();

        architecture.configureBootSource(data);
        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(7, architecture.globalInteger("counter"));
    }

    @Test
    void exposesComputerUptimeToLua() {
        LuaArchitecture architecture = new LuaArchitecture("seconds = computer.uptime()");
        architecture.bind(machineWithUptime(12.5D));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(12.5D, architecture.globalDouble("seconds"), 0.000_001D);
    }

    @Test
    void exposesComputerPullSignalToLua() {
        LuaArchitecture architecture = new LuaArchitecture("name, value = computer.pullSignal()");
        architecture.bind(machineWithSignals(new TestSignal("event", new Object[]{"payload"})));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("event", architecture.globalString("name"));
        assertEquals("payload", architecture.globalString("value"));
    }

    @Test
    void exposesComputerAddressesToLua() {
        LuaArchitecture architecture = new LuaArchitecture("address = computer.address(); tmp = computer.tmpAddress()");
        architecture.bind(machineWithAddress("machine-address"));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("machine-address", architecture.globalString("address"));
        assertEquals("machine-address", architecture.globalString("tmp"));
    }

    @Test
    void exposesComputerShutdownToLua() {
        LuaArchitecture architecture = new LuaArchitecture("computer.shutdown()");

        assertTrue(architecture.initialize());
        ExecutionResult.Shutdown result = assertInstanceOf(ExecutionResult.Shutdown.class, architecture.runThreaded(false));

        assertEquals(false, result.reboot);
    }

    @Test
    void exposesComputerRebootToLua() {
        LuaArchitecture architecture = new LuaArchitecture("computer.shutdown(true)");

        assertTrue(architecture.initialize());
        ExecutionResult.Shutdown result = assertInstanceOf(ExecutionResult.Shutdown.class, architecture.runThreaded(false));

        assertEquals(true, result.reboot);
    }

    @Test
    void exposesComputerBeepToLua() {
        String[] beepPattern = {null};
        LuaArchitecture architecture = new LuaArchitecture("computer.beep('..-')");
        architecture.bind(machineWithBeep(beepPattern));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("..-", beepPattern[0]);
    }

    @Test
    void exposesComponentListToLua() {
        LuaArchitecture architecture = new LuaArchitecture("components = component.list(); fs = components['fs-address']");
        architecture.bind(machineWithComponents(Map.of("fs-address", "filesystem")));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("filesystem", architecture.globalString("fs"));
    }

    @Test
    void exposesComponentInvokeToLua() {
        LuaArchitecture architecture = new LuaArchitecture("result = component.invoke('fs-address', 'label', 'arg')");
        architecture.bind(machineWithInvokeResult(new Object[]{"tmp"}));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("tmp", architecture.globalString("result"));
    }

    @Test
    void exposesComponentProxyToLua() {
        LuaArchitecture architecture = new LuaArchitecture("fs = component.proxy('fs-address'); result = fs.label('arg')");
        architecture.bind(machineWithInvokeResult(new Object[]{"tmp"}));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("tmp", architecture.globalString("result"));
    }

    private static Machine machineWithUptime(final double uptime) {
        return machine(new ArrayDeque<>(), uptime);
    }

    private static Machine machineWithSignals(final Signal... signals) {
        return machine(new ArrayDeque<>(Arrays.asList(signals)), 0D);
    }

    private static Machine machineWithAddress(final String address) {
        return machine(new ArrayDeque<>(), 0D, address);
    }

    private static Machine machineWithBeep(final String[] beepPattern) {
        return machine(new ArrayDeque<>(), 0D, null, beepPattern);
    }

    private static Machine machineWithComponents(final Map<String, String> components) {
        return machine(new ArrayDeque<>(), 0D, null, null, components, new Object[0]);
    }

    private static Machine machineWithInvokeResult(final Object[] invokeResult) {
        return machine(new ArrayDeque<>(), 0D, null, null, Map.of(), invokeResult);
    }

    private static Machine machine(final Queue<Signal> signals, final double uptime) {
        return machine(signals, uptime, null);
    }

    private static Machine machine(final Queue<Signal> signals, final double uptime, final String address) {
        return machine(signals, uptime, address, null);
    }

    private static Machine machine(final Queue<Signal> signals, final double uptime, final String address, final String[] beepPattern) {
        return machine(signals, uptime, address, beepPattern, Map.of(), new Object[0]);
    }

    private static Machine machine(
        final Queue<Signal> signals,
        final double uptime,
        final String address,
        final String[] beepPattern,
        final Map<String, String> components,
        final Object[] invokeResult
    ) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "upTime" -> uptime;
                case "popSignal" -> signals.poll();
                case "tmpAddress" -> address;
                case "components" -> components;
                case "invoke" -> invokeResult;
                case "beep" -> {
                    if (beepPattern != null && args.length == 1) {
                        beepPattern[0] = (String) args[0];
                    }
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
        return null;
    }

    private record TestSignal(String name, Object[] args) implements Signal {
    }
}
