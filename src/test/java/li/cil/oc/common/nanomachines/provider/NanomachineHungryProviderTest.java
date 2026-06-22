package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class NanomachineHungryProviderTest {
    @Test
    void createsTenHiddenHungryBehaviors() {
        final NanomachineHungryProvider provider = new NanomachineHungryProvider();

        final List<String> names = behaviorNames(provider.createBehaviors(null));

        assertEquals(List.of("", "", "", "", "", "", "", "", "", ""), names);
    }

    @Test
    void roundTripsHungryBehaviorWithoutState() {
        final NanomachineHungryProvider provider = new NanomachineHungryProvider();
        final Behavior behavior = provider.createBehaviors(null).iterator().next();

        final CompoundTag tag = provider.writeToNBT(behavior);
        final Behavior loaded = provider.readFromNBT(null, tag);

        loaded.onEnable();
        loaded.update();
        loaded.onDisable(DisableReason.Default);
        assertEquals("d697c24a-014c-4773-a288-23084a59e9e8", tag.getString("provider"));
        assertEquals("", loaded.getNameHint());
    }

    @Test
    void ignoresBehaviorNbtOwnedByOtherProviders() {
        final NanomachineHungryProvider provider = new NanomachineHungryProvider();
        final CompoundTag tag = new CompoundTag();
        tag.putString("provider", "9324d5ec-71f1-41c2-b51c-406e527668fc");

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
