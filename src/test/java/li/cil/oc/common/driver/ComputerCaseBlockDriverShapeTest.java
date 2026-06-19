package li.cil.oc.common.driver;

import li.cil.oc.api.driver.DriverBlock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComputerCaseBlockDriverShapeTest {
    @Test
    void computerCaseBlockDriverImplementsDriverBlock() {
        assertTrue(DriverBlock.class.isAssignableFrom(ComputerCaseBlockDriver.class));
    }
}
