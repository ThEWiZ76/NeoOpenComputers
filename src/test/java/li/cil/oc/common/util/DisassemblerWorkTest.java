package li.cil.oc.common.util;

import li.cil.oc.common.ModSettings;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DisassemblerWorkTest {
    @Test
    void usesConfiguredTickAmountLikeUpstream() throws Exception {
        withCachedConfig(ModSettings.DISASSEMBLER_TICK_AMOUNT, 7D, () -> {
            assertEquals(7D, DisassemblerWork.energyToApply(0D), 0.000_001D);
            assertEquals(7D, DisassemblerWork.energyToApply(1999D), 0.000_001D);
            assertEquals(0D, DisassemblerWork.energyToApply(2000D), 0.000_001D);
        });
    }

    @Test
    void usesConfiguredPerItemCostLikeUpstream() throws Exception {
        withCachedConfig(ModSettings.DISASSEMBLER_ITEM_COST, 5D, () -> {
            assertFalse(DisassemblerWork.canReleaseOutput(4D));
            assertTrue(DisassemblerWork.canReleaseOutput(5D));
            assertEquals(2D, DisassemblerWork.remainingBufferAfterRelease(7D), 0.000_001D);
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
