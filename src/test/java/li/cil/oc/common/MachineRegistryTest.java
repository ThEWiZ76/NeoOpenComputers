package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.MethodWhitelist;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.FilteredEnvironment;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.CallBudget;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.common.machine.ProgramLocations;
import li.cil.oc.common.machine.MachineBoundArchitecture;
import li.cil.oc.common.machine.SynchronizedCallAware;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MachineRegistryTest {
    @AfterEach
    void resetApi() {
        API.machine = null;
        API.network = null;
        li.cil.oc.api.Machine.LuaArchitecture = null;
        TimedArchitecture.clock = null;
        ProgramLocations.clear();
    }

    @Test
    void bootstrapInstallsMachineApi() {
        OpenComputersApi.initialize();

        assertTrue(API.machine instanceof MachineRegistry);
    }

    @Test
    void bootstrapInstallsLuaArchitecture() {
        OpenComputersApi.initialize();

        assertNotNull(li.cil.oc.api.Machine.LuaArchitecture);
        assertTrue(li.cil.oc.api.Machine.architectures().contains(li.cil.oc.api.Machine.LuaArchitecture));
        assertEquals("Lua", li.cil.oc.api.Machine.getArchitectureName(li.cil.oc.api.Machine.LuaArchitecture));
    }

    @Test
    void createdMachineHasInitialEnergyForBootIo() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);

        Connector connector = assertInstanceOf(Connector.class, machine.node());
        assertTrue(connector.localBufferSize() > 0);
        assertTrue(connector.localBuffer() > 0);
    }

    @Test
    void createdMachineUsesConfiguredComputerBuffer() throws Exception {
        withCachedConfig(ModSettings.COMPUTER_BUFFER, 77D, () -> {
            OpenComputersApi.initialize();
            Machine machine = API.machine.create(null);

            Connector connector = assertInstanceOf(Connector.class, machine.node());
            assertEquals(77D, connector.localBufferSize(), 0.000_001D);
            assertEquals(77D, connector.localBuffer(), 0.000_001D);
        });
    }

    @Test
    void computerComponentIsNeighborVisibleLikeUpstream() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);

        Component computer = assertInstanceOf(Component.class, machine.node());
        assertEquals(Visibility.Neighbors, computer.visibility());
    }

    @Test
    void createdMachineExposesTemporaryFilesystemAddress() throws Exception {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        Network.joinNewNetwork(machine.node());

        String tmpAddress = machine.tmpAddress();

        assertNotNull(tmpAddress);
        assertEquals("filesystem", machine.components().get(tmpAddress));
        assertTrue(machine.methods(tmpAddress).containsKey("makeDirectory"));
        assertArrayEquals(new Object[]{64L * 1024L}, machine.invoke(tmpAddress, "spaceTotal", new Object[0]));
    }

    @Test
    void registersArchitecturesInOrder() {
        MachineRegistry registry = new MachineRegistry();

        registry.add(TestArchitecture.class);
        registry.add(NamedArchitecture.class);

        assertEquals(List.of(TestArchitecture.class, NamedArchitecture.class), registry.architectures());
    }

    @Test
    void resolvesArchitectureNames() {
        MachineRegistry registry = new MachineRegistry();

        assertEquals("TestArchitecture", registry.getArchitectureName(TestArchitecture.class));
        assertEquals("named", registry.getArchitectureName(NamedArchitecture.class));
    }

    @Test
    void createsRunnableMachineSkeleton() {
        Machine machine = new MachineRegistry().create(null);

        assertNotNull(machine);
        assertNotNull(machine.node());
        assertFalse(machine.isRunning());
        assertTrue(machine.start());
        assertTrue(machine.isRunning());
        assertTrue(machine.signal("boot", "ok"));

        Signal signal = machine.popSignal();
        assertEquals("boot", signal.name());
        assertArrayEquals(new Object[]{"ok"}, signal.args());

        assertTrue(machine.crash("bad"));
        assertEquals("bad", machine.lastError());
        assertFalse(machine.isRunning());
    }

    @Test
    void machineDiscoversAndInvokesConnectedComponents() throws Exception {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        FileSystem fileSystem = API.fileSystem.fromMemory(128);
        ManagedEnvironment fileSystemEnvironment = API.fileSystem.asManagedEnvironment(fileSystem, "tmp", null, null, 1);

        Network.joinNewNetwork(machine.node());
        machine.node().connect(fileSystemEnvironment.node());

        assertEquals("filesystem", machine.components().get(fileSystemEnvironment.node().address()));
        assertEquals(0, machine.componentCount());
        assertTrue(machine.methods(fileSystemEnvironment.node().address()).containsKey("isReadOnly"));
        assertArrayEquals(new Object[]{false}, machine.invoke(fileSystemEnvironment.node().address(), "isReadOnly", new Object[0]));
    }

    @Test
    void callbackArgumentSnapshotsDecodeByteArraysLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        SnapshotArgumentsEnvironment environment = new SnapshotArgumentsEnvironment();
        Network.joinNewNetwork(machine.node());
        machine.node().connect(environment.node());

        Object[] result = machine.invoke(
            environment.node().address(),
            "snapshotFirst",
            new Object[]{"payload".getBytes(StandardCharsets.UTF_8)});

        assertArrayEquals(new Object[]{"payload"}, result);
    }

    @Test
    void valueCallbacksHonorFiltersLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        FilteredValue value = new FilteredValue();

        Map<String, Callback> methods = machine.methods(value);

        assertTrue(methods.containsKey("echo"));
        assertFalse(methods.containsKey("hidden"));
        assertFalse(methods.containsKey("extra"));
        assertArrayEquals(new Object[]{"ok"}, machine.invoke(value, "echo", new Object[0]));
        assertThrows(NoSuchMethodException.class, () -> machine.invoke(value, "hidden", new Object[0]));
        assertThrows(NoSuchMethodException.class, () -> machine.invoke(value, "extra", new Object[0]));
    }

    @Test
    void valueCallbacksIgnoreInvalidCallbackShapesLikeUpstream() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        InvalidCallbackValue value = new InvalidCallbackValue();

        Map<String, Callback> methods = machine.methods(value);

        assertTrue(methods.containsKey("valid"));
        assertFalse(methods.containsKey("badReturn"));
        assertFalse(methods.containsKey("badArgs"));
        assertFalse(methods.containsKey("privateCallback"));
        assertThrows(NoSuchMethodException.class, () -> machine.invoke(value, "badReturn", new Object[0]));
        assertThrows(NoSuchMethodException.class, () -> machine.invoke(value, "badArgs", new Object[0]));
        assertThrows(NoSuchMethodException.class, () -> machine.invoke(value, "privateCallback", new Object[0]));
    }

    @Test
    void valueCallbacksUseMethodNameForBlankCallbackNamesLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        BlankCallbackNameValue value = new BlankCallbackNameValue();

        Map<String, Callback> methods = machine.methods(value);

        assertTrue(methods.containsKey("fallback"));
        assertFalse(methods.containsKey("   "));
        assertArrayEquals(new Object[]{"fallback"}, machine.invoke(value, "fallback", new Object[0]));
    }

    @Test
    void valueCallbacksExposeManagedPeripheralMethodsLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        PeripheralValue value = new PeripheralValue();

        Map<String, Callback> methods = machine.methods(value);

        assertTrue(methods.containsKey("dynamic"));
        assertTrue(methods.get("dynamic").direct());
        assertEquals(100, methods.get("dynamic").limit());
        assertArrayEquals(new Object[]{"dynamic", "payload"}, machine.invoke(value, "dynamic", new Object[]{"payload"}));
    }

    @Test
    void valueCallbackArgumentsTreatNonNaNNumbersAsIntegersLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        NumericArgumentsValue value = new NumericArgumentsValue();

        Object[] result = machine.invoke(value, "integerFlags", new Object[]{1.5D, Float.POSITIVE_INFINITY});

        assertArrayEquals(new Object[]{true, true, true, true}, result);
    }

    @Test
    void valueCallbackArgumentsClampLongIntegerOverflowLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        NumericArgumentsValue value = new NumericArgumentsValue();

        Object[] result = machine.invoke(value, "clampIntegers", new Object[]{Long.MAX_VALUE, Long.MIN_VALUE});

        assertArrayEquals(new Object[]{Integer.MAX_VALUE, Integer.MIN_VALUE}, result);
    }

    @Test
    void valueCallbackArgumentsRejectNaNIntegersLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        NumericArgumentsValue value = new NumericArgumentsValue();

        assertThrows(IllegalArgumentException.class, () -> machine.invoke(value, "checkInteger", new Object[]{Double.NaN}));
        assertThrows(IllegalArgumentException.class, () -> machine.invoke(value, "checkLong", new Object[]{Float.NaN}));
    }

    @Test
    void machineInvokeRejectsMissingComponentsLikeUpstream() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        Network.joinNewNetwork(machine.node());

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> machine.invoke("missing-address", "isReadOnly", new Object[0]));

        assertEquals("no such component", error.getMessage());
    }

    @Test
    void machineInvokeRejectsMissingComponentMethodsLikeUpstream() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(
            API.fileSystem.fromMemory(128),
            "tmp",
            null,
            null,
            1);
        Network.joinNewNetwork(machine.node());
        machine.node().connect(environment.node());

        assertThrows(NoSuchMethodException.class, () -> machine.invoke(environment.node().address(), "missing", new Object[0]));
    }

    @Test
    void componentCountWeightsFilesystemsLikeUpstream() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        Network.joinNewNetwork(machine.node());

        for (int i = 0; i < 4; i++) {
            ManagedEnvironment environment = API.fileSystem.asManagedEnvironment(
                API.fileSystem.fromMemory(128),
                "fs" + i,
                null,
                null,
                1);
            machine.node().connect(environment.node());
        }

        assertEquals(1, machine.componentCount());
    }

    @Test
    void machineDelegatesDeviceInfoToHost() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(new TestHost());

        assertTrue(machine instanceof DeviceInfo);
        Map<String, String> metadata = ((DeviceInfo) machine).getDeviceInfo();
        assertEquals(DeviceInfo.DeviceClass.System, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Test host", metadata.get(DeviceInfo.DeviceAttribute.Description));
    }

    @Test
    void computerDeviceInfoCallbackReportsReachableDeviceInfo() throws Exception {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(new TestHost());
        DeviceInfoEnvironment environment = new DeviceInfoEnvironment();
        Network.joinNewNetwork(machine.node());
        machine.node().connect(environment.node());

        Object[] result = machine.invoke(machine.node().address(), "getDeviceInfo", new Object[0]);

        assertEquals(1, result.length);
        Map<?, ?> deviceInfo = assertInstanceOf(Map.class, result[0]);
        assertTrue(deviceInfo.containsKey(machine.node().address()));
        assertTrue(deviceInfo.containsKey(environment.node().address()));
        Map<?, ?> environmentInfo = assertInstanceOf(Map.class, deviceInfo.get(environment.node().address()));
        assertEquals("Test peripheral", environmentInfo.get(DeviceInfo.DeviceAttribute.Description));
    }

    @Test
    void computerCallbacksExposeStateControls() throws Exception {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        Network.joinNewNetwork(machine.node());
        String address = machine.node().address();

        assertTrue(machine.methods(address).containsKey("start"));
        assertTrue(machine.methods(address).containsKey("stop"));
        assertTrue(machine.methods(address).containsKey("isRunning"));
        assertTrue(machine.methods(address).containsKey("beep"));

        assertArrayEquals(new Object[]{true}, machine.invoke(address, "start", new Object[0]));
        assertArrayEquals(new Object[]{true}, machine.invoke(address, "isRunning", new Object[0]));
        assertArrayEquals(new Object[]{true}, machine.invoke(address, "stop", new Object[0]));
        assertArrayEquals(new Object[]{false}, machine.invoke(address, "isRunning", new Object[0]));
    }

    @Test
    void computerCallbacksExposeEnergyState() throws Exception {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        Network.joinNewNetwork(machine.node());
        Connector connector = assertInstanceOf(Connector.class, machine.node());
        connector.setLocalBufferSize(25D);
        connector.changeBuffer(-connector.localBuffer());
        connector.changeBuffer(9D);
        String address = machine.node().address();

        assertTrue(machine.methods(address).containsKey("energy"));
        assertTrue(machine.methods(address).containsKey("maxEnergy"));
        assertArrayEquals(new Object[]{9D}, machine.invoke(address, "energy", new Object[0]));
        assertArrayEquals(new Object[]{25D}, machine.invoke(address, "maxEnergy", new Object[0]));
    }

    @Test
    void computerCallbacksExposeUserManagement() throws Exception {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        Network.joinNewNetwork(machine.node());
        String address = machine.node().address();

        assertTrue(machine.methods(address).containsKey("users"));
        assertTrue(machine.methods(address).containsKey("addUser"));
        assertTrue(machine.methods(address).containsKey("removeUser"));
        assertArrayEquals(new Object[]{true}, machine.invoke(address, "addUser", new Object[]{"alice"}));
        assertArrayEquals(new Object[]{true}, machine.invoke(address, "addUser", new Object[]{"bob"}));
        Object[] usersResult = machine.invoke(address, "users", new Object[0]);
        assertArrayEquals(new String[]{"alice", "bob"}, assertInstanceOf(String[].class, usersResult[0]));
        assertArrayEquals(new Object[]{true}, machine.invoke(address, "removeUser", new Object[]{"alice"}));
        assertArrayEquals(new Object[]{false}, machine.invoke(address, "removeUser", new Object[]{"carol"}));
        assertArrayEquals(new String[]{"bob"}, machine.users());
    }

    @Test
    void machineUserManagementMatchesUpstreamLimits() throws Exception {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);

        for (int index = 0; index < ModSettings.maxUsers(); index++) {
            machine.addUser("user" + index);
        }

        Exception tooMany = assertThrows(Exception.class, () -> machine.addUser("overflow"));
        assertEquals("too many users", tooMany.getMessage());

        Machine duplicate = API.machine.create(null);
        duplicate.addUser("alice");
        Exception existing = assertThrows(Exception.class, () -> duplicate.addUser("alice"));
        assertEquals("user exists", existing.getMessage());

        String tooLongName = "x".repeat(ModSettings.maxUsernameLength() + 1);
        Exception tooLong = assertThrows(Exception.class, () -> API.machine.create(null).addUser(tooLongName));
        assertEquals("username too long", tooLong.getMessage());
    }

    @Test
    void computerBeepCallbackMatchesUpstreamDurationSemantics() throws Exception {
        OpenComputersApi.initialize();
        SimpleMachine machine = assertInstanceOf(SimpleMachine.class, API.machine.create(null));
        Network.joinNewNetwork(machine.node());
        String address = machine.node().address();

        assertThrows(IllegalArgumentException.class, () -> machine.invoke(address, "beep", new Object[]{19}));

        machine.invoke(address, "start", new Object[0]);
        machine.invoke(address, "beep", new Object[]{440, 0.01D});
        assertEquals(440, machine.lastBeepFrequency());
        assertEquals(50, machine.lastBeepDuration());
        assertTrue(machine.isPaused());

        machine.invoke(address, "beep", new Object[]{1200, 10D});
        assertEquals(1200, machine.lastBeepFrequency());
        assertEquals(5000, machine.lastBeepDuration());
    }

    @Test
    void computerCallbackReportsProgramLocations() throws Exception {
        OpenComputersApi.initialize();
        ProgramLocations.addMapping("edit", "OpenOS");
        ProgramLocations.addMapping("dig", "Network", "TrackingArchitecture");
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new TestProcessorDriver());
        API.driver = driverRegistry;
        Machine machine = API.machine.create(new TestHost());
        machine.onHostChanged();
        Network.joinNewNetwork(machine.node());

        Object[] result = machine.invoke(machine.node().address(), "getProgramLocations", new Object[0]);

        assertEquals(1, result.length);
        Map<?, ?> locations = assertInstanceOf(Map.class, result[0]);
        assertEquals("Network", locations.get("dig"));
        assertEquals("OpenOS", locations.get("edit"));
    }

    @Test
    void queuesComponentAddedSignalWhenVisibleComponentConnects() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        TestEnvironment environment = new TestEnvironment();
        Network.joinNewNetwork(machine.node());
        assertTrue(machine.start());

        machine.node().connect(environment.node());

        Signal signal = machine.popSignal();
        assertEquals("component_added", signal.name());
        assertArrayEquals(new Object[]{environment.node().address(), "test_component"}, signal.args());
    }

    @Test
    void queuesComponentAddedSignalWhenVisibleComponentConnectsThroughNeighbor() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        TestEnvironment bridge = new TestEnvironment();
        TestEnvironment environment = new TestEnvironment();
        Network.joinNewNetwork(machine.node());
        assertTrue(machine.start());
        machine.node().connect(bridge.node());
        machine.popSignal();

        bridge.node().connect(environment.node());

        Signal signal = machine.popSignal();
        assertNotNull(signal);
        assertEquals("component_added", signal.name());
        assertArrayEquals(new Object[]{environment.node().address(), "test_component"}, signal.args());
    }

    @Test
    void queuesComponentRemovedSignalWhenVisibleComponentDisconnects() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        TestEnvironment environment = new TestEnvironment();
        Network.joinNewNetwork(machine.node());
        assertTrue(machine.start());
        machine.node().connect(environment.node());
        machine.popSignal();

        machine.node().disconnect(environment.node());

        Signal signal = machine.popSignal();
        assertEquals("component_removed", signal.name());
        assertArrayEquals(new Object[]{environment.node().address(), "test_component"}, signal.args());
    }

    @Test
    void queuesComponentRemovedSignalWhenVisibleComponentDisconnectsThroughNeighbor() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        TestEnvironment bridge = new TestEnvironment();
        TestEnvironment environment = new TestEnvironment();
        Network.joinNewNetwork(machine.node());
        assertTrue(machine.start());
        machine.node().connect(bridge.node());
        machine.popSignal();
        bridge.node().connect(environment.node());
        machine.popSignal();

        bridge.node().disconnect(environment.node());

        Signal signal = machine.popSignal();
        assertNotNull(signal);
        assertEquals("component_removed", signal.name());
        assertArrayEquals(new Object[]{environment.node().address(), "test_component"}, signal.args());
    }

    @Test
    void hostChangedConnectsInternalComponentEnvironments() {
        OpenComputersApi.initialize();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new TestDriver());
        API.driver = driverRegistry;
        Machine machine = API.machine.create(new TestHost());

        machine.onHostChanged();

        assertEquals("test_component", machine.components().values().iterator().next());
        assertEquals(1, machine.componentCount());
    }

    @Test
    void hostChangedSavesComponentEnvironmentsBeforeRemovingThem() {
        OpenComputersApi.initialize();
        SavingDriver driver = new SavingDriver();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(driver);
        API.driver = driverRegistry;
        Machine machine = API.machine.create(new TestHost());
        machine.onHostChanged();
        SavingEnvironment firstEnvironment = driver.environments.getFirst();

        machine.onHostChanged();

        assertEquals(1, firstEnvironment.saves);
    }

    @Test
    void savePersistsComponentEnvironments() {
        OpenComputersApi.initialize();
        SavingDriver driver = new SavingDriver();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(driver);
        API.driver = driverRegistry;
        Machine machine = API.machine.create(new TestHost());
        machine.onHostChanged();
        SavingEnvironment environment = driver.environments.getFirst();

        machine.save(new CompoundTag());

        assertEquals(1, environment.saves);
    }

    @Test
    void hostChangedSelectsProcessorArchitectureAndStartInitializesIt() {
        OpenComputersApi.initialize();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new TestProcessorDriver());
        API.driver = driverRegistry;
        Machine machine = API.machine.create(new TestHost());

        machine.onHostChanged();

        assertTrue(machine.architecture() instanceof TrackingArchitecture);
        assertFalse(machine.architecture().isInitialized());
        assertTrue(machine.start());
        assertTrue(machine.architecture().isInitialized());
        assertTrue(machine.stop());
        assertFalse(machine.architecture().isInitialized());
    }

    @Test
    void maxComponentsComesFromInstalledProcessors() {
        OpenComputersApi.initialize();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new TestProcessorDriver());
        API.driver = driverRegistry;
        Machine machine = API.machine.create(new TestHost());

        machine.onHostChanged();

        assertEquals(4, machine.maxComponents());
    }

    @Test
    void hostChangedRejectsArchitectureWhenMemoryRecomputeFails() {
        OpenComputersApi.initialize();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new RejectingProcessorDriver());
        API.driver = driverRegistry;
        Machine machine = API.machine.create(new TestHost());

        machine.onHostChanged();

        assertNull(machine.architecture());
        assertFalse(machine.start());
    }

    @Test
    void hostChangedBindsMachineAwareArchitecture() {
        OpenComputersApi.initialize();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new BoundProcessorDriver());
        API.driver = driverRegistry;
        Machine machine = API.machine.create(new TestHost());

        machine.onHostChanged();

        BoundArchitecture architecture = (BoundArchitecture) machine.architecture();
        assertSame(machine, architecture.boundMachine);
    }

    @Test
    void savesAndLoadsSelectedArchitectureState() {
        OpenComputersApi.initialize();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new TestProcessorDriver());
        API.driver = driverRegistry;
        Machine saved = API.machine.create(new TestHost());
        saved.onHostChanged();
        saved.start();
        CompoundTag tag = new CompoundTag();

        saved.save(tag);

        Machine loaded = API.machine.create(new TestHost());
        loaded.onHostChanged();
        loaded.load(tag);

        assertTrue(loaded.isRunning());
        assertTrue(loaded.architecture().isInitialized());
    }

    @Test
    void loadedRunningMachineHonorsConfiguredStartupDelay() throws Exception {
        OpenComputersApi.initialize();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new TestProcessorDriver());
        API.driver = driverRegistry;
        SimpleMachine saved = new SimpleMachine(new TestHost(), new MutableClock());
        saved.onHostChanged();
        saved.start();
        CompoundTag tag = new CompoundTag();
        saved.save(tag);

        withCachedConfig(ModSettings.STARTUP_DELAY, 0.25D, () -> {
            MutableClock clock = new MutableClock();
            SimpleMachine loaded = new SimpleMachine(new TestHost(), clock);
            loaded.onHostChanged();
            TrackingArchitecture architecture = (TrackingArchitecture) loaded.architecture();

            loaded.load(tag);
            loaded.update();
            clock.nanos += TimeUnit.MILLISECONDS.toNanos(249);
            loaded.update();

            assertTrue(loaded.isRunning());
            assertTrue(loaded.isPaused());
            assertEquals(0, architecture.synchronizedRuns);
            assertEquals(0, architecture.threadedRuns);

            clock.nanos += TimeUnit.MILLISECONDS.toNanos(1);
            loaded.update();

            assertFalse(loaded.isPaused());
            assertEquals(1, architecture.synchronizedRuns);
            assertEquals(1, architecture.threadedRuns);
        });
    }

    @Test
    void savesAndLoadsMachineUsers() throws Exception {
        OpenComputersApi.initialize();
        Machine saved = API.machine.create(null);
        saved.addUser("alice");
        saved.addUser("bob");
        CompoundTag tag = new CompoundTag();

        saved.save(tag);

        Machine loaded = API.machine.create(null);
        loaded.load(tag);

        assertArrayEquals(new String[]{"alice", "bob"}, loaded.users());
        assertTrue(loaded.canInteract("alice"));
        assertFalse(loaded.canInteract("carol"));
    }

    @Test
    void savesAndLoadsQueuedSignals() {
        OpenComputersApi.initialize();
        Machine saved = API.machine.create(null);
        assertTrue(saved.start());
        assertTrue(saved.signal("boot", "disk", 1, true, null));
        CompoundTag tag = new CompoundTag();

        saved.save(tag);

        Machine loaded = API.machine.create(null);
        loaded.load(tag);

        Signal signal = loaded.popSignal();
        assertEquals("boot", signal.name());
        assertArrayEquals(new Object[]{"disk", 1, true, null}, signal.args());
        assertNull(loaded.popSignal());
    }

    @Test
    void queuedSignalsNormalizeArgumentsLikeUpstream() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        assertTrue(machine.start());
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put('a', 2.5F);
        map.put("ignored", new Object());

        assertTrue(machine.signal("event", 'x', 1.25F, new Object(), map));

        Signal signal = machine.popSignal();
        assertEquals("event", signal.name());
        Map<?, ?> convertedMap = assertInstanceOf(Map.class, signal.args()[3]);
        assertArrayEquals(new Object[]{120, 1.25D, null, convertedMap}, signal.args());
        assertEquals(Map.of(97, 2.5D), convertedMap);
    }

    @Test
    void rejectsSignalsWhileStopped() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);

        assertFalse(machine.signal("boot"));
        assertNull(machine.popSignal());
    }

    @Test
    void rejectsSignalsWhenQueueIsFull() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        assertTrue(machine.start());

        for (int i = 0; i < 256; i++) {
            assertTrue(machine.signal("event", i), "signal " + i);
        }
        assertFalse(machine.signal("overflow"));

        int count = 0;
        while (machine.popSignal() != null) {
            count++;
        }
        assertEquals(256, count);
    }

    @Test
    void stopClearsQueuedSignals() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        assertTrue(machine.start());
        assertTrue(machine.signal("event"));

        assertTrue(machine.stop());

        assertNull(machine.popSignal());
    }

    @Test
    void crashClearsQueuedSignals() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        assertTrue(machine.start());
        assertTrue(machine.signal("event"));

        assertTrue(machine.crash("failed"));

        assertNull(machine.popSignal());
    }

    @Test
    void hostChangedDropsArchitectureWhenProcessorRemoved() {
        OpenComputersApi.initialize();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new TestProcessorDriver());
        API.driver = driverRegistry;
        TestHost host = new TestHost();
        Machine machine = API.machine.create(host);
        machine.onHostChanged();
        machine.start();

        host.components = Collections.emptyList();
        machine.onHostChanged();

        assertFalse(machine.isRunning());
        assertNull(machine.architecture());
    }

    @Test
    void runningMachineDispatchesSignalsAndExecutionStepsToArchitecture() {
        OpenComputersApi.initialize();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new TestProcessorDriver());
        API.driver = driverRegistry;
        Machine machine = API.machine.create(new TestHost());
        machine.onHostChanged();
        TrackingArchitecture architecture = (TrackingArchitecture) machine.architecture();
        machine.start();

        assertTrue(machine.canUpdate());
        assertTrue(machine.signal("event"));
        machine.update();

        assertEquals(1, architecture.signalCount);
        assertEquals(1, architecture.synchronizedRuns);
        assertEquals(1, architecture.threadedRuns);
    }

    @Test
    void directCallbacksConsumeBudgetUntilNextUpdate() throws Exception {
        OpenComputersApi.initialize();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new TestProcessorDriver());
        API.driver = driverRegistry;
        Machine machine = API.machine.create(new TestHost());
        DirectBudgetEnvironment environment = new DirectBudgetEnvironment();
        machine.onHostChanged();
        Network.joinNewNetwork(machine.node());
        machine.node().connect(environment.node());
        assertTrue(machine.start());

        machine.update();
        assertArrayEquals(new Object[]{"direct"}, machine.invoke(environment.node().address(), "direct", new Object[0]));
        assertArrayEquals(new Object[]{"direct"}, machine.invoke(environment.node().address(), "direct", new Object[0]));
        assertThrows(LimitReachedException.class, () -> machine.invoke(environment.node().address(), "direct", new Object[0]));

        machine.update();

        assertArrayEquals(new Object[]{"direct"}, machine.invoke(environment.node().address(), "direct", new Object[0]));
    }

    @Test
    void callBudgetDriversSetDirectCallbackBudget() throws Exception {
        OpenComputersApi.initialize();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new BudgetProcessorDriver());
        API.driver = driverRegistry;
        Machine machine = API.machine.create(new TestHost());
        DirectBudgetEnvironment environment = new DirectBudgetEnvironment();
        machine.onHostChanged();
        Network.joinNewNetwork(machine.node());
        machine.node().connect(environment.node());
        assertTrue(machine.start());

        machine.update();
        assertArrayEquals(new Object[]{"direct"}, machine.invoke(environment.node().address(), "direct", new Object[0]));
        assertThrows(LimitReachedException.class, () -> machine.invoke(environment.node().address(), "direct", new Object[0]));
        assertArrayEquals(new Object[]{"regular"}, machine.invoke(environment.node().address(), "regular", new Object[0]));
    }

    @Test
    void synchronizedArchitectureCallsDoNotConsumeDirectBudget() throws Exception {
        OpenComputersApi.initialize();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new SynchronizedBudgetProcessorDriver());
        API.driver = driverRegistry;
        Machine machine = API.machine.create(new TestHost());
        DirectBudgetEnvironment environment = new DirectBudgetEnvironment();
        machine.onHostChanged();
        Network.joinNewNetwork(machine.node());
        machine.node().connect(environment.node());
        SynchronizedBudgetArchitecture.targetAddress = environment.node().address();
        assertTrue(machine.start());

        machine.update();

        assertNull(SynchronizedBudgetArchitecture.error);
        assertArrayEquals(new Object[]{"direct"}, machine.invoke(environment.node().address(), "direct", new Object[0]));
        assertThrows(LimitReachedException.class, () -> machine.invoke(environment.node().address(), "direct", new Object[0]));
    }

    @Test
    void updateDrainsPendingSynchronizedArchitectureCalls() {
        OpenComputersApi.initialize();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new ChainedSynchronizedProcessorDriver());
        API.driver = driverRegistry;
        Machine machine = API.machine.create(new TestHost());
        machine.onHostChanged();
        assertTrue(machine.start());

        machine.update();

        assertEquals(3, ChainedSynchronizedArchitecture.synchronizedRuns);
        assertEquals(4, ChainedSynchronizedArchitecture.threadedRuns);
        assertTrue(ChainedSynchronizedArchitecture.completed);
    }

    @Test
    void threadedArchitectureCallsConsumeDirectBudget() throws Exception {
        OpenComputersApi.initialize();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new ThreadedBudgetProcessorDriver());
        API.driver = driverRegistry;
        Machine machine = API.machine.create(new TestHost());
        DirectBudgetEnvironment environment = new DirectBudgetEnvironment();
        machine.onHostChanged();
        Network.joinNewNetwork(machine.node());
        machine.node().connect(environment.node());
        ThreadedBudgetArchitecture.targetAddress = environment.node().address();
        assertTrue(machine.start());

        machine.update();

        assertInstanceOf(LimitReachedException.class, ThreadedBudgetArchitecture.error);
        assertThrows(LimitReachedException.class, () -> machine.invoke(environment.node().address(), "direct", new Object[0]));
    }

    @Test
    void runningMachineHonorsArchitectureSleepTicks() {
        OpenComputersApi.initialize();
        MutableClock clock = new MutableClock();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new SleepyProcessorDriver());
        API.driver = driverRegistry;
        SimpleMachine machine = new SimpleMachine(new TestHost(), clock);
        machine.onHostChanged();
        SleepyArchitecture architecture = (SleepyArchitecture) machine.architecture();
        assertTrue(machine.start());

        clock.nanos = 1_000_000_000L;
        machine.update();
        clock.nanos += 99_000_000L;
        machine.update();

        assertEquals(1, architecture.synchronizedRuns);
        assertEquals(1, architecture.threadedRuns);

        clock.nanos += 1_000_000L;
        machine.update();

        assertEquals(2, architecture.synchronizedRuns);
        assertEquals(2, architecture.threadedRuns);
    }

    @Test
    void zeroTickYieldsHonorConfiguredExecutionDelay() throws Exception {
        OpenComputersApi.initialize();
        MutableClock clock = new MutableClock();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new ImmediateYieldProcessorDriver());
        API.driver = driverRegistry;

        withCachedConfig(ModSettings.EXECUTION_DELAY, 12, () -> {
            SimpleMachine machine = new SimpleMachine(new TestHost(), clock);
            machine.onHostChanged();
            ImmediateYieldArchitecture architecture = (ImmediateYieldArchitecture) machine.architecture();
            assertTrue(machine.start());

            clock.nanos = 1_000_000_000L;
            machine.update();
            machine.update();

            assertEquals(1, architecture.threadedRuns);

            clock.nanos += TimeUnit.MILLISECONDS.toNanos(11);
            machine.update();

            assertEquals(1, architecture.threadedRuns);

            clock.nanos += TimeUnit.MILLISECONDS.toNanos(1);
            machine.update();

            assertEquals(2, architecture.threadedRuns);
        });
    }

    @Test
    void pausedMachineResumesAfterRequestedDelay() {
        OpenComputersApi.initialize();
        MutableClock clock = new MutableClock();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new TestProcessorDriver());
        API.driver = driverRegistry;
        SimpleMachine machine = new SimpleMachine(new TestHost(), clock);
        machine.onHostChanged();
        TrackingArchitecture architecture = (TrackingArchitecture) machine.architecture();
        assertTrue(machine.start());

        clock.nanos = 1_000_000_000L;
        assertTrue(machine.pause(0.1D));
        machine.update();

        assertTrue(machine.isPaused());
        assertEquals(0, architecture.synchronizedRuns);
        assertEquals(0, architecture.threadedRuns);

        clock.nanos += 99_000_000L;
        machine.update();
        assertTrue(machine.isPaused());
        assertEquals(0, architecture.synchronizedRuns);
        assertEquals(0, architecture.threadedRuns);

        clock.nanos += 1_000_000L;
        machine.update();
        assertFalse(machine.isPaused());
        assertEquals(1, architecture.synchronizedRuns);
        assertEquals(1, architecture.threadedRuns);
    }

    @Test
    void startAndStopNotifyReachableComponents() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        TestEnvironment environment = new TestEnvironment();
        Network.joinNewNetwork(machine.node());
        machine.node().connect(environment.node());

        assertTrue(machine.start());
        assertTrue(machine.stop());

        assertEquals(List.of("computer.started", "computer.stopped"), environment.messages);
    }

    @Test
    void uptimeUsesElapsedSecondsSinceStart() {
        MutableClock clock = new MutableClock();
        SimpleMachine machine = new SimpleMachine(null, clock);

        clock.nanos = 1_000_000_000L;
        assertTrue(machine.start());
        clock.nanos = 3_500_000_000L;

        assertEquals(2.5D, machine.upTime(), 0.000_001D);
        assertTrue(machine.stop());
        assertEquals(0D, machine.upTime(), 0.000_001D);
    }

    @Test
    void cpuTimeAccumulatesArchitectureUpdateDuration() {
        OpenComputersApi.initialize();
        MutableClock clock = new MutableClock();
        TimedArchitecture.clock = clock;
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new TimedProcessorDriver());
        API.driver = driverRegistry;
        SimpleMachine machine = new SimpleMachine(new TestHost(), clock);

        machine.onHostChanged();
        assertTrue(machine.start());
        machine.update();

        assertEquals(0.003D, machine.cpuTime(), 0.000_001D);
    }

    @Test
    void rebootErasesTemporaryFilesystemWhenConfigured() throws Exception {
        OpenComputersApi.initialize();
        DriverRegistry driverRegistry = new DriverRegistry();
        driverRegistry.add(new RebootingProcessorDriver());
        API.driver = driverRegistry;

        withCachedConfig(ModSettings.ERASE_TMP_ON_REBOOT, true, () -> {
            SimpleMachine machine = new SimpleMachine(new TestHost(), new MutableClock());
            machine.onHostChanged();
            writeTemporaryFile(machine, "boot.txt", "keep");
            assertArrayEquals(new Object[]{true}, machine.invoke(machine.tmpAddress(), "exists", new Object[]{"boot.txt"}));

            assertTrue(machine.start());
            machine.update();

            assertTrue(machine.isRunning());
            assertArrayEquals(new Object[]{false}, machine.invoke(machine.tmpAddress(), "exists", new Object[]{"boot.txt"}));
        });
    }

    @Test
    void checkedSignalNetworkMessagesQueueMachineSignals() {
        Machine machine = new MachineRegistry().create(null);
        TestEnvironment source = new TestEnvironment();
        Network.joinNewNetwork(source.node());
        assertTrue(machine.start());

        machine.onMessage(new TestMessage(source.node(), "computer.checked_signal", new Object[]{null, "key_down", 'a', 30}));
        machine.onMessage(new TestMessage(source.node(), "computer.checked_signal", new Object[]{null, "touch", 2, 3, 0}));

        Signal keySignal = machine.popSignal();
        assertEquals("key_down", keySignal.name());
        assertArrayEquals(new Object[]{source.node().address(), (int) 'a', 30}, keySignal.args());
        Signal touchSignal = machine.popSignal();
        assertEquals("touch", touchSignal.name());
        assertArrayEquals(new Object[]{source.node().address(), 2, 3, 0}, touchSignal.args());
    }

    @Test
    void computerSignalNetworkMessagesQueueMachineSignalsWithSourceAddress() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        TestEnvironment source = new TestEnvironment();
        assertTrue(machine.start());

        machine.onMessage(new TestMessage(source.node(), "computer.signal", new Object[]{"modem_message", 123}));

        Signal signal = machine.popSignal();
        assertEquals("modem_message", signal.name());
        assertArrayEquals(new Object[]{source.node().address(), 123}, signal.args());
    }

    @Test
    void computerStopNetworkMessageStopsMachine() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        assertTrue(machine.start());

        machine.onMessage(new TestMessage(null, "computer.stop", new Object[0]));

        assertFalse(machine.isRunning());
    }

    @Test
    void computerStartNetworkMessageDoesNotResumePausedMachine() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        assertTrue(machine.start());
        assertTrue(machine.pause(1D));

        machine.onMessage(new TestMessage(null, "computer.start", new Object[0]));

        assertTrue(machine.isPaused());
    }

    @Test
    void hostChangedWhileRunningNotifiesReachableComponentsStopped() {
        OpenComputersApi.initialize();
        Machine machine = API.machine.create(null);
        TestEnvironment environment = new TestEnvironment();
        Network.joinNewNetwork(machine.node());
        machine.node().connect(environment.node());

        assertTrue(machine.start());
        machine.onHostChanged();

        assertEquals(List.of("computer.started", "computer.stopped"), environment.messages);
    }

    private static class TestArchitecture implements Architecture {
        @Override public boolean isInitialized() { return false; }
        @Override public boolean recomputeMemory(final Iterable<ItemStack> components) { return false; }
        @Override public boolean initialize() { return false; }
        @Override public void close() {}
        @Override public void runSynchronized() {}
        @Override public ExecutionResult runThreaded(final boolean isSynchronizedReturn) { return null; }
        @Override public void onSignal() {}
        @Override public void onConnect() {}
        @Override public void load(final CompoundTag nbt) {}
        @Override public void save(final CompoundTag nbt) {}
    }

    @Architecture.Name("named")
    private static final class NamedArchitecture extends TestArchitecture {
    }

    private static class TestDriver implements DriverItem {
        @Override
        public boolean worksWith(final ItemStack stack) {
            return true;
        }

        @Override
        public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
            return new TestEnvironment();
        }

        @Override
        public String slot(final ItemStack stack) {
            return Slot.Card;
        }

        @Override
        public int tier(final ItemStack stack) {
            return 0;
        }

        @Override
        public CompoundTag dataTag(final ItemStack stack) {
            return new CompoundTag();
        }
    }

    private static final class TestProcessorDriver extends TestDriver implements Processor {
        @Override
        public String slot(final ItemStack stack) {
            return Slot.CPU;
        }

        @Override
        public int supportedComponents(final ItemStack stack) {
            return 4;
        }

        @Override
        public Class<? extends Architecture> architecture(final ItemStack stack) {
            return TrackingArchitecture.class;
        }
    }

    private static final class TimedProcessorDriver extends TestDriver implements Processor {
        @Override
        public String slot(final ItemStack stack) {
            return Slot.CPU;
        }

        @Override
        public int supportedComponents(final ItemStack stack) {
            return 4;
        }

        @Override
        public Class<? extends Architecture> architecture(final ItemStack stack) {
            return TimedArchitecture.class;
        }
    }

    private static final class BudgetProcessorDriver extends TestDriver implements Processor, CallBudget {
        @Override
        public String slot(final ItemStack stack) {
            return Slot.CPU;
        }

        @Override
        public int supportedComponents(final ItemStack stack) {
            return 4;
        }

        @Override
        public Class<? extends Architecture> architecture(final ItemStack stack) {
            return TrackingArchitecture.class;
        }

        @Override
        public double getCallBudget(final ItemStack stack) {
            return 0.5D;
        }
    }

    private static final class SynchronizedBudgetProcessorDriver extends TestDriver implements Processor, CallBudget {
        @Override
        public String slot(final ItemStack stack) {
            return Slot.CPU;
        }

        @Override
        public int supportedComponents(final ItemStack stack) {
            return 4;
        }

        @Override
        public Class<? extends Architecture> architecture(final ItemStack stack) {
            return SynchronizedBudgetArchitecture.class;
        }

        @Override
        public double getCallBudget(final ItemStack stack) {
            return 0.5D;
        }
    }

    private static final class ThreadedBudgetProcessorDriver extends TestDriver implements Processor, CallBudget {
        @Override
        public String slot(final ItemStack stack) {
            return Slot.CPU;
        }

        @Override
        public int supportedComponents(final ItemStack stack) {
            return 4;
        }

        @Override
        public Class<? extends Architecture> architecture(final ItemStack stack) {
            return ThreadedBudgetArchitecture.class;
        }

        @Override
        public double getCallBudget(final ItemStack stack) {
            return 0.5D;
        }
    }

    private static final class ChainedSynchronizedProcessorDriver extends TestDriver implements Processor {
        @Override
        public String slot(final ItemStack stack) {
            return Slot.CPU;
        }

        @Override
        public int supportedComponents(final ItemStack stack) {
            return 4;
        }

        @Override
        public Class<? extends Architecture> architecture(final ItemStack stack) {
            return ChainedSynchronizedArchitecture.class;
        }
    }

    public static final class ChainedSynchronizedArchitecture extends TrackingArchitecture implements SynchronizedCallAware {
        private static int synchronizedRuns;
        private static int threadedRuns;
        private static boolean completed;
        private int pendingCalls;
        private boolean synchronizedReturn;

        @Override
        public boolean initialize() {
            synchronizedRuns = 0;
            threadedRuns = 0;
            completed = false;
            pendingCalls = 0;
            synchronizedReturn = false;
            return super.initialize();
        }

        @Override
        public void runSynchronized() {
            if (pendingCalls > 0) {
                synchronizedRuns++;
                pendingCalls--;
                synchronizedReturn = true;
            }
        }

        @Override
        public ExecutionResult runThreaded(final boolean isSynchronizedReturn) {
            threadedRuns++;
            synchronizedReturn = false;
            if (threadedRuns <= 3) {
                pendingCalls++;
            } else {
                completed = true;
            }
            return new ExecutionResult.Sleep(1);
        }

        @Override
        public boolean hasPendingSynchronizedCall() {
            return pendingCalls > 0;
        }

        @Override
        public boolean hasSynchronizedReturn() {
            return synchronizedReturn;
        }
    }

    private static final class SleepyProcessorDriver extends TestDriver implements Processor {
        @Override
        public String slot(final ItemStack stack) {
            return Slot.CPU;
        }

        @Override
        public int supportedComponents(final ItemStack stack) {
            return 4;
        }

        @Override
        public Class<? extends Architecture> architecture(final ItemStack stack) {
            return SleepyArchitecture.class;
        }
    }

    private static final class RebootingProcessorDriver extends TestDriver implements Processor {
        @Override
        public String slot(final ItemStack stack) {
            return Slot.CPU;
        }

        @Override
        public int supportedComponents(final ItemStack stack) {
            return 4;
        }

        @Override
        public Class<? extends Architecture> architecture(final ItemStack stack) {
            return RebootingArchitecture.class;
        }
    }

    private static final class RejectingProcessorDriver extends TestDriver implements Processor {
        @Override
        public String slot(final ItemStack stack) {
            return Slot.CPU;
        }

        @Override
        public int supportedComponents(final ItemStack stack) {
            return 4;
        }

        @Override
        public Class<? extends Architecture> architecture(final ItemStack stack) {
            return TestArchitecture.class;
        }
    }

    private static final class ImmediateYieldProcessorDriver extends TestDriver implements Processor {
        @Override
        public String slot(final ItemStack stack) {
            return Slot.CPU;
        }

        @Override
        public int supportedComponents(final ItemStack stack) {
            return 4;
        }

        @Override
        public Class<? extends Architecture> architecture(final ItemStack stack) {
            return ImmediateYieldArchitecture.class;
        }
    }

    private static final class TimedArchitecture implements Architecture {
        private static MutableClock clock;
        private boolean initialized;

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public boolean recomputeMemory(final Iterable<ItemStack> components) {
            return true;
        }

        @Override
        public boolean initialize() {
            initialized = true;
            return true;
        }

        @Override
        public void close() {
            initialized = false;
        }

        @Override
        public void runSynchronized() {
            clock.nanos += 1_000_000L;
        }

        @Override
        public ExecutionResult runThreaded(final boolean isSynchronizedReturn) {
            clock.nanos += 2_000_000L;
            return new ExecutionResult.Sleep(1);
        }

        @Override
        public void onSignal() {
        }

        @Override
        public void onConnect() {
        }

        @Override
        public void load(final CompoundTag nbt) {
            initialized = nbt.getBoolean("initialized");
        }

        @Override
        public void save(final CompoundTag nbt) {
            nbt.putBoolean("initialized", initialized);
        }
    }

    private static final class BoundProcessorDriver extends TestDriver implements Processor {
        @Override
        public String slot(final ItemStack stack) {
            return Slot.CPU;
        }

        @Override
        public int supportedComponents(final ItemStack stack) {
            return 4;
        }

        @Override
        public Class<? extends Architecture> architecture(final ItemStack stack) {
            return BoundArchitecture.class;
        }
    }

    public static final class BoundArchitecture extends TrackingArchitecture implements MachineBoundArchitecture {
        private Machine boundMachine;

        @Override
        public void bind(final Machine machine) {
            boundMachine = machine;
        }
    }

    public static final class SynchronizedBudgetArchitecture extends TrackingArchitecture implements MachineBoundArchitecture {
        private static String targetAddress;
        private static Exception error;
        private Machine machine;

        @Override
        public void bind(final Machine machine) {
            this.machine = machine;
            error = null;
        }

        @Override
        public void runSynchronized() {
            super.runSynchronized();
            try {
                machine.invoke(targetAddress, "direct", new Object[0]);
                machine.invoke(targetAddress, "direct", new Object[0]);
            } catch (Exception e) {
                error = e;
            }
        }
    }

    public static final class ThreadedBudgetArchitecture extends TrackingArchitecture implements MachineBoundArchitecture {
        private static String targetAddress;
        private static Exception error;
        private Machine machine;

        @Override
        public void bind(final Machine machine) {
            this.machine = machine;
            error = null;
        }

        @Override
        public ExecutionResult runThreaded(final boolean isSynchronizedReturn) {
            super.runThreaded(isSynchronizedReturn);
            try {
                machine.invoke(targetAddress, "direct", new Object[0]);
                machine.invoke(targetAddress, "direct", new Object[0]);
            } catch (Exception e) {
                error = e;
            }
            return new ExecutionResult.Sleep(1);
        }
    }

    public static class TrackingArchitecture implements Architecture {
        private boolean initialized;
        private int signalCount;
        protected int synchronizedRuns;
        protected int threadedRuns;

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public boolean recomputeMemory(final Iterable<ItemStack> components) {
            return true;
        }

        @Override
        public boolean initialize() {
            initialized = true;
            return true;
        }

        @Override
        public void close() {
            initialized = false;
        }

        @Override
        public void runSynchronized() {
            synchronizedRuns++;
        }

        @Override
        public ExecutionResult runThreaded(final boolean isSynchronizedReturn) {
            threadedRuns++;
            return new ExecutionResult.Sleep(1);
        }

        @Override
        public void onSignal() {
            signalCount++;
        }

        @Override
        public void onConnect() {
        }

        @Override
        public void load(final CompoundTag nbt) {
            initialized = nbt.getBoolean("initialized");
        }

        @Override
        public void save(final CompoundTag nbt) {
            nbt.putBoolean("initialized", initialized);
        }
    }

    public static final class SleepyArchitecture extends TrackingArchitecture {
        @Override
        public ExecutionResult runThreaded(final boolean isSynchronizedReturn) {
            threadedRuns++;
            return new ExecutionResult.Sleep(2);
        }
    }

    public static final class ImmediateYieldArchitecture extends TrackingArchitecture {
        @Override
        public ExecutionResult runThreaded(final boolean isSynchronizedReturn) {
            threadedRuns++;
            return new ExecutionResult.Sleep(0);
        }
    }

    public static final class RebootingArchitecture extends TrackingArchitecture {
        private boolean rebooted;

        @Override
        public ExecutionResult runThreaded(final boolean isSynchronizedReturn) {
            threadedRuns++;
            if (!rebooted) {
                rebooted = true;
                return new ExecutionResult.Shutdown(true);
            }
            return new ExecutionResult.Sleep(1);
        }
    }

    private static final class TestEnvironment extends AbstractManagedEnvironment {
        private final List<String> messages = new ArrayList<>();

        private TestEnvironment() {
            setNode(Network.newNode(this, Visibility.Network)
                .withComponent("test_component", Visibility.Network)
                .create());
        }

        @Override
        public void onMessage(final Message message) {
            messages.add(message.name());
        }
    }

    private static final class DirectBudgetEnvironment extends AbstractManagedEnvironment {
        private DirectBudgetEnvironment() {
            setNode(Network.newNode(this, Visibility.Network)
                .withComponent("budget_component", Visibility.Network)
                .create());
        }

        @li.cil.oc.api.machine.Callback(direct = true, limit = 2)
        public Object[] direct(final li.cil.oc.api.machine.Context context, final li.cil.oc.api.machine.Arguments arguments) {
            return new Object[]{"direct"};
        }

        @li.cil.oc.api.machine.Callback
        public Object[] regular(final li.cil.oc.api.machine.Context context, final li.cil.oc.api.machine.Arguments arguments) {
            return new Object[]{"regular"};
        }
    }

    private static final class SnapshotArgumentsEnvironment extends AbstractManagedEnvironment {
        private SnapshotArgumentsEnvironment() {
            setNode(Network.newNode(this, Visibility.Network)
                .withComponent("snapshot_component", Visibility.Network)
                .create());
        }

        @li.cil.oc.api.machine.Callback
        public Object[] snapshotFirst(final li.cil.oc.api.machine.Context context, final li.cil.oc.api.machine.Arguments arguments) {
            return new Object[]{arguments.toArray()[0]};
        }
    }

    private static final class FilteredValue extends AbstractValue implements FilteredEnvironment, MethodWhitelist {
        @Override
        public boolean isCallbackEnabled(final String name) {
            return !"hidden".equals(name);
        }

        @Override
        public String[] whitelistedMethods() {
            return new String[]{"echo", "hidden"};
        }

        @Callback
        public Object[] echo(final Context context, final Arguments arguments) {
            return new Object[]{"ok"};
        }

        @Callback
        public Object[] hidden(final Context context, final Arguments arguments) {
            return new Object[]{"hidden"};
        }

        @Callback
        public Object[] extra(final Context context, final Arguments arguments) {
            return new Object[]{"extra"};
        }
    }

    private static final class BlankCallbackNameValue extends AbstractValue {
        @Callback("   ")
        public Object[] fallback(final Context context, final Arguments arguments) {
            return new Object[]{"fallback"};
        }
    }

    private static final class InvalidCallbackValue extends AbstractValue {
        @Callback
        public Object[] valid(final Context context, final Arguments arguments) {
            return new Object[]{"valid"};
        }

        @Callback
        public String badReturn(final Context context, final Arguments arguments) {
            return "bad";
        }

        @Callback
        public Object[] badArgs() {
            return new Object[]{"bad"};
        }

        @Callback
        private Object[] privateCallback(final Context context, final Arguments arguments) {
            return new Object[]{"bad"};
        }
    }

    private static final class PeripheralValue extends AbstractValue implements ManagedPeripheral {
        @Override
        public String[] methods() {
            return new String[]{"dynamic"};
        }

        @Override
        public Object[] invoke(final String method, final Context context, final Arguments args) throws Exception {
            if (!"dynamic".equals(method)) {
                throw new NoSuchMethodException(method);
            }
            return new Object[]{"dynamic", args.checkString(0)};
        }
    }

    private static final class NumericArgumentsValue extends AbstractValue {
        @Callback
        public Object[] integerFlags(final Context context, final Arguments arguments) {
            return new Object[]{
                arguments.isInteger(0),
                arguments.isInteger(1),
                arguments.isLong(0),
                arguments.isLong(1)
            };
        }

        @Callback
        public Object[] clampIntegers(final Context context, final Arguments arguments) {
            return new Object[]{
                arguments.checkInteger(0),
                arguments.checkInteger(1)
            };
        }

        @Callback
        public Object[] checkInteger(final Context context, final Arguments arguments) {
            return new Object[]{arguments.checkInteger(0)};
        }

        @Callback
        public Object[] checkLong(final Context context, final Arguments arguments) {
            return new Object[]{arguments.checkLong(0)};
        }
    }

    private static final class DeviceInfoEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Test peripheral",
            DeviceInfo.DeviceAttribute.Vendor, "NeoOpenComputers",
            DeviceInfo.DeviceAttribute.Product, "Test Device"
        );

        private DeviceInfoEnvironment() {
            setNode(Network.newNode(this, Visibility.Network)
                .withComponent("test_device", Visibility.Network)
                .create());
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }
    }

    private static final class SavingDriver extends TestDriver {
        private final List<SavingEnvironment> environments = new ArrayList<>();

        @Override
        public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
            final SavingEnvironment environment = new SavingEnvironment();
            environments.add(environment);
            return environment;
        }
    }

    private static final class SavingEnvironment extends AbstractManagedEnvironment {
        private int saves;

        private SavingEnvironment() {
            setNode(Network.newNode(this, Visibility.Network)
                .withComponent("saving_component", Visibility.Network)
                .create());
        }

        @Override
        public void save(final CompoundTag nbt) {
            super.save(nbt);
            saves++;
        }
    }

    private record TestMessage(Node source, String name, Object[] data) implements Message {
        @Override
        public void cancel() {
        }
    }

    private static void writeTemporaryFile(final Machine machine, final String path, final String content) throws Exception {
        final String tmpAddress = machine.tmpAddress();
        final Object handle = machine.invoke(tmpAddress, "open", new Object[]{path, "w"})[0];
        machine.invoke(tmpAddress, "write", new Object[]{handle, content.getBytes(StandardCharsets.UTF_8)});
        machine.invoke(tmpAddress, "close", new Object[]{handle});
    }

    private static <T> void withCachedConfig(final ModConfigSpec.ConfigValue<T> value, final T override, final ThrowingRunnable action) throws Exception {
        final Field cachedValue = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cachedValue.setAccessible(true);
        final Object previous = cachedValue.get(value);
        cachedValue.set(value, override);
        try {
            action.run();
        } finally {
            cachedValue.set(value, previous);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class TestHost implements MachineHost, DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.System,
            DeviceInfo.DeviceAttribute.Description, "Test host",
            DeviceInfo.DeviceAttribute.Vendor, "NeoOpenComputers",
            DeviceInfo.DeviceAttribute.Product, "Test Computer"
        );
        private Iterable<ItemStack> components = Collections.singletonList(null);

        @Override
        public Machine machine() {
            return null;
        }

        @Override
        public Iterable<ItemStack> internalComponents() {
            return components;
        }

        @Override
        public int componentSlot(final String address) {
            return -1;
        }

        @Override
        public void onMachineConnect(final Node node) {
        }

        @Override
        public void onMachineDisconnect(final Node node) {
        }

        @Override
        public Level world() {
            return null;
        }

        @Override
        public double xPosition() {
            return 0;
        }

        @Override
        public double yPosition() {
            return 0;
        }

        @Override
        public double zPosition() {
            return 0;
        }

        @Override
        public void markChanged() {
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }
    }

    private static final class MutableClock implements LongSupplier {
        private long nanos;

        @Override
        public long getAsLong() {
            return nanos;
        }
    }
}
