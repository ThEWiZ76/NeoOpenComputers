package li.cil.oc.common.util;

import li.cil.oc.common.ModSettings;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HologramPowerTest {
    @Test
    void usesConfiguredHologramCostLikeUpstream() throws Exception {
        withCachedConfig(ModSettings.HOLOGRAM_COST, 0.75D, () -> {
            assertEquals(0D, HologramPower.energyCost(0D, 3D), 0.000_001D);
            assertEquals(0.75D, HologramPower.energyCost(0.5D, 2D), 0.000_001D);
        });
    }

    private static <T> void withCachedConfig(final ModConfigSpec.ConfigValue<T> value, final T override, final ThrowingRunnable action) throws Exception {
        final Field cachedValue = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cachedValue.setAccessible(true);
        final Object previous = cachedValue.get(value);
        cachedValue.set(value, override);
        try {
            action.run();
        } finally {
            cachedValue.set(value, previous);
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
