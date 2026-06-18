package li.cil.oc.api.manual;

/**
 * Manual image renderer with tooltip and click behavior.
 */
public interface InteractiveImageRenderer extends ImageRenderer {
    String getTooltip(String tooltip);

    boolean onMouseClick(int mouseX, int mouseY);
}
