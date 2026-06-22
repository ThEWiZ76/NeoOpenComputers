package li.cil.oc.client;

import li.cil.oc.common.network.NanomachineClientState;
import li.cil.oc.common.network.NanomachinePowerPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
}
