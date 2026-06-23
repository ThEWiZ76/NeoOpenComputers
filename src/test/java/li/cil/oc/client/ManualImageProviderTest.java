package li.cil.oc.client;

import li.cil.oc.api.manual.InteractiveImageRenderer;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ManualImageProviderTest {
    @Test
    void textureProviderCreatesRendererForResourceLocations() {
        TextureImageRenderer renderer = assertInstanceOf(
            TextureImageRenderer.class,
            new TextureImageProvider().getImage("neoopencomputers:doc/img/manual.png"));

        assertEquals(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "doc/img/manual.png"), renderer.location());
        assertTrue(renderer.getWidth() > 0);
        assertTrue(renderer.getHeight() > 0);
    }

    @Test
    void missingTextureProviderCreatesWarningRenderer() {
        ImageRenderer renderer = new TextureImageProvider().getImage("not a resource location");

        InteractiveImageRenderer interactive = assertInstanceOf(InteractiveImageRenderer.class, renderer);
        assertEquals("oc:gui.Manual.Warning.ImageMissing", interactive.getTooltip(""));
    }

    @Test
    void missingItemBlockAndOreDictProvidersCreateWarningRenderers() {
        assertWarning(new ItemImageProvider().getImage("not a resource location"), "oc:gui.Manual.Warning.ItemMissing");
        assertWarning(new BlockImageProvider().getImage("not a resource location"), "oc:gui.Manual.Warning.BlockMissing");
        assertWarning(new OreDictImageProvider().getImage("not a tag name"), "oc:gui.Manual.Warning.OreDictMissing");
    }

    private static void assertWarning(final ImageRenderer renderer, final String tooltip) {
        InteractiveImageRenderer interactive = assertInstanceOf(InteractiveImageRenderer.class, renderer);
        assertEquals(tooltip, interactive.getTooltip(""));
    }
}
