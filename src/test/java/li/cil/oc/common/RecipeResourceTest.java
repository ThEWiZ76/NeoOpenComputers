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
            ModContentIds.ADAPTER,
            ModContentIds.CUTTING_WIRE,
            ModContentIds.ACID,
            ModContentIds.RAW_CIRCUIT_BOARD,
            ModContentIds.CIRCUIT_BOARD,
            ModContentIds.PRINTED_CIRCUIT_BOARD,
            ModContentIds.CARD,
            ModContentIds.TRANSISTOR,
            ModContentIds.MICROCHIP_TIER1,
            ModContentIds.MICROCHIP_TIER2,
            ModContentIds.MICROCHIP_TIER3,
            ModContentIds.ALU,
            ModContentIds.CONTROL_UNIT,
            ModContentIds.DISK_PLATTER,
            ModContentIds.INTERWEB,
            ModContentIds.BUTTON_GROUP,
            ModContentIds.ARROW_KEYS,
            ModContentIds.NUM_PAD,
            ModContentIds.ANALYZER,
            ModContentIds.ASSEMBLER,
            ModContentIds.BATTERY_UPGRADE_TIER1,
            ModContentIds.BATTERY_UPGRADE_TIER2,
            ModContentIds.BATTERY_UPGRADE_TIER3,
            ModContentIds.CABLE,
            ModContentIds.CARD_CONTAINER_TIER1,
            ModContentIds.CARD_CONTAINER_TIER2,
            ModContentIds.CARD_CONTAINER_TIER3,
            ModContentIds.COMPUTER_CASE_TIER1,
            ModContentIds.COMPUTER_CASE_TIER2,
            ModContentIds.COMPUTER_CASE_TIER3,
            ModContentIds.CPU_TIER1,
            ModContentIds.CPU_TIER2,
            ModContentIds.CPU_TIER3,
            ModContentIds.DATA_CARD_TIER1,
            ModContentIds.DATA_CARD_TIER2,
            ModContentIds.DATA_CARD_TIER3,
            ModContentIds.DATABASE_UPGRADE_TIER1,
            ModContentIds.DATABASE_UPGRADE_TIER2,
            ModContentIds.DATABASE_UPGRADE_TIER3,
            ModContentIds.DISASSEMBLER,
            ModContentIds.DISK_DRIVE,
            ModContentIds.EEPROM,
            ModContentIds.FLOPPY,
            ModContentIds.GEOLYZER,
            ModContentIds.GRAPHICS_CARD_TIER1,
            ModContentIds.GRAPHICS_CARD_TIER2,
            ModContentIds.GRAPHICS_CARD_TIER3,
            ModContentIds.HOVER_UPGRADE_TIER1,
            ModContentIds.HOVER_UPGRADE_TIER2,
            ModContentIds.HOLOGRAM_TIER1,
            ModContentIds.HOLOGRAM_TIER2,
            ModContentIds.HDD_TIER1,
            ModContentIds.HDD_TIER2,
            ModContentIds.HDD_TIER3,
            ModContentIds.INTERNET_CARD,
            ModContentIds.INVENTORY_UPGRADE,
            ModContentIds.KEYBOARD,
            ModContentIds.LINKED_CARD,
            ModContentIds.MANUAL,
            ModContentIds.MEMORY_TIER1,
            ModContentIds.MEMORY_TIER2,
            ModContentIds.MEMORY_TIER3,
            ModContentIds.MOTION_SENSOR,
            ModContentIds.NAVIGATION_UPGRADE,
            ModContentIds.NETWORK_CARD,
            ModContentIds.REDSTONE_IO,
            ModContentIds.TANK_UPGRADE,
            ModContentIds.TRANSPOSER,
            ModContentIds.UPGRADE_CONTAINER_TIER1,
            ModContentIds.UPGRADE_CONTAINER_TIER2,
            ModContentIds.UPGRADE_CONTAINER_TIER3,
            ModContentIds.WAYPOINT,
            ModContentIds.WIRELESS_NETWORK_CARD_TIER1,
            ModContentIds.WIRELESS_NETWORK_CARD_TIER2,
            ModContentIds.REDSTONE_CARD,
            ModContentIds.SCREEN_TIER1,
            ModContentIds.SCREEN_TIER2,
            ModContentIds.SCREEN_TIER3,
            ModContentIds.TABLET_CASE_TIER1,
            ModContentIds.TABLET_CASE_TIER2,
            ModContentIds.SOLAR_GENERATOR_UPGRADE);

        for (String id : ids) {
            Path recipe = RECIPE_ROOT.resolve(id + ".json");
            assertTrue(Files.isRegularFile(recipe), "Missing recipe for " + id);
            JsonObject json = readJson(recipe);
            assertTrue(json.get("type").getAsString().startsWith("minecraft:crafting_"));
            assertEquals("neoopencomputers:" + id, json.getAsJsonObject("result").get("id").getAsString());
        }
    }

    @Test
    void geolyzerRecipeUsesAnalyzer() throws IOException {
        JsonObject json = readJson(RECIPE_ROOT.resolve(ModContentIds.GEOLYZER + ".json"));
        JsonObject keys = json.getAsJsonObject("key");

        assertTrue(keys.has("A"), "Geolyzer recipe must define an Analyzer ingredient key");
        assertEquals("neoopencomputers:" + ModContentIds.ANALYZER, keys.getAsJsonObject("A").get("item").getAsString());
    }

    @Test
    void geolyzerRecipeUsesMaterialParts() throws IOException {
        JsonObject json = readJson(RECIPE_ROOT.resolve(ModContentIds.GEOLYZER + ".json"));
        JsonObject keys = json.getAsJsonObject("key");

        assertItem(keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(keys, "M", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(keys, "E", "minecraft:ender_eye");
        assertTag(keys, "G", "c:ingots/gold");
    }

    @Test
    void analyzerRecipeUsesMaterialParts() throws IOException {
        JsonObject json = readJson(RECIPE_ROOT.resolve(ModContentIds.ANALYZER + ".json"));
        JsonObject keys = json.getAsJsonObject("key");

        assertItem(keys, "T", "neoopencomputers:" + ModContentIds.TRANSISTOR);
        assertItem(keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
    }

    @Test
    void networkCardRecipeUsesMaterialParts() throws IOException {
        JsonObject keys = recipeKeys(ModContentIds.NETWORK_CARD);

        assertItem(keys, "B", "neoopencomputers:" + ModContentIds.CARD);
        assertItem(keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(keys, "L", "neoopencomputers:" + ModContentIds.CABLE);
    }

    @Test
    void wirelessCardRecipesUseCardProgression() throws IOException {
        JsonObject tier1 = recipeKeys(ModContentIds.WIRELESS_NETWORK_CARD_TIER1);
        JsonObject tier2 = recipeKeys(ModContentIds.WIRELESS_NETWORK_CARD_TIER2);

        assertItem(tier1, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(tier1, "N", "neoopencomputers:" + ModContentIds.NETWORK_CARD);
        assertItem(tier1, "T", "minecraft:redstone_torch");
        assertItem(tier2, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier2, "N", "neoopencomputers:" + ModContentIds.NETWORK_CARD);
        assertItem(tier2, "P", "minecraft:ender_pearl");
    }

    @Test
    void communicationCardRecipesUseMaterialParts() throws IOException {
        JsonObject redstone = recipeKeys(ModContentIds.REDSTONE_CARD);
        JsonObject internet = recipeKeys(ModContentIds.INTERNET_CARD);
        JsonObject linked = recipeKeys(ModContentIds.LINKED_CARD);

        assertItem(redstone, "B", "neoopencomputers:" + ModContentIds.CARD);
        assertItem(redstone, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(redstone, "T", "minecraft:redstone_torch");
        assertItem(internet, "I", "neoopencomputers:" + ModContentIds.INTERWEB);
        assertItem(internet, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(internet, "W", "neoopencomputers:" + ModContentIds.WIRELESS_NETWORK_CARD_TIER2);
        assertItem(linked, "I", "neoopencomputers:" + ModContentIds.INTERWEB);
        assertItem(linked, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(linked, "W", "neoopencomputers:" + ModContentIds.WIRELESS_NETWORK_CARD_TIER2);
    }

    private static JsonObject readJson(final Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement element = JsonParser.parseReader(reader);
            return element.getAsJsonObject();
        }
    }

    private static JsonObject recipeKeys(final String id) throws IOException {
        JsonObject json = readJson(RECIPE_ROOT.resolve(id + ".json"));
        return json.getAsJsonObject("key");
    }

    private static void assertItem(final JsonObject keys, final String key, final String item) {
        assertEquals(item, keys.getAsJsonObject(key).get("item").getAsString());
    }

    private static void assertTag(final JsonObject keys, final String key, final String tag) {
        assertEquals(tag, keys.getAsJsonObject(key).get("tag").getAsString());
    }
}
