package li.cil.oc.client;

import li.cil.oc.common.ModSettings;
import li.cil.oc.common.network.NanomachineClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

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

    static void spawnAmbient(final Minecraft minecraft) {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return;
        }
        final double chance = ambientChance(ModSettings.enableNanomachinePfx());
        if (!shouldSpawn(chance, minecraft.level.random.nextDouble())) {
            return;
        }
        spawnParticleAround(minecraft.player);
    }

    private static void spawnParticleAround(final Player player) {
        final AABB bounds = player.getBoundingBox();
        final double x = bounds.minX + bounds.getXsize() * player.getRandom().nextDouble() * 1.5D;
        final double y = bounds.minY + bounds.getYsize() * player.getRandom().nextDouble() * 0.5D;
        final double z = bounds.minZ + bounds.getZsize() * player.getRandom().nextDouble() * 1.5D;
        player.level().addParticle(ParticleTypes.PORTAL, x, y, z, 0D, 0D, 0D);
    }
}
