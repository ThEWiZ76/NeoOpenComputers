package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Keyboard;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.internal.Tiered;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.component.ScreenEnvironment;
import li.cil.oc.common.component.ScreenInputDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Map;

public final class ScreenItemEnvironment extends AbstractManagedEnvironment implements TextBuffer, DeviceInfo, Tiered {
    private static final String TAG_BUFFER = "buffer";

    private final EnvironmentHost host;
    private final int tier;
    private final int[] palette = new int[16];
    private final ScreenInputDispatcher inputDispatcher = new ScreenInputDispatcher();
    private final TextBufferState buffer;
    private double energyCostPerTick;
    private boolean powered = true;
    private boolean hasPower = true;
    private int updateTicks;
    private int maximumWidth;
    private int maximumHeight;
    private double aspectWidth = 1.0D;
    private double aspectHeight = 1.0D;
    private int width;
    private int height;
    private int viewportWidth;
    private int viewportHeight;
    private ColorDepth maximumColorDepth;
    private ColorDepth colorDepth;
    private int foregroundColor = 0xFFFFFF;
    private int backgroundColor = 0x000000;
    private boolean foregroundFromPalette;
    private boolean backgroundFromPalette;
    private boolean precisionMode;
    private boolean touchModeInverted;
    private boolean renderingEnabled = true;

    public ScreenItemEnvironment(final EnvironmentHost host, final int tier) {
        this.host = host;
        this.tier = Math.clamp(tier, 0, ModSettings.screenWidthsByTier().size() - 1);
        maximumWidth = ModSettings.screenWidthByTier(this.tier);
        maximumHeight = ModSettings.screenHeightByTier(this.tier);
        maximumColorDepth = ModSettings.screenDepthByTier(this.tier);
        energyCostPerTick = ModSettings.screenCost();
        colorDepth = maximumColorDepth;
        width = maximumWidth;
        height = maximumHeight;
        viewportWidth = width;
        viewportHeight = height;
        buffer = new TextBufferState(width, height);
        setNode(ScreenEnvironment.createNode(this));
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return ScreenEnvironment.deviceInfo(maximumWidth, maximumHeight, maximumColorDepth);
    }

    @Override
    public void setEnergyCostPerTick(final double value) {
        energyCostPerTick = Math.max(0.0D, value);
    }

    @Override
    public double getEnergyCostPerTick() {
        return energyCostPerTick;
    }

    public double fullyLitEnergyCostPerTick() {
        final double basicPixels = (double) ModSettings.screenWidthByTier(0) * (double) ModSettings.screenHeightByTier(0);
        final double maximumPixels = (double) maximumWidth * (double) maximumHeight;
        return energyCostPerTick * maximumPixels / Math.max(1D, basicPixels);
    }

    @Override
    public void setPowerState(final boolean value) {
        powered = value;
        markChanged();
    }

