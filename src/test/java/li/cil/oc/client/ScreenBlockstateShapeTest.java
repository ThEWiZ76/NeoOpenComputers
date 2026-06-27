package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenBlockstateShapeTest {
    @Test
    void screenBlockstateUsesSameHorizontalFrontRotationConventionAsOtherBlocks() throws Exception {
        for (final String tier : new String[]{"screen_tier1", "screen_tier2", "screen_tier3"}) {
            final String json = Files.readString(Path.of("src/main/resources/assets/neoopencomputers/blockstates/" + tier + ".json"));
            assertTrue(json.contains("\"pitch=north,yaw=east\": { \"model\": \"neoopencomputers:block/" + tier + "\", \"y\": 90 }"));
            assertTrue(json.contains("\"pitch=north,yaw=west\": { \"model\": \"neoopencomputers:block/" + tier + "\", \"y\": 270 }"));
        }
    }
}
