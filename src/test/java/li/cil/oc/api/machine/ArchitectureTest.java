package li.cil.oc.api.machine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ArchitectureTest {
    @Test
    void exposesArchitectureLifecyclePersistenceAndAnnotations() {
        Architecture architecture = new TestArchitecture();
        CompoundTag tag = new CompoundTag();

        assertTrue(architecture.recomputeMemory(List.of()));
        assertTrue(architecture.initialize());
        assertTrue(architecture.isInitialized());
        assertSame(ExecutionResult.Sleep.class, architecture.runThreaded(false).getClass());
        architecture.runSynchronized();
        architecture.onSignal();
        architecture.onConnect();
        architecture.save(tag);
        architecture.load(tag);
        architecture.close();

        assertEquals("saved", tag.getString("state"));
        assertEquals("test", TestArchitecture.class.getAnnotation(Architecture.Name.class).value());
        assertNotNull(TestArchitecture.class.getAnnotation(Architecture.NoMemoryRequirements.class));
    }

    @Test
    void architectureAnnotationsKeepRuntimeTypeTargets() {
        Retention nameRetention = Architecture.Name.class.getAnnotation(Retention.class);
        Target nameTarget = Architecture.Name.class.getAnnotation(Target.class);
        Retention noMemoryRetention = Architecture.NoMemoryRequirements.class.getAnnotation(Retention.class);
        Target noMemoryTarget = Architecture.NoMemoryRequirements.class.getAnnotation(Target.class);

        assertSame(RetentionPolicy.RUNTIME, nameRetention.value());
        assertArrayEquals(new ElementType[]{ElementType.TYPE}, nameTarget.value());
        assertSame(RetentionPolicy.RUNTIME, noMemoryRetention.value());
        assertArrayEquals(new ElementType[]{ElementType.TYPE}, noMemoryTarget.value());
        assertNotNull(Architecture.NoMemoryRequirements.class.getAnnotation(Inherited.class));
    }

    @Architecture.Name("test")
    @Architecture.NoMemoryRequirements
    private static final class TestArchitecture implements Architecture {
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
            return new ExecutionResult.Sleep(0);
        }

        @Override
        public void onSignal() {
        }

        @Override
        public void onConnect() {
        }

        @Override
        public void load(final CompoundTag nbt) {
            initialized = "saved".equals(nbt.getString("state"));
        }

        @Override
        public void save(final CompoundTag nbt) {
            nbt.putString("state", "saved");
        }
    }
}
