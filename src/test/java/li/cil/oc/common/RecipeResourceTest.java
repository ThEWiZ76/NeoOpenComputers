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

final class RecipeResourceTest {
    private static final Path RECIPE_ROOT = Path.of("src/main/resources/data/neoopencomputers/recipe");

    @Test
    void registeredContentHasCraftingRecipes() throws IOException {
        List<String> ids = List.of(
            ModContentIds.COMPUTER_CASE_TIER1,
            ModContentIds.CPU_TIER1,
            ModContentIds.CPU_TIER2,
            ModContentIds.CPU_TIER3,
            ModContentIds.DISK_DRIVE,
            ModContentIds.EEPROM,
            ModContentIds.FLOPPY,
            ModContentIds.GRAPHICS_CARD_TIER1,
            ModContentIds.GRAPHICS_CARD_TIER2,
            ModContentIds.GRAPHICS_CARD_TIER3,
            ModContentIds.HDD_TIER1,
            ModContentIds.HDD_TIER2,
            ModContentIds.HDD_TIER3,
            ModContentIds.KEYBOARD,
            ModContentIds.MANUAL,
            ModContentIds.MEMORY_TIER1,
            ModContentIds.MEMORY_TIER2,
            ModContentIds.MEMORY_TIER3,
            ModContentIds.NETWORK_CARD,
            ModContentIds.SCREEN_TIER1);

        for (String id : ids) {
            Path recipe = RECIPE_ROOT.resolve(id + ".json");
            assertTrue(Files.isRegularFile(recipe), "Missing recipe for " + id);
            JsonObject json = readJson(recipe);
            assertTrue(json.get("type").getAsString().startsWith("minecraft:crafting_"));
            assertEquals("neoopencomputers:" + id, json.getAsJsonObject("result").get("id").getAsString());
        }
    }

    private static JsonObject readJson(final Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            return element.getAsJsonObject();
        }
    }
}
