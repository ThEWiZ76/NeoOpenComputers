package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NanomachinesRegistryTest {
    @AfterEach
    void resetApi() {
        API.nanomachines = null;
    }

    @Test
    void bootstrapInstallsNanomachinesApi() {
        OpenComputersApi.initialize();

        assertTrue(API.nanomachines instanceof NanomachinesRegistry);
    }

    @Test
    void registersProvidersInOrder() {
        NanomachinesRegistry registry = new NanomachinesRegistry();
        BehaviorProvider first = new TestBehaviorProvider();
        BehaviorProvider second = new TestBehaviorProvider();

        registry.addProvider(first);
        registry.addProvider(second);
        registry.addProvider(first);

        assertIterableEquals(List.of(first, second), registry.getProviders());
    }

    @Test
    void controllerRuntimeIsDeferred() {
        NanomachinesRegistry registry = new NanomachinesRegistry();

        assertFalse(registry.hasController(null));
        assertNull(registry.getController(null));
        assertNull(registry.installController(null));
        registry.uninstallController(null);
    }

    @Test
    void controllerPersistsBehaviorConfigurationThroughProviders() {
        NanomachinesRegistry registry = new NanomachinesRegistry();
        TrackingBehaviorProvider provider = new TrackingBehaviorProvider();
        registry.addProvider(provider);
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();

        controller.save(tag);

        assertEquals(1, provider.writeCount);

        SimpleNanomachineController loaded = new SimpleNanomachineController(null, registry);
        loaded.load(tag);

        assertEquals(1, provider.readCount);
    }

    @Test
    void controllerActivatesOnlyBehaviorsConnectedToActiveInputs() {
        TestBehavior first = new TestBehavior("first");
        TestBehavior second = new TestBehavior("second");
        TestBehavior third = new TestBehavior("third");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new ListBehaviorProvider(List.of(first, second, third)));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("activeInputs", new int[]{0});

        controller.load(tag);

        assertIterableEquals(List.of(first, third), controller.getActiveBehaviors());
        assertEquals(1, controller.getInputCount(first));
        assertEquals(0, controller.getInputCount(second));
        assertEquals(1, controller.getInputCount(third));
    }

    private static final class TestBehaviorProvider implements BehaviorProvider {
        @Override
        public Iterable<Behavior> createBehaviors(final Player player) {
            return List.of();
        }

        @Override
        public CompoundTag writeToNBT(final Behavior behavior) {
            return new CompoundTag();
        }

        @Override
        public Behavior readFromNBT(final Player player, final CompoundTag nbt) {
            return null;
        }
    }

    private record ListBehaviorProvider(List<Behavior> behaviors) implements BehaviorProvider {
        @Override
        public Iterable<Behavior> createBehaviors(final Player player) {
            return behaviors;
        }

        @Override
        public CompoundTag writeToNBT(final Behavior behavior) {
            return new CompoundTag();
        }

        @Override
        public Behavior readFromNBT(final Player player, final CompoundTag nbt) {
            return null;
        }
    }

    private static final class TrackingBehaviorProvider implements BehaviorProvider {
        private int writeCount;
        private int readCount;

        @Override
        public Iterable<Behavior> createBehaviors(final Player player) {
            return List.of(new TestBehavior("test"));
        }

        @Override
        public CompoundTag writeToNBT(final Behavior behavior) {
            writeCount++;
            final CompoundTag tag = new CompoundTag();
            tag.putString("name", behavior.getNameHint());
            return tag;
        }

        @Override
        public Behavior readFromNBT(final Player player, final CompoundTag nbt) {
            readCount++;
            return new TestBehavior("test");
        }
    }

    private record TestBehavior(String name) implements Behavior {
        @Override
        public String getNameHint() {
            return name;
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable(final li.cil.oc.api.nanomachines.DisableReason reason) {
        }

        @Override
        public void update() {
        }
    }
}
