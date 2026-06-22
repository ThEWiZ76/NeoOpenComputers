package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.api.nanomachines.Behavior;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class NanomachineParticleProviderTest {
    @Test
    void createsUpstreamParticleBehaviorsInOrder() {
        final NanomachineParticleProvider provider = new NanomachineParticleProvider();

        final List<String> names = behaviorNames(provider.createBehaviors(null));

        assertEquals(List.of(
            "particles.fireworksSpark",
            "particles.townaura",
            "particles.smoke",
            "particles.witchMagic",
            "particles.note",
            "particles.enchantmenttable",
            "particles.flame",
            "particles.lava",
            "particles.splash",
            "particles.reddust",
            "particles.slime",
            "particles.heart",
            "particles.happyVillager"), names);
    }

    @Test
    void roundTripsParticleEffectNameThroughNbt() {
        final NanomachineParticleProvider provider = new NanomachineParticleProvider();
        final Behavior flame = behaviorByName(provider.createBehaviors(null), "particles.flame");

        final CompoundTag tag = provider.writeToNBT(flame);
        final Behavior loaded = provider.readFromNBT(null, tag);

        assertEquals("b48c4bbd-51bb-4915-9367-16cff3220e4b", tag.getString("provider"));
        assertEquals("flame", tag.getString("effectName"));
        assertEquals("particles.flame", loaded.getNameHint());
    }

    @Test
    void ignoresBehaviorNbtOwnedByOtherProviders() {
        final NanomachineParticleProvider provider = new NanomachineParticleProvider();
        final CompoundTag tag = new CompoundTag();
        tag.putString("provider", "c29e4eec-5a46-479a-9b3d-ad0f06da784a");
        tag.putString("potionId", "minecraft:speed");

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
