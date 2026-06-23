package li.cil.oc.common.component;

import li.cil.oc.common.ModSettings;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GeneratorUpgradeEnvironmentTest {
    @Test
    void usesConfiguredGeneratorEfficiencyLikeUpstream() throws Exception {
        withCachedConfig(ModSettings.GENERATOR_EFFICIENCY, 0.25D, () -> {
            assertEquals(0.25D, GeneratorUpgradeEnvironment.generatedEnergyPerTick(), 0.000_001D);
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
