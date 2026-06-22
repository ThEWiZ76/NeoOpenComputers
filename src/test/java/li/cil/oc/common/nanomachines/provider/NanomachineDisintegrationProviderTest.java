package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class NanomachineDisintegrationProviderTest {
    @Test
    void createsOneHiddenDisintegrationBehavior() {
        final NanomachineDisintegrationProvider provider = new NanomachineDisintegrationProvider();

        final List<String> names = behaviorNames(provider.createBehaviors(null));

        assertEquals(List.of(""), names);
    }

    @Test
    void roundTripsDisintegrationBehaviorWithoutState() {
        final NanomachineDisintegrationProvider provider = new NanomachineDisintegrationProvider();
        final Behavior behavior = provider.createBehaviors(null).iterator().next();

        final CompoundTag tag = provider.writeToNBT(behavior);
        final Behavior loaded = provider.readFromNBT(null, tag);

        loaded.onEnable();
        loaded.update();
        loaded.onDisable(DisableReason.Default);
        assertEquals("c4e7e3c2-8069-4fbb-b08e-74b1bddcdfe7", tag.getString("provider"));
        assertEquals("", loaded.getNameHint());
    }

    @Test
    void ignoresBehaviorNbtOwnedByOtherProviders() {
        final NanomachineDisintegrationProvider provider = new NanomachineDisintegrationProvider();
        final CompoundTag tag = new CompoundTag();
        tag.putString("provider", "d697c24a-014c-4773-a288-23084a59e9e8");

        assertNull(provider.readFromNBT(null, tag));
    }

    private static List<String> behaviorNames(final Iterable<Behavior> behaviors) {
        final List<String> names = new ArrayList<>();
        for (final Behavior behavior : behaviors) {
            names.add(behavior.getNameHint());
        }
        return names;
    }
}
