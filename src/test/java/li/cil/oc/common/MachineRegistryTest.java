package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.Signal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
    }

    @Test
    void bootstrapInstallsMachineApi() {
        OpenComputersApi.initialize();

        assertTrue(API.machine instanceof MachineRegistry);
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
}
