package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.InteractiveImageRenderer;

/**
 * Base implementation of an interactive manual image renderer.
 */
public abstract class AbstractInteractiveImageRenderer implements InteractiveImageRenderer {
    @Override
    public String getTooltip(final String tooltip) {
        return tooltip;
    }

    @Override
    public boolean onMouseClick(final int mouseX, final int mouseY) {
        return false;
    }
}