    @Override
    public boolean getPowerState() {
        return powered;
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the screen is currently on.")
    public Object[] isOn(final Context context, final Arguments args) {
        return new Object[]{getPowerState()};
    }

    @Callback(doc = "function():boolean, boolean -- Turns the screen on. Returns whether the state changed, and whether it is now on.")
    public Object[] turnOn(final Context context, final Arguments args) {
        final boolean oldPowerState = getPowerState();
        setPowerState(true);
        return new Object[]{getPowerState() != oldPowerState, getPowerState()};
    }

    @Callback(doc = "function():boolean, boolean -- Turns off the screen. Returns whether the state changed, and whether it is now on.")
    public Object[] turnOff(final Context context, final Arguments args) {
        final boolean oldPowerState = getPowerState();
        setPowerState(false);
        return new Object[]{getPowerState() != oldPowerState, getPowerState()};
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

    @Callback(direct = true, doc = "function():number, number -- The aspect ratio of the screen.")
    public Object[] getAspectRatio(final Context context, final Arguments args) {
        return new Object[]{aspectWidth, aspectHeight};
    }

    @Callback(doc = "function():table -- The list of keyboards attached to the screen.")
    public Object[] getKeyboards(final Context context, final Arguments args) {
        final ArrayList<String> addresses = new ArrayList<>();
        if (node() != null) {
            for (Node neighbor : node().neighbors()) {
                if (neighbor.host() instanceof Keyboard && neighbor.address() != null) {
                    addresses.add(neighbor.address());
                }
            }
        }
        return new Object[]{addresses.toArray(String[]::new)};
    }

    @Callback(direct = true, doc = "function():boolean -- Whether touch mode is inverted.")
    public Object[] isTouchModeInverted(final Context context, final Arguments args) {
        return new Object[]{touchModeInverted};
    }

    @Callback(doc = "function(value:boolean):boolean -- Sets whether to invert touch mode.")
    public Object[] setTouchModeInverted(final Context context, final Arguments args) {
        final boolean oldValue = touchModeInverted;
        touchModeInverted = args.checkBoolean(0);
        if (touchModeInverted != oldValue) {
            markChanged();
        }
        return new Object[]{oldValue};
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the screen is in high precision mode.")
    public Object[] isPrecise(final Context context, final Arguments args) {
        return new Object[]{precisionMode};
    }

    @Callback(doc = "function(enabled:boolean):boolean -- Set whether to use high precision mode.")
    public Object[] setPrecise(final Context context, final Arguments args) {
        final boolean oldValue = precisionMode;
        precisionMode = args.checkBoolean(0);
        if (precisionMode != oldValue) {
            markChanged();
        }
        return new Object[]{oldValue};
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
        markChanged();
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
        markChanged();
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
        markChanged();
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
            markChanged();
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
        markChanged();
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
        markChanged();
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
        markChanged();
    }

    @Override
    public void fill(final int column, final int row, final int width, final int height, final char value) {
        fill(column, row, width, height, (int) value);
    }

    @Override
    public void fill(final int column, final int row, final int width, final int height, final int value) {
        buffer.fill(column, row, width, height, value, foregroundColor, foregroundFromPalette, backgroundColor, backgroundFromPalette);
        markChanged();
    }

    @Override
    public void set(final int column, final int row, final String value, final boolean vertical) {
        buffer.set(column, row, value, vertical, foregroundColor, foregroundFromPalette, backgroundColor, backgroundFromPalette);
        markChanged();
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
        return buffer.getForegroundColor(column, row);
    }

    @Override
    public boolean isForegroundFromPalette(final int column, final int row) {
        return buffer.isForegroundFromPalette(column, row);
    }

    @Override
    public int getBackgroundColor(final int column, final int row) {
        return buffer.getBackgroundColor(column, row);
    }

    @Override
    public boolean isBackgroundFromPalette(final int column, final int row) {
        return buffer.isBackgroundFromPalette(column, row);
    }

    @Override
    public void rawSetText(final int column, final int row, final char[][] text) {
        buffer.rawSetText(column, row, text);
        markChanged();
    }

    @Override
    public void rawSetText(final int column, final int row, final int[][] text) {
        buffer.rawSetText(column, row, text);
        markChanged();
    }

    @Override
    public void rawSetForeground(final int column, final int row, final int[][] color) {
        buffer.rawSetForeground(column, row, color);
        markChanged();
    }

    @Override
    public void rawSetBackground(final int column, final int row, final int[][] color) {
        buffer.rawSetBackground(column, row, color);
        markChanged();
    }

    @Override
    public boolean renderText() {
        return renderingEnabled && powered && hasPower;
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
        inputDispatcher.keyDown(node(), character, code, player);
    }

    @Override
    public void keyUp(final char character, final int code, final Player player) {
        inputDispatcher.keyUp(node(), character, code, player);
    }

    @Override
    public void clipboard(final String value, final Player player) {
        inputDispatcher.clipboard(node(), value, player);
    }

    @Override
    public void mouseDown(final double x, final double y, final int button, final Player player) {
        inputDispatcher.mouseDown(node(), x, y, button, player);
    }

    @Override
    public void mouseDrag(final double x, final double y, final int button, final Player player) {
        inputDispatcher.mouseDrag(node(), x, y, button, player);
    }

    @Override
    public void mouseUp(final double x, final double y, final int button, final Player player) {
        inputDispatcher.mouseUp(node(), x, y, button, player);
    }

    @Override
    public void mouseScroll(final double x, final double y, final int delta, final Player player) {
        inputDispatcher.mouseScroll(node(), x, y, delta, player);
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        if (!powered) {
            return;
        }
        updateTicks++;
        final int tickFrequency = Math.max(1, ModSettings.mfuTickFrequency());
        if (updateTicks % tickFrequency != 0) {
            return;
        }
        final double cost = fullyLitEnergyCostPerTick() * buffer.litRatio(viewportWidth, viewportHeight) * tickFrequency;
        final boolean newHasPower = cost <= 0D || node() instanceof Connector connector && connector.tryChangeBuffer(-cost);
        if (hasPower != newHasPower) {
            hasPower = newHasPower;
            markChanged();
        }
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        powered = !nbt.contains("powered") || nbt.getBoolean("powered");
        hasPower = !nbt.contains("hasPower") || nbt.getBoolean("hasPower");
        width = Math.clamp(nbt.getInt("width"), 1, maximumWidth);
        height = Math.clamp(nbt.getInt("height"), 1, maximumHeight);
        viewportWidth = Math.clamp(nbt.getInt("viewportWidth"), 1, width);
        viewportHeight = Math.clamp(nbt.getInt("viewportHeight"), 1, height);
        foregroundColor = nbt.contains("foreground") ? nbt.getInt("foreground") : 0xFFFFFF;
        backgroundColor = nbt.getInt("background");
        precisionMode = nbt.getBoolean("precisionMode");
        touchModeInverted = nbt.getBoolean("touchModeInverted");
        renderingEnabled = !nbt.contains("renderingEnabled") || nbt.getBoolean("renderingEnabled");
        if (nbt.contains(TAG_BUFFER)) {
            buffer.load(nbt.getCompound(TAG_BUFFER));
        }
        buffer.resize(width, height);
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        nbt.putBoolean("powered", powered);
        nbt.putBoolean("hasPower", hasPower);
        nbt.putInt("width", width);
        nbt.putInt("height", height);
        nbt.putInt("viewportWidth", viewportWidth);
        nbt.putInt("viewportHeight", viewportHeight);
        nbt.putInt("foreground", foregroundColor);
        nbt.putInt("background", backgroundColor);
        nbt.putBoolean("precisionMode", precisionMode);
        nbt.putBoolean("touchModeInverted", touchModeInverted);
        nbt.putBoolean("renderingEnabled", renderingEnabled);
        final CompoundTag bufferTag = new CompoundTag();
        buffer.save(bufferTag);
        nbt.put(TAG_BUFFER, bufferTag);
    }

    private void markChanged() {
        if (host != null) {
            host.markChanged();
        }
    }
}
