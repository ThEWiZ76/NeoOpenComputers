package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.item.HoverBootsItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class HoverBootsHandler {
    private static final ResourceLocation STEP = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "hover_boots_step");
    // Modern players step 0.6 blocks by default; raise that to one without replacing other modifiers.
    private static final AttributeModifier STEP_MODIFIER = new AttributeModifier(STEP, 0.4, AttributeModifier.Operation.ADD_VALUE);

    private HoverBootsHandler() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(HoverBootsHandler::onTick);
        NeoForge.EVENT_BUS.addListener(HoverBootsHandler::onJump);
        NeoForge.EVENT_BUS.addListener(HoverBootsHandler::onFall);
    }

    private static void onTick(final PlayerTickEvent.Pre event) {
        final Player player = event.getEntity();
        final ItemStack stack = player.getItemBySlot(EquipmentSlot.FEET);
        final HoverBootsItem boots = stack.getItem() instanceof HoverBootsItem item ? item : null;
        if (boots != null && !ModSettings.ignorePower() && boots.getCharge(stack) == 0
            && !player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
        }
        if (player instanceof FakePlayer) return;
        boolean active = boots != null && !player.isShiftKeyDown();
        if (active && !ModSettings.ignorePower()) {
            if (player.onGround() && !player.isCreative()
                && player.level().getGameTime() % ModSettings.mfuTickFrequency() == 0
                && player.getDeltaMovement().lengthSqr() > 0.015F) {
                boots.charge(stack, -ModSettings.hoverBootMove(), false);
            }
            active = boots.getCharge(stack) > 0;
        }
        final var height = player.getAttribute(Attributes.STEP_HEIGHT);
        if (height != null) {
            if (active && !height.hasModifier(STEP)) height.addTransientModifier(STEP_MODIFIER);
            else if (!active) height.removeModifier(STEP);
        }
        if (active && !player.onGround() && player.fallDistance < 5 && player.getDeltaMovement().y < 0) {
            final var velocity = player.getDeltaMovement();
            player.setDeltaMovement(velocity.x, velocity.y * 0.9F, velocity.z);
        }
    }

    private static boolean pay(final Player player, final double cost) {
        final ItemStack stack = player.getItemBySlot(EquipmentSlot.FEET);
        if (!(stack.getItem() instanceof HoverBootsItem boots)) return false;
        if (ModSettings.ignorePower() || player.isCreative()) return true;
        if (boots.charge(stack, -cost, true) != -cost) return false;
        boots.charge(stack, -cost, false);
        return true;
    }

    private static void onJump(final LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player && !(player instanceof FakePlayer)
            && !player.isShiftKeyDown() && pay(player, ModSettings.hoverBootJump())) {
            final var velocity = player.getDeltaMovement();
            player.setDeltaMovement(velocity.add(player.isSprinting() ? velocity.x * 0.5 : 0, 0.4,
                player.isSprinting() ? velocity.z * 0.5 : 0));
        }
    }

    private static void onFall(final LivingFallEvent event) {
        if (event.getDistance() > 3 && event.getEntity() instanceof Player player && !(player instanceof FakePlayer)
            && pay(player, ModSettings.hoverBootAbsorb())) {
            event.setDistance(event.getDistance() * 0.3F);
        }
    }
}
