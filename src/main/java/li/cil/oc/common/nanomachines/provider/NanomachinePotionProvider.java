package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.prefab.AbstractProvider;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class NanomachinePotionProvider extends AbstractProvider {
    private static final String PROVIDER_ID = "c29e4eec-5a46-479a-9b3d-ad0f06da784a";
    private static final String POTION_ID_TAG = "potionId";
    private static final int DURATION = 600;
    private static final Map<String, Integer> UPSTREAM_POTION_ORDER = Map.ofEntries(
        Map.entry("speed", 1),
        Map.entry("slowness", 2),
        Map.entry("haste", 3),
        Map.entry("mining_fatigue", 4),
        Map.entry("strength", 5),
        Map.entry("instant_health", 6),
        Map.entry("instant_damage", 7),
        Map.entry("jump_boost", 8),
        Map.entry("nausea", 9),
        Map.entry("regeneration", 10),
        Map.entry("resistance", 11),
        Map.entry("fire_resistance", 12),
        Map.entry("water_breathing", 13),
        Map.entry("invisibility", 14),
        Map.entry("blindness", 15),
        Map.entry("night_vision", 16),
        Map.entry("hunger", 17),
        Map.entry("weakness", 18),
        Map.entry("poison", 19),
        Map.entry("wither", 20),
        Map.entry("health_boost", 21),
        Map.entry("absorption", 22));

    public NanomachinePotionProvider() {
        super(PROVIDER_ID);
    }

    @Override
    public Iterable<Behavior> createBehaviors(final Player player) {
        final Set<ResourceLocation> whitelist = potionWhitelist();
        try {
            return BuiltInRegistries.MOB_EFFECT.holders()
                .map(NanomachinePotionProvider::effectId)
                .filter(whitelist::contains)
                .map(effectId -> new PotionBehavior(effectId, player))
                .map(Behavior.class::cast)
                .toList();
        } catch (final ExceptionInInitializerError | NoClassDefFoundError ignored) {
            return fallbackPotionOrder(whitelist).stream()
                .map(effectId -> new PotionBehavior(effectId, player))
                .map(Behavior.class::cast)
                .toList();
        }
    }

    @Override
    protected void writeBehaviorToNBT(final Behavior behavior, final CompoundTag tag) {
        if (behavior instanceof PotionBehavior potionBehavior) {
            tag.putString(POTION_ID_TAG, potionBehavior.effectId().toString());
        }
    }

    @Override
    protected Behavior readBehaviorFromNBT(final Player player, final CompoundTag nbt) {
        return new PotionBehavior(resourceLocation(nbt.getString(POTION_ID_TAG)), player);
    }

    private static Set<ResourceLocation> potionWhitelist() {
        final Set<ResourceLocation> whitelist = new LinkedHashSet<>();
        for (final Object entry : ModSettings.nanomachinesPotionWhitelist()) {
            effectIdFromConfigEntry(entry).ifPresent(whitelist::add);
        }
        return whitelist;
    }

    private static Optional<ResourceLocation> effectIdFromConfigEntry(final Object entry) {
        if (entry instanceof String id) {
            return effectIdFromString(id);
        }
        if (entry instanceof Number number) {
            return effectFromNumericId(number.intValue());
        }
        return Optional.empty();
    }

    private static Optional<ResourceLocation> effectIdFromString(final String id) {
        final ResourceLocation effectId = resourceLocation(id);
        try {
            return BuiltInRegistries.MOB_EFFECT.getHolder(effectId)
                .map(NanomachinePotionProvider::effectId);
        } catch (final ExceptionInInitializerError | NoClassDefFoundError ignored) {
            return Optional.of(effectId);
        } catch (final IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<ResourceLocation> effectFromNumericId(final int id) {
        try {
            final Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.asHolderIdMap().byId(id);
            return effect == null ? Optional.empty() : Optional.of(effectId(effect));
        } catch (final IllegalArgumentException | ExceptionInInitializerError | NoClassDefFoundError ignored) {
            return Optional.empty();
        }
    }

    private static List<ResourceLocation> fallbackPotionOrder(final Set<ResourceLocation> whitelist) {
        return whitelist.stream()
            .sorted(Comparator
                .comparingInt(NanomachinePotionProvider::upstreamPotionOrder)
                .thenComparing(ResourceLocation::toString))
            .toList();
    }

    private static int upstreamPotionOrder(final ResourceLocation effectId) {
        return UPSTREAM_POTION_ORDER.getOrDefault(effectId.getPath(), Integer.MAX_VALUE);
    }

    private static Optional<Holder.Reference<MobEffect>> effectFromId(final ResourceLocation id) {
        if (id == null) {
            return Optional.empty();
        }
        try {
            return BuiltInRegistries.MOB_EFFECT.getHolder(id);
        } catch (final IllegalArgumentException | ExceptionInInitializerError | NoClassDefFoundError ignored) {
            return Optional.empty();
        }
    }

    private static ResourceLocation resourceLocation(final String id) {
        if (id == null || id.isBlank()) {
            return ResourceLocation.withDefaultNamespace("empty");
        }
        return id.indexOf(':') >= 0 ? ResourceLocation.parse(id) : ResourceLocation.withDefaultNamespace(id);
    }

    private static ResourceLocation effectId(final Holder<MobEffect> effect) {
        return effect.unwrapKey()
            .map(key -> key.location())
            .orElseGet(() -> BuiltInRegistries.MOB_EFFECT.getKey(effect.value()));
    }

    private record PotionBehavior(ResourceLocation effectId, Player player) implements Behavior {
        @Override
        public String getNameHint() {
            return effectId.getPath();
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable(final DisableReason reason) {
            if (player == null) {
                return;
            }
            final Optional<Holder.Reference<MobEffect>> effect = effectFromId(effectId);
            if (effect.isPresent()) {
                player.removeEffect(effect.get());
            }
        }

        @Override
        public void update() {
            if (player == null) {
                return;
            }
            final Optional<Holder.Reference<MobEffect>> effect = effectFromId(effectId);
            if (effect.isEmpty()) {
                return;
            }
            final Controller controller = Nanomachines.getController(player);
            final int amplifier = controller == null ? 0 : Math.max(0, controller.getInputCount(this) - 1);
            player.addEffect(new MobEffectInstance(effect.get(), DURATION, amplifier, true, ModSettings.enableNanomachinePfx()));
        }
    }
}
