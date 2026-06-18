package li.cil.oc.api.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TieredTest {
    @Test
    void exposesZeroBasedTier() {
        Tiered tiered = () -> 2;

        assertEquals(2, tiered.tier());
    }
}
