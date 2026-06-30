package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class BiosResourceTest {
    @Test
    void bundledLuaBiosBootsFilesystemInit() {
        String bios = new String(ModEeproms.luaBiosCode(), StandardCharsets.UTF_8);

        assertTrue(bios.contains("computer.getBootAddress"));
        assertTrue(bios.contains("/init.lua"));
        assertTrue(bios.contains("component.list(\"filesystem\")"));
    }

    @Test
    void bundledLuaBiosDoesNotProvideOpenOsRequire() {
        String bios = new String(ModEeproms.luaBiosCode(), StandardCharsets.UTF_8);

        assertTrue(!bios.contains("function require"),
            "Lua BIOS must stay minimal; OpenOS provides require/package.");
        assertTrue(!bios.contains("package.path"),
            "Lua BIOS must not install OpenOS package search paths.");
    }

    @Test
    void bundledOpenOsProvidesRequireAndComponentPrimaryConvenience() throws IOException {
        final String packageLib = Files.readString(Path.of("src/main/resources/assets/neoopencomputers/loot/openos/lib/package.lua"));
        final String componentBoot = Files.readString(Path.of("src/main/resources/assets/neoopencomputers/loot/openos/boot/04_component.lua"));

        assertTrue(packageLib.contains("function require(module)"),
            "OpenOS package library must provide require().");
        assertTrue(packageLib.contains("package.path = \"/lib/?.lua"),
            "OpenOS require must search library paths.");
        assertTrue(packageLib.contains("[\"package\"] = package"),
            "OpenOS package library must register itself as loaded.");
        assertTrue(!packageLib.contains("[\"component\"]"),
            "OpenOS component API should be loaded through require/boot setup, not package.loaded defaults.");
        assertTrue(componentBoot.contains("return component.getPrimary(key)"),
            "OpenOS component boot wrapper must keep component.<type> primary-proxy convenience.");
    }
}
