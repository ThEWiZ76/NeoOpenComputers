package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.ContentProvider;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class ResourceContentProviderTest {
    @Test
    void implementsContentProvider() {
        assertInstanceOf(ContentProvider.class, new TestResourceContentProvider("neoopencomputers"));
    }

    @Test
    void createsModernResourceLocationsFromManualPaths() {
        TestResourceContentProvider provider = new TestResourceContentProvider("neoopencomputers", "manual/");

        assertEquals(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "manual/index.md"), provider.location("index.md"));
        assertEquals(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "manual/index.md"), provider.location("/index.md"));
    }

    @Test
    void normalizesManualPathsForMinecraftResourceLocations() {
        TestResourceContentProvider provider = new TestResourceContentProvider("neoopencomputers", "doc/");

        assertEquals(
            ResourceLocation.fromNamespaceAndPath("neoopencomputers", "doc/en_us/item/batteryupgrade1.md"),
            provider.location("en_us/item/batteryUpgrade1.md"));
    }

    private static final class TestResourceContentProvider extends ResourceContentProvider {
        private TestResourceContentProvider(final String resourceDomain) {
            super(resourceDomain);
        }

        private TestResourceContentProvider(final String resourceDomain, final String basePath) {
            super(resourceDomain, basePath);
        }

        private ResourceLocation location(final String path) {
            return resourceLocation(path);
        }
    }
}
