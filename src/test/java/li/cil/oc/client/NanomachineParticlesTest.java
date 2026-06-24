package li.cil.oc.client;

import li.cil.oc.common.network.NanomachineClientState;
import li.cil.oc.common.network.NanomachinePowerPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NanomachineParticlesTest {
    @AfterEach
    void clearState() {
        NanomachineClientState.clear();
    }

    @Test
    void ambientChanceUsesUpstreamEnergyAndTriggerRatios() {
        NanomachineClientState.apply(new NanomachinePowerPayload(true, 50D, 100D, 2, 4));

        final double expected = ((50D / 101D) + (2D / 5D)) * 0.25D;

        assertEquals(expected, NanomachineParticles.ambientChance(true));
    }

    @Test
    void ambientChanceIsZeroWhenParticlesDisabledOrControllerAbsent() {
        NanomachineClientState.apply(new NanomachinePowerPayload(true, 50D, 100D, 2, 4));

        assertEquals(0D, NanomachineParticles.ambientChance(false));

        NanomachineClientState.clear();

        assertEquals(0D, NanomachineParticles.ambientChance(true));
    }

    @Test
    void ambientSpawnDecisionUsesChanceThreshold() {
        assertEquals(true, NanomachineParticles.shouldSpawn(1D, 0.99D));
        assertEquals(true, NanomachineParticles.shouldSpawn(0.25D, 0.24D));
        assertEquals(false, NanomachineParticles.shouldSpawn(0.25D, 0.25D));
        assertEquals(false, NanomachineParticles.shouldSpawn(0D, 0D));
    }

    @Test
    void usesSyncedParticleEffectsWhenAvailable() {
        NanomachineClientState.apply(new NanomachinePowerPayload(true, 50D, 100D, 1, 2, List.of("flame", "heart")));

        assertEquals(List.of("flame", "heart"), NanomachineParticles.ambientParticleEffects());
        assertEquals("flame", NanomachineParticles.ambientParticleEffect(0));
        assertEquals("heart", NanomachineParticles.ambientParticleEffect(1));
        assertEquals("flame", NanomachineParticles.ambientParticleEffect(2));

        NanomachineClientState.clear();

        assertEquals("portal", NanomachineParticles.ambientParticleEffect(0));
    }

    @Test
    void activeParticleEffectsUseUpstreamPerInputChance() {
        NanomachineClientState.apply(new NanomachinePowerPayload(true, 50D, 100D, 3, 4, List.of("flame", "flame", "heart")));

        assertEquals(Map.of(
            "flame", 0.5D,
            "heart", 0.25D), NanomachineParticles.activeEffectSpawnChances());
    }

    @Test
    void mapsUpstreamParticleEffectNamesToModernParticleNames() {
        assertEquals("firework", NanomachineParticles.particleTypeName("fireworksSpark"));
        assertEquals("mycelium", NanomachineParticles.particleTypeName("townaura"));
        assertEquals("dust", NanomachineParticles.particleTypeName("reddust"));
        assertEquals("happy_villager", NanomachineParticles.particleTypeName("happyVillager"));
        assertEquals("portal", NanomachineParticles.particleTypeName("unknown"));
    }
}
