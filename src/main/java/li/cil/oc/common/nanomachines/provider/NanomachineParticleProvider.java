package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.prefab.AbstractProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;

public final class NanomachineParticleProvider extends AbstractProvider {
    private static final String PROVIDER_ID = "b48c4bbd-51bb-4915-9367-16cff3220e4b";
    private static final String EFFECT_TAG = "effectName";
    private static final Map<String, Integer> EFFECT_IDS = Map.ofEntries(
        Map.entry("fireworksSpark", 3),
        Map.entry("townaura", 22),
        Map.entry("smoke", 11),
        Map.entry("witchMagic", 17),
        Map.entry("note", 23),
        Map.entry("enchantmenttable", 25),
        Map.entry("flame", 26),
        Map.entry("lava", 27),
        Map.entry("splash", 5),
        Map.entry("reddust", 30),
        Map.entry("slime", 33),
        Map.entry("heart", 34),
        Map.entry("happyVillager", 21));
    private static final Map<Integer, String> EFFECT_NAMES_BY_ID = Map.ofEntries(
        Map.entry(3, "fireworksSpark"),
        Map.entry(22, "townaura"),
        Map.entry(11, "smoke"),
        Map.entry(17, "witchMagic"),
        Map.entry(23, "note"),
        Map.entry(25, "enchantmenttable"),
        Map.entry(26, "flame"),
        Map.entry(27, "lava"),
        Map.entry(5, "splash"),
        Map.entry(30, "reddust"),
        Map.entry(33, "slime"),
        Map.entry(34, "heart"),
        Map.entry(21, "happyVillager"));
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
            tag.putInt(EFFECT_TAG, EFFECT_IDS.get(particleBehavior.effectName()));
        }
    }

    @Override
    protected Behavior readBehaviorFromNBT(final Player player, final CompoundTag nbt) {
        final String effectName = effectNameFromNBT(nbt);
        if (EFFECT_NAMES.contains(effectName)) {
            return new ParticleBehavior(effectName);
        }
        return null;
    }

    private static String effectNameFromNBT(final CompoundTag nbt) {
        if (nbt.contains(EFFECT_TAG, Tag.TAG_STRING)) {
            return nbt.getString(EFFECT_TAG);
        }
        if (nbt.contains(EFFECT_TAG, Tag.TAG_INT)) {
            return EFFECT_NAMES_BY_ID.get(nbt.getInt(EFFECT_TAG));
        }
        return "";
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
