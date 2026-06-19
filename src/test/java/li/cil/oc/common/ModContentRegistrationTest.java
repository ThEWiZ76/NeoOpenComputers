package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ModContentRegistrationTest {
    @Test
    void manualItemIdIsStable() {
        assertEquals("manual", ModContentIds.MANUAL);
    }

    @Test
    void creativeTabIdIsStable() {
        assertEquals("main", ModContentIds.MAIN_CREATIVE_TAB);
    }
}
