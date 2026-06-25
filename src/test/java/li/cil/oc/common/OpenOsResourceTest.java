package li.cil.oc.common;

import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Mode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OpenOsResourceTest {
    private static final Path OPENOS_RECIPE = Path.of("src/main/resources/data/neoopencomputers/recipe/openos_floppy.json");
    private static final Path OPPM_RECIPE = Path.of("src/main/resources/data/neoopencomputers/recipe/oppm_floppy.json");
    private static final Path OPENOS_ROOT = Path.of("src/main/resources/assets/neoopencomputers/loot/openos");

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
    void bundledOpenOsKeepsUpstreamBootCriticalSnapshot() throws IOException, NoSuchAlgorithmException {
        assertEquals(179, Files.walk(OPENOS_ROOT).filter(Files::isRegularFile).count());

        Map<String, String> bootCriticalHashes = new LinkedHashMap<>();
        bootCriticalHashes.put("init.lua", "46a1347ae0ad82b803787027b8a7aa447f961dd73afb22390c550084c676f159");
        bootCriticalHashes.put("lib/core/boot.lua", "a8babcd38222480a9a7af48cda246c6d3bf5a75e31704ef39df7f8627c55ebd0");
        bootCriticalHashes.put("lib/core/full_shell.lua", "50e7d3c7c0a0e7c862429309c81f5f9d0686a450eb92e17ae7ed0af7e1d4cc71");
        bootCriticalHashes.put("bin/sh.lua", "f5a03e7a5f0d6e6c0ce1a0f74eb4429bfca9f4d875b72f8cd9cbb99c7556b848");
        bootCriticalHashes.put("bin/lua.lua", "548801e583814c499666b31ce54df778d0430efd1793affd1be889c00d5cfb15");
        bootCriticalHashes.put("bin/edit.lua", "1df3511dbe5efaa9fd132cebfd54536c90d815cc3ab65ef18a6c52418e6493bc");
        bootCriticalHashes.put("lib/filesystem.lua", "9b7d4cea39b852691a75391c5f5fc025e69bdfed0616cd7bb7eb8b5137c14b71");

        for (Map.Entry<String, String> entry : bootCriticalHashes.entrySet()) {
            assertEquals(entry.getValue(), sha256(OPENOS_ROOT.resolve(entry.getKey())), entry.getKey());
        }
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

    private static String sha256(final Path path) throws IOException, NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
