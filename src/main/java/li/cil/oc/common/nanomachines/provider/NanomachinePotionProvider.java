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

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class NanomachinePotionProvider extends AbstractProvider {
    private static final String PROVIDER_ID = "c29e4eec-5a46-479a-9b3d-ad0f06da784a";
    private static final String POTION_ID_TAG = "potionId";
    private static final int DURATION = 600;

    public NanomachinePotionProvider() {
        super(PROVIDER_ID);
    }

    @Override
    public Iterable<Behavior> createBehaviors(final Player player) {
        return potionWhitelist().stream()
            .map(effectId -> new PotionBehavior(effectId, player))
            .map(Behavior.class::cast)
            .toList();
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
            return Optional.of(resourceLocation(id));
        }
        if (entry instanceof Number number) {
            return effectFromNumericId(number.intValue());
        }
        return Optional.empty();
    }

    private static Optional<ResourceLocation> effectFromNumericId(final int id) {
        try {
            final Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.asHolderIdMap().byId(id);
            return effect == null ? Optional.empty() : Optional.of(effectId(effect));
        } catch (final IllegalArgumentException | ExceptionInInitializerError | NoClassDefFoundError ignored) {
            return Optional.empty();
        }
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
