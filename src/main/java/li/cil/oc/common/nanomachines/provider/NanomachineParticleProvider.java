package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.prefab.AbstractProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class NanomachineParticleProvider extends AbstractProvider {
    private static final String PROVIDER_ID = "b48c4bbd-51bb-4915-9367-16cff3220e4b";
    private static final String EFFECT_TAG = "effectName";
    private static final List<String> EFFECT_NAMES = List.of(
        "fireworksSpark",
        "townaura",
        "smoke",
        "witchMagic",
        "note",
        "enchantmenttable",
        "flame",
        "lava",
        "splash",
        "reddust",
        "slime",
        "heart",
        "happyVillager");

    public NanomachineParticleProvider() {
        super(PROVIDER_ID);
    }

    @Override
    public Iterable<Behavior> createBehaviors(final Player player) {
        return EFFECT_NAMES.stream()
            .map(ParticleBehavior::new)
            .map(Behavior.class::cast)
            .toList();
    }

    @Override
    protected void writeBehaviorToNBT(final Behavior behavior, final CompoundTag tag) {
        if (behavior instanceof ParticleBehavior particleBehavior) {
            tag.putString(EFFECT_TAG, particleBehavior.effectName());
        }
    }

    @Override
    protected Behavior readBehaviorFromNBT(final Player player, final CompoundTag nbt) {
        final String effectName = nbt.getString(EFFECT_TAG);
        if (EFFECT_NAMES.contains(effectName)) {
            return new ParticleBehavior(effectName);
        }
        return null;
    }

    private record ParticleBehavior(String effectName) implements Behavior {
        private ParticleBehavior {
            if (!EFFECT_NAMES.contains(effectName)) {
                throw new IllegalArgumentException("Unknown nanomachine particle effect " + effectName);
            }
        }

        @Override
        public String getNameHint() {
            return "particles." + effectName;
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable(final DisableReason reason) {
        }

        @Override
        public void update() {
        }
    }
}
