package li.cil.oc.common.network;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NanomachineClientStateTest {
    @AfterEach
    void clearState() {
        NanomachineClientState.clear();
    }

    @Test
    void applyPowerPayloadStoresInstalledPowerState() {
        NanomachineClientState.apply(new NanomachinePowerPayload(true, 25D, 100D, 2, 8, List.of("flame", "heart")));

        assertTrue(NanomachineClientState.installed());
        assertEquals(25D, NanomachineClientState.buffer());
        assertEquals(100D, NanomachineClientState.maxBuffer());
        assertEquals(2, NanomachineClientState.activeInputs());
        assertEquals(8, NanomachineClientState.totalInputs());
        assertEquals(List.of("flame", "heart"), NanomachineClientState.activeParticleEffects());
        assertEquals(0.25D, NanomachineClientState.fill());
    }

    @Test
    void absentPayloadClearsPowerState() {
        NanomachineClientState.apply(new NanomachinePowerPayload(true, 25D, 100D, 2, 8, List.of("flame")));

        NanomachineClientState.apply(new NanomachinePowerPayload(false, 25D, 100D, 2, 8, List.of("flame")));

        assertFalse(NanomachineClientState.installed());
        assertEquals(0D, NanomachineClientState.buffer());
        assertEquals(0D, NanomachineClientState.maxBuffer());
        assertEquals(0, NanomachineClientState.activeInputs());
        assertEquals(0, NanomachineClientState.totalInputs());
        assertEquals(List.of(), NanomachineClientState.activeParticleEffects());
        assertEquals(0D, NanomachineClientState.fill());
    }

    @Test
    void invalidPowerPayloadValuesClampToEmptyState() {
        NanomachineClientState.apply(new NanomachinePowerPayload(true, Double.NaN, Double.NaN, -3, -1, null));

        assertTrue(NanomachineClientState.installed());
        assertEquals(0D, NanomachineClientState.buffer());
        assertEquals(0D, NanomachineClientState.maxBuffer());
        assertEquals(0, NanomachineClientState.activeInputs());
        assertEquals(0, NanomachineClientState.totalInputs());
        assertEquals(List.of(), NanomachineClientState.activeParticleEffects());
        assertEquals(0D, NanomachineClientState.fill());
    }
}
