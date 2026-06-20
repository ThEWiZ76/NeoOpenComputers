package li.cil.oc.common;

import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Mode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OpenOsResourceTest {
    private static final Path OPENOS_RECIPE = Path.of("src/main/resources/data/neoopencomputers/recipe/openos_floppy.json");
    private static final Path OPPM_RECIPE = Path.of("src/main/resources/data/neoopencomputers/recipe/oppm_floppy.json");

    @Test
    void bundledOpenOsFilesystemContainsBootFiles() throws IOException {
        FileSystem fileSystem = new FileSystemRegistry().fromClass(getClass(), "neoopencomputers", "loot/openos");

        assertNotNull(fileSystem);
        assertTrue(fileSystem.isReadOnly());
        assertTrue(fileSystem.exists("init.lua"));
        assertTrue(fileSystem.exists("lib/core/boot.lua"));

        int handle = fileSystem.open("init.lua", Mode.Read);
        byte[] buffer = new byte[256];
        int read = fileSystem.getHandle(handle).read(buffer);
        fileSystem.getHandle(handle).close();

        String init = new String(buffer, 0, read, StandardCharsets.UTF_8);
        assertTrue(init.contains("computer.getBootAddress()"));
    }

    @Test
    void bundledOpenOsFilesystemIsExposedByLootDiskCatalog() {
        OpenComputersApi.initialize();

        assertNotNull(ModLootDisks.openOsFileSystem());
    }

    @Test
    void bundledOppmFilesystemContainsInstallerFiles() throws IOException {
        FileSystem fileSystem = new FileSystemRegistry().fromClass(getClass(), "neoopencomputers", "loot/oppm");

        assertNotNull(fileSystem);
        assertTrue(fileSystem.isReadOnly());
        assertTrue(fileSystem.exists(".install"));
        assertTrue(fileSystem.exists("etc/oppm.cfg"));
        assertTrue(fileSystem.exists("usr/bin/oppm.lua"));
    }

    @Test
    void bundledLootDescriptorsIncludeOpenOs() {
        List<ModLootDisks.Descriptor> descriptors = ModLootDisks.bundledDescriptors();

        assertEquals(2, descriptors.size());
        ModLootDisks.Descriptor openOs = descriptors.getFirst();
        assertEquals("openos", openOs.path());
        assertEquals("OpenOS (Operating System)", openOs.label());
        assertEquals(0, openOs.weight());
        assertEquals(net.minecraft.world.item.DyeColor.GREEN, openOs.color());
        ModLootDisks.Descriptor oppm = descriptors.get(1);
        assertEquals("oppm", oppm.path());
        assertEquals("OPPM (Package Manager)", oppm.label());
        assertEquals(0, oppm.weight());
        assertEquals(net.minecraft.world.item.DyeColor.CYAN, oppm.color());
    }

    @Test
    void openOsFloppyRecipeCraftsStableLootDisk() throws IOException {
        try (Reader reader = Files.newBufferedReader(OPENOS_RECIPE)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray ingredients = json.getAsJsonArray("ingredients");
            JsonObject result = json.getAsJsonObject("result");
            JsonObject customData = result.getAsJsonObject("components").getAsJsonObject("minecraft:custom_data");

            assertEquals("minecraft:crafting_shapeless", json.get("type").getAsString());
            assertEquals("neoopencomputers:floppy", ingredientItem(ingredients, 0));
            assertEquals("neoopencomputers:manual", ingredientItem(ingredients, 1));
            assertEquals("neoopencomputers:floppy", result.get("id").getAsString());
            assertEquals("OpenOS (Operating System)", customData.get(ItemRegistry.FLOPPY_LABEL_TAG).getAsString());
            assertEquals("green", customData.get(ItemRegistry.FLOPPY_COLOR_TAG).getAsString());
            assertEquals("neoopencomputers:loot/openos", customData.get(ItemRegistry.FLOPPY_FACTORY_ID_TAG).getAsString());
            assertTrue(customData.get(ItemRegistry.FLOPPY_RECIPE_CYCLING_TAG).getAsBoolean());
        }
    }

    @Test
    void oppmFloppyRecipeCraftsStableLootDisk() throws IOException {
        try (Reader reader = Files.newBufferedReader(OPPM_RECIPE)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray ingredients = json.getAsJsonArray("ingredients");
            JsonObject result = json.getAsJsonObject("result");
            JsonObject customData = result.getAsJsonObject("components").getAsJsonObject("minecraft:custom_data");

            assertEquals("minecraft:crafting_shapeless", json.get("type").getAsString());
            assertEquals("neoopencomputers:floppy", ingredientItem(ingredients, 0));
            assertEquals("neoopencomputers:interweb", ingredientItem(ingredients, 1));
            assertEquals("neoopencomputers:floppy", result.get("id").getAsString());
            assertEquals("OPPM (Package Manager)", customData.get(ItemRegistry.FLOPPY_LABEL_TAG).getAsString());
            assertEquals("cyan", customData.get(ItemRegistry.FLOPPY_COLOR_TAG).getAsString());
            assertEquals("neoopencomputers:loot/oppm", customData.get(ItemRegistry.FLOPPY_FACTORY_ID_TAG).getAsString());
            assertTrue(customData.get(ItemRegistry.FLOPPY_RECIPE_CYCLING_TAG).getAsBoolean());
        }
    }

    private static String ingredientItem(final JsonArray ingredients, final int index) {
        return ingredients.get(index).getAsJsonObject().get("item").getAsString();
    }
}
