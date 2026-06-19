package li.cil.oc.api.internal;

import li.cil.oc.api.Persistable;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.world.entity.player.Player;

public interface TextBuffer extends ManagedEnvironment, Persistable {
    void setEnergyCostPerTick(double value);

    double getEnergyCostPerTick();

    void setPowerState(boolean value);

    boolean getPowerState();

    void setMaximumResolution(int width, int height);

    int getMaximumWidth();

    int getMaximumHeight();

    void setAspectRatio(double width, double height);

    double getAspectRatio();

    boolean setResolution(int width, int height);

    int getWidth();

    int getHeight();

    boolean setViewport(int width, int height);

    int getViewportWidth();

    int getViewportHeight();

    void setMaximumColorDepth(ColorDepth depth);

    ColorDepth getMaximumColorDepth();

    boolean setColorDepth(ColorDepth depth);

    ColorDepth getColorDepth();

    void setPaletteColor(int index, int color);

    int getPaletteColor(int index);

    void setForegroundColor(int color);

    void setForegroundColor(int color, boolean isFromPalette);

    int getForegroundColor();

    boolean isForegroundFromPalette();

    void setBackgroundColor(int color);

    void setBackgroundColor(int color, boolean isFromPalette);

    int getBackgroundColor();

    boolean isBackgroundFromPalette();

    void copy(int column, int row, int width, int height, int horizontalTranslation, int verticalTranslation);

    @Deprecated
    void fill(int column, int row, int width, int height, char value);

    void fill(int column, int row, int width, int height, int value);

    void set(int column, int row, String value, boolean vertical);

    @Deprecated
    char get(int column, int row);

    int getCodePoint(int column, int row);

    int getForegroundColor(int column, int row);

    boolean isForegroundFromPalette(int column, int row);

    int getBackgroundColor(int column, int row);

    boolean isBackgroundFromPalette(int column, int row);

    @Deprecated
    void rawSetText(int column, int row, char[][] text);

    void rawSetText(int column, int row, int[][] text);

    void rawSetForeground(int column, int row, int[][] color);

    void rawSetBackground(int column, int row, int[][] color);

    boolean renderText();

    int renderWidth();

    int renderHeight();

    void setRenderingEnabled(boolean enabled);

    boolean isRenderingEnabled();

    void keyDown(char character, int code, Player player);

    void keyUp(char character, int code, Player player);

    void clipboard(String value, Player player);

    void mouseDown(double x, double y, int button, Player player);

    void mouseDrag(double x, double y, int button, Player player);

    void mouseUp(double x, double y, int button, Player player);

    void mouseScroll(double x, double y, int delta, Player player);

    enum ColorDepth {
        OneBit,
        FourBit,
        EightBit
    }
}
