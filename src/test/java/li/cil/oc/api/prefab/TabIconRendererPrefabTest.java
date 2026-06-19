package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class TabIconRendererPrefabTest {
    @Test
    void itemStackTabIconRendererImplementsManualRenderer() {
        assertInstanceOf(TabIconRenderer.class, new ItemStackTabIconRenderer(null));
    }

    @Test
    void textureTabIconRendererImplementsManualRenderer() {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath("neoopencomputers", "textures/gui/icon.png");

        assertInstanceOf(TabIconRenderer.class, new TextureTabIconRenderer(location));
    }
}
