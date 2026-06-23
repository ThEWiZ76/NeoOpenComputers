package li.cil.oc.common.util;

import li.cil.oc.common.ModSettings;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AssemblerWorkTest {
    @Test
    void usesConfiguredAssemblerTickAmountLikeUpstream() throws Exception {
        withCachedConfig(ModSettings.ASSEMBLER_TICK_AMOUNT, 7D, () -> {
            assertEquals(7D, AssemblerWork.energyToApply(10D), 0.000_001D);
            assertEquals(3D, AssemblerWork.energyToApply(3D), 0.000_001D);
        });
    }

    @Test
    void convertsSignedConnectorRemainderToConsumedEnergy() {
        assertEquals(50D, AssemblerWork.energyConsumed(50D, 0D), 0.000_001D);
        assertEquals(32D, AssemblerWork.energyConsumed(50D, -18D), 0.000_001D);
        assertEquals(0D, AssemblerWork.energyConsumed(50D, -50D), 0.000_001D);
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
