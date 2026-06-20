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
            ModContentIds.TANK_CONTROLLER_UPGRADE,
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

    @Test
    void dataCardRecipesUseMaterialProgression() throws IOException {
        JsonObject tier1 = recipeKeys(ModContentIds.DATA_CARD_TIER1);
        JsonObject tier2 = recipeKeys(ModContentIds.DATA_CARD_TIER2);
        JsonObject tier3 = recipeKeys(ModContentIds.DATA_CARD_TIER3);

        assertItem(tier1, "A", "neoopencomputers:" + ModContentIds.ALU);
        assertItem(tier1, "B", "neoopencomputers:" + ModContentIds.CARD);
        assertItem(tier1, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier2, "C", "neoopencomputers:" + ModContentIds.CPU_TIER1);
        assertItem(tier2, "D", "neoopencomputers:" + ModContentIds.DATA_CARD_TIER1);
        assertItem(tier2, "M", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertTag(tier2, "G", "c:nuggets/gold");
        assertItem(tier3, "C", "neoopencomputers:" + ModContentIds.CPU_TIER2);
        assertItem(tier3, "D", "neoopencomputers:" + ModContentIds.DATA_CARD_TIER2);
        assertItem(tier3, "M", "neoopencomputers:" + ModContentIds.MEMORY_TIER3);
        assertItem(tier3, "X", "minecraft:diamond");
    }

    @Test
    void graphicsCardRecipesUseMaterialProgression() throws IOException {
        JsonObject tier1 = recipeKeys(ModContentIds.GRAPHICS_CARD_TIER1);
        JsonObject tier2 = recipeKeys(ModContentIds.GRAPHICS_CARD_TIER2);
        JsonObject tier3 = recipeKeys(ModContentIds.GRAPHICS_CARD_TIER3);

        assertItem(tier1, "A", "neoopencomputers:" + ModContentIds.ALU);
        assertItem(tier1, "B", "neoopencomputers:" + ModContentIds.CARD);
        assertItem(tier1, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(tier1, "M", "neoopencomputers:" + ModContentIds.MEMORY_TIER1);
        assertItem(tier2, "B", "neoopencomputers:" + ModContentIds.GRAPHICS_CARD_TIER1);
        assertItem(tier2, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier2, "M", "neoopencomputers:" + ModContentIds.MEMORY_TIER2);
        assertItem(tier3, "B", "neoopencomputers:" + ModContentIds.GRAPHICS_CARD_TIER2);
        assertItem(tier3, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(tier3, "M", "neoopencomputers:" + ModContentIds.MEMORY_TIER3);
    }

    @Test
    void memoryRecipesUseMaterialProgression() throws IOException {
        JsonObject tier1 = recipeKeys(ModContentIds.MEMORY_TIER1);
        JsonObject tier2 = recipeKeys(ModContentIds.MEMORY_TIER2);
        JsonObject tier3 = recipeKeys(ModContentIds.MEMORY_TIER3);

        assertItem(tier1, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(tier1, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(tier2, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(tier2, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier2, "M", "neoopencomputers:" + ModContentIds.MEMORY_TIER1);
        assertItem(tier3, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(tier3, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(tier3, "M", "neoopencomputers:" + ModContentIds.MEMORY_TIER2);
    }

    @Test
    void cpuRecipesUseMaterialProgression() throws IOException {
        JsonObject tier1 = recipeKeys(ModContentIds.CPU_TIER1);
        JsonObject tier2 = recipeKeys(ModContentIds.CPU_TIER2);
        JsonObject tier3 = recipeKeys(ModContentIds.CPU_TIER3);

        assertItem(tier1, "A", "neoopencomputers:" + ModContentIds.ALU);
        assertItem(tier1, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(tier1, "U", "neoopencomputers:" + ModContentIds.CONTROL_UNIT);
        assertItem(tier2, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier2, "M", "neoopencomputers:" + ModContentIds.MEMORY_TIER2);
        assertItem(tier2, "P", "neoopencomputers:" + ModContentIds.CPU_TIER1);
        assertTag(tier2, "G", "c:nuggets/gold");
        assertItem(tier3, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(tier3, "M", "neoopencomputers:" + ModContentIds.MEMORY_TIER3);
        assertItem(tier3, "P", "neoopencomputers:" + ModContentIds.CPU_TIER2);
        assertItem(tier3, "X", "minecraft:diamond");
    }

    @Test
    void storageRecipesUseMaterialProgression() throws IOException {
        JsonObject eeprom = recipeKeys(ModContentIds.EEPROM);
        JsonObject floppy = recipeKeys(ModContentIds.FLOPPY);
        JsonObject drive = recipeKeys(ModContentIds.DISK_DRIVE);
        JsonObject hdd1 = recipeKeys(ModContentIds.HDD_TIER1);
        JsonObject hdd2 = recipeKeys(ModContentIds.HDD_TIER2);
        JsonObject hdd3 = recipeKeys(ModContentIds.HDD_TIER3);

        assertItem(eeprom, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(eeprom, "T", "neoopencomputers:" + ModContentIds.TRANSISTOR);
        assertItem(eeprom, "X", "minecraft:redstone_torch");
        assertTag(eeprom, "G", "c:nuggets/gold");
        assertItem(floppy, "B", "neoopencomputers:" + ModContentIds.CIRCUIT_BOARD);
        assertItem(floppy, "D", "neoopencomputers:" + ModContentIds.DISK_PLATTER);
        assertItem(drive, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(drive, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(drive, "P", "minecraft:piston");
        assertItem(hdd1, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(hdd1, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(hdd1, "D", "neoopencomputers:" + ModContentIds.DISK_PLATTER);
        assertItem(hdd1, "P", "minecraft:piston");
        assertItem(hdd2, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(hdd2, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(hdd2, "H", "neoopencomputers:" + ModContentIds.HDD_TIER1);
        assertItem(hdd3, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(hdd3, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(hdd3, "H", "neoopencomputers:" + ModContentIds.HDD_TIER2);
        assertItem(hdd3, "M", "neoopencomputers:" + ModContentIds.MEMORY_TIER1);
    }

    @Test
    void databaseUpgradeRecipesUseMaterialProgression() throws IOException {
        JsonObject tier1 = recipeKeys(ModContentIds.DATABASE_UPGRADE_TIER1);
        JsonObject tier2 = recipeKeys(ModContentIds.DATABASE_UPGRADE_TIER2);
        JsonObject tier3 = recipeKeys(ModContentIds.DATABASE_UPGRADE_TIER3);

        assertItem(tier1, "A", "neoopencomputers:" + ModContentIds.ANALYZER);
        assertItem(tier1, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(tier1, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(tier1, "H", "neoopencomputers:" + ModContentIds.HDD_TIER1);
        assertTag(tier1, "I", "c:ingots/iron");
        assertItem(tier2, "A", "neoopencomputers:" + ModContentIds.ANALYZER);
        assertItem(tier2, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(tier2, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier2, "H", "neoopencomputers:" + ModContentIds.HDD_TIER2);
        assertTag(tier2, "I", "c:ingots/iron");
        assertItem(tier3, "A", "neoopencomputers:" + ModContentIds.ANALYZER);
        assertItem(tier3, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(tier3, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(tier3, "H", "neoopencomputers:" + ModContentIds.HDD_TIER3);
        assertTag(tier3, "I", "c:ingots/iron");
    }

    @Test
    void inventoryAndTankUpgradeRecipesUseUpstreamShape() throws IOException {
        JsonObject inventory = recipeKeys(ModContentIds.INVENTORY_UPGRADE);
        JsonObject tank = recipeKeys(ModContentIds.TANK_UPGRADE);

        assertTag(inventory, "P", "minecraft:planks");
        assertItem(inventory, "H", "minecraft:hopper");
        assertItem(inventory, "D", "minecraft:dropper");
        assertItem(inventory, "C", "minecraft:chest");
        assertItem(inventory, "M", "minecraft:piston");
        assertItem(inventory, "B", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertTag(tank, "P", "minecraft:planks");
        assertItem(tank, "F", "minecraft:iron_bars");
        assertItem(tank, "D", "minecraft:dispenser");
        assertItem(tank, "C", "minecraft:cauldron");
        assertItem(tank, "M", "minecraft:piston");
        assertItem(tank, "B", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
    }

    @Test
    void containerRecipesUseDirectUpstreamMaterials() throws IOException {
        JsonObject card1 = recipeKeys(ModContentIds.CARD_CONTAINER_TIER1);
        JsonObject card2 = recipeKeys(ModContentIds.CARD_CONTAINER_TIER2);
        JsonObject card3 = recipeKeys(ModContentIds.CARD_CONTAINER_TIER3);
        JsonObject upgrade1 = recipeKeys(ModContentIds.UPGRADE_CONTAINER_TIER1);
        JsonObject upgrade2 = recipeKeys(ModContentIds.UPGRADE_CONTAINER_TIER2);
        JsonObject upgrade3 = recipeKeys(ModContentIds.UPGRADE_CONTAINER_TIER3);

        assertContainerRecipe(card1, ModContentIds.MICROCHIP_TIER1, ModContentIds.CARD, "c:ingots/iron");
        assertContainerRecipe(card2, ModContentIds.MICROCHIP_TIER2, ModContentIds.CARD, "c:ingots/iron");
        assertContainerRecipe(card3, ModContentIds.MICROCHIP_TIER2, ModContentIds.CARD, "c:ingots/gold");
        assertContainerRecipe(upgrade1, ModContentIds.MICROCHIP_TIER1, ModContentIds.PRINTED_CIRCUIT_BOARD, "c:ingots/iron");
        assertContainerRecipe(upgrade2, ModContentIds.MICROCHIP_TIER2, ModContentIds.PRINTED_CIRCUIT_BOARD, "c:ingots/iron");
        assertContainerRecipe(upgrade3, ModContentIds.MICROCHIP_TIER2, ModContentIds.PRINTED_CIRCUIT_BOARD, "c:ingots/gold");
    }

    @Test
    void utilityUpgradeRecipesUsePortedMaterials() throws IOException {
        JsonObject waypoint = recipeKeys(ModContentIds.WAYPOINT);
        JsonObject solar = recipeKeys(ModContentIds.SOLAR_GENERATOR_UPGRADE);
        JsonObject hover1 = recipeKeys(ModContentIds.HOVER_UPGRADE_TIER1);

        assertTag(waypoint, "I", "c:ingots/iron");
        assertItem(waypoint, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(waypoint, "T", "neoopencomputers:" + ModContentIds.TRANSISTOR);
        assertItem(waypoint, "W", "neoopencomputers:" + ModContentIds.INTERWEB);
        assertItem(waypoint, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(solar, "G", "minecraft:glass");
        assertItem(solar, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(solar, "L", "minecraft:lapis_block");
        assertTag(solar, "I", "c:ingots/iron");
        assertItem(solar, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(hover1, "F", "minecraft:feather");
        assertItem(hover1, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertTag(hover1, "I", "c:nuggets/iron");
        assertItem(hover1, "L", "minecraft:leather");
        assertItem(hover1, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
    }

    @Test
    void blockDeviceRecipesUseMaterialProgression() throws IOException {
        JsonObject adapter = recipeKeys(ModContentIds.ADAPTER);
        JsonObject redstone = recipeKeys(ModContentIds.REDSTONE_IO);
        JsonObject motion = recipeKeys(ModContentIds.MOTION_SENSOR);
        JsonObject transposer = recipeKeys(ModContentIds.TRANSPOSER);

        assertItem(adapter, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(adapter, "C", "neoopencomputers:" + ModContentIds.CABLE);
        assertItem(adapter, "M", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(redstone, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(redstone, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(redstone, "R", "neoopencomputers:" + ModContentIds.REDSTONE_CARD);
        assertItem(redstone, "X", "minecraft:redstone_block");
        assertItem(motion, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(motion, "C", "neoopencomputers:" + ModContentIds.CPU_TIER2);
        assertItem(motion, "D", "minecraft:daylight_detector");
        assertTag(motion, "G", "c:ingots/gold");
        assertItem(transposer, "I", "neoopencomputers:" + ModContentIds.INVENTORY_CONTROLLER_UPGRADE);
        assertItem(transposer, "T", "neoopencomputers:" + ModContentIds.TANK_CONTROLLER_UPGRADE);
        assertItem(transposer, "H", "minecraft:hopper");
        assertItem(transposer, "B", "minecraft:bucket");
        assertTag(transposer, "G", "c:ingots/iron");
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

    private static void assertContainerRecipe(final JsonObject keys, final String chip, final String base, final String ingotTag) {
        assertTag(keys, "I", ingotTag);
        assertItem(keys, "C", "neoopencomputers:" + chip);
        assertItem(keys, "P", "minecraft:piston");
        assertItem(keys, "H", "minecraft:chest");
        assertItem(keys, "B", "neoopencomputers:" + base);
    }
}
