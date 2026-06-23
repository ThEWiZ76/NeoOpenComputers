package li.cil.oc.common.component;

import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TransposerEnvironmentTest {
    @Test
    void usesConfiguredTransferCostLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        withCachedConfig(ModSettings.TRANSPOSER_COST, 2.25D, () -> {
            final TransposerEnvironment environment = new TransposerEnvironment(null);
            final ComponentConnector connector = assertInstanceOf(ComponentConnector.class, environment.node());
            connector.setLocalBufferSize(10D);
            connector.changeBuffer(10D);

            assertTrue(consumeTransferEnergy(environment));
            assertEquals(7.75D, connector.localBuffer(), 0.000_001D);
        });
    }

    private static boolean consumeTransferEnergy(final TransposerEnvironment environment) throws Exception {
        final Method method = TransposerEnvironment.class.getDeclaredMethod("consumeTransferEnergy");
        method.setAccessible(true);
        return (Boolean) method.invoke(environment);
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
