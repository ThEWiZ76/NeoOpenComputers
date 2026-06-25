package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModPackagingTest {
    private static final Path BUILD_GRADLE = Path.of("build.gradle");

    @Test
    void jarPackagingIncludesProjectAndBundledLibraryLicenses() throws IOException {
        final String build = Files.readString(BUILD_GRADLE);

        assertTrue(build.contains("LICENSE-neoopencomputers.txt"));
        assertTrue(build.contains("LICENSE-luaj.txt"));
    }

    @Test
    void builtModJarContainsLoadMetadataLicensesAndBootResources() throws IOException {
        try (ZipFile jar = new ZipFile(System.getProperty("neoopencomputers.modJar"))) {
            assertContains(jar, "META-INF/LICENSE-neoopencomputers.txt");
            assertContains(jar, "META-INF/LICENSE-luaj.txt");
            assertContains(jar, "META-INF/LICENSE-typesafe-config.txt");
            assertContains(jar, "META-INF/neoforge.mods.toml");
            assertContains(jar, "neoopencomputers.mixins.json");
            assertContains(jar, "assets/neoopencomputers/loot/openos/init.lua");

            final String modsToml = readEntry(jar, "META-INF/neoforge.mods.toml");
            assertTrue(!modsToml.contains("${"), "Packaged mods.toml still has unexpanded Gradle tokens");
            assertTrue(modsToml.contains("modId=\"neoopencomputers\""));
            assertTrue(modsToml.contains("displayName=\"NeoOpenComputers\""));
            assertTrue(modsToml.contains("license=\"MIT\""));
            assertTrue(modsToml.contains("config=\"neoopencomputers.mixins.json\""));
        }
    }

    @Test
    void builtAllJarContainsJarJarLibrariesForRuntime() throws IOException {
        try (ZipFile jar = new ZipFile(System.getProperty("neoopencomputers.allJar"))) {
            assertContains(jar, "META-INF/jarjar/metadata.json");
            assertContains(jar, "META-INF/jarjar/config-1.4.3.jar");
            assertContains(jar, "META-INF/jarjar/luaj-jse-3.0.1.jar");
            assertContains(jar, "META-INF/LICENSE-luaj.txt");
            assertContains(jar, "META-INF/LICENSE-typesafe-config.txt");
        }
    }

    @Test
    void builtApiJarContainsPublicApiSourcesAndClassesOnly() throws IOException {
        try (ZipFile jar = new ZipFile(System.getProperty("neoopencomputers.apiJar"))) {
            assertContains(jar, "li/cil/oc/api/API.java");
            assertContains(jar, "li/cil/oc/api/API.class");
            assertContains(jar, "li/cil/oc/api/Network.java");
            assertContains(jar, "li/cil/oc/api/Network.class");
            assertContains(jar, "li/cil/oc/api/machine/Machine.java");
            assertContains(jar, "li/cil/oc/api/machine/Machine.class");
            assertContains(jar, "li/cil/oc/api/nanomachines/Controller.java");
            assertContains(jar, "li/cil/oc/api/nanomachines/Controller.class");
            assertTrue(!containsEntryStartingWith(jar, "li/cil/oc/common/"), "API jar leaked internal common package");
        }
    }

    @Test
    void builtJavadocJarContainsApiDocumentation() throws IOException {
        try (ZipFile jar = new ZipFile(System.getProperty("neoopencomputers.javadocJar"))) {
            assertContains(jar, "index.html");
            assertContains(jar, "li/cil/oc/api/API.html");
            assertContains(jar, "li/cil/oc/api/Network.html");
            assertContains(jar, "li/cil/oc/api/machine/Machine.html");
            assertContains(jar, "li/cil/oc/api/nanomachines/Controller.html");
        }
    }

    private static void assertContains(final ZipFile jar, final String entryName) {
        assertTrue(jar.getEntry(entryName) != null, () -> "Missing packaged entry " + entryName);
    }

    private static String readEntry(final ZipFile jar, final String entryName) throws IOException {
        return new String(jar.getInputStream(jar.getEntry(entryName)).readAllBytes(), StandardCharsets.UTF_8);
    }

    private static boolean containsEntryStartingWith(final ZipFile jar, final String prefix) {
        final Enumeration<? extends ZipEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            if (entries.nextElement().getName().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
