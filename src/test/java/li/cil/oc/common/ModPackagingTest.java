package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import javax.xml.parsers.DocumentBuilderFactory;

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
    void builtInstallableModJarContainsJarJarLibrariesForRuntime() throws IOException {
        try (ZipFile jar = new ZipFile(System.getProperty("neoopencomputers.modJar"))) {
            assertContains(jar, "META-INF/jarjar/metadata.json");
            assertContains(jar, "META-INF/jarjar/config-1.4.3.jar");
            assertContains(jar, "META-INF/jarjar/luaj-jse-3.0.1.jar");
        }
    }

    @Test
    void builtModJarDoesNotDependOnAnonymousComputerCaseMenuClasses() throws IOException {
        try (ZipFile jar = new ZipFile(System.getProperty("neoopencomputers.modJar"))) {
            assertContains(jar, "li/cil/oc/common/menu/ComputerCaseMenu.class");
            assertContains(jar, "li/cil/oc/common/menu/ComputerCaseMenu$ComputerSlot.class");
            assertContains(jar, "li/cil/oc/common/menu/ComputerCaseMenu$ServerComputerData.class");
            assertTrue(jar.getEntry("li/cil/oc/common/menu/ComputerCaseMenu$1.class") == null,
                "Computer case menu must not rely on anonymous slot class");
            assertTrue(jar.getEntry("li/cil/oc/common/menu/ComputerCaseMenu$2.class") == null,
                "Computer case menu must not rely on anonymous server data class");
        }
    }

    @Test
    void builtModJarContainsRuntimeInnerClassesUsedByAlphaSmokePaths() throws IOException {
        try (ZipFile jar = new ZipFile(System.getProperty("neoopencomputers.modJar"))) {
            assertContains(jar, "li/cil/oc/common/FileSystemRegistry$ResourceFileSystem$ResourceHandle.class");
            assertContains(jar, "li/cil/oc/common/component/InternetCardEnvironment$HttpTransport.class");
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

    @Test
    void generatedMavenPomContainsCommunityMetadata() throws Exception {
        final Path pom = Path.of(System.getProperty("neoopencomputers.mavenPom"));
        assertTrue(Files.isRegularFile(pom), "Generated Maven POM is missing");

        final var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        final var document = factory.newDocumentBuilder().parse(pom.toFile());

        assertTrue("li.cil.oc".equals(textOf(document, "groupId")), "POM must use stable public groupId");
        assertTrue("neoopencomputers".equals(textOf(document, "artifactId")), "POM must publish NeoOpenComputers artifactId");
        assertTrue("0.1.0".equals(textOf(document, "version")), "POM must publish current mod version");
        assertTrue("NeoOpenComputers".equals(textOf(document, "name")), "POM must publish mod name");
        assertTrue(textOf(document, "description").contains("Java-first NeoForge port"), "POM must describe the port");
        assertTrue(textOf(document, "url").contains("ThEWiZ76/NeoOpenComputers"), "POM must point at community repository");
        assertTrue(textOf(document, "license").contains("MIT License"), "POM must publish MIT license metadata");
        assertTrue(textOf(document, "connection").contains("ThEWiZ76/NeoOpenComputers.git"), "POM must publish SCM metadata");
    }

    @Test
    void localMavenRepositoryUsesPortableGradleUri() throws IOException {
        final String build = Files.readString(BUILD_GRADLE);

        assertTrue(build.contains("url = uri(layout.buildDirectory.dir('repo'))"),
            "Local Maven repository must use a portable Gradle URI");
        assertTrue(!build.contains("file://${project.projectDir}/repo"),
            "Local Maven repository must not use a Windows-hostile file URI string");
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

    private static String textOf(final org.w3c.dom.Document document, final String tagName) {
        final var nodes = document.getElementsByTagName(tagName);
        assertTrue(nodes.getLength() > 0, () -> "Missing POM element " + tagName);
        return nodes.item(0).getTextContent().trim();
    }
}
