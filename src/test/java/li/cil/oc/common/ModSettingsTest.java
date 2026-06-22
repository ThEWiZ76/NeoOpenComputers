package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ModSettingsTest {
    @Test
    void mfuSettingsExposeUpstreamDefaultsWhenConfigIsUnloaded() {
        assertEquals(3D, ModSettings.mfuRange());
        assertEquals(1D, ModSettings.mfuRelayCost());
        assertEquals(10, ModSettings.mfuTickFrequency());
        assertEquals(0.2D, ModSettings.solarGeneratorEfficiency());
        assertEquals(true, ModSettings.inputUsername());
    }

    @Test
    void mfuConfigUsesUpstreamCompatiblePaths() {
        assertEquals(List.of("misc", "mfuRange"), ModSettings.MFU_RANGE.getPath());
        assertEquals(List.of("power", "cost", "mfuRelay"), ModSettings.MFU_RELAY_COST.getPath());
        assertEquals(List.of("power", "tickFrequency"), ModSettings.MFU_TICK_FREQUENCY.getPath());
        assertEquals(List.of("power", "solarGeneratorEfficiency"), ModSettings.SOLAR_GENERATOR_EFFICIENCY.getPath());
        assertEquals(List.of("misc", "inputUsername"), ModSettings.INPUT_USERNAME.getPath());
    }
}
