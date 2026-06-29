package li.cil.oc.common.driver;

import li.cil.oc.api.driver.DriverBlock;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenBlockDriverShapeTest {
    @Test
    void screenBlockDriverIsBlockDriver() {
        assertTrue(DriverBlock.class.isAssignableFrom(ScreenBlockDriver.class));
    }

    @Test
    void screenBlockDriverHonorsScreenSidedNodeExposure() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/driver/ScreenBlockDriver.java"));

        assertTrue(source.contains("screen.canConnect(side)"),
            "Screen block driver must not bypass upstream sided screen node exposure");
        assertTrue(source.contains("worksWith(world, pos, side)"),
            "Screen block driver environment creation should use the same sided gate");
    }
}
