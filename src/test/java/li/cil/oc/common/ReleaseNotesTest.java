package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReleaseNotesTest {
    @Test
    void changelogDocumentsFirstAlphaScope() throws IOException {
        final Path changelogPath = Path.of("CHANGELOG.md");
        final String readme = Files.readString(Path.of("README.md"));

        assertTrue(Files.isRegularFile(changelogPath), "Changelog must exist before first alpha sharing");
        assertTrue(readme.contains("[Changelog](CHANGELOG.md)"),
            "README must link changelog for alpha testers");

        final String changelog = Files.readString(changelogPath);
        assertTrue(changelog.contains("0.1.0-alpha"), "Changelog must name first alpha line");
        assertTrue(changelog.contains("Minecraft 1.21.1"), "Changelog must name target Minecraft version");
        assertTrue(changelog.contains("NeoForge 21.1.234"), "Changelog must name target NeoForge version");
        assertTrue(changelog.contains("OpenOS boot"), "Changelog must summarize OpenOS boot scope");
        assertTrue(changelog.contains("Computer cases, servers, racks, screens, keyboards, storage, cards, and upgrades"),
            "Changelog must summarize current alpha device scope");
        assertTrue(changelog.contains("Power and charging"),
            "Changelog must summarize power and charging smoke scope");
        assertTrue(changelog.contains("Texture picker"),
            "Changelog must summarize texture picker atlas smoke scope");
        assertTrue(changelog.contains("Robots, drones, and block microcontrollers are not available"),
            "Changelog must list known unavailable devices");
        assertTrue(changelog.contains("Microcontroller case items are present for recipe/API compatibility only"),
            "Changelog must keep microcontroller case item scope clear");
        assertTrue(changelog.contains("GitHub Actions are intentionally disabled"),
            "Changelog must keep CI state visible");
    }
}
