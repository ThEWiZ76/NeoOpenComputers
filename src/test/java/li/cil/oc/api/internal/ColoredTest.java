package li.cil.oc.api.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ColoredTest {
    @Test
    void exposesColorAndConnectivityContract() {
        var colored = new TestColored();

        colored.setColor(0x336699);

        assertEquals(0x336699, colored.getColor());
        assertTrue(colored.controlsConnectivity());
    }

    private static final class TestColored implements Colored {
        private int color;

        @Override
        public int getColor() {
            return color;
        }

        @Override
        public void setColor(final int value) {
            color = value;
        }

        @Override
        public boolean controlsConnectivity() {
            return true;
        }
    }
}
