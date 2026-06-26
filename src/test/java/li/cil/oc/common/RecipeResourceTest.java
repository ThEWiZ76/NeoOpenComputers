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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
            ModContentIds.POWER_CONVERTER,
            ModContentIds.POWER_DISTRIBUTOR,
            ModContentIds.PRINTER,
            ModContentIds.RACK,
            ModContentIds.RAID,
            ModContentIds.RELAY,
            ModContentIds.NET_SPLITTER,
            ModContentIds.INK_CARTRIDGE_EMPTY,
            ModContentIds.INK_CARTRIDGE,
            ModContentIds.BUTTON_GROUP,
            ModContentIds.CHAMELIUM,
            ModContentIds.ARROW_KEYS,
            ModContentIds.NUM_PAD,
            ModContentIds.ANALYZER,
            ModContentIds.WRENCH,
            ModContentIds.TEXTURE_PICKER,
            ModContentIds.TERMINAL,
            ModContentIds.TERMINAL_SERVER,
            ModContentIds.NANOMACHINES,
            ModContentIds.SERVER_TIER1,
            ModContentIds.SERVER_TIER2,
            ModContentIds.SERVER_TIER3,
            ModContentIds.APU_TIER1,
            ModContentIds.APU_TIER2,
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
            ModContentIds.DISK_DRIVE_MOUNTABLE,
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
            ModContentIds.CHUNKLOADER_UPGRADE,
            ModContentIds.MFU,
            ModContentIds.KEYBOARD,
            ModContentIds.LINKED_CARD,
            ModContentIds.MANUAL,
            ModContentIds.MEMORY_TIER1,
            ModContentIds.MEMORY_TIER2,
            ModContentIds.MEMORY_TIER3,
            ModContentIds.MEMORY_TIER4,
            ModContentIds.MEMORY_TIER5,
            ModContentIds.MEMORY_TIER6,
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
    void geolyzerRecipeUsesUpstreamCompass() throws IOException {
        JsonObject json = readJson(RECIPE_ROOT.resolve(ModContentIds.GEOLYZER + ".json"));
        JsonObject keys = json.getAsJsonObject("key");

        assertPattern(json, "GCG", "EME", "GBG");
        assertItem(keys, "C", "minecraft:compass");
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
    void analyzerRecipeUsesUpstreamDefaultInputs() throws IOException {
        JsonObject json = readJson(RECIPE_ROOT.resolve(ModContentIds.ANALYZER + ".json"));
        JsonObject keys = json.getAsJsonObject("key");

        assertPattern(json, "X  ", "TG ", "BG ");
        assertItem(keys, "X", "minecraft:redstone_torch");
        assertItem(keys, "T", "neoopencomputers:" + ModContentIds.TRANSISTOR);
        assertTag(keys, "G", "c:nuggets/gold");
        assertItem(keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
    }

    @Test
    void wrenchRecipeUsesUpstreamShape() throws IOException {
        JsonObject json = readJson(RECIPE_ROOT.resolve(ModContentIds.WRENCH + ".json"));
        JsonObject keys = json.getAsJsonObject("key");

        assertPattern(json, "I I", " C ", " I ");
        assertTag(keys, "I", "c:ingots/iron");
        assertItem(keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
    }

    @Test
    void texturePickerRecipeUsesUpstreamShape() throws IOException {
        JsonObject json = readJson(RECIPE_ROOT.resolve(ModContentIds.TEXTURE_PICKER + ".json"));
        JsonObject keys = json.getAsJsonObject("key");

        assertPattern(json, "BRG", "PAT", "YMW");
        assertItem(keys, "B", "minecraft:black_dye");
        assertItem(keys, "R", "minecraft:red_dye");
        assertItem(keys, "G", "minecraft:green_dye");
        assertItem(keys, "P", "minecraft:blue_dye");
        assertItem(keys, "A", "neoopencomputers:" + ModContentIds.ANALYZER);
        assertItem(keys, "T", "minecraft:purple_dye");
        assertItem(keys, "Y", "minecraft:yellow_dye");
        assertItem(keys, "M", "minecraft:magenta_dye");
        assertItem(keys, "W", "minecraft:white_dye");
    }

    @Test
    void terminalRecipeUsesUpstreamShape() throws IOException {
        JsonObject json = readJson(RECIPE_ROOT.resolve(ModContentIds.TERMINAL + ".json"));
        JsonObject keys = json.getAsJsonObject("key");

        assertPattern(json, "ISI", "CMW", "IKI");
        assertTag(keys, "I", "c:nuggets/iron");
        assertItem(keys, "S", "neoopencomputers:" + ModContentIds.SOLAR_GENERATOR_UPGRADE);
        assertItem(keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(keys, "M", "neoopencomputers:" + ModContentIds.SCREEN_TIER2);
        assertItem(keys, "W", "neoopencomputers:" + ModContentIds.WIRELESS_NETWORK_CARD_TIER2);
        assertItem(keys, "K", "neoopencomputers:" + ModContentIds.KEYBOARD);
    }

    @Test
    void terminalServerRecipeUsesUpstreamShape() throws IOException {
        JsonObject json = readJson(RECIPE_ROOT.resolve(ModContentIds.TERMINAL_SERVER + ".json"));
        JsonObject keys = json.getAsJsonObject("key");

        assertPattern(json, "OWO", "NCW", "OBO");
        assertItem(keys, "O", "minecraft:obsidian");
        assertItem(keys, "N", "neoopencomputers:" + ModContentIds.WIRELESS_NETWORK_CARD_TIER1);
        assertItem(keys, "W", "neoopencomputers:" + ModContentIds.WIRELESS_NETWORK_CARD_TIER2);
        assertItem(keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
    }

    @Test
    void diskDriveMountableRecipeUsesUpstreamDefaultInputs() throws IOException {
        JsonObject json = readJson(RECIPE_ROOT.resolve(ModContentIds.DISK_DRIVE_MOUNTABLE + ".json"));
        JsonObject keys = json.getAsJsonObject("key");

        assertPattern(json, "OCO", "FDF", "OBO");
        assertItem(keys, "O", "minecraft:obsidian");
        assertItem(keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(keys, "F", "minecraft:iron_bars");
        assertItem(keys, "D", "neoopencomputers:" + ModContentIds.DISK_DRIVE);
        assertItem(keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
    }

    @Test
    void nanomachinesRecipeUsesUpstreamShape() throws IOException {
        JsonObject json = readJson(RECIPE_ROOT.resolve(ModContentIds.NANOMACHINES + ".json"));
        JsonObject keys = json.getAsJsonObject("key");

        assertPattern(json, "CWC", "PAR", "CTC");
        assertItem(keys, "C", "neoopencomputers:" + ModContentIds.CHAMELIUM);
        assertItem(keys, "W", "neoopencomputers:" + ModContentIds.WIRELESS_NETWORK_CARD_TIER2);
        assertItem(keys, "P", "neoopencomputers:" + ModContentIds.CPU_TIER2);
        assertItem(keys, "A", "neoopencomputers:" + ModContentIds.ACID);
        assertItem(keys, "R", "neoopencomputers:" + ModContentIds.MEMORY_TIER1);
        assertItem(keys, "T", "neoopencomputers:" + ModContentIds.CAPACITOR);
    }

    @Test
    void serverRecipesUseUpstreamRackMountableShape() throws IOException {
        JsonObject tier1 = readJson(RECIPE_ROOT.resolve(ModContentIds.SERVER_TIER1 + ".json"));
        JsonObject tier2 = readJson(RECIPE_ROOT.resolve(ModContentIds.SERVER_TIER2 + ".json"));
        JsonObject tier3 = readJson(RECIPE_ROOT.resolve(ModContentIds.SERVER_TIER3 + ".json"));

        assertServerRecipe(tier1, "c:ingots/iron", ModContentIds.MEMORY_TIER2, ModContentIds.MICROCHIP_TIER1, ModContentIds.COMPONENT_BUS_TIER1);
        assertServerRecipe(tier2, "c:ingots/gold", ModContentIds.MEMORY_TIER4, ModContentIds.MICROCHIP_TIER2, ModContentIds.COMPONENT_BUS_TIER2);
        assertPattern(tier3, "IMI", "CUC", "OBO");
        assertItem(tier3.getAsJsonObject("key"), "I", "minecraft:diamond");
        assertItem(tier3.getAsJsonObject("key"), "M", "neoopencomputers:" + ModContentIds.MEMORY_TIER6);
        assertItem(tier3.getAsJsonObject("key"), "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(tier3.getAsJsonObject("key"), "U", "neoopencomputers:" + ModContentIds.COMPONENT_BUS_TIER3);
        assertItem(tier3.getAsJsonObject("key"), "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(tier3.getAsJsonObject("key"), "O", "minecraft:obsidian");
    }

    @Test
    void apuRecipesUseUpstreamShape() throws IOException {
        JsonObject tier1 = readJson(RECIPE_ROOT.resolve(ModContentIds.APU_TIER1 + ".json"));
        JsonObject tier2 = readJson(RECIPE_ROOT.resolve(ModContentIds.APU_TIER2 + ".json"));

        assertApuRecipe(tier1, true, ModContentIds.MICROCHIP_TIER1, ModContentIds.CPU_TIER2, ModContentIds.COMPONENT_BUS_TIER1, ModContentIds.GRAPHICS_CARD_TIER1);
        assertApuRecipe(tier2, false, ModContentIds.MICROCHIP_TIER2, ModContentIds.CPU_TIER3, ModContentIds.COMPONENT_BUS_TIER2, ModContentIds.GRAPHICS_CARD_TIER2);
    }

    @Test
    void networkInfrastructureRecipesUseUpstreamMaterials() throws IOException {
        JsonObject powerDistributor = readJson(RECIPE_ROOT.resolve(ModContentIds.POWER_DISTRIBUTOR + ".json"));
        JsonObject powerConverter = readJson(RECIPE_ROOT.resolve(ModContentIds.POWER_CONVERTER + ".json"));
        JsonObject relay = readJson(RECIPE_ROOT.resolve(ModContentIds.RELAY + ".json"));
        JsonObject netSplitter = readJson(RECIPE_ROOT.resolve(ModContentIds.NET_SPLITTER + ".json"));

        assertPattern(powerConverter, "ICI", "GMG", "IBI");
        JsonObject converterKeys = powerConverter.getAsJsonObject("key");
        assertTag(converterKeys, "I", "c:ingots/iron");
        assertTag(converterKeys, "G", "c:ingots/gold");
        assertItem(converterKeys, "C", "neoopencomputers:" + ModContentIds.CABLE);
        assertItem(converterKeys, "M", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(converterKeys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);

        assertPattern(powerDistributor, "IGI", "CMC", "IBI");
        JsonObject distributorKeys = powerDistributor.getAsJsonObject("key");
        assertTag(distributorKeys, "I", "c:ingots/iron");
        assertTag(distributorKeys, "G", "c:ingots/gold");
        assertItem(distributorKeys, "C", "neoopencomputers:" + ModContentIds.CABLE);
        assertItem(distributorKeys, "M", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(distributorKeys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);

        assertPattern(relay, "ICI", "CNC", "IBI");
        JsonObject relayKeys = relay.getAsJsonObject("key");
        assertTag(relayKeys, "I", "c:ingots/iron");
        assertItem(relayKeys, "C", "neoopencomputers:" + ModContentIds.CABLE);
        assertItem(relayKeys, "N", "neoopencomputers:" + ModContentIds.NETWORK_CARD);
        assertItem(relayKeys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);

        assertPattern(netSplitter, "ICI", "CPC", "IBI");
        JsonObject splitterKeys = netSplitter.getAsJsonObject("key");
        assertTag(splitterKeys, "I", "c:ingots/iron");
        assertItem(splitterKeys, "C", "neoopencomputers:" + ModContentIds.CABLE);
        assertItem(splitterKeys, "P", "minecraft:piston");
        assertItem(splitterKeys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
    }

    @Test
    void rackRecipeUsesUpstreamNetworkRackShape() throws IOException {
        JsonObject rack = readJson(RECIPE_ROOT.resolve(ModContentIds.RACK + ".json"));

        assertPattern(rack, "DWD", "FHF", "RBP");
        JsonObject keys = rack.getAsJsonObject("key");
        assertItem(keys, "D", "minecraft:diamond");
        assertItem(keys, "W", "neoopencomputers:" + ModContentIds.WIRELESS_NETWORK_CARD_TIER2);
        assertItem(keys, "F", "minecraft:iron_bars");
        assertItem(keys, "H", "minecraft:chest");
        assertItem(keys, "R", "neoopencomputers:" + ModContentIds.RELAY);
        assertItem(keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(keys, "P", "neoopencomputers:" + ModContentIds.POWER_DISTRIBUTOR);
    }

    @Test
    void raidRecipeUsesUpstreamDiskArrayShape() throws IOException {
        JsonObject raid = readJson(RECIPE_ROOT.resolve(ModContentIds.RAID + ".json"));

        assertPattern(raid, "ICI", "MDM", "IHI");
        JsonObject keys = raid.getAsJsonObject("key");
        assertTag(keys, "I", "c:nuggets/iron");
        assertItem(keys, "C", "neoopencomputers:" + ModContentIds.CPU_TIER3);
        assertItem(keys, "M", "neoopencomputers:" + ModContentIds.MEMORY_TIER1);
        assertItem(keys, "D", "neoopencomputers:" + ModContentIds.DISK_DRIVE);
        assertItem(keys, "H", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
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
        JsonObject tier1Recipe = readJson(RECIPE_ROOT.resolve(ModContentIds.WIRELESS_NETWORK_CARD_TIER1 + ".json"));
        JsonObject tier2Recipe = readJson(RECIPE_ROOT.resolve(ModContentIds.WIRELESS_NETWORK_CARD_TIER2 + ".json"));
        JsonObject tier1 = tier1Recipe.getAsJsonObject("key");
        JsonObject tier2 = tier2Recipe.getAsJsonObject("key");

        assertPattern(tier1Recipe, "TCT", " B ");
        assertItem(tier1, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(tier1, "B", "neoopencomputers:" + ModContentIds.CARD);
        assertItem(tier1, "T", "minecraft:redstone_torch");
        assertPattern(tier2Recipe, "PC ", " B ");
        assertItem(tier2, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier2, "B", "neoopencomputers:" + ModContentIds.CARD);
        assertItem(tier2, "P", "minecraft:ender_pearl");
    }

    @Test
    void communicationCardRecipesUseMaterialParts() throws IOException {
        JsonObject internetRecipe = readJson(RECIPE_ROOT.resolve(ModContentIds.INTERNET_CARD + ".json"));
        JsonObject linkedRecipe = readJson(RECIPE_ROOT.resolve(ModContentIds.LINKED_CARD + ".json"));
        JsonObject redstone = recipeKeys(ModContentIds.REDSTONE_CARD);
        JsonObject internet = recipeKeys(ModContentIds.INTERNET_CARD);
        JsonObject linked = recipeKeys(ModContentIds.LINKED_CARD);

        assertItem(redstone, "B", "neoopencomputers:" + ModContentIds.CARD);
        assertItem(redstone, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(redstone, "T", "minecraft:redstone_torch");
        assertPattern(internetRecipe, "ICT", " BO");
        assertItem(internet, "I", "neoopencomputers:" + ModContentIds.INTERWEB);
        assertItem(internet, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(internet, "T", "minecraft:redstone_torch");
        assertItem(internet, "B", "neoopencomputers:" + ModContentIds.CARD);
        assertItem(internet, "O", "minecraft:obsidian");
        assertPattern(linkedRecipe, "E E", "WIW", "C C");
        assertResultCount(linkedRecipe, 2);
        assertItem(linked, "E", "minecraft:ender_eye");
        assertItem(linked, "I", "neoopencomputers:" + ModContentIds.INTERWEB);
        assertItem(linked, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(linked, "W", "neoopencomputers:" + ModContentIds.NETWORK_CARD);
    }

    @Test
    void dataCardRecipesUseUpstreamDefaultInputs() throws IOException {
        JsonObject tier1 = recipeKeys(ModContentIds.DATA_CARD_TIER1);
        JsonObject tier2 = recipeKeys(ModContentIds.DATA_CARD_TIER2);
        JsonObject tier3 = recipeKeys(ModContentIds.DATA_CARD_TIER3);

        assertItem(tier1, "A", "neoopencomputers:" + ModContentIds.ALU);
        assertItem(tier1, "B", "neoopencomputers:" + ModContentIds.CARD);
        assertItem(tier1, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier1, "I", "minecraft:iron_nugget");
        assertPattern(readJson(RECIPE_ROOT.resolve(ModContentIds.DATA_CARD_TIER2 + ".json")), "GCM", " B ");
        assertItem(tier2, "B", "neoopencomputers:" + ModContentIds.CARD);
        assertItem(tier2, "C", "neoopencomputers:" + ModContentIds.CPU_TIER1);
        assertItem(tier2, "M", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertTag(tier2, "G", "c:nuggets/gold");
        assertPattern(readJson(RECIPE_ROOT.resolve(ModContentIds.DATA_CARD_TIER3 + ".json")), "XCM", " B ");
        assertItem(tier3, "B", "neoopencomputers:" + ModContentIds.CARD);
        assertItem(tier3, "C", "neoopencomputers:" + ModContentIds.CPU_TIER2);
        assertItem(tier3, "M", "neoopencomputers:" + ModContentIds.MEMORY_TIER5);
        assertItem(tier3, "X", "minecraft:diamond");
    }

    @Test
    void graphicsCardRecipesUseUpstreamDefaultInputs() throws IOException {
        JsonObject tier1 = recipeKeys(ModContentIds.GRAPHICS_CARD_TIER1);
        JsonObject tier2 = recipeKeys(ModContentIds.GRAPHICS_CARD_TIER2);
        JsonObject tier3 = recipeKeys(ModContentIds.GRAPHICS_CARD_TIER3);

        assertItem(tier1, "A", "neoopencomputers:" + ModContentIds.ALU);
        assertItem(tier1, "B", "neoopencomputers:" + ModContentIds.CARD);
        assertItem(tier1, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(tier1, "M", "neoopencomputers:" + ModContentIds.MEMORY_TIER1);
        assertItem(tier2, "A", "neoopencomputers:" + ModContentIds.ALU);
        assertItem(tier2, "B", "neoopencomputers:" + ModContentIds.CARD);
        assertItem(tier2, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier2, "M", "neoopencomputers:" + ModContentIds.MEMORY_TIER3);
        assertItem(tier3, "A", "neoopencomputers:" + ModContentIds.ALU);
        assertItem(tier3, "B", "neoopencomputers:" + ModContentIds.CARD);
        assertItem(tier3, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(tier3, "M", "neoopencomputers:" + ModContentIds.MEMORY_TIER5);
    }

    @Test
    void memoryRecipesUseUpstreamRamProgression() throws IOException {
        JsonObject tier1 = recipeKeys(ModContentIds.MEMORY_TIER1);
        JsonObject tier2 = recipeKeys(ModContentIds.MEMORY_TIER2);
        JsonObject tier3 = recipeKeys(ModContentIds.MEMORY_TIER3);
        JsonObject tier4 = recipeKeys(ModContentIds.MEMORY_TIER4);
        JsonObject tier5 = recipeKeys(ModContentIds.MEMORY_TIER5);
        JsonObject tier6 = recipeKeys(ModContentIds.MEMORY_TIER6);

        assertItem(tier1, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(tier1, "I", "minecraft:iron_nugget");
        assertItem(tier1, "P", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(tier2, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(tier2, "U", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier2, "P", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(tier3, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier3, "I", "minecraft:iron_nugget");
        assertItem(tier3, "P", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(tier4, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier4, "U", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(tier4, "P", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(tier5, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(tier5, "I", "minecraft:iron_nugget");
        assertItem(tier5, "P", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(tier6, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(tier6, "U", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier6, "P", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
    }

    @Test
    void cpuRecipesUseMaterialProgression() throws IOException {
        JsonObject tier1 = recipeKeys(ModContentIds.CPU_TIER1);
        JsonObject tier2 = recipeKeys(ModContentIds.CPU_TIER2);
        JsonObject tier3 = recipeKeys(ModContentIds.CPU_TIER3);

        assertItem(tier1, "A", "neoopencomputers:" + ModContentIds.ALU);
        assertItem(tier1, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(tier1, "U", "neoopencomputers:" + ModContentIds.CONTROL_UNIT);
        assertItem(tier2, "A", "neoopencomputers:" + ModContentIds.ALU);
        assertItem(tier2, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier2, "R", "minecraft:redstone");
        assertItem(tier2, "U", "neoopencomputers:" + ModContentIds.CONTROL_UNIT);
        assertTag(tier2, "G", "c:nuggets/gold");
        assertPattern(readJson(RECIPE_ROOT.resolve(ModContentIds.CPU_TIER2 + ".json")), "GRG", "CUC", "GAG");
        assertItem(tier3, "A", "neoopencomputers:" + ModContentIds.ALU);
        assertItem(tier3, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(tier3, "R", "minecraft:redstone");
        assertItem(tier3, "U", "neoopencomputers:" + ModContentIds.CONTROL_UNIT);
        assertItem(tier3, "X", "minecraft:diamond");
        assertPattern(readJson(RECIPE_ROOT.resolve(ModContentIds.CPU_TIER3 + ".json")), "XRX", "CUC", "XAX");
    }

    @Test
    void storageRecipesUseMaterialProgression() throws IOException {
        JsonObject eeprom = recipeKeys(ModContentIds.EEPROM);
        JsonObject floppy = recipeKeys(ModContentIds.FLOPPY);
        JsonObject drive = recipeKeys(ModContentIds.DISK_DRIVE);
        JsonObject hdd1Recipe = readJson(RECIPE_ROOT.resolve(ModContentIds.HDD_TIER1 + ".json"));
        JsonObject hdd2Recipe = readJson(RECIPE_ROOT.resolve(ModContentIds.HDD_TIER2 + ".json"));
        JsonObject hdd3Recipe = readJson(RECIPE_ROOT.resolve(ModContentIds.HDD_TIER3 + ".json"));
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
        assertPattern(hdd1Recipe, "CDI", "BDP", "CDI");
        assertItem(hdd1, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(hdd1, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(hdd1, "D", "neoopencomputers:" + ModContentIds.DISK_PLATTER);
        assertTag(hdd1, "I", "c:ingots/iron");
        assertItem(hdd1, "P", "minecraft:piston");
        assertPattern(hdd2Recipe, "CDI", "BDP", "CDI");
        assertItem(hdd2, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(hdd2, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(hdd2, "D", "neoopencomputers:" + ModContentIds.DISK_PLATTER);
        assertTag(hdd2, "I", "c:ingots/gold");
        assertItem(hdd2, "P", "minecraft:piston");
        assertPattern(hdd3Recipe, "CDI", "BDP", "CDI");
        assertItem(hdd3, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(hdd3, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(hdd3, "D", "neoopencomputers:" + ModContentIds.DISK_PLATTER);
        assertItem(hdd3, "I", "minecraft:diamond");
        assertItem(hdd3, "P", "minecraft:piston");
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
    void interwebRecipeUsesUpstreamDefaultInputs() throws IOException {
        JsonObject recipe = readJson(RECIPE_ROOT.resolve(ModContentIds.INTERWEB + ".json"));
        JsonObject keys = recipe.getAsJsonObject("key");

        assertPattern(recipe, "SSS", "SES", "SSS");
        assertItem(keys, "S", "minecraft:string");
        assertItem(keys, "E", "minecraft:ender_pearl");
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

        assertPattern(recipe, "I  ", "IP ", "IG ");
        assertTag(keys, "I", "c:nuggets/iron");
        assertItem(keys, "P", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertTag(keys, "G", "c:nuggets/gold");
        assertFalse(keys.has("C"));
        assertFalse(keys.has("T"));
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

        assertPattern(alu, "IDI", "TCT", "ITI");
        assertTag(aluKeys, "I", "c:nuggets/iron");
        assertItem(aluKeys, "D", "minecraft:redstone");
        assertItem(aluKeys, "T", "neoopencomputers:" + ModContentIds.TRANSISTOR);
        assertItem(aluKeys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertResultCount(alu, 1);

        assertPattern(controlUnit, "GDG", "TCT", "GTG");
        assertTag(controlUnitKeys, "G", "c:nuggets/gold");
        assertItem(controlUnitKeys, "D", "minecraft:redstone");
        assertItem(controlUnitKeys, "T", "neoopencomputers:" + ModContentIds.TRANSISTOR);
        assertItem(controlUnitKeys, "C", "minecraft:clock");
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
    void luaBiosRecipeUsesUpstreamInputs() throws IOException {
        JsonObject recipe = readJson(RECIPE_ROOT.resolve("lua_bios.json"));

        assertEquals("neoopencomputers:lua_bios", recipe.get("type").getAsString());
        assertEquals("misc", recipe.get("category").getAsString());
    }

    @Test
    void chameliumRecipeUsesUpstreamInputs() throws IOException {
        JsonObject chamelium = readJson(RECIPE_ROOT.resolve(ModContentIds.CHAMELIUM + ".json"));
        JsonObject keys = chamelium.getAsJsonObject("key");

        assertPattern(chamelium, "GRG", "RCR", "GWG");
        assertItem(keys, "G", "minecraft:gravel");
        assertItem(keys, "R", "minecraft:redstone");
        assertItem(keys, "C", "minecraft:charcoal");
        assertItem(keys, "W", "minecraft:water_bucket");
        assertResultCount(chamelium, 16);
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
        JsonObject tier1Recipe = readJson(RECIPE_ROOT.resolve(ModContentIds.BATTERY_UPGRADE_TIER1 + ".json"));
        JsonObject tier2Recipe = readJson(RECIPE_ROOT.resolve(ModContentIds.BATTERY_UPGRADE_TIER2 + ".json"));
        JsonObject tier3Recipe = readJson(RECIPE_ROOT.resolve(ModContentIds.BATTERY_UPGRADE_TIER3 + ".json"));
        JsonObject tier1 = tier1Recipe.getAsJsonObject("key");
        JsonObject tier2 = tier2Recipe.getAsJsonObject("key");
        JsonObject tier3 = tier3Recipe.getAsJsonObject("key");

        assertPattern(tier1Recipe, "IGI", "BCB", "IGI");
        assertTag(tier1, "I", "c:nuggets/iron");
        assertTag(tier1, "G", "c:nuggets/gold");
        assertItem(tier1, "B", "minecraft:iron_bars");
        assertItem(tier1, "C", "neoopencomputers:" + ModContentIds.CAPACITOR);

        assertPattern(tier2Recipe, "ICI", "BGB", "ICI");
        assertTag(tier2, "I", "c:nuggets/iron");
        assertTag(tier2, "G", "c:nuggets/gold");
        assertItem(tier2, "B", "minecraft:iron_bars");
        assertItem(tier2, "C", "neoopencomputers:" + ModContentIds.CAPACITOR);

        assertPattern(tier3Recipe, "ICI", "CDC", "ICI");
        assertTag(tier3, "I", "c:nuggets/iron");
        assertItem(tier3, "D", "minecraft:diamond");
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

        assertPattern(inventoryRecipe, "PHP", "DCM", "PBP");
        assertTag(inventory, "P", "minecraft:planks");
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
        JsonObject chunkloaderRecipe = readJson(RECIPE_ROOT.resolve(ModContentIds.CHUNKLOADER_UPGRADE + ".json"));
        JsonObject chunkloader = chunkloaderRecipe.getAsJsonObject("key");
        JsonObject mfuRecipe = readJson(RECIPE_ROOT.resolve(ModContentIds.MFU + ".json"));
        JsonObject mfu = mfuRecipe.getAsJsonObject("key");
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

        assertPattern(craftingRecipe, "I I", "CTC", "IBI");
        assertTag(crafting, "I", "c:ingots/iron");
        assertItem(crafting, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(crafting, "T", "minecraft:crafting_table");
        assertItem(crafting, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);

        assertPattern(experienceRecipe, "G G", "CEC", "GBG");
        assertTag(experience, "G", "c:ingots/gold");
        assertItem(experience, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
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

        assertPattern(chunkloaderRecipe, "GLG", "CEC", "OBO");
        assertTag(chunkloader, "G", "c:ingots/gold");
        assertItem(chunkloader, "L", "minecraft:glass");
        assertItem(chunkloader, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(chunkloader, "E", "minecraft:ender_eye");
        assertItem(chunkloader, "O", "minecraft:obsidian");
        assertItem(chunkloader, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);

        assertPattern(mfuRecipe, "CLC", "TAT", "CLC");
        assertItem(mfu, "C", "neoopencomputers:" + ModContentIds.CHAMELIUM);
        assertItem(mfu, "L", "minecraft:lapis_lazuli");
        assertItem(mfu, "T", "neoopencomputers:" + ModContentIds.LINKED_CARD);
        assertItem(mfu, "A", "neoopencomputers:" + ModContentIds.ADAPTER);

        assertPattern(solarRecipe, "GGG", "CLC", "IBI");
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
    void screenRecipesUseUpstreamDefaultInputs() throws IOException {
        JsonObject tier1 = readJson(RECIPE_ROOT.resolve(ModContentIds.SCREEN_TIER1 + ".json"));
        JsonObject tier2 = readJson(RECIPE_ROOT.resolve(ModContentIds.SCREEN_TIER2 + ".json"));
        JsonObject tier3 = readJson(RECIPE_ROOT.resolve(ModContentIds.SCREEN_TIER3 + ".json"));
        JsonObject tier1Keys = tier1.getAsJsonObject("key");
        JsonObject tier2Keys = tier2.getAsJsonObject("key");
        JsonObject tier3Keys = tier3.getAsJsonObject("key");

        assertPattern(tier1, "IRI", "RCG", "IRI");
        assertTag(tier1Keys, "I", "c:ingots/iron");
        assertItem(tier1Keys, "R", "minecraft:redstone");
        assertItem(tier1Keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertItem(tier1Keys, "G", "minecraft:glass");

        assertPattern(tier2, "IRI", "GCL", "IBI");
        assertTag(tier2Keys, "I", "c:ingots/gold");
        assertItem(tier2Keys, "R", "minecraft:red_dye");
        assertItem(tier2Keys, "G", "minecraft:green_dye");
        assertItem(tier2Keys, "B", "minecraft:blue_dye");
        assertItem(tier2Keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER2);
        assertItem(tier2Keys, "L", "minecraft:glass");

        assertPattern(tier3, "OYO", "YCG", "OYO");
        assertItem(tier3Keys, "O", "minecraft:obsidian");
        assertItem(tier3Keys, "Y", "minecraft:glowstone_dust");
        assertItem(tier3Keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(tier3Keys, "G", "minecraft:glass");
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
        JsonObject inkCartridgeEmpty = readJson(RECIPE_ROOT.resolve(ModContentIds.INK_CARTRIDGE_EMPTY + ".json"));
        JsonObject inkCartridgeEmptyKeys = inkCartridgeEmpty.getAsJsonObject("key");
        JsonObject inkCartridge = readJson(RECIPE_ROOT.resolve(ModContentIds.INK_CARTRIDGE + ".json"));
        JsonObject buttonGroup = readJson(RECIPE_ROOT.resolve(ModContentIds.BUTTON_GROUP + ".json"));
        JsonObject arrowKeys = readJson(RECIPE_ROOT.resolve(ModContentIds.ARROW_KEYS + ".json"));
        JsonObject numPad = readJson(RECIPE_ROOT.resolve(ModContentIds.NUM_PAD + ".json"));
        JsonObject keyboard = readJson(RECIPE_ROOT.resolve(ModContentIds.KEYBOARD + ".json"));

        assertEquals("minecraft:crafting_shapeless", manual.get("type").getAsString());
        assertIngredientItem(manual, "minecraft:book");
        assertIngredientItem(manual, "neoopencomputers:" + ModContentIds.MICROCHIP_TIER1);
        assertPattern(inkCartridgeEmpty, "IDI", "TBT", "IPI");
        assertTag(inkCartridgeEmptyKeys, "I", "c:nuggets/iron");
        assertItem(inkCartridgeEmptyKeys, "D", "minecraft:dispenser");
        assertItem(inkCartridgeEmptyKeys, "T", "neoopencomputers:" + ModContentIds.TRANSISTOR);
        assertItem(inkCartridgeEmptyKeys, "B", "minecraft:bucket");
        assertItem(inkCartridgeEmptyKeys, "P", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertEquals("minecraft:crafting_shapeless", inkCartridge.get("type").getAsString());
        assertIngredientItem(inkCartridge, "minecraft:cyan_dye");
        assertIngredientItem(inkCartridge, "minecraft:magenta_dye");
        assertIngredientItem(inkCartridge, "minecraft:yellow_dye");
        assertIngredientItem(inkCartridge, "minecraft:black_dye");
        assertIngredientItem(inkCartridge, "neoopencomputers:" + ModContentIds.INK_CARTRIDGE_EMPTY);
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
    void printerRecipeUsesUpstreamDefaultInputs() throws IOException {
        JsonObject printer = readJson(RECIPE_ROOT.resolve(ModContentIds.PRINTER + ".json"));
        JsonObject keys = printer.getAsJsonObject("key");

        assertPattern(printer, "IHI", "PCP", "IBI");
        assertTag(keys, "I", "c:ingots/iron");
        assertItem(keys, "H", "minecraft:hopper");
        assertItem(keys, "P", "minecraft:piston");
        assertItem(keys, "C", "neoopencomputers:" + ModContentIds.MICROCHIP_TIER3);
        assertItem(keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertResultCount(printer, 1);
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

    private static void assertServerRecipe(final JsonObject recipe, final String shellTag, final String memory, final String chip, final String bus) {
        JsonObject keys = recipe.getAsJsonObject("key");
        assertPattern(recipe, "IMI", "CUC", "OBO");
        assertTag(keys, "I", shellTag);
        assertItem(keys, "M", "neoopencomputers:" + memory);
        assertItem(keys, "C", "neoopencomputers:" + chip);
        assertItem(keys, "U", "neoopencomputers:" + bus);
        assertItem(keys, "B", "neoopencomputers:" + ModContentIds.PRINTED_CIRCUIT_BOARD);
        assertItem(keys, "O", "minecraft:obsidian");
    }

    private static void assertApuRecipe(final JsonObject recipe, final boolean goldCorners, final String chip, final String cpu, final String bus, final String graphicsCard) {
        JsonObject keys = recipe.getAsJsonObject("key");
        assertPattern(recipe, "GCG", "PUB", "GCG");
        if (goldCorners) {
            assertTag(keys, "G", "c:nuggets/gold");
        } else {
            assertItem(keys, "G", "minecraft:diamond");
        }
        assertItem(keys, "C", "neoopencomputers:" + chip);
        assertItem(keys, "P", "neoopencomputers:" + cpu);
        assertItem(keys, "U", "neoopencomputers:" + bus);
        assertItem(keys, "B", "neoopencomputers:" + graphicsCard);
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
        assertTrue(type.startsWith("minecraft:crafting_")
            || "minecraft:smelting".equals(type)
            || "neoopencomputers:linked_card".equals(type)
            || "neoopencomputers:loot_disk_cycling".equals(type)
            || "neoopencomputers:lua_bios".equals(type)
            || "neoopencomputers:navigation_upgrade".equals(type));
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
