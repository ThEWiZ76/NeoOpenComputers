package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void resolvesImagesByPrefix() {
        ManualRegistry registry = new ManualRegistry();
        ImageRenderer renderer = new TestImageRenderer();

        registry.addProvider("chart", data -> renderer);

        assertSame(renderer, registry.imageFor("chart:data"));
        assertNull(registry.imageFor("missing:data"));
        assertNull(registry.imageFor("bad"));
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
}
