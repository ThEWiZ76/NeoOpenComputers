package li.cil.oc.client;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.manual.InteractiveImageRenderer;
import net.minecraft.resources.ResourceLocation;

final class MissingManualImageRenderer extends TextureImageRenderer implements InteractiveImageRenderer {
    private static final ResourceLocation MISSING_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        NeoOpenComputers.MODID,
        "textures/gui/manual_missing_item.png");

    private final String tooltip;

    MissingManualImageRenderer(final String tooltip) {
        super(MISSING_TEXTURE);
        this.tooltip = tooltip;
    }

    @Override
    public String getTooltip(final String tooltip) {
        return this.tooltip;
    }

    @Override
    public boolean onMouseClick(final int mouseX, final int mouseY) {
        return false;
    }
}
