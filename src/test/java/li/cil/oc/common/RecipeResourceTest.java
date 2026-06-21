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
            ModContentIds.CAPACITOR,
            ModContentIds.COMPONENT_BUS_TIER1,
            ModContentIds.COMPONENT_BUS_TIER2,
            ModContentIds.COMPONENT_BUS_TIER3,
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
            "generator_upgrade",
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
            ModContentIds.INVENTORY_CONTROLLER_UPGRADE,
            ModContentIds.INVENTORY_UPGRADE,
            ModContentIds.CRAFTING_UPGRADE,
            ModContentIds.EXPERIENCE_UPGRADE,
            ModContentIds.PISTON_UPGRADE,
            ModContentIds.STICKY_PISTON_UPGRADE,
            ModContentIds.SIGN_UPGRADE,
            ModContentIds.TRADING_UPGRADE,
            ModContentIds.TRACTOR_BEAM_UPGRADE,
            ModContentIds.LEASH_UPGRADE,
            ModContentIds.ANGEL_UPGRADE,
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
            assertKnownRecipeType(json);
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
        assertPattern(readJson(RECIPE_ROOT.resolve(ModContentIds.FLOPPY + ".json")), "ILI", "PDP", "IPI");
        assertItem(floppy, "D", "neoopencomputers:" + ModContentIds.DISK_PLATTER);
        assertItem(floppy, "L", "minecraft:lever");
        assertItem(floppy, "P", "minecraft:paper");
        assertTag(floppy, "I", "c:nuggets/iron");
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
    void cableRecipeUsesUpstreamShape() throws IOException {
        JsonObject recipe = readJson(RECIPE_ROOT.resolve(ModContentIds.CABLE + ".json"));
        JsonObject keys = recipe.getAsJsonObject("key");

        assertPattern(recipe, " I ", "IRI", " I ");
        assertTag(keys, "I", "c:nuggets/iron");
        assertItem(keys, "R", "minecraft:redstone");
        assertResultCount(recipe, 4);
    }

    @Test
    void diskPlatterRecipeUsesUpstreamShape() throws IOException {
        JsonObject recipe = readJson(RECIPE_ROOT.resolve(ModContentIds.DISK_PLATTER + ".json"));
        JsonObject keys = recipe.getAsJsonObject("key");

        assertPattern(recipe, " I ", "I I", " I ");
        assertTag(keys, "I", "c:nuggets/iron");
        assertResultCount(recipe, 1);
    }

    @Test
    void circuitBoardRecipesUseUpstreamProcessing() throws IOException {
        JsonObject circuitBoard = readJson(RECIPE_ROOT.resolve(ModContentIds.CIRCUIT_BOARD + ".json"));
        JsonObject printedCircuitBoard = readJson(RECIPE_ROOT.resolve(ModContentIds.PRINTED_CIRCUIT_BOARD + ".json"));

        assertEquals("minecraft:smelting", circuitBoard.get("type").getAsString());
        assertItem(circuitBoard, "ingredient", "neoopencomputers:" + ModContentIds.RAW_CIRCUIT_BOARD);
        assertEquals("neoopencomputers:" + ModContentIds.CIRCUIT_BOARD, circuitBoard.getAsJsonObject("result").get("id").getAsString());

        assertEquals("minecraft:crafting_shapeless", printedCircuitBoard.get("type").getAsString());
        assertIngredientItem(printedCircuitBoard, "neoopencomputers:" + ModContentIds.CIRCUIT_BOARD);
        assertIngredientItem(printedCircuitBoard, "neoopencomputers:" + ModContentIds.ACID);
        assertIngredientTag(printedCircuitBoard, "c:nuggets/gold");
        assertIngredientCount(printedCircuitBoard, 3);
        assertResultCount(printedCircuitBoard, 1);
    }

    @Test
    void cardRecipeUsesUpstreamMaterialShape() throws IOException {
        JsonObject recipe = readJson(RECIPE_ROOT.resolve(ModContentIds.CARD + ".json"));
        JsonObject keys = recipe.getAsJsonObject("key");

        assertPattern(recipe, "ICT", "IPP", "IGG");
        assertTag(keys, "I", "c:nuggets/iron");
        assertItem(keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(keys, "T", "neoopencomputers:" + ModContentIds.TRANSISTOR);
        assertItem(keys, "P", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertTag(keys, "G", "c:nuggets/gold");
        assertResultCount(recipe, 1);
    }

    @Test
    void transistorRecipeUsesUpstreamHardmodeShape() throws IOException {
        JsonObject recipe = readJson(RECIPE_ROOT.resolve(ModContentIds.TRANSISTOR + ".json"));
        JsonObject keys = recipe.getAsJsonObject("key");

        assertPattern(recipe, "III", "GPG", " R ");
        assertTag(keys, "I", "c:nuggets/iron");
        assertTag(keys, "G", "c:nuggets/gold");
        assertItem(keys, "P", "minecraft:paper");
        assertItem(keys, "R", "minecraft:redstone");
        assertResultCount(recipe, 1);
    }

    @Test
    void microchipTierOneRecipeUsesUpstreamHardmodeShape() throws IOException {
        JsonObject recipe = readJson(RECIPE_ROOT.resolve(ModContentIds.MICROCHIP_TIER1 + ".json"));
        JsonObject keys = recipe.getAsJsonObject("key");

        assertPattern(recipe, "I I", "TGT", "I I");
        assertTag(keys, "I", "c:nuggets/iron");
        assertItem(keys, "T", "neoopencomputers:" + ModContentIds.TRANSISTOR);
        assertTag(keys, "G", "c:nuggets/gold");
        assertResultCount(recipe, 1);
    }

    @Test
    void higherMicrochipRecipesUseUpstreamHardmodeShapes() throws IOException {
        JsonObject tier2 = readJson(RECIPE_ROOT.resolve(ModContentIds.MICROCHIP_TIER2 + ".json"));
        JsonObject tier3 = readJson(RECIPE_ROOT.resolve(ModContentIds.MICROCHIP_TIER3 + ".json"));
        JsonObject tier2Keys = tier2.getAsJsonObject("key");
        JsonObject tier3Keys = tier3.getAsJsonObject("key");

        assertPattern(tier2, "GLG", "CQC", "GLG");
        assertTag(tier2Keys, "G", "c:nuggets/gold");
        assertItem(tier2Keys, "L", "minecraft:lapis_lazuli");
        assertItem(tier2Keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(tier2Keys, "Q", "minecraft:quartz");
        assertResultCount(tier2, 1);

        assertPattern(tier3, "RPR", "CDC", "RPR");
        assertItem(tier3Keys, "R", "minecraft:glowstone_dust");
        assertItem(tier3Keys, "P", "minecraft:comparator");
        assertItem(tier3Keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier3Keys, "D", "minecraft:diamond");
        assertResultCount(tier3, 1);
    }

    @Test
    void processorMaterialRecipesUseUpstreamHardmodeShapes() throws IOException {
        JsonObject alu = readJson(RECIPE_ROOT.resolve(ModContentIds.ALU + ".json"));
        JsonObject controlUnit = readJson(RECIPE_ROOT.resolve(ModContentIds.CONTROL_UNIT + ".json"));
        JsonObject aluKeys = alu.getAsJsonObject("key");
        JsonObject controlUnitKeys = controlUnit.getAsJsonObject("key");

        assertPattern(alu, "PRP", "TTT", "IDI");
        assertItem(aluKeys, "P", "minecraft:repeater");
        assertItem(aluKeys, "R", "minecraft:redstone_torch");
        assertItem(aluKeys, "T", "neoopencomputers:" + ModContentIds.TRANSISTOR);
        assertTag(aluKeys, "I", "c:nuggets/iron");
        assertItem(aluKeys, "D", "minecraft:redstone");
        assertResultCount(alu, 1);

        assertPattern(controlUnit, "GRG", "TCT", "GDG");
        assertTag(controlUnitKeys, "G", "c:nuggets/gold");
        assertItem(controlUnitKeys, "R", "minecraft:redstone_torch");
        assertItem(controlUnitKeys, "T", "neoopencomputers:" + ModContentIds.TRANSISTOR);
        assertItem(controlUnitKeys, "C", "minecraft:clock");
        assertItem(controlUnitKeys, "D", "minecraft:redstone");
        assertResultCount(controlUnit, 1);
    }

    @Test
    void basicMaterialRecipesUseUpstreamInputs() throws IOException {
        JsonObject cuttingWire = readJson(RECIPE_ROOT.resolve(ModContentIds.CUTTING_WIRE + ".json"));
        JsonObject acid = readJson(RECIPE_ROOT.resolve(ModContentIds.ACID + ".json"));
        JsonObject cuttingWireKeys = cuttingWire.getAsJsonObject("key");

        assertPattern(cuttingWire, "SIS");
        assertItem(cuttingWireKeys, "S", "minecraft:stick");
        assertTag(cuttingWireKeys, "I", "c:nuggets/iron");
        assertResultCount(cuttingWire, 1);

        assertEquals("minecraft:crafting_shapeless", acid.get("type").getAsString());
        assertIngredientItem(acid, "minecraft:water_bucket");
        assertIngredientItem(acid, "minecraft:sugar");
        assertIngredientItem(acid, "minecraft:slime_ball");
        assertIngredientItem(acid, "minecraft:fermented_spider_eye");
        assertIngredientItem(acid, "minecraft:bone");
        assertIngredientCount(acid, 5);
        assertResultCount(acid, 1);
    }

    @Test
    void capacitorRecipeUsesMaterialProgression() throws IOException {
        JsonObject keys = recipeKeys(ModContentIds.CAPACITOR);

        assertTag(keys, "I", "c:ingots/iron");
        assertItem(keys, "T", "neoopencomputers:" + ModContentIds.TRANSISTOR);
        assertTag(keys, "G", "c:nuggets/gold");
        assertItem(keys, "P", "minecraft:paper");
        assertItem(keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
    }

    @Test
    void componentBusRecipesUseMaterialProgression() throws IOException {
        JsonObject tier1 = readJson(RECIPE_ROOT.resolve(ModContentIds.COMPONENT_BUS_TIER1 + ".json"));
        JsonObject tier2 = readJson(RECIPE_ROOT.resolve(ModContentIds.COMPONENT_BUS_TIER2 + ".json"));
        JsonObject tier3 = readJson(RECIPE_ROOT.resolve(ModContentIds.COMPONENT_BUS_TIER3 + ".json"));
        JsonObject tier1Keys = tier1.getAsJsonObject("key");
        JsonObject tier2Keys = tier2.getAsJsonObject("key");
        JsonObject tier3Keys = tier3.getAsJsonObject("key");

        assertPattern(tier1, "IRI", "CU ", "IBI");
        assertTag(tier1Keys, "I", "c:nuggets/iron");
        assertItem(tier1Keys, "R", "minecraft:redstone");
        assertItem(tier1Keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(tier1Keys, "U", "neoopencomputers:" + ModContentIds.CONTROL_UNIT);
        assertItem(tier1Keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);

        assertPattern(tier2, "IRI", "CU ", "IBI");
        assertTag(tier2Keys, "I", "c:nuggets/gold");
        assertItem(tier2Keys, "R", "minecraft:redstone");
        assertItem(tier2Keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier2Keys, "U", "neoopencomputers:" + ModContentIds.CONTROL_UNIT);
        assertItem(tier2Keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);

        assertPattern(tier3, "IRI", "CU ", "IBI");
        assertItem(tier3Keys, "I", "minecraft:diamond");
        assertItem(tier3Keys, "R", "minecraft:redstone");
        assertItem(tier3Keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(tier3Keys, "U", "neoopencomputers:" + ModContentIds.CONTROL_UNIT);
        assertItem(tier3Keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
    }

    @Test
    void batteryUpgradeRecipesUseCapacitorProgression() throws IOException {
        JsonObject tier1 = recipeKeys(ModContentIds.BATTERY_UPGRADE_TIER1);
        JsonObject tier2 = recipeKeys(ModContentIds.BATTERY_UPGRADE_TIER2);
        JsonObject tier3 = recipeKeys(ModContentIds.BATTERY_UPGRADE_TIER3);

        assertTag(tier1, "I", "c:ingots/iron");
        assertTag(tier1, "G", "c:nuggets/gold");
        assertItem(tier1, "T", "neoopencomputers:" + ModContentIds.TRANSISTOR);
        assertItem(tier1, "C", "neoopencomputers:" + ModContentIds.CAPACITOR);
        assertTag(tier2, "I", "c:ingots/gold");
        assertTag(tier2, "G", "c:nuggets/gold");
        assertItem(tier2, "T", "neoopencomputers:" + ModContentIds.TRANSISTOR);
        assertItem(tier2, "C", "neoopencomputers:" + ModContentIds.CAPACITOR);
        assertItem(tier3, "D", "minecraft:diamond");
        assertItem(tier3, "T", "neoopencomputers:" + ModContentIds.TRANSISTOR);
        assertItem(tier3, "C", "neoopencomputers:" + ModContentIds.CAPACITOR);
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
        JsonObject inventoryRecipe = readJson(RECIPE_ROOT.resolve(ModContentIds.INVENTORY_UPGRADE + ".json"));
        JsonObject tankRecipe = readJson(RECIPE_ROOT.resolve(ModContentIds.TANK_UPGRADE + ".json"));
        JsonObject inventory = inventoryRecipe.getAsJsonObject("key");
        JsonObject tank = tankRecipe.getAsJsonObject("key");

        assertPattern(inventoryRecipe, "IHI", "DCM", "IBI");
        assertTag(inventory, "I", "c:ingots/iron");
        assertItem(inventory, "H", "minecraft:hopper");
        assertItem(inventory, "D", "minecraft:dropper");
        assertItem(inventory, "C", "minecraft:chest");
        assertItem(inventory, "M", "minecraft:piston");
        assertItem(inventory, "B", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);

        assertPattern(tankRecipe, "PFP", "DCM", "PBP");
        assertTag(tank, "P", "minecraft:planks");
        assertItem(tank, "F", "minecraft:iron_bars");
        assertItem(tank, "D", "minecraft:dispenser");
        assertItem(tank, "C", "minecraft:cauldron");
        assertItem(tank, "M", "minecraft:piston");
        assertItem(tank, "B", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
    }

    @Test
    void controllerUpgradeRecipesUseUpstreamShape() throws IOException {
        JsonObject inventory = readJson(RECIPE_ROOT.resolve(ModContentIds.INVENTORY_CONTROLLER_UPGRADE + ".json"));
        JsonObject tank = readJson(RECIPE_ROOT.resolve(ModContentIds.TANK_CONTROLLER_UPGRADE + ".json"));
        JsonObject inventoryKeys = inventory.getAsJsonObject("key");
        JsonObject tankKeys = tank.getAsJsonObject("key");

        assertPattern(inventory, "GAG", "DCP", "GBG");
        assertTag(inventoryKeys, "G", "c:ingots/gold");
        assertItem(inventoryKeys, "A", "neoopencomputers:" + ModContentIds.ANALYZER);
        assertItem(inventoryKeys, "D", "minecraft:dropper");
        assertItem(inventoryKeys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(inventoryKeys, "P", "minecraft:piston");
        assertItem(inventoryKeys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);

        assertPattern(tank, "GBG", "DCP", "GRG");
        assertTag(tankKeys, "G", "c:ingots/gold");
        assertItem(tankKeys, "B", "minecraft:glass_bottle");
        assertItem(tankKeys, "D", "minecraft:dispenser");
        assertItem(tankKeys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tankKeys, "P", "minecraft:piston");
        assertItem(tankKeys, "R", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
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
        JsonObject craftingRecipe = readJson(RECIPE_ROOT.resolve(ModContentIds.CRAFTING_UPGRADE + ".json"));
        JsonObject crafting = craftingRecipe.getAsJsonObject("key");
        JsonObject experienceRecipe = readJson(RECIPE_ROOT.resolve(ModContentIds.EXPERIENCE_UPGRADE + ".json"));
        JsonObject experience = experienceRecipe.getAsJsonObject("key");
        JsonObject pistonRecipe = readJson(RECIPE_ROOT.resolve(ModContentIds.PISTON_UPGRADE + ".json"));
        JsonObject piston = pistonRecipe.getAsJsonObject("key");
        JsonObject stickyPiston = readJson(RECIPE_ROOT.resolve(ModContentIds.STICKY_PISTON_UPGRADE + ".json"));
        JsonObject signRecipe = readJson(RECIPE_ROOT.resolve(ModContentIds.SIGN_UPGRADE + ".json"));
        JsonObject sign = signRecipe.getAsJsonObject("key");
        JsonObject tradingRecipe = readJson(RECIPE_ROOT.resolve(ModContentIds.TRADING_UPGRADE + ".json"));
        JsonObject trading = tradingRecipe.getAsJsonObject("key");
        JsonObject tractorBeamRecipe = readJson(RECIPE_ROOT.resolve(ModContentIds.TRACTOR_BEAM_UPGRADE + ".json"));
        JsonObject tractorBeam = tractorBeamRecipe.getAsJsonObject("key");
        JsonObject leashRecipe = readJson(RECIPE_ROOT.resolve(ModContentIds.LEASH_UPGRADE + ".json"));
        JsonObject leash = leashRecipe.getAsJsonObject("key");
        JsonObject angelRecipe = readJson(RECIPE_ROOT.resolve(ModContentIds.ANGEL_UPGRADE + ".json"));
        JsonObject angel = angelRecipe.getAsJsonObject("key");
        JsonObject solarRecipe = readJson(RECIPE_ROOT.resolve(ModContentIds.SOLAR_GENERATOR_UPGRADE + ".json"));
        JsonObject solar = solarRecipe.getAsJsonObject("key");
        JsonObject hover1 = recipeKeys(ModContentIds.HOVER_UPGRADE_TIER1);
        JsonObject hover2Recipe = readJson(RECIPE_ROOT.resolve(ModContentIds.HOVER_UPGRADE_TIER2 + ".json"));
        JsonObject hover2 = hover2Recipe.getAsJsonObject("key");

        assertTag(waypoint, "I", "c:ingots/iron");
        assertItem(waypoint, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(waypoint, "T", "neoopencomputers:" + ModContentIds.TRANSISTOR);
        assertItem(waypoint, "W", "neoopencomputers:" + ModContentIds.INTERWEB);
        assertItem(waypoint, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);

        assertPattern(craftingRecipe, "IPI", "CTC", "IBI");
        assertTag(crafting, "I", "c:ingots/iron");
        assertItem(crafting, "P", "minecraft:piston");
        assertItem(crafting, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(crafting, "T", "minecraft:crafting_table");
        assertItem(crafting, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);

        assertPattern(experienceRecipe, "GAG", "CEC", "GBG");
        assertTag(experience, "G", "c:ingots/gold");
        assertItem(experience, "A", "neoopencomputers:" + ModContentIds.ANALYZER);
        assertItem(experience, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(experience, "E", "minecraft:emerald");
        assertItem(experience, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);

        assertPattern(pistonRecipe, "IPI", "SCS", "IBI");
        assertTag(piston, "I", "c:ingots/iron");
        assertItem(piston, "P", "minecraft:piston");
        assertItem(piston, "S", "minecraft:stick");
        assertItem(piston, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(piston, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);

        assertEquals("minecraft:crafting_shapeless", stickyPiston.get("type").getAsString());
        assertIngredientItem(stickyPiston, "neoopencomputers:" + ModContentIds.PISTON_UPGRADE);
        assertIngredientItem(stickyPiston, "minecraft:slime_ball");

        assertPattern(signRecipe, "IDI", "CSC", "IPI");
        assertTag(sign, "I", "c:ingots/iron");
        assertItem(sign, "D", "minecraft:black_dye");
        assertItem(sign, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(sign, "S", "minecraft:stick");
        assertItem(sign, "P", "minecraft:sticky_piston");

        assertPattern(tradingRecipe, "CHC", "ECE", "DBP");
        assertItem(trading, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(trading, "H", "minecraft:chest");
        assertItem(trading, "E", "minecraft:emerald");
        assertItem(trading, "D", "minecraft:dropper");
        assertItem(trading, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(trading, "P", "minecraft:piston");

        assertPattern(tractorBeamRecipe, "GPG", "ICI", "GHG");
        assertTag(tractorBeam, "G", "c:ingots/gold");
        assertItem(tractorBeam, "P", "minecraft:piston");
        assertTag(tractorBeam, "I", "c:ingots/iron");
        assertItem(tractorBeam, "C", "neoopencomputers:" + ModContentIds.CAPACITOR);
        assertItem(tractorBeam, "H", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);

        assertPattern(leashRecipe, "ILI", "LCL", "ILI");
        assertTag(leash, "I", "c:ingots/iron");
        assertItem(leash, "L", "minecraft:lead");
        assertItem(leash, "C", "neoopencomputers:" + ModContentIds.CONTROL_UNIT);

        assertPattern(angelRecipe, "IPI", "CSC", "IPI");
        assertTag(angel, "I", "c:ingots/iron");
        assertItem(angel, "P", "minecraft:ender_pearl");
        assertItem(angel, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(angel, "S", "minecraft:sticky_piston");

        assertPattern(solarRecipe, "GGG", "CUC");
        assertItem(solar, "G", "minecraft:glass");
        assertItem(solar, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(solar, "U", "neoopencomputers:generator_upgrade");

        assertItem(hover1, "F", "minecraft:feather");
        assertItem(hover1, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertTag(hover1, "I", "c:nuggets/iron");
        assertItem(hover1, "L", "minecraft:leather");
        assertItem(hover1, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);

        assertPattern(hover2Recipe, "ECE", "GIG", "EBE");
        assertItem(hover2, "E", "minecraft:end_stone");
        assertItem(hover2, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertTag(hover2, "G", "c:nuggets/gold");
        assertTag(hover2, "I", "c:ingots/iron");
        assertItem(hover2, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
    }

    @Test
    void navigationUpgradeRecipeUsesUpstreamShape() throws IOException {
        JsonObject navigation = readJson(RECIPE_ROOT.resolve(ModContentIds.NAVIGATION_UPGRADE + ".json"));
        JsonObject keys = navigation.getAsJsonObject("key");

        assertPattern(navigation, "GCG", "MFM", "GPG");
        assertTag(keys, "G", "c:ingots/gold");
        assertItem(keys, "C", "minecraft:compass");
        assertItem(keys, "M", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(keys, "F", "minecraft:filled_map");
        assertItem(keys, "P", "minecraft:potion");
    }

    @Test
    void hologramRecipesUseMaterialProgression() throws IOException {
        JsonObject tier1 = readJson(RECIPE_ROOT.resolve(ModContentIds.HOLOGRAM_TIER1 + ".json"));
        JsonObject tier2 = readJson(RECIPE_ROOT.resolve(ModContentIds.HOLOGRAM_TIER2 + ".json"));
        JsonObject tier1Keys = tier1.getAsJsonObject("key");
        JsonObject tier2Keys = tier2.getAsJsonObject("key");

        assertPattern(tier1, "CGC", "BDB", "OYO");
        assertItem(tier1Keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier1Keys, "G", "minecraft:glass_pane");
        assertItem(tier1Keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(tier1Keys, "D", "minecraft:diamond");
        assertItem(tier1Keys, "O", "minecraft:obsidian");
        assertItem(tier1Keys, "Y", "minecraft:glowstone_dust");

        assertPattern(tier2, "CGC", "BDB", "OFO");
        assertItem(tier2Keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(tier2Keys, "G", "minecraft:glass");
        assertItem(tier2Keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(tier2Keys, "D", "minecraft:diamond_block");
        assertItem(tier2Keys, "O", "minecraft:obsidian");
        assertItem(tier2Keys, "F", "minecraft:blaze_powder");
    }

    @Test
    void screenRecipesUseHardmodeProgression() throws IOException {
        JsonObject tier1 = readJson(RECIPE_ROOT.resolve(ModContentIds.SCREEN_TIER1 + ".json"));
        JsonObject tier2 = readJson(RECIPE_ROOT.resolve(ModContentIds.SCREEN_TIER2 + ".json"));
        JsonObject tier3 = readJson(RECIPE_ROOT.resolve(ModContentIds.SCREEN_TIER3 + ".json"));
        JsonObject tier1Keys = tier1.getAsJsonObject("key");
        JsonObject tier2Keys = tier2.getAsJsonObject("key");
        JsonObject tier3Keys = tier3.getAsJsonObject("key");

        assertPattern(tier1, "IIG", "RTG", "IIG");
        assertTag(tier1Keys, "I", "c:ingots/iron");
        assertItem(tier1Keys, "G", "minecraft:glass");
        assertItem(tier1Keys, "R", "minecraft:redstone");
        assertItem(tier1Keys, "T", "neoopencomputers:" + ModContentIds.TRANSISTOR);

        assertPattern(tier2, "IRI", "CGS", "IBI");
        assertTag(tier2Keys, "I", "c:ingots/gold");
        assertItem(tier2Keys, "R", "minecraft:red_dye");
        assertItem(tier2Keys, "G", "minecraft:green_dye");
        assertItem(tier2Keys, "B", "minecraft:blue_dye");
        assertItem(tier2Keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier2Keys, "S", "neoopencomputers:" + ModContentIds.SCREEN_TIER1);

        assertPattern(tier3, "OBC", "RQS", "OBC");
        assertItem(tier3Keys, "O", "minecraft:obsidian");
        assertItem(tier3Keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(tier3Keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(tier3Keys, "R", "minecraft:blaze_rod");
        assertItem(tier3Keys, "Q", "minecraft:quartz");
        assertItem(tier3Keys, "S", "neoopencomputers:" + ModContentIds.SCREEN_TIER2);
    }

    @Test
    void computerCaseRecipesUseMaterialProgression() throws IOException {
        JsonObject tier1 = readJson(RECIPE_ROOT.resolve(ModContentIds.COMPUTER_CASE_TIER1 + ".json"));
        JsonObject tier2 = readJson(RECIPE_ROOT.resolve(ModContentIds.COMPUTER_CASE_TIER2 + ".json"));
        JsonObject tier3 = readJson(RECIPE_ROOT.resolve(ModContentIds.COMPUTER_CASE_TIER3 + ".json"));
        JsonObject tier1Keys = tier1.getAsJsonObject("key");
        JsonObject tier2Keys = tier2.getAsJsonObject("key");
        JsonObject tier3Keys = tier3.getAsJsonObject("key");

        assertPattern(tier1, "ICI", "FHF", "IBI");
        assertTag(tier1Keys, "I", "c:ingots/iron");
        assertItem(tier1Keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(tier1Keys, "F", "minecraft:iron_bars");
        assertItem(tier1Keys, "H", "minecraft:chest");
        assertItem(tier1Keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);

        assertPattern(tier2, "ICI", "FHF", "IBI");
        assertTag(tier2Keys, "I", "c:ingots/gold");
        assertItem(tier2Keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier2Keys, "F", "minecraft:iron_bars");
        assertItem(tier2Keys, "H", "minecraft:chest");
        assertItem(tier2Keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);

        assertPattern(tier3, "ICI", "FHF", "IBI");
        assertItem(tier3Keys, "I", "minecraft:diamond");
        assertItem(tier3Keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(tier3Keys, "F", "minecraft:iron_bars");
        assertItem(tier3Keys, "H", "minecraft:chest");
        assertItem(tier3Keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
    }

    @Test
    void tabletCaseRecipesUseComponentBusProgression() throws IOException {
        JsonObject tier1 = readJson(RECIPE_ROOT.resolve(ModContentIds.TABLET_CASE_TIER1 + ".json"));
        JsonObject tier2 = readJson(RECIPE_ROOT.resolve(ModContentIds.TABLET_CASE_TIER2 + ".json"));
        JsonObject tier1Keys = tier1.getAsJsonObject("key");
        JsonObject tier2Keys = tier2.getAsJsonObject("key");

        assertPattern(tier1, "GBG", "USC", "GPG");
        assertTag(tier1Keys, "G", "c:ingots/gold");
        assertItem(tier1Keys, "B", "minecraft:stone_button");
        assertItem(tier1Keys, "U", "neoopencomputers:" + ModContentIds.COMPONENT_BUS_TIER1);
        assertItem(tier1Keys, "S", "neoopencomputers:" + ModContentIds.SCREEN_TIER2);
        assertItem(tier1Keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(tier1Keys, "P", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);

        assertPattern(tier2, "CBG", "USM", "CPG");
        assertItem(tier2Keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier2Keys, "B", "minecraft:stone_button");
        assertTag(tier2Keys, "G", "c:ingots/gold");
        assertItem(tier2Keys, "U", "neoopencomputers:" + ModContentIds.COMPONENT_BUS_TIER3);
        assertItem(tier2Keys, "S", "neoopencomputers:" + ModContentIds.SCREEN_TIER2);
        assertItem(tier2Keys, "M", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(tier2Keys, "P", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
    }

    @Test
    void inputDeviceRecipesUseMaterialParts() throws IOException {
        JsonObject manual = readJson(RECIPE_ROOT.resolve(ModContentIds.MANUAL + ".json"));
        JsonObject buttonGroup = readJson(RECIPE_ROOT.resolve(ModContentIds.BUTTON_GROUP + ".json"));
        JsonObject arrowKeys = readJson(RECIPE_ROOT.resolve(ModContentIds.ARROW_KEYS + ".json"));
        JsonObject numPad = readJson(RECIPE_ROOT.resolve(ModContentIds.NUM_PAD + ".json"));
        JsonObject keyboard = readJson(RECIPE_ROOT.resolve(ModContentIds.KEYBOARD + ".json"));

        assertEquals("minecraft:crafting_shapeless", manual.get("type").getAsString());
        assertIngredientItem(manual, "minecraft:book");
        assertIngredientItem(manual, "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertPattern(buttonGroup, "BBB", "BBB");
        assertItem(buttonGroup.getAsJsonObject("key"), "B", "minecraft:stone_button");
        assertPattern(arrowKeys, " B ", "BBB");
        assertItem(arrowKeys.getAsJsonObject("key"), "B", "minecraft:stone_button");
        assertPattern(numPad, "BBB", "BBB", "BBB");
        assertItem(numPad.getAsJsonObject("key"), "B", "minecraft:stone_button");
        assertPattern(keyboard, "GGG", "GAN");
        assertItem(keyboard.getAsJsonObject("key"), "G", "neoopencomputers:" + ModContentIds.BUTTON_GROUP);
        assertItem(keyboard.getAsJsonObject("key"), "A", "neoopencomputers:" + ModContentIds.ARROW_KEYS);
        assertItem(keyboard.getAsJsonObject("key"), "N", "neoopencomputers:" + ModContentIds.NUM_PAD);
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

    @Test
    void assemblerAndDisassemblerRecipesUseMaterialProgression() throws IOException {
        JsonObject assembler = readJson(RECIPE_ROOT.resolve(ModContentIds.ASSEMBLER + ".json"));
        JsonObject disassembler = readJson(RECIPE_ROOT.resolve(ModContentIds.DISASSEMBLER + ".json"));
        JsonObject assemblerKeys = assembler.getAsJsonObject("key");
        JsonObject disassemblerKeys = disassembler.getAsJsonObject("key");

        assertPattern(assembler, "ITI", "PCP", "IBI");
        assertTag(assemblerKeys, "I", "c:ingots/iron");
        assertItem(assemblerKeys, "T", "minecraft:crafting_table");
        assertItem(assemblerKeys, "P", "minecraft:piston");
        assertItem(assemblerKeys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(assemblerKeys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);

        assertPattern(disassembler, "CGA", "P O", "ILI");
        assertItem(disassemblerKeys, "C", "neoopencomputers:" + ModContentIds.CONTROL_UNIT);
        assertItem(disassemblerKeys, "G", "minecraft:glass_pane");
        assertItem(disassemblerKeys, "A", "neoopencomputers:" + ModContentIds.ANALYZER);
        assertItem(disassemblerKeys, "P", "minecraft:piston");
        assertItem(disassemblerKeys, "O", "minecraft:obsidian");
        assertTag(disassemblerKeys, "I", "c:ingots/iron");
        assertItem(disassemblerKeys, "L", "minecraft:lava_bucket");
    }

    @Test
    void generatorUpgradeRecipeUsesUpstreamHardmodeShape() throws IOException {
        JsonObject generator = readJson(RECIPE_ROOT.resolve("generator_upgrade.json"));
        JsonObject keys = generator.getAsJsonObject("key");

        assertPattern(generator, "I I", "CPC", "BIB");
        assertTag(keys, "I", "c:ingots/iron");
        assertItem(keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(keys, "P", "minecraft:piston");
        assertItem(keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
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

    private static void assertPattern(final JsonObject json, final String... expected) {
        JsonElement pattern = json.get("pattern");
        assertTrue(pattern != null && pattern.isJsonArray());
        assertEquals(expected.length, pattern.getAsJsonArray().size());
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], pattern.getAsJsonArray().get(index).getAsString());
        }
    }

    private static void assertResultCount(final JsonObject json, final int expected) {
        assertEquals(expected, json.getAsJsonObject("result").get("count").getAsInt());
    }

    private static void assertKnownRecipeType(final JsonObject json) {
        String type = json.get("type").getAsString();
        assertTrue(type.startsWith("minecraft:crafting_") || "minecraft:smelting".equals(type));
    }

    private static void assertIngredientCount(final JsonObject json, final int expected) {
        assertEquals(expected, json.getAsJsonArray("ingredients").size());
    }

    private static void assertIngredientItem(final JsonObject json, final String item) {
        for (JsonElement element : json.getAsJsonArray("ingredients")) {
            JsonObject ingredient = element.getAsJsonObject();
            if (ingredient.has("item") && item.equals(ingredient.get("item").getAsString())) {
                return;
            }
        }
        assertTrue(false, "Missing ingredient " + item);
    }

    private static void assertIngredientTag(final JsonObject json, final String tag) {
        for (JsonElement element : json.getAsJsonArray("ingredients")) {
            JsonObject ingredient = element.getAsJsonObject();
            if (ingredient.has("tag") && tag.equals(ingredient.get("tag").getAsString())) {
                return;
            }
        }
        assertTrue(false, "Missing ingredient tag " + tag);
    }
}
