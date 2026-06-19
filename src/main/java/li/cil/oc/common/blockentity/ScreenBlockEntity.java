package li.cil.oc.common.blockentity;

import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ScreenBlockEntity extends BlockEntity implements TextBuffer {
    private static final int DEFAULT_WIDTH = 40;
    private static final int DEFAULT_HEIGHT = 16;
    private static final int DEFAULT_FOREGROUND = 0xFFFFFF;
    private static final int DEFAULT_BACKGROUND = 0x000000;

    private double energyCostPerTick;
    private boolean powered = true;
    private int maximumWidth = DEFAULT_WIDTH;
    private int maximumHeight = DEFAULT_HEIGHT;
    private double aspectWidth = 1.0D;
    private double aspectHeight = 1.0D;
    private int width = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;
    private int viewportWidth = DEFAULT_WIDTH;
    private int viewportHeight = DEFAULT_HEIGHT;
    private ColorDepth maximumColorDepth = ColorDepth.OneBit;
    private ColorDepth colorDepth = ColorDepth.OneBit;
    private int foregroundColor = DEFAULT_FOREGROUND;
    private int backgroundColor = DEFAULT_BACKGROUND;
    private boolean foregroundFromPalette;
    private boolean backgroundFromPalette;
    private boolean renderingEnabled = true;
    private final int[] palette = new int[16];
    private final TextBufferState buffer = new TextBufferState(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    public ScreenBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.SCREEN.get(), pos, blockState);
    }

    @Override
    public void setEnergyCostPerTick(final double value) {
        energyCostPerTick = Math.max(0.0D, value);
    }

    @Override
    public double getEnergyCostPerTick() {
        return energyCostPerTick;
    }

    @Override
    public void setPowerState(final boolean value) {
        powered = value;
        setChanged();
    }

    @Override
    public boolean getPowerState() {
        return powered;
    }

    @Override
    public void setMaximumResolution(final int width, final int height) {
        maximumWidth = Math.max(1, width);
        maximumHeight = Math.max(1, height);
        setResolution(Math.min(this.width, maximumWidth), Math.min(this.height, maximumHeight));
    }

    @Override
    public int getMaximumWidth() {
        return maximumWidth;
    }

    @Override
    public int getMaximumHeight() {
        return maximumHeight;
    }

    @Override
    public void setAspectRatio(final double width, final double height) {
        aspectWidth = Math.max(0.0D, width);
        aspectHeight = Math.max(0.0D, height);
    }

    @Override
    public double getAspectRatio() {
        return aspectWidth / Math.max(1.0D, aspectHeight);
    }

    @Override
    public boolean setResolution(final int width, final int height) {
        if (width < 1 || height < 1 || width > maximumWidth || height > maximumHeight) {
            return false;
        }
        this.width = width;
        this.height = height;
        buffer.resize(width, height);
        viewportWidth = Math.min(viewportWidth, width);
        viewportHeight = Math.min(viewportHeight, height);
        setChanged();
        return true;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public boolean setViewport(final int width, final int height) {
        if (width < 1 || height < 1 || width > this.width || height > this.height) {
            return false;
        }
        viewportWidth = width;
        viewportHeight = height;
        setChanged();
        return true;
    }

    @Override
    public int getViewportWidth() {
        return viewportWidth;
    }

    @Override
    public int getViewportHeight() {
        return viewportHeight;
    }

    @Override
    public void setMaximumColorDepth(final ColorDepth depth) {
        maximumColorDepth = depth == null ? ColorDepth.OneBit : depth;
        if (colorDepth.ordinal() > maximumColorDepth.ordinal()) {
            colorDepth = maximumColorDepth;
        }
    }

    @Override
    public ColorDepth getMaximumColorDepth() {
        return maximumColorDepth;
    }

    @Override
    public boolean setColorDepth(final ColorDepth depth) {
        if (depth == null || depth.ordinal() > maximumColorDepth.ordinal()) {
            return false;
        }
        colorDepth = depth;
        setChanged();
        return true;
    }

    @Override
    public ColorDepth getColorDepth() {
        return colorDepth;
    }

    @Override
    public void setPaletteColor(final int index, final int color) {
        if (index >= 0 && index < palette.length) {
            palette[index] = color;
            setChanged();
        }
    }

    @Override
    public int getPaletteColor(final int index) {
        return index >= 0 && index < palette.length ? palette[index] : 0;
    }

    @Override
    public void setForegroundColor(final int color) {
        setForegroundColor(color, false);
    }

    @Override
    public void setForegroundColor(final int color, final boolean isFromPalette) {
        foregroundColor = color;
        foregroundFromPalette = isFromPalette;
        setChanged();
    }

    @Override
    public int getForegroundColor() {
        return foregroundColor;
    }

    @Override
    public boolean isForegroundFromPalette() {
        return foregroundFromPalette;
    }

    @Override
    public void setBackgroundColor(final int color) {
        setBackgroundColor(color, false);
    }

    @Override
    public void setBackgroundColor(final int color, final boolean isFromPalette) {
        backgroundColor = color;
        backgroundFromPalette = isFromPalette;
        setChanged();
    }

    @Override
    public int getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public boolean isBackgroundFromPalette() {
        return backgroundFromPalette;
    }

    @Override
    public void copy(final int column, final int row, final int width, final int height, final int horizontalTranslation, final int verticalTranslation) {
        buffer.copy(column, row, width, height, horizontalTranslation, verticalTranslation);
        setChanged();
    }

    @Override
    public void fill(final int column, final int row, final int width, final int height, final char value) {
        fill(column, row, width, height, (int) value);
    }

    @Override
    public void fill(final int column, final int row, final int width, final int height, final int value) {
        buffer.fill(column, row, width, height, value);
        setChanged();
    }

    @Override
    public void set(final int column, final int row, final String value, final boolean vertical) {
        buffer.set(column, row, value, vertical);
        setChanged();
    }

    @Override
    public char get(final int column, final int row) {
        return (char) getCodePoint(column, row);
    }

    @Override
    public int getCodePoint(final int column, final int row) {
        return buffer.getCodePoint(column, row);
    }

    @Override
    public int getForegroundColor(final int column, final int row) {
        return foregroundColor;
    }

    @Override
    public boolean isForegroundFromPalette(final int column, final int row) {
        return foregroundFromPalette;
    }

    @Override
    public int getBackgroundColor(final int column, final int row) {
        return backgroundColor;
    }

    @Override
    public boolean isBackgroundFromPalette(final int column, final int row) {
        return backgroundFromPalette;
    }

    @Override
    public void rawSetText(final int column, final int row, final char[][] text) {
        buffer.rawSetText(column, row, text);
        setChanged();
    }

    @Override
    public void rawSetText(final int column, final int row, final int[][] text) {
        buffer.rawSetText(column, row, text);
        setChanged();
    }

    @Override
    public void rawSetForeground(final int column, final int row, final int[][] color) {
    }

    @Override
    public void rawSetBackground(final int column, final int row, final int[][] color) {
    }

    @Override
    public boolean renderText() {
        return renderingEnabled;
    }

    @Override
    public int renderWidth() {
        return viewportWidth;
    }

    @Override
    public int renderHeight() {
        return viewportHeight;
    }

    @Override
    public void setRenderingEnabled(final boolean enabled) {
        renderingEnabled = enabled;
    }

    @Override
    public boolean isRenderingEnabled() {
        return renderingEnabled;
    }

    @Override
    public void keyDown(final char character, final int code, final Player player) {
    }

    @Override
    public void keyUp(final char character, final int code, final Player player) {
    }

    @Override
    public void clipboard(final String value, final Player player) {
    }

    @Override
    public void mouseDown(final double x, final double y, final int button, final Player player) {
    }

    @Override
    public void mouseDrag(final double x, final double y, final int button, final Player player) {
    }

    @Override
    public void mouseUp(final double x, final double y, final int button, final Player player) {
    }

    @Override
    public void mouseScroll(final double x, final double y, final int delta, final Player player) {
    }

    @Override
    public Node node() {
        return null;
    }

    @Override
    public void onConnect(final Node node) {
    }

    @Override
    public void onDisconnect(final Node node) {
    }

    @Override
    public void onMessage(final Message message) {
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public void update() {
    }

    @Override
    public void load(final CompoundTag nbt) {
        powered = nbt.getBoolean("powered");
        width = Math.max(1, nbt.getInt("width"));
        height = Math.max(1, nbt.getInt("height"));
        viewportWidth = Math.max(1, nbt.getInt("viewportWidth"));
        viewportHeight = Math.max(1, nbt.getInt("viewportHeight"));
        foregroundColor = nbt.getInt("foreground");
        backgroundColor = nbt.getInt("background");
        renderingEnabled = !nbt.contains("renderingEnabled") || nbt.getBoolean("renderingEnabled");
    }

    @Override
    public void save(final CompoundTag nbt) {
        nbt.putBoolean("powered", powered);
        nbt.putInt("width", width);
        nbt.putInt("height", height);
        nbt.putInt("viewportWidth", viewportWidth);
        nbt.putInt("viewportHeight", viewportHeight);
        nbt.putInt("foreground", foregroundColor);
        nbt.putInt("background", backgroundColor);
        nbt.putBoolean("renderingEnabled", renderingEnabled);
    }
}
