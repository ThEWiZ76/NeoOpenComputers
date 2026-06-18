package li.cil.oc.api.manual;

/**
 * Renders a custom image area in a manual page.
 */
public interface ImageRenderer {
    int getWidth();

    int getHeight();

    void render(int mouseX, int mouseY);
}
