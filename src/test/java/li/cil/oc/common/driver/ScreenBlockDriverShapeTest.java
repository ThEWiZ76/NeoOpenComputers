package li.cil.oc.common.driver;

import li.cil.oc.api.driver.DriverBlock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenBlockDriverShapeTest {
    @Test
    void screenBlockDriverIsBlockDriver() {
        assertTrue(DriverBlock.class.isAssignableFrom(ScreenBlockDriver.class));
    }
}
