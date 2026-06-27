package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Keyboard;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.internal.Tiered;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.block.ScreenBlock;
import li.cil.oc.common.component.TerminalScreenSnapshot;
import li.cil.oc.common.component.ScreenEnvironment;
import li.cil.oc.common.component.ScreenInputDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ScreenBlockEntity extends BlockEntity implements TextBuffer, DeviceInfo, Tiered {
    private static final int MAX_MULTIBLOCK_WIDTH = 8;
    private static final int MAX_MULTIBLOCK_HEIGHT = 6;
    private static final int DEFAULT_WIDTH = 50;
    private static final int DEFAULT_HEIGHT = 16;
    private static final int DEFAULT_FOREGROUND = 0xFFFFFF;
    private static final int DEFAULT_BACKGROUND = 0x000000;
    private static final String TAG_BUFFER = "buffer";
    private static final String TAG_NODE = "node";

    private double energyCostPerTick;
    private int tier;
    private boolean powered = true;
    private boolean hasPower = true;
    private int updateTicks;
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
    private boolean precisionMode;
    private boolean touchModeInverted;
    private boolean renderingEnabled = true;
    private final int[] palette = new int[16];
    private final TextBufferState buffer = new TextBufferState(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    private final ScreenInputDispatcher inputDispatcher = new ScreenInputDispatcher();
    private volatile boolean pendingServerThreadChangeMark;
    private Node node;

    public ScreenBlockEntity(final BlockPos pos, final BlockState blockState) {
        this(pos, blockState, tierFromBlockState(blockState));
    }

    private ScreenBlockEntity(final BlockPos pos, final BlockState blockState, final int tier) {
        super(ModBlockEntities.SCREEN.get(), pos, blockState);
        configureTier(tier);
        node = ScreenEnvironment.createNode(this);
    }

    @Override
    public int tier() {
        return tier;
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

    public boolean hasKeyboard(final Player player) {
        if (node() != null) {
            for (final Node neighbor : node().neighbors()) {
                if (neighbor.host() instanceof KeyboardBlockEntity keyboard && keyboard.isUsableByPlayer(player)) {
                    return true;
                }
                if (neighbor.host() instanceof Keyboard) {
                    return true;
                }
            }
        }
        if (level == null) {
            return false;
        }
        final BlockState state = getBlockState();
        final Direction right = localRight(state);
        final Direction up = ScreenBlock.up(state);
        final Direction pitch = ScreenBlock.pitch(state);
        final Direction yaw = ScreenBlock.yaw(state);
        for (final BlockPos screenPos : connectedScreens(pitch, yaw, right, up)) {
            for (final Direction side : Direction.values()) {
                if (level.getBlockEntity(screenPos.relative(side)) instanceof KeyboardBlockEntity keyboard
                    && keyboard.isUsableByPlayer(player)) {
                    return true;
                }
            }
        }
        return false;
    }

    public TerminalScreenSnapshot terminalSnapshot() {
        final int snapshotWidth = renderWidth();
        final int snapshotHeight = renderHeight();
        final String[] lines = new String[snapshotHeight];
        final int[][] foreground = new int[snapshotHeight][snapshotWidth];
        final int[][] background = new int[snapshotHeight][snapshotWidth];
        for (int row = 0; row < snapshotHeight; row++) {
            final StringBuilder line = new StringBuilder(snapshotWidth);
            for (int column = 0; column < snapshotWidth; column++) {
                line.appendCodePoint(getCodePoint(column, row));
                foreground[row][column] = getForegroundColor(column, row);
                background[row][column] = getBackgroundColor(column, row);
            }
            lines[row] = line.toString();
        }
        return new TerminalScreenSnapshot(snapshotWidth, snapshotHeight, lines, foreground, background);
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

    public boolean isRenderOrigin() {
        return screenLayout().origin.equals(worldPosition);
    }

    public ScreenBlockEntity originScreen() {
        if (level == null) {
            return this;
        }
        final BlockPos origin = screenLayout().origin();
        if (origin.equals(worldPosition)) {
            return this;
        }
        return level.getBlockEntity(origin) instanceof ScreenBlockEntity screen ? screen : this;
    }

    public int renderBlockWidth() {
        return screenLayout().width;
    }

    public int renderBlockHeight() {
        return screenLayout().height;
    }

    public int localBlockX() {
        return screenLayout().localX;
    }

    public int localBlockY() {
        return screenLayout().localY;
    }

    private ScreenLayout screenLayout() {
        if (level == null) {
            return new ScreenLayout(worldPosition, 1, 1, 0, 0);
        }

        final BlockState state = getBlockState();
        final Direction right = localRight(state);
        final Direction up = ScreenBlock.up(state);
        final Direction pitch = ScreenBlock.pitch(state);
        final Direction yaw = ScreenBlock.yaw(state);
        final Set<BlockPos> connected = connectedScreens(pitch, yaw, right, up);
        final Set<ScreenCell> cells = new HashSet<>();
        int observedMinX = 0;
        int observedMinY = 0;
        int observedMaxX = 0;
        int observedMaxY = 0;
        for (final BlockPos pos : connected) {
            final int x = localX(worldPosition, pos, right);
            final int y = localY(worldPosition, pos, up);
            cells.add(new ScreenCell(x, y));
            observedMinX = Math.min(observedMinX, x);
            observedMinY = Math.min(observedMinY, y);
            observedMaxX = Math.max(observedMaxX, x);
            observedMaxY = Math.max(observedMaxY, y);
        }
        final ScreenRectangle rectangle = largestCompleteRectangle(cells, observedMinX, observedMinY, observedMaxX, observedMaxY);
        final BlockPos origin = worldPosition.relative(right, rectangle.minX()).relative(up, rectangle.minY());
        return new ScreenLayout(
            origin,
            rectangle.width(),
            rectangle.height(),
            -rectangle.minX(),
            -rectangle.minY());
    }

    private Set<BlockPos> connectedScreens(final Direction pitch, final Direction yaw, final Direction right, final Direction up) {
        final Set<BlockPos> visited = new HashSet<>();
        final ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        pending.add(worldPosition);
        while (!pending.isEmpty()) {
            final BlockPos current = pending.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            queueMatchingScreen(pending, visited, current.relative(right), pitch, yaw, tier);
            queueMatchingScreen(pending, visited, current.relative(right.getOpposite()), pitch, yaw, tier);
            queueMatchingScreen(pending, visited, current.relative(up), pitch, yaw, tier);
            queueMatchingScreen(pending, visited, current.relative(up.getOpposite()), pitch, yaw, tier);
        }
        return visited;
    }

    private void queueMatchingScreen(final ArrayDeque<BlockPos> pending, final Set<BlockPos> visited, final BlockPos pos, final Direction pitch, final Direction yaw, final int tier) {
        if (visited.contains(pos) || level == null) {
            return;
        }
        final BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ScreenBlock screenBlock && screenBlock.tier() == tier && ScreenBlock.pitch(state) == pitch && ScreenBlock.yaw(state) == yaw) {
            pending.add(pos);
        }
    }

    private static ScreenRectangle largestCompleteRectangle(final Set<ScreenCell> cells, final int observedMinX, final int observedMinY, final int observedMaxX, final int observedMaxY) {
        ScreenRectangle best = new ScreenRectangle(0, 0, 1, 1);
        final int minXLimit = Math.max(observedMinX, -MAX_MULTIBLOCK_WIDTH + 1);
        final int minYLimit = Math.max(observedMinY, -MAX_MULTIBLOCK_HEIGHT + 1);
        final int maxXLimit = Math.min(observedMaxX, MAX_MULTIBLOCK_WIDTH - 1);
        final int maxYLimit = Math.min(observedMaxY, MAX_MULTIBLOCK_HEIGHT - 1);
        for (int minX = minXLimit; minX <= 0; minX++) {
            for (int minY = minYLimit; minY <= 0; minY++) {
                for (int maxX = 0; maxX <= maxXLimit; maxX++) {
                    for (int maxY = 0; maxY <= maxYLimit; maxY++) {
                        final int width = maxX - minX + 1;
                        final int height = maxY - minY + 1;
                        if (width > MAX_MULTIBLOCK_WIDTH || height > MAX_MULTIBLOCK_HEIGHT) {
                            continue;
                        }
                        final ScreenRectangle candidate = new ScreenRectangle(minX, minY, width, height);
                        if (candidate.area() > best.area() && containsAll(cells, candidate)) {
                            best = candidate;
                        }
                    }
                }
            }
        }
        return best;
    }

    private static boolean containsAll(final Set<ScreenCell> cells, final ScreenRectangle rectangle) {
        for (int x = rectangle.minX(); x < rectangle.minX() + rectangle.width(); x++) {
            for (int y = rectangle.minY(); y < rectangle.minY() + rectangle.height(); y++) {
                if (!cells.contains(new ScreenCell(x, y))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Direction localRight(final BlockState state) {
        return ScreenBlock.localRight(state);
    }

    private static int localX(final BlockPos origin, final BlockPos pos, final Direction right) {
        return dot(pos.subtract(origin), right);
    }

    private static int localY(final BlockPos origin, final BlockPos pos, final Direction up) {
        return dot(pos.subtract(origin), up);
    }

    private static int dot(final BlockPos delta, final Direction direction) {
        return delta.getX() * direction.getStepX() + delta.getY() * direction.getStepY() + delta.getZ() * direction.getStepZ();
    }

    private record ScreenLayout(BlockPos origin, int width, int height, int localX, int localY) {
    }

    private record ScreenCell(int x, int y) {
    }

    private record ScreenRectangle(int minX, int minY, int width, int height) {
        private int area() {
            return width * height;
        }
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
        inputDispatcher.mouseDown(node(), x, y, button, player, precisionMode);
    }

    @Override
    public void mouseDrag(final double x, final double y, final int button, final Player player) {
        inputDispatcher.mouseDrag(node(), x, y, button, player, precisionMode);
    }

    @Override
    public void mouseUp(final double x, final double y, final int button, final Player player) {
        inputDispatcher.mouseUp(node(), x, y, button, player, precisionMode);
    }

    @Override
    public void mouseScroll(final double x, final double y, final int delta, final Player player) {
        inputDispatcher.mouseScroll(node(), x, y, delta, player, precisionMode);
    }

    @Override
    public Node node() {
        if (node == null) {
            node = ScreenEnvironment.createNode(this);
        }
        return node;
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
    public Map<String, String> getDeviceInfo() {
        return ScreenEnvironment.deviceInfo(maximumWidth, maximumHeight, maximumColorDepth);
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        updateMultiblockState();
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

    private void updateMultiblockState() {
        if (level == null) {
            return;
        }
        final ScreenLayout layout = screenLayout();
        final boolean origin = layout.origin().equals(worldPosition);
        if (node() instanceof Component component) {
            component.setVisibility(origin ? Visibility.Network : Visibility.None);
        }
        if (origin) {
            setEnergyCostPerTick(ModSettings.screenCost() * layout.width() * layout.height());
            setAspectRatio(layout.width(), layout.height());
        } else {
            setEnergyCostPerTick(ModSettings.screenCost());
            setAspectRatio(1.0D, 1.0D);
        }
    }

    @Override
    public void load(final CompoundTag nbt) {
        ensureTierConfigured();
        if (nbt.contains(TAG_NODE) && node() != null) {
            node().load(nbt.getCompound(TAG_NODE));
        }
        powered = !nbt.contains("powered") || nbt.getBoolean("powered");
        hasPower = !nbt.contains("hasPower") || nbt.getBoolean("hasPower");
        width = Math.clamp(nbt.getInt("width"), 1, maximumWidth);
        height = Math.clamp(nbt.getInt("height"), 1, maximumHeight);
        viewportWidth = Math.clamp(nbt.getInt("viewportWidth"), 1, width);
        viewportHeight = Math.clamp(nbt.getInt("viewportHeight"), 1, height);
        foregroundColor = nbt.getInt("foreground");
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
    protected void loadAdditional(final CompoundTag nbt, final HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        load(nbt);
    }

    @Override
    public void save(final CompoundTag nbt) {
        ensureTierConfigured();
        saveNode(nbt);
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

    @Override
    protected void saveAdditional(final CompoundTag nbt, final HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        save(nbt);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        save(tag);
        return tag;
    }

    private void saveNode(final CompoundTag nbt) {
        if (node() == null) {
            return;
        }

        final CompoundTag nodeTag = new CompoundTag();
        if (node().address() == null) {
            Network.joinNewNetwork(node());
            node().save(nodeTag);
            node().remove();
        } else {
            node().save(nodeTag);
        }
        nbt.put(TAG_NODE, nodeTag);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        removeNode();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeNode();
    }

    private void removeNode() {
        if (node != null) {
            node.remove();
        }
    }

    private void configureTier(final int tier) {
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
        buffer.resize(width, height);
    }

    private void ensureTierConfigured() {
        if (maximumWidth < 1 || maximumHeight < 1 || maximumColorDepth == null) {
            configureTier(tier);
        }
    }

    private static int tierFromBlockState(final BlockState blockState) {
        if (blockState != null && blockState.getBlock() instanceof ScreenBlock screenBlock) {
            return screenBlock.tier();
        }
        return 0;
    }

    private void markChanged() {
        if (level != null && level.getServer() != null && !level.getServer().isSameThread()) {
            if (!pendingServerThreadChangeMark) {
                pendingServerThreadChangeMark = true;
                level.getServer().execute(this::markChangedOnServerThread);
            }
            return;
        }

        markChangedOnServerThread();
    }

    private void markChangedOnServerThread() {
        pendingServerThreadChangeMark = false;
        super.setChanged();
        if (level != null && !level.isClientSide) {
            final BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, 3);
        }
    }
}
