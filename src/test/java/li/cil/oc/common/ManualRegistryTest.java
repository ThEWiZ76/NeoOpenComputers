package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.client.ManualContent;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ManualRegistryTest {
    @AfterEach
    void resetApi() {
        API.manual = null;
    }

    @Test
    void bootstrapInstallsManualApi() {
        OpenComputersApi.initialize();

        assertTrue(API.manual instanceof ManualRegistry);
    }

    @Test
    void resolvesPathsAndContentInProviderOrder() {
        ManualRegistry registry = new ManualRegistry();

        registry.addProvider(new TestPathProvider(null, null));
        registry.addProvider(new TestPathProvider("stack/path", "block/path"));
        registry.addProvider(path -> null);
        registry.addProvider(path -> List.of("line"));

        assertEquals("stack/path", registry.pathFor(null));
        assertEquals("block/path", registry.pathFor(null, BlockPos.ZERO));
        assertIterableEquals(List.of("line"), registry.contentFor("index"));
    }

    @Test
    void resolvesManualContentLanguageFallbackAndRedirectsLikeUpstream() {
        ManualRegistry registry = new ManualRegistry();

        registry.addProvider(new MapContentProvider(Map.of(
            "en_us/index.md", List.of("#redirect block/../item/start.md"),
            "en_us/item/start.md", List.of("manual page")
        )));

        assertIterableEquals(List.of("manual page"), registry.contentFor("%LANGUAGE%/./index.md"));
    }

    @Test
    void triesCurrentManualLanguageBeforeFallbackLanguageLikeUpstream() {
        ManualRegistry registry = new ManualRegistry(() -> "nl_nl");

        registry.addProvider(new MapContentProvider(Map.of(
            "nl_nl/index.md", List.of("dutch page"),
            "en_us/index.md", List.of("english page"),
            "en_us/fallback.md", List.of("fallback page")
        )));

        assertIterableEquals(List.of("dutch page"), registry.contentFor("%LANGUAGE%/index.md"));
        assertIterableEquals(List.of("fallback page"), registry.contentFor("%LANGUAGE%/fallback.md"));
    }

    @Test
    void reportsManualContentRedirectLoopsLikeUpstream() {
        ManualRegistry registry = new ManualRegistry();

        registry.addProvider(new MapContentProvider(Map.of(
            "en_us/a.md", List.of("#redirect b.md"),
            "en_us/b.md", List.of("#redirect a.md")
        )));

        Iterable<String> content = registry.contentFor("%LANGUAGE%/a.md");

        assertTrue(content.iterator().next().startsWith("Redirection loop: "));
    }

    @Test
    void resolvesImagesByPrefix() {
        ManualRegistry registry = new ManualRegistry();
        ImageRenderer renderer = new TestImageRenderer();

        registry.addProvider("chart", data -> renderer);

        assertSame(renderer, registry.imageFor("chart:data"));
        assertNull(registry.imageFor("missing:data"));
        assertNull(registry.imageFor("bad"));
    }

    @Test
    void resolvesManualImagesLikeUpstreamPrefixRegistry() {
        ManualRegistry registry = new ManualRegistry();
        ImageRenderer fallback = new TestImageRenderer();
        ImageRenderer firstItem = new TestImageRenderer();
        ImageRenderer secondItem = new TestImageRenderer();

        registry.addProvider("", data -> fallback);
        registry.addProvider("item", data -> firstItem);
        registry.addProvider("item", data -> secondItem);

        assertSame(fallback, registry.imageFor("textures/gui/manual/home.png"));
        assertSame(secondItem, registry.imageFor("item:cpu1"));
    }

    @Test
    void storesTabsAndNavigationStateUntilGuiExists() {
        ManualRegistry registry = new ManualRegistry();
        TabIconRenderer tab = () -> {};

        registry.addTab(tab, "tooltip", "index");
        registry.openFor(null);
        registry.navigate("next");
        registry.reset();

        assertEquals(1, registry.tabCount());
        assertEquals("next", registry.lastNavigationPath());
        assertTrue(registry.wasOpened());
        assertTrue(registry.wasReset());
    }

    @Test
    void tracksManualHistoryLikeUpstream() {
        ManualRegistry registry = new ManualRegistry();

        assertEquals("%LANGUAGE%/index.md", registry.currentPath());
        assertEquals(1, registry.historySize());

        registry.navigate("general/computer.md");
        registry.navigate("item/cpu1.md");

        assertEquals("item/cpu1.md", registry.currentPath());
        assertEquals(3, registry.historySize());

        registry.reset();

        assertEquals("%LANGUAGE%/index.md", registry.currentPath());
        assertEquals(1, registry.historySize());
    }

    @Test
    void defaultManualContentRegistersProvidersAndTabsLikeUpstream() {
        ManualRegistry registry = new ManualRegistry();

        ManualContent.registerDefaults(registry);

        assertEquals(1, registry.pathProviderCount());
        assertEquals(1, registry.contentProviderCount());
        assertEquals(4, registry.imageProviderCount());
        assertEquals(3, registry.tabCount());
    }

    private record TestPathProvider(String itemPath, String blockPath) implements li.cil.oc.api.manual.PathProvider {
        @Override
        public String pathFor(final net.minecraft.world.item.ItemStack stack) {
            return itemPath;
        }

        @Override
        public String pathFor(final net.minecraft.world.level.Level world, final BlockPos pos) {
            return blockPath;
        }
    }

    private static final class TestImageRenderer implements ImageRenderer {
        @Override public int getWidth() { return 1; }
        @Override public int getHeight() { return 1; }
        @Override public void render(final int mouseX, final int mouseY) {}
    }

    private record MapContentProvider(Map<String, List<String>> content) implements li.cil.oc.api.manual.ContentProvider {
        @Override
        public Iterable<String> getContent(final String path) {
            return content.get(path);
        }
    }
}
