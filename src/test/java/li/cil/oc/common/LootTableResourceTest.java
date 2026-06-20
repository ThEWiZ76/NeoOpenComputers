package li.cil.oc.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LootTableResourceTest {
    private static final Path LOOT_ROOT = Path.of("src/main/resources/data/neoopencomputers/loot_table/blocks");

    @Test
    void registeredBlocksHaveSelfDropLootTables() throws IOException {
        List<String> ids = List.of(
            ModContentIds.ADAPTER,
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
            ModContentIds.TRANSPOSER);

        for (String id : ids) {
            Path lootTable = LOOT_ROOT.resolve(id + ".json");
            assertTrue(Files.isRegularFile(lootTable), "Missing loot table for " + id);
            JsonObject json = readJson(lootTable);
            JsonObject firstEntry = json.getAsJsonArray("pools")
                .get(0).getAsJsonObject()
                .getAsJsonArray("entries")
                .get(0).getAsJsonObject();

            assertEquals("minecraft:block", json.get("type").getAsString());
            assertEquals("neoopencomputers:blocks/" + id, json.get("random_sequence").getAsString());
            assertEquals("minecraft:item", firstEntry.get("type").getAsString());
            assertEquals("neoopencomputers:" + id, firstEntry.get("name").getAsString());
        }
    }

    private static JsonObject readJson(final Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            return element.getAsJsonObject();
        }
    }
}
