package li.cil.oc.client;

import li.cil.oc.common.ModSettings;
import li.cil.oc.common.network.NanomachineClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

final class NanomachineParticles {
    private NanomachineParticles() {
    }

    static double ambientChance(final boolean particlesEnabled) {
        if (!particlesEnabled || !NanomachineClientState.installed() || NanomachineClientState.buffer() <= 0D) {
            return 0D;
        }
        final double energyRatio = NanomachineClientState.buffer() / (NanomachineClientState.maxBuffer() + 1D);
        final double triggerRatio = (double) NanomachineClientState.activeInputs() / (NanomachineClientState.totalInputs() + 1D);
        return Math.max(0D, (energyRatio + triggerRatio) * 0.25D);
    }

    static boolean shouldSpawn(final double chance, final double sample) {
        return chance >= 1D || (chance > 0D && sample < chance);
    }

    static List<String> ambientParticleEffects() {
        return NanomachineClientState.activeParticleEffects();
    }

    static String ambientParticleEffect(final int index) {
        final List<String> effects = ambientParticleEffects();
        if (effects.isEmpty()) {
            return "portal";
        }
        return effects.get(Math.floorMod(index, effects.size()));
    }

    static ParticleOptions particleType(final String effectName) {
        return switch (effectName) {
            case "fireworksSpark" -> ParticleTypes.FIREWORK;
            case "townaura" -> ParticleTypes.MYCELIUM;
            case "smoke" -> ParticleTypes.SMOKE;
            case "witchMagic" -> ParticleTypes.WITCH;
            case "note" -> ParticleTypes.NOTE;
            case "enchantmenttable" -> ParticleTypes.ENCHANT;
            case "flame" -> ParticleTypes.FLAME;
            case "lava" -> ParticleTypes.LAVA;
            case "splash" -> ParticleTypes.SPLASH;
            case "slime" -> ParticleTypes.ITEM_SLIME;
            case "heart" -> ParticleTypes.HEART;
            case "happyVillager" -> ParticleTypes.HAPPY_VILLAGER;
            default -> ParticleTypes.PORTAL;
        };
    }

    static void spawnAmbient(final Minecraft minecraft) {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return;
        }
        final double chance = ambientChance(ModSettings.enableNanomachinePfx());
        if (!shouldSpawn(chance, minecraft.level.random.nextDouble())) {
            return;
        }
        final List<String> effects = ambientParticleEffects();
        final ParticleOptions particle = particleType(ambientParticleEffect(effects.isEmpty() ? 0 : minecraft.level.random.nextInt(effects.size())));
        spawnParticleAround(minecraft.player, particle);
    }

    private static void spawnParticleAround(final Player player, final ParticleOptions particle) {
        final AABB bounds = player.getBoundingBox();
        final double x = bounds.minX + bounds.getXsize() * player.getRandom().nextDouble() * 1.5D;
        final double y = bounds.minY + bounds.getYsize() * player.getRandom().nextDouble() * 0.5D;
        final double z = bounds.minZ + bounds.getZsize() * player.getRandom().nextDouble() * 1.5D;
        player.level().addParticle(particle, x, y, z, 0D, 0D, 0D);
    }
}
