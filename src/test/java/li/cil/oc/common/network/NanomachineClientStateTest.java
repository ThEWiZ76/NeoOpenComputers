package li.cil.oc.common.network;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
        NanomachineClientState.apply(new NanomachinePowerPayload(true, 25D, 100D));

        assertTrue(NanomachineClientState.installed());
        assertEquals(25D, NanomachineClientState.buffer());
        assertEquals(100D, NanomachineClientState.maxBuffer());
        assertEquals(0.25D, NanomachineClientState.fill());
    }

    @Test
    void absentPayloadClearsPowerState() {
        NanomachineClientState.apply(new NanomachinePowerPayload(true, 25D, 100D));

        NanomachineClientState.apply(new NanomachinePowerPayload(false, 25D, 100D));

        assertFalse(NanomachineClientState.installed());
        assertEquals(0D, NanomachineClientState.buffer());
        assertEquals(0D, NanomachineClientState.maxBuffer());
        assertEquals(0D, NanomachineClientState.fill());
    }
}
