package li.cil.oc.common.blockentity;

import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ScreenItemEnvironmentTest {
    @Test
    void usesConfiguredScreenResolutionTiers() throws Exception {
        OpenComputersApi.initialize();

        withCachedConfig(ModSettings.SCREEN_WIDTHS_BY_TIER, List.of(7, 9, 11), () ->
            withCachedConfig(ModSettings.SCREEN_HEIGHTS_BY_TIER, List.of(3, 5, 13), () -> {
                final ScreenItemEnvironment tierOne = new ScreenItemEnvironment(null, 0);
                final ScreenItemEnvironment tierThree = new ScreenItemEnvironment(null, 2);

                assertEquals(7, tierOne.getMaximumWidth());
                assertEquals(3, tierOne.getMaximumHeight());
                assertEquals(7, tierOne.getWidth());
                assertEquals(3, tierOne.getHeight());
                assertEquals(11, tierThree.getMaximumWidth());
                assertEquals(13, tierThree.getMaximumHeight());
            }));
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

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
