package li.cil.oc.api.internal;

/**
 * Object with player-configurable RGB color.
 */
public interface Colored {
    int getColor();

    void setColor(int value);

    boolean controlsConnectivity();
}
