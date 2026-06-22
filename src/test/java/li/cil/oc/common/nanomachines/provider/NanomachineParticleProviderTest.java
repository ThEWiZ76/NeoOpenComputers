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
    void roundTripsParticleEffectNameThroughUpstreamNumericNbt() {
        final NanomachineParticleProvider provider = new NanomachineParticleProvider();
        final Behavior flame = behaviorByName(provider.createBehaviors(null), "particles.flame");

        final CompoundTag tag = provider.writeToNBT(flame);
        final Behavior loaded = provider.readFromNBT(null, tag);

        assertEquals("b48c4bbd-51bb-4915-9367-16cff3220e4b", tag.getString("provider"));
        assertEquals(26, tag.getInt("effectName"));
        assertEquals("particles.flame", loaded.getNameHint());
    }

    @Test
    void writesUpstreamParticleIdsToNbt() {
        final NanomachineParticleProvider provider = new NanomachineParticleProvider();
        final List<Behavior> behaviors = behaviorList(provider.createBehaviors(null));

        assertEquals(3, provider.writeToNBT(behaviors.get(0)).getInt("effectName"));
        assertEquals(22, provider.writeToNBT(behaviors.get(1)).getInt("effectName"));
        assertEquals(30, provider.writeToNBT(behaviors.get(9)).getInt("effectName"));
        assertEquals(21, provider.writeToNBT(behaviors.get(12)).getInt("effectName"));
    }

    @Test
    void readsUpstreamParticleIdsFromNbt() {
        final NanomachineParticleProvider provider = new NanomachineParticleProvider();

        assertEquals("particles.fireworksSpark", provider.readFromNBT(null, particleTag(3)).getNameHint());
        assertEquals("particles.townaura", provider.readFromNBT(null, particleTag(22)).getNameHint());
        assertEquals("particles.reddust", provider.readFromNBT(null, particleTag(30)).getNameHint());
        assertEquals("particles.happyVillager", provider.readFromNBT(null, particleTag(21)).getNameHint());
    }

    @Test
    void stillReadsStringParticleNamesFromEarlierNeoOpenComputersSaves() {
        final NanomachineParticleProvider provider = new NanomachineParticleProvider();
        final CompoundTag tag = new CompoundTag();
        tag.putString("provider", "b48c4bbd-51bb-4915-9367-16cff3220e4b");
        tag.putString("effectName", "flame");

        assertEquals("particles.flame", provider.readFromNBT(null, tag).getNameHint());
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

    private static List<Behavior> behaviorList(final Iterable<Behavior> behaviors) {
        final List<Behavior> list = new ArrayList<>();
        for (final Behavior behavior : behaviors) {
            list.add(behavior);
        }
        return list;
    }

    private static CompoundTag particleTag(final int effectId) {
        final CompoundTag tag = new CompoundTag();
        tag.putString("provider", "b48c4bbd-51bb-4915-9367-16cff3220e4b");
        tag.putInt("effectName", effectId);
        return tag;
    }
}
