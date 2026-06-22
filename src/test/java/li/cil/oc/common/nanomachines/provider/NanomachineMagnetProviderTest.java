package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class NanomachineMagnetProviderTest {
    @Test
    void createsOneMagnetBehavior() {
        final NanomachineMagnetProvider provider = new NanomachineMagnetProvider();

        final List<String> names = behaviorNames(provider.createBehaviors(null));

        assertEquals(List.of("magnet"), names);
    }

    @Test
    void roundTripsMagnetBehaviorWithoutState() {
        final NanomachineMagnetProvider provider = new NanomachineMagnetProvider();
        final Behavior behavior = provider.createBehaviors(null).iterator().next();

        final CompoundTag tag = provider.writeToNBT(behavior);
        final Behavior loaded = provider.readFromNBT(null, tag);

        loaded.onEnable();
        loaded.update();
        loaded.onDisable(DisableReason.Default);
        assertEquals("9324d5ec-71f1-41c2-b51c-406e527668fc", tag.getString("provider"));
        assertEquals("magnet", loaded.getNameHint());
    }

    @Test
    void ignoresBehaviorNbtOwnedByOtherProviders() {
        final NanomachineMagnetProvider provider = new NanomachineMagnetProvider();
        final CompoundTag tag = new CompoundTag();
        tag.putString("provider", "c4e7e3c2-8069-4fbb-b08e-74b1bddcdfe7");

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
