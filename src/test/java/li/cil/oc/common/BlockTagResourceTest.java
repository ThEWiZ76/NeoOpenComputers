package li.cil.oc.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BlockTagResourceTest {
    private static final Path PICKAXE_TAG = Path.of("src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json");

    @Test
    void registeredBlocksAreMineableWithPickaxe() throws IOException {
        assertTrue(Files.isRegularFile(PICKAXE_TAG), "Missing pickaxe mineable tag");
        JsonObject json = readJson(PICKAXE_TAG);
        JsonArray values = json.getAsJsonArray("values");
        List<String> ids = List.of(
            ModContentIds.ADAPTER,
            ModContentIds.ASSEMBLER,
            ModContentIds.CABLE,
            ModContentIds.COMPUTER_CASE_TIER1,
            ModContentIds.COMPUTER_CASE_TIER2,
            ModContentIds.COMPUTER_CASE_TIER3,
            ModContentIds.DISK_DRIVE,
            ModContentIds.GEOLYZER,
            ModContentIds.HOLOGRAM_TIER1,
            ModContentIds.HOLOGRAM_TIER2,
            ModContentIds.KEYBOARD,
            ModContentIds.MOTION_SENSOR,
            ModContentIds.REDSTONE_IO,
            ModContentIds.SCREEN_TIER1,
            ModContentIds.SCREEN_TIER2,
            ModContentIds.SCREEN_TIER3,
            ModContentIds.TRANSPOSER,
            ModContentIds.WAYPOINT);

        assertFalse(json.get("replace").getAsBoolean());
        for (String id : ids) {
            assertTrue(contains(values, "neoopencomputers:" + id), "Missing pickaxe mining tag value for " + id);
        }
    }

    private static boolean contains(final JsonArray values, final String expected) {
        for (JsonElement value : values) {
            if (expected.equals(value.getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static JsonObject readJson(final Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            return element.getAsJsonObject();
        }
    }
}
