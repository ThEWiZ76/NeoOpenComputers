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
import java.util.function.Function;
import java.util.stream.Collectors;

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
    void bundledOppmInstallFileOpensForCatCommand() throws IOException {
        FileSystem fileSystem = new FileSystemRegistry().fromClass(getClass(), "neoopencomputers", "loot/oppm");

        assertNotNull(fileSystem);
        int handle = fileSystem.open(".install", Mode.Read);
        byte[] buffer = new byte[1024];
        int read = fileSystem.getHandle(handle).read(buffer);
        fileSystem.getHandle(handle).close();

        assertTrue(read > 0);
        String install = new String(buffer, 0, read, StandardCharsets.UTF_8);
        assertTrue(install.contains("install.root"));
        assertTrue(install.contains("oppm install"));
    }

    @Test
    void bundledLootDescriptorsIncludeUpstreamCatalog() {
        List<ModLootDisks.Descriptor> descriptors = ModLootDisks.bundledDescriptors();
        Map<String, ModLootDisks.Descriptor> byPath = descriptors.stream()
            .collect(Collectors.toMap(ModLootDisks.Descriptor::path, Function.identity()));

        assertEquals(11, descriptors.size());
        assertDescriptor(byPath, "builder", "Builder", 1, net.minecraft.world.item.DyeColor.YELLOW);
        assertDescriptor(byPath, "data", "Data Card Software", 0, net.minecraft.world.item.DyeColor.PINK);
        assertDescriptor(byPath, "dig", "Digger", 2, net.minecraft.world.item.DyeColor.BROWN);
        assertDescriptor(byPath, "generator", "Generator Upgrade Software", 0, net.minecraft.world.item.DyeColor.PURPLE);
        assertDescriptor(byPath, "irc", "OpenIRC (IRC Client)", 1, net.minecraft.world.item.DyeColor.LIGHT_BLUE);
        assertDescriptor(byPath, "maze", "Mazer", 1, net.minecraft.world.item.DyeColor.ORANGE);
        assertDescriptor(byPath, "network", "Network (Network Stack)", 1, net.minecraft.world.item.DyeColor.LIME);
        assertDescriptor(byPath, "openloader", "OpenLoader (Boot Loader)", 1, net.minecraft.world.item.DyeColor.MAGENTA);
        assertDescriptor(byPath, "openos", "OpenOS (Operating System)", 0, net.minecraft.world.item.DyeColor.GREEN);
        assertDescriptor(byPath, "oppm", "OPPM (Package Manager)", 0, net.minecraft.world.item.DyeColor.CYAN);
        assertDescriptor(byPath, "plan9k", "Plan9k (Operating System)", 1, net.minecraft.world.item.DyeColor.RED);
    }

    @Test
    void bundledLootFilesystemsIncludeUpstreamProgramDisks() throws IOException {
        assertBundledFile("builder", "usr/bin/build.lua");
        assertBundledFile("data", "usr/bin/base64.lua");
        assertBundledFile("data", "usr/lib/data.lua");
        assertBundledFile("dig", "usr/bin/dig.lua");
        assertBundledFile("generator", "usr/bin/refuel.lua");
        assertBundledFile("irc", "usr/bin/irc.lua");
        assertBundledFile("maze", "usr/bin/maze.lua");
        assertBundledFile("network", "data/bin/ifconfig.lua");
        assertBundledFile("network", "data/lib/network.lua");
        assertBundledFile("openloader", ".install");
        assertBundledFile("openloader", "bin/opl-flash.lua");
        assertBundledFile("plan9k", "init.lua");
        assertBundledFile("plan9k", "usr/bin/mpt.lua");
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

    private static void assertDescriptor(final Map<String, ModLootDisks.Descriptor> descriptors, final String path, final String label, final int weight, final net.minecraft.world.item.DyeColor color) {
        ModLootDisks.Descriptor descriptor = descriptors.get(path);
        assertNotNull(descriptor, "Missing bundled loot descriptor " + path);
        assertEquals(label, descriptor.label());
        assertEquals(weight, descriptor.weight());
        assertEquals(color, descriptor.color());
    }

    private static void assertBundledFile(final String path, final String file) throws IOException {
        FileSystem fileSystem = new FileSystemRegistry().fromClass(OpenOsResourceTest.class, "neoopencomputers", "loot/" + path);

        assertNotNull(fileSystem, path);
        assertTrue(fileSystem.isReadOnly(), path);
        assertTrue(fileSystem.exists(file), path + " missing " + file);
    }

    private static String sha256(final Path path) throws IOException, NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
