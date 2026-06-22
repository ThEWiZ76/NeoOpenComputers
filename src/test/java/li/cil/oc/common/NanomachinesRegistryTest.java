package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
