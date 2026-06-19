package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.Network;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MachineRegistryTest {
    @AfterEach
    void resetApi() {
        API.machine = null;
        API.network = null;
        li.cil.oc.api.Machine.LuaArchitecture = null;
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
        assertEquals(1, machine.componentCount());
        assertArrayEquals(new Object[]{false}, machine.invoke(fileSystemEnvironment.node().address(), "isReadOnly", new Object[0]));
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

    public static final class TrackingArchitecture implements Architecture {
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
        }

        @Override
        public ExecutionResult runThreaded(final boolean isSynchronizedReturn) {
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

    private static final class TestEnvironment extends AbstractManagedEnvironment {
        private TestEnvironment() {
            setNode(Network.newNode(this, Visibility.Network)
                .withComponent("test_component", Visibility.Network)
                .create());
        }
    }

    private static final class TestHost implements MachineHost {
        @Override
        public Machine machine() {
            return null;
        }

        @Override
        public Iterable<ItemStack> internalComponents() {
            return Collections.singletonList(null);
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
    }
}
