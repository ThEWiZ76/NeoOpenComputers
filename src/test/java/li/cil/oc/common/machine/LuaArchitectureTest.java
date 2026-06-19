package li.cil.oc.common.machine;

import li.cil.oc.api.API;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.DriverRegistry;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

final class LuaArchitectureTest {
    @AfterEach
    void resetApi() {
        API.driver = null;
        API.fileSystem = null;
        API.items = null;
        API.machine = null;
        API.manual = null;
        API.nanomachines = null;
        API.network = null;
        li.cil.oc.api.Machine.LuaArchitecture = null;
    }

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
    void exposesComputerPushSignalToLua() {
        String[] signalName = {null};
        Object[][] signalArguments = {null};
        LuaArchitecture architecture = new LuaArchitecture("result = computer.pushSignal('event', 'payload', 7, true)");
        architecture.bind(machineWithSignalCapture(signalName, signalArguments));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(true, architecture.globalBoolean("result"));
        assertEquals("event", signalName[0]);
        assertArrayEquals(new Object[]{"payload", 7D, true}, signalArguments[0]);
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
    void exposesComputerMemoryToLua() {
        DriverRegistry drivers = new DriverRegistry();
        drivers.add(new TestMemoryDriver(192D));
        API.driver = drivers;
        LuaArchitecture architecture = new LuaArchitecture("free = computer.freeMemory(); total = computer.totalMemory()");

        assertTrue(architecture.recomputeMemory(Collections.singletonList(null)));
        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(98_304D, architecture.globalDouble("free"), 0.000_001D);
        assertEquals(196_608D, architecture.globalDouble("total"), 0.000_001D);
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
    void exposesComputerUsersToLua() {
        String[] added = {null};
        String[] removed = {null};
        LuaArchitecture architecture = new LuaArchitecture("users = computer.users(); first = users[1]; second = users[2]; added = computer.addUser('carol'); removed = computer.removeUser('bob')");
        architecture.bind(machineWithUserAccess(new String[]{"alice", "bob"}, added, removed));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("alice", architecture.globalString("first"));
        assertEquals("bob", architecture.globalString("second"));
        assertEquals(true, architecture.globalBoolean("added"));
        assertEquals(true, architecture.globalBoolean("removed"));
        assertEquals("carol", added[0]);
        assertEquals("bob", removed[0]);
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
    void filtersComponentListForLua() {
        LuaArchitecture architecture = new LuaArchitecture("components = component.list('file', false); fs = components['fs-address']; gpu = components['gpu-address']");
        architecture.bind(machineWithComponents(Map.of("fs-address", "filesystem", "gpu-address", "gpu")));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("filesystem", architecture.globalString("fs"));
        assertEquals("nil", architecture.globalString("gpu"));
    }

    @Test
    void iteratesComponentListInLua() {
        LuaArchitecture architecture = new LuaArchitecture("for address, kind in component.list('file', false) do firstAddress = address; firstKind = kind end");
        architecture.bind(machineWithComponents(Map.of("fs-address", "filesystem", "gpu-address", "gpu")));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("fs-address", architecture.globalString("firstAddress"));
        assertEquals("filesystem", architecture.globalString("firstKind"));
    }

    @Test
    void exposesComponentTypeToLua() {
        LuaArchitecture architecture = new LuaArchitecture("kind = component.type('fs-address'); missing = component.type('missing')");
        architecture.bind(machineWithComponents(Map.of("fs-address", "filesystem")));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("filesystem", architecture.globalString("kind"));
        assertEquals("nil", architecture.globalString("missing"));
    }

    @Test
    void exposesComponentAvailabilityToLua() {
        LuaArchitecture architecture = new LuaArchitecture("hasFs = component.isAvailable('filesystem'); hasGpu = component.isAvailable('gpu')");
        architecture.bind(machineWithComponents(Map.of("fs-address", "filesystem")));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(true, architecture.globalBoolean("hasFs"));
        assertEquals(false, architecture.globalBoolean("hasGpu"));
    }

    @Test
    void exposesComponentSlotToLua() {
        LuaArchitecture architecture = new LuaArchitecture("slot = component.slot('fs-address'); missing = component.slot('missing')");
        architecture.bind(machineWithHostSlot("fs-address", 3));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(3, architecture.globalInteger("slot"));
        assertEquals("nil", architecture.globalString("missing"));
    }

    @Test
    void exposesComponentMethodsToLua() {
        Map<String, Callback> methods = new LinkedHashMap<>();
        methods.put("label", callback("labelCallback"));
        methods.put("direct", callback("directCallback"));
        LuaArchitecture architecture = new LuaArchitecture("methods = component.methods('fs-address'); label = methods.label; direct = methods.direct");
        architecture.bind(machineWithMethods(methods));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(false, architecture.globalBoolean("label"));
        assertEquals(true, architecture.globalBoolean("direct"));
    }

    @Test
    void exposesComponentDocumentationToLua() {
        Map<String, Callback> methods = new LinkedHashMap<>();
        methods.put("label", callback("labelCallback"));
        LuaArchitecture architecture = new LuaArchitecture("doc = component.doc('fs-address', 'label'); missing = component.doc('fs-address', 'missing')");
        architecture.bind(machineWithMethods(methods));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("function():string -- Regular callback.", architecture.globalString("doc"));
        assertEquals("nil", architecture.globalString("missing"));
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
    void convertsArrayInvokeResultsToLuaTables() {
        LuaArchitecture architecture = new LuaArchitecture("items = component.invoke('fs-address', 'list'); first = items[1]; second = items[2]");
        architecture.bind(machineWithInvokeResult(new Object[]{new String[]{"init.lua", "bin"}}));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("init.lua", architecture.globalString("first"));
        assertEquals("bin", architecture.globalString("second"));
    }

    @Test
    void roundTripsOpaqueJavaHandlesThroughLua() {
        Object handle = new Object();
        boolean[] handleRoundTripped = {false};
        LuaArchitecture architecture = new LuaArchitecture("handle = component.invoke('fs-address', 'open'); result = component.invoke('fs-address', 'read', handle)");
        architecture.bind(machineWithHandleInvoke(handle, handleRoundTripped));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("ok", architecture.globalString("result"));
        assertEquals(true, handleRoundTripped[0]);
    }

    @Test
    void convertsLuaTableArgumentsToJavaMaps() {
        Object[] capturedArgument = {null};
        LuaArchitecture architecture = new LuaArchitecture("result = component.invoke('fs-address', 'configure', {label = 'disk', size = 4})");
        architecture.bind(machineWithArgumentCapture(capturedArgument));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("ok", architecture.globalString("result"));
        Map<?, ?> argument = (Map<?, ?>) capturedArgument[0];
        assertEquals("disk", argument.get("label"));
        assertEquals(4D, argument.get("size"));
    }

    @Test
    void exposesComponentProxyToLua() {
        LuaArchitecture architecture = new LuaArchitecture("fs = component.proxy('fs-address'); result = fs.label('arg')");
        architecture.bind(machineWithInvokeResult(new Object[]{"tmp"}));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("tmp", architecture.globalString("result"));
    }

    @Test
    void exposesPrimaryComponentProxyToLua() {
        LuaArchitecture architecture = new LuaArchitecture("fs = component.getPrimary('filesystem'); result = fs.label(); missing = component.getPrimary('gpu')");
        architecture.bind(machineWithComponentsAndInvokeResult(Map.of("fs-address", "filesystem"), new Object[]{"tmp"}));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals("tmp", architecture.globalString("result"));
        assertEquals("nil", architecture.globalString("missing"));
    }

    @Test
    void setsPrimaryComponentForLua() {
        String[] invokedAddress = {null};
        Map<String, String> components = new LinkedHashMap<>();
        components.put("fs1-address", "filesystem");
        components.put("fs2-address", "filesystem");
        components.put("gpu-address", "gpu");
        LuaArchitecture architecture = new LuaArchitecture("selected = component.setPrimary('filesystem', 'fs2-address'); rejected = component.setPrimary('filesystem', 'gpu-address'); fs = component.getPrimary('filesystem'); result = fs.label()");
        architecture.bind(machineWithInvokeCapture(components, invokedAddress));

        assertTrue(architecture.initialize());
        assertInstanceOf(ExecutionResult.Sleep.class, architecture.runThreaded(false));

        assertEquals(true, architecture.globalBoolean("selected"));
        assertEquals(false, architecture.globalBoolean("rejected"));
        assertEquals("tmp", architecture.globalString("result"));
        assertEquals("fs2-address", invokedAddress[0]);
    }

    @Test
    void invokesRealFilesystemComponentFromLua() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        FileSystem fileSystem = API.fileSystem.fromMemory(512);
        ManagedEnvironment fileSystemEnvironment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);
        Network.joinNewNetwork(machine.node());
        machine.node().connect(fileSystemEnvironment.node());
        LuaArchitecture architecture = new LuaArchitecture("""
            fs = component.getPrimary('filesystem')
            fs.makeDirectory('tmp')
            handle = fs.open('tmp/data.txt', 'w')
            wrote = fs.write(handle, 'hello')
            fs.close(handle)
            handle = fs.open('tmp/data.txt', 'r')
            data = fs.read(handle, 5)
            fs.close(handle)
            entries = fs.list('tmp')
            firstEntry = entries[1]
            exists = fs.exists('tmp/data.txt')
            """);
        architecture.bind(machine);

        assertTrue(architecture.initialize());
        ExecutionResult result = architecture.runThreaded(false);
        if (result instanceof ExecutionResult.Error error) {
            fail(error.message);
        }
        assertInstanceOf(ExecutionResult.Sleep.class, result);

        assertEquals(true, architecture.globalBoolean("wrote"));
        assertEquals("hello", architecture.globalString("data"));
        assertEquals("data.txt", architecture.globalString("firstEntry"));
        assertEquals(true, architecture.globalBoolean("exists"));
    }

    private static Machine machineWithUptime(final double uptime) {
        return machine(new ArrayDeque<>(), uptime);
    }

    private static Machine machineWithSignals(final Signal... signals) {
        return machine(new ArrayDeque<>(Arrays.asList(signals)), 0D);
    }

    private static Machine machineWithSignalCapture(final String[] signalName, final Object[][] signalArguments) {
        return machine(new ArrayDeque<>(), 0D, null, null, Map.of(), new Object[0], Map.of(), new String[0], null, null, signalName, signalArguments);
    }

    private static Machine machineWithAddress(final String address) {
        return machine(new ArrayDeque<>(), 0D, address);
    }

    private static Machine machineWithBeep(final String[] beepPattern) {
        return machine(new ArrayDeque<>(), 0D, null, beepPattern);
    }

    private static Machine machineWithUserAccess(final String[] users, final String[] added, final String[] removed) {
        return machine(new ArrayDeque<>(), 0D, null, null, Map.of(), new Object[0], Map.of(), users, added, removed);
    }

    private static Machine machineWithComponents(final Map<String, String> components) {
        return machine(new ArrayDeque<>(), 0D, null, null, components, new Object[0]);
    }

    private static Machine machineWithInvokeResult(final Object[] invokeResult) {
        return machine(new ArrayDeque<>(), 0D, null, null, Map.of(), invokeResult);
    }

    private static Machine machineWithComponentsAndInvokeResult(final Map<String, String> components, final Object[] invokeResult) {
        return machine(new ArrayDeque<>(), 0D, null, null, components, invokeResult);
    }

    private static Machine machineWithInvokeCapture(final Map<String, String> components, final String[] invokedAddress) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "components" -> components;
                case "invoke" -> {
                    invokedAddress[0] = (String) args[0];
                    yield new Object[]{"tmp"};
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithHandleInvoke(final Object handle, final boolean[] handleRoundTripped) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "invoke" -> {
                    final String componentMethod = (String) args[1];
                    final Object[] javaArgs = (Object[]) args[2];
                    if ("open".equals(componentMethod)) {
                        yield new Object[]{handle};
                    }
                    handleRoundTripped[0] = javaArgs.length == 1 && javaArgs[0] == handle;
                    yield handleRoundTripped[0] ? new Object[]{"ok"} : new Object[]{"bad"};
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithArgumentCapture(final Object[] capturedArgument) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "invoke" -> {
                    final Object[] javaArgs = (Object[]) args[2];
                    capturedArgument[0] = javaArgs[0];
                    yield new Object[]{"ok"};
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-machine";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Machine machineWithMethods(final Map<String, Callback> methods) {
        return machine(new ArrayDeque<>(), 0D, null, null, Map.of(), new Object[0], methods);
    }

    private static Machine machineWithHostSlot(final String address, final int slot) {
        final MachineHost host = (MachineHost) Proxy.newProxyInstance(
            MachineHost.class.getClassLoader(),
            new Class<?>[]{MachineHost.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "componentSlot" -> address.equals(args[0]) ? slot : -1;
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-host";
                default -> defaultValue(method.getReturnType());
            });
        return machine(new ArrayDeque<>(), 0D, null, null, Map.of(), new Object[0], Map.of(), new String[0], null, null, null, null, host);
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
        return machine(signals, uptime, address, beepPattern, components, invokeResult, Map.of());
    }

    private static Machine machine(
        final Queue<Signal> signals,
        final double uptime,
        final String address,
        final String[] beepPattern,
        final Map<String, String> components,
        final Object[] invokeResult,
        final Map<String, Callback> methods
    ) {
        return machine(signals, uptime, address, beepPattern, components, invokeResult, methods, new String[0], null, null);
    }

    private static Machine machine(
        final Queue<Signal> signals,
        final double uptime,
        final String address,
        final String[] beepPattern,
        final Map<String, String> components,
        final Object[] invokeResult,
        final Map<String, Callback> methods,
        final String[] users,
        final String[] added,
        final String[] removed
    ) {
        return machine(signals, uptime, address, beepPattern, components, invokeResult, methods, users, added, removed, null, null);
    }

    private static Machine machine(
        final Queue<Signal> signals,
        final double uptime,
        final String address,
        final String[] beepPattern,
        final Map<String, String> components,
        final Object[] invokeResult,
        final Map<String, Callback> methods,
        final String[] users,
        final String[] added,
        final String[] removed,
        final String[] signalName,
        final Object[][] signalArguments
    ) {
        return machine(signals, uptime, address, beepPattern, components, invokeResult, methods, users, added, removed, signalName, signalArguments, null);
    }

    private static Machine machine(
        final Queue<Signal> signals,
        final double uptime,
        final String address,
        final String[] beepPattern,
        final Map<String, String> components,
        final Object[] invokeResult,
        final Map<String, Callback> methods,
        final String[] users,
        final String[] added,
        final String[] removed,
        final String[] signalName,
        final Object[][] signalArguments,
        final MachineHost host
    ) {
        return (Machine) Proxy.newProxyInstance(
            Machine.class.getClassLoader(),
            new Class<?>[]{Machine.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "upTime" -> uptime;
                case "popSignal" -> signals.poll();
                case "tmpAddress" -> address;
                case "host" -> host;
                case "components" -> components;
                case "methods" -> methods;
                case "invoke" -> invokeResult;
                case "users" -> users;
                case "addUser" -> {
                    if (added != null) {
                        added[0] = (String) args[0];
                    }
                    yield null;
                }
                case "removeUser" -> {
                    if (removed != null) {
                        removed[0] = (String) args[0];
                    }
                    yield true;
                }
                case "signal" -> {
                    if (signalName != null) {
                        signalName[0] = (String) args[0];
                    }
                    if (signalArguments != null) {
                        signalArguments[0] = (Object[]) args[1];
                    }
                    yield true;
                }
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

    private static Callback callback(final String name) {
        try {
            return LuaArchitectureTest.class.getDeclaredMethod(name).getAnnotation(Callback.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    @Callback(doc = "function():string -- Regular callback.")
    private static void labelCallback() {
    }

    @Callback(direct = true, doc = "function():string -- Direct callback.")
    private static void directCallback() {
    }

    private record TestMemoryDriver(double amount) implements Memory {
        @Override
        public boolean worksWith(final ItemStack stack) {
            return true;
        }

        @Override
        public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
            return null;
        }

        @Override
        public String slot(final ItemStack stack) {
            return Slot.Memory;
        }

        @Override
        public int tier(final ItemStack stack) {
            return 0;
        }

        @Override
        public CompoundTag dataTag(final ItemStack stack) {
            return new CompoundTag();
        }

        @Override
        public double amount(final ItemStack stack) {
            return amount;
        }
    }

    private record TestSignal(String name, Object[] args) implements Signal {
    }
}
