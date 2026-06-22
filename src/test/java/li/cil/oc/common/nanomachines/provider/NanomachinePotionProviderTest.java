package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NanomachinePotionProviderTest {
    @Test
    void createsWhitelistedPotionBehaviors() {
        final NanomachinePotionProvider provider = new NanomachinePotionProvider();

        final List<String> names = behaviorNames(provider.createBehaviors(null));

        assertTrue(names.contains("speed"));
        assertTrue(names.contains("haste"));
        assertTrue(names.contains("wither"));
    }

    @Test
    void roundTripsPotionIdThroughNbt() {
        final NanomachinePotionProvider provider = new NanomachinePotionProvider();
        final Behavior speed = behaviorByName(provider.createBehaviors(null), "speed");

        final CompoundTag tag = provider.writeToNBT(speed);
        final Behavior loaded = provider.readFromNBT(null, tag);

        assertEquals("c29e4eec-5a46-479a-9b3d-ad0f06da784a", tag.getString("provider"));
        loaded.onEnable();
        loaded.update();
        loaded.onDisable(DisableReason.Default);
        assertEquals("minecraft:speed", tag.getString("potionId"));
        assertEquals("speed", loaded.getNameHint());
    }

    @Test
    void ignoresBehaviorNbtOwnedByOtherProviders() {
        final NanomachinePotionProvider provider = new NanomachinePotionProvider();
        final CompoundTag tag = new CompoundTag();
        tag.putString("provider", "b48c4bbd-51bb-4915-9367-16cff3220e4b");
        tag.putString("effectName", "flame");

        assertNull(provider.readFromNBT(null, tag));
    }

    private static List<String> behaviorNames(final Iterable<Behavior> behaviors) {
        final List<String> names = new ArrayList<>();
        for (final Behavior behavior : behaviors) {
            names.add(behavior.getNameHint());
        }
        return names;
    }

    private static Behavior behaviorByName(final Iterable<Behavior> behaviors, final String name) {
        for (final Behavior behavior : behaviors) {
            if (name.equals(behavior.getNameHint())) {
                return behavior;
            }
        }
        throw new AssertionError("Missing behavior " + name);
    }
}
