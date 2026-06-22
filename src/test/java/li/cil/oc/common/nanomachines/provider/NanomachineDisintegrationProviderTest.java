package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(0, tag.size());
        assertEquals("", loaded.getNameHint());
    }

    private static List<String> behaviorNames(final Iterable<Behavior> behaviors) {
        final List<String> names = new ArrayList<>();
        for (final Behavior behavior : behaviors) {
            names.add(behavior.getNameHint());
        }
        return names;
    }
}
