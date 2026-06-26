package li.cil.oc.common.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SolarGeneratorUpgradeEnvironmentTest {
    @Test
    void weatherBlocksSunOnlyForBiomesWithPrecipitationLikeUpstream() {
        assertTrue(SolarGeneratorUpgradeEnvironment.weatherAllowsSun(false, true, false));
        assertTrue(SolarGeneratorUpgradeEnvironment.weatherAllowsSun(false, false, true));
        assertTrue(SolarGeneratorUpgradeEnvironment.weatherAllowsSun(false, true, true));
        assertTrue(SolarGeneratorUpgradeEnvironment.weatherAllowsSun(true, false, false));

        assertFalse(SolarGeneratorUpgradeEnvironment.weatherAllowsSun(true, true, false));
        assertFalse(SolarGeneratorUpgradeEnvironment.weatherAllowsSun(true, false, true));
        assertFalse(SolarGeneratorUpgradeEnvironment.weatherAllowsSun(true, true, true));
    }
}
