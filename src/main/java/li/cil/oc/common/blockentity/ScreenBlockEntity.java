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
import li.cil.oc.api.network.SidedEnvironment;
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
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScreenBlockEntity extends BlockEntity implements TextBuffer, DeviceInfo, Tiered, SidedEnvironment {
    private static final int MAX_MULTIBLOCK_WIDTH = 8;
    private static final int MAX_MULTIBLOCK_HEIGHT = 6;
    private static final int DEFAULT_WIDTH = 50;
    private static final int DEFAULT_HEIGHT = 16;
    private static final int DEFAULT_FOREGROUND = 0xFFFFFF;
    private static final int DEFAULT_BACKGROUND = 0x000000;
    private static final String TAG_BUFFER = "buffer";
    private static final String TAG_NODE = "node";
    private static final String TAG_RENDER_COLOR = "renderColorRGB";
    private static final String TAG_LAYOUT_ORIGIN_X = "layoutOriginX";
    private static final String TAG_LAYOUT_ORIGIN_Y = "layoutOriginY";
    private static final String TAG_LAYOUT_ORIGIN_Z = "layoutOriginZ";
    private static final String TAG_LAYOUT_WIDTH = "layoutWidth";
    private static final String TAG_LAYOUT_HEIGHT = "layoutHeight";
    private static final String TAG_LAYOUT_LOCAL_X = "layoutLocalX";
    private static final String TAG_LAYOUT_LOCAL_Y = "layoutLocalY";
    private static final String TAG_HAD_REDSTONE_INPUT = "oc:hadRedstoneInput";

    private double energyCostPerTick;
    private int tier;
    private int renderColor;
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
    private boolean hadRedstoneInput;
    private boolean renderingEnabled = true;
    private final int[] palette = new int[16];
    private final TextBufferState buffer = new TextBufferState(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    private final ScreenInputDispatcher inputDispatcher = new ScreenInputDispatcher();
    private BlockPos lastLayoutOrigin = BlockPos.ZERO;
    private int lastLayoutWidth = -1;
    private int lastLayoutHeight = -1;
    private int lastLayoutLocalX = -1;
    private int lastLayoutLocalY = -1;
    private boolean lastRenderOrigin = true;
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

    public int getRenderColor() {
        ensureTierConfigured();
        return renderColor;
    }

    public void setRenderColor(final DyeColor color) {
        setRenderColor(rgbValue(color));
    }

    public void setRenderColor(final int color) {
        final int rgb = color & 0xFFFFFF;
        if (renderColor == rgb) {
            return;
        }
        renderColor = rgb;
        markChanged();
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
                    && keyboard.canConnect(side.getOpposite())) {
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
        return new Object[]{isTouchModeInverted()};
    }

    public boolean isTouchModeInverted() {
        return supportsTouchMode() && touchModeInverted;
    }

    @Callback(doc = "function(value:boolean):boolean -- Sets whether to invert touch mode.")
    public Object[] setTouchModeInverted(final Context context, final Arguments args) {
        if (!supportsTouchMode()) {
            return new Object[]{null, "unsupported operation"};
        }
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
        if (!supportsPrecisionMode()) {
            return new Object[]{null, "unsupported operation"};
        }
        final boolean oldValue = precisionMode;
        precisionMode = args.checkBoolean(0);
        if (precisionMode != oldValue) {
            markChanged();
        }
        return new Object[]{oldValue};
    }

    public void updateRedstoneInput() {
        if (level == null || level.isClientSide) {
            return;
        }
        handleRedstoneInput(connectedScreensHaveRedstoneInput());
    }

    boolean handleRedstoneInput(final boolean hasRedstoneInput) {
        if (hadRedstoneInput == hasRedstoneInput) {
            return false;
        }
        hadRedstoneInput = hasRedstoneInput;
        markChanged();
        if (hasRedstoneInput) {
            final ScreenBlockEntity origin = level == null ? this : originScreen();
            origin.setPowerState(!origin.getPowerState());
            return true;
        }
        return false;
    }

    private boolean connectedScreensHaveRedstoneInput() {
        final BlockState state = getBlockState();
        final Direction right = localRight(state);
        final Direction up = ScreenBlock.up(state);
        final Direction pitch = ScreenBlock.pitch(state);
        final Direction yaw = ScreenBlock.yaw(state);
        for (final BlockPos screenPos : connectedScreens(pitch, yaw, right, up)) {
            if (level.hasNeighborSignal(screenPos)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setResolution(final int width, final int height) {
        if (width < 1 || height < 1 || width > maximumWidth || height > maximumHeight) {
            return false;
        }
        this.width = width;
        this.height = height;
        buffer.resize(width, height);
        viewportWidth = width;
        viewportHeight = height;
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
        return renderLayout().origin.equals(worldPosition);
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
        return renderLayout().width;
    }

    public int renderBlockHeight() {
        return renderLayout().height;
    }

    public int localBlockX() {
        return renderLayout().localX;
    }

    public int localBlockY() {
        return renderLayout().localY;
    }

    private ScreenLayout renderLayout() {
        if (lastLayoutWidth > 0 && lastLayoutHeight > 0 && (level == null || level.isClientSide)) {
            return new ScreenLayout(lastLayoutOrigin == null ? BlockPos.ZERO : lastLayoutOrigin, lastLayoutWidth, lastLayoutHeight, lastLayoutLocalX, lastLayoutLocalY);
        }
        return screenLayout();
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
        final ScreenGroup group = deterministicMergedGroup(connected, right, up);
        final ScreenCell originCell = absoluteCell(group.origin(), right, up);
        final ScreenCell localCell = absoluteCell(worldPosition, right, up);
        return new ScreenLayout(
            group.origin(),
            group.width(),
            group.height(),
            localCell.x() - originCell.x(),
            localCell.y() - originCell.y());
    }

    private ScreenGroup deterministicMergedGroup(final Set<BlockPos> connected, final Direction right, final Direction up) {
        final Map<ScreenCell, BlockPos> positionsByCell = new HashMap<>();
        final Map<BlockPos, ScreenGroup> groupsByPosition = new HashMap<>();
        final List<BlockPos> pending = new ArrayList<>(connected);
        pending.sort(Comparator.<BlockPos>comparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getY)
            .thenComparingInt(BlockPos::getZ));

        for (final BlockPos pos : connected) {
            positionsByCell.put(absoluteCell(pos, right, up), pos);
            groupsByPosition.put(pos, new ScreenGroup(pos));
        }

        while (!pending.isEmpty()) {
            final BlockPos first = pending.getFirst();
            final ScreenGroup group = groupsByPosition.get(first);
            while (tryMerge(group, positionsByCell, groupsByPosition, right, up)) {
                // Upstream keeps trying the current origin until no neighboring group can be merged.
            }
            pending.removeIf(group.positions()::contains);
        }
        return groupsByPosition.getOrDefault(worldPosition, new ScreenGroup(worldPosition));
    }

    private boolean tryMerge(
        final ScreenGroup group,
        final Map<ScreenCell, BlockPos> positionsByCell,
        final Map<BlockPos, ScreenGroup> groupsByPosition,
        final Direction right,
        final Direction up) {
        return tryMergeTowards(group, 0, group.height(), positionsByCell, groupsByPosition, right, up)
            || tryMergeTowards(group, 0, -1, positionsByCell, groupsByPosition, right, up)
            || tryMergeTowards(group, group.width(), 0, positionsByCell, groupsByPosition, right, up)
            || tryMergeTowards(group, -1, 0, positionsByCell, groupsByPosition, right, up);
    }

    private boolean tryMergeTowards(
        final ScreenGroup group,
        final int dx,
        final int dy,
        final Map<ScreenCell, BlockPos> positionsByCell,
        final Map<BlockPos, ScreenGroup> groupsByPosition,
        final Direction right,
        final Direction up) {
        final ScreenCell originCell = absoluteCell(group.origin(), right, up);
        final BlockPos otherPos = positionsByCell.get(new ScreenCell(originCell.x() + dx, originCell.y() + dy));
        if (otherPos == null) {
            return false;
        }
        final ScreenGroup other = groupsByPosition.get(otherPos);
        if (other == null || other == group) {
            return false;
        }

        final ScreenCell otherOriginCell = absoluteCell(other.origin(), right, up);
        final boolean canMergeAlongX = otherOriginCell.y() == originCell.y()
            && other.height() == group.height()
            && other.width() + group.width() <= MAX_MULTIBLOCK_WIDTH;
        final boolean canMergeAlongY = otherOriginCell.x() == originCell.x()
            && other.width() == group.width()
            && other.height() + group.height() <= MAX_MULTIBLOCK_HEIGHT;
        if (!canMergeAlongX && !canMergeAlongY) {
            return false;
        }

        final BlockPos newOrigin = canMergeAlongX
            ? (otherOriginCell.x() < originCell.x() ? other.origin() : group.origin())
            : (otherOriginCell.y() < originCell.y() ? other.origin() : group.origin());
        final int newWidth = canMergeAlongX ? group.width() + other.width() : group.width();
        final int newHeight = canMergeAlongX ? group.height() : group.height() + other.height();
        group.merge(other, newOrigin, newWidth, newHeight);
        for (final BlockPos pos : group.positions()) {
            groupsByPosition.put(pos, group);
        }
        return true;
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
            queueMatchingScreen(pending, visited, current.relative(right), pitch, yaw, tier, renderColor);
            queueMatchingScreen(pending, visited, current.relative(right.getOpposite()), pitch, yaw, tier, renderColor);
            queueMatchingScreen(pending, visited, current.relative(up), pitch, yaw, tier, renderColor);
            queueMatchingScreen(pending, visited, current.relative(up.getOpposite()), pitch, yaw, tier, renderColor);
        }
        return visited;
    }

    private void queueMatchingScreen(final ArrayDeque<BlockPos> pending, final Set<BlockPos> visited, final BlockPos pos, final Direction pitch, final Direction yaw, final int tier, final int renderColor) {
        if (visited.contains(pos) || level == null) {
            return;
        }
        final BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ScreenBlock screenBlock &&
            screenBlock.tier() == tier &&
            ScreenBlock.pitch(state) == pitch &&
            ScreenBlock.yaw(state) == yaw &&
            level.getBlockEntity(pos) instanceof ScreenBlockEntity screen &&
            screen.getRenderColor() == renderColor) {
            pending.add(pos);
        }
    }

    private static Direction localRight(final BlockState state) {
        return ScreenBlock.localRight(state);
    }

    private static ScreenCell absoluteCell(final BlockPos pos, final Direction right, final Direction up) {
        return new ScreenCell(
            pos.getX() * right.getStepX() + pos.getY() * right.getStepY() + pos.getZ() * right.getStepZ(),
            pos.getX() * up.getStepX() + pos.getY() * up.getStepY() + pos.getZ() * up.getStepZ());
    }

    private record ScreenLayout(BlockPos origin, int width, int height, int localX, int localY) {
    }

    private record ScreenCell(int x, int y) {
    }

    private static final class ScreenGroup {
        private final Set<BlockPos> positions = new HashSet<>();
        private BlockPos origin;
        private int width = 1;
        private int height = 1;

        private ScreenGroup(final BlockPos origin) {
            this.origin = origin;
            positions.add(origin);
        }

        private void merge(final ScreenGroup other, final BlockPos newOrigin, final int newWidth, final int newHeight) {
            positions.addAll(other.positions);
            origin = newOrigin;
            width = newWidth;
            height = newHeight;
        }

        private Set<BlockPos> positions() {
            return positions;
        }

        private BlockPos origin() {
            return origin;
        }

        private int width() {
            return width;
        }

        private int height() {
            return height;
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
    public Node sidedNode(final Direction side) {
        return canConnect(side) ? node() : null;
    }

    @Override
    public boolean canConnect(final Direction side) {
        return allowsNodeOnSide(ScreenBlock.facing(getBlockState()), side, hasKeyboardOnSide(side));
    }

    private boolean hasKeyboardOnSide(final Direction side) {
        return side != null && level != null && level.getBlockEntity(worldPosition.relative(side)) instanceof Keyboard;
    }

    private static boolean allowsNodeOnSide(final Direction facing, final Direction side, final boolean frontHasKeyboard) {
        return side != null && (side != facing || frontHasKeyboard);
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
        final boolean layoutChanged = !layout.origin().equals(lastLayoutOrigin)
            || layout.width() != lastLayoutWidth
            || layout.height() != lastLayoutHeight
            || layout.localX() != lastLayoutLocalX
            || layout.localY() != lastLayoutLocalY;
        if (layoutChanged) {
            lastLayoutOrigin = layout.origin();
            lastLayoutWidth = layout.width();
            lastLayoutHeight = layout.height();
            lastLayoutLocalX = layout.localX();
            lastLayoutLocalY = layout.localY();
            markChanged();
        }
        final boolean origin = layout.origin().equals(worldPosition);
        if (node() instanceof Component component) {
            component.setVisibility(origin ? Visibility.Network : Visibility.None);
        }
        if (origin) {
            lastRenderOrigin = true;
            setEnergyCostPerTick(ModSettings.screenCost() * layout.width() * layout.height());
            setAspectRatio(layout.width(), layout.height());
        } else {
            if (layoutChanged || lastRenderOrigin) {
                clearNonOriginBufferLikeUpstream();
            }
            lastRenderOrigin = false;
            setEnergyCostPerTick(ModSettings.screenCost());
            setAspectRatio(1.0D, 1.0D);
        }
    }

    private void clearNonOriginBufferLikeUpstream() {
        setForegroundColor(DEFAULT_FOREGROUND, false);
        setBackgroundColor(DEFAULT_BACKGROUND, false);
        fill(0, 0, getWidth(), getHeight(), ' ');
    }

    @Override
    public void load(final CompoundTag nbt) {
        ensureTierConfigured();
        if (nbt.contains(TAG_NODE) && node() != null) {
            node().load(nbt.getCompound(TAG_NODE));
        }
        powered = !nbt.contains("powered") || nbt.getBoolean("powered");
        hasPower = !nbt.contains("hasPower") || nbt.getBoolean("hasPower");
        renderColor = nbt.contains(TAG_RENDER_COLOR) ? nbt.getInt(TAG_RENDER_COLOR) & 0xFFFFFF : defaultRenderColor(tier);
        width = Math.clamp(nbt.getInt("width"), 1, maximumWidth);
        height = Math.clamp(nbt.getInt("height"), 1, maximumHeight);
        viewportWidth = Math.clamp(nbt.getInt("viewportWidth"), 1, width);
        viewportHeight = Math.clamp(nbt.getInt("viewportHeight"), 1, height);
        foregroundColor = nbt.getInt("foreground");
        backgroundColor = nbt.getInt("background");
        precisionMode = nbt.getBoolean("precisionMode");
        touchModeInverted = nbt.getBoolean("touchModeInverted");
        hadRedstoneInput = nbt.getBoolean(TAG_HAD_REDSTONE_INPUT);
        renderingEnabled = !nbt.contains("renderingEnabled") || nbt.getBoolean("renderingEnabled");
        if (nbt.contains(TAG_LAYOUT_WIDTH) && nbt.contains(TAG_LAYOUT_HEIGHT)) {
            lastLayoutOrigin = new BlockPos(
                nbt.getInt(TAG_LAYOUT_ORIGIN_X),
                nbt.getInt(TAG_LAYOUT_ORIGIN_Y),
                nbt.getInt(TAG_LAYOUT_ORIGIN_Z));
            lastLayoutWidth = nbt.getInt(TAG_LAYOUT_WIDTH);
            lastLayoutHeight = nbt.getInt(TAG_LAYOUT_HEIGHT);
            lastLayoutLocalX = nbt.getInt(TAG_LAYOUT_LOCAL_X);
            lastLayoutLocalY = nbt.getInt(TAG_LAYOUT_LOCAL_Y);
        }
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
        nbt.putInt(TAG_RENDER_COLOR, renderColor);
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
        nbt.putBoolean(TAG_HAD_REDSTONE_INPUT, hadRedstoneInput);
        nbt.putBoolean("renderingEnabled", renderingEnabled);
        if (lastLayoutWidth > 0 && lastLayoutHeight > 0) {
            final BlockPos layoutOrigin = lastLayoutOrigin == null ? BlockPos.ZERO : lastLayoutOrigin;
            nbt.putInt(TAG_LAYOUT_ORIGIN_X, layoutOrigin.getX());
            nbt.putInt(TAG_LAYOUT_ORIGIN_Y, layoutOrigin.getY());
            nbt.putInt(TAG_LAYOUT_ORIGIN_Z, layoutOrigin.getZ());
            nbt.putInt(TAG_LAYOUT_WIDTH, lastLayoutWidth);
            nbt.putInt(TAG_LAYOUT_HEIGHT, lastLayoutHeight);
            nbt.putInt(TAG_LAYOUT_LOCAL_X, lastLayoutLocalX);
            nbt.putInt(TAG_LAYOUT_LOCAL_Y, lastLayoutLocalY);
        }
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

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        load(tag);
    }

    @Override
    public void onDataPacket(final Connection net, final ClientboundBlockEntityDataPacket packet, final HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
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
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            Network.joinOrCreateNetwork(this);
        }
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
        if (renderColor == 0) {
            renderColor = defaultRenderColor(this.tier);
        }
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

    private boolean supportsPrecisionMode() {
        ensureTierConfigured();
        return maximumColorDepth == ModSettings.screenDepthByTier(2);
    }

    private boolean supportsTouchMode() {
        return tier > 0;
    }

    private static int tierFromBlockState(final BlockState blockState) {
        if (blockState != null && blockState.getBlock() instanceof ScreenBlock screenBlock) {
            return screenBlock.tier();
        }
        return 0;
    }

    private static int defaultRenderColor(final int tier) {
        return switch (Math.clamp(tier, 0, 2)) {
            case 1 -> rgbValue(DyeColor.YELLOW);
            case 2 -> rgbValue(DyeColor.CYAN);
            default -> rgbValue(DyeColor.LIGHT_GRAY);
        };
    }

    public static int rgbValue(final DyeColor color) {
        return switch (color) {
            case BLACK -> 0x444444;
            case RED -> 0xB3312C;
            case GREEN -> 0x339911;
            case BROWN -> 0x51301A;
            case BLUE -> 0x6666FF;
            case PURPLE -> 0x7B2FBE;
            case CYAN -> 0x66FFFF;
            case LIGHT_GRAY -> 0xABABAB;
            case GRAY -> 0x666666;
            case PINK -> 0xD88198;
            case LIME -> 0x66FF66;
            case YELLOW -> 0xFFFF66;
            case LIGHT_BLUE -> 0xAAAAFF;
            case MAGENTA -> 0xC354CD;
            case ORANGE -> 0xEB8844;
            case WHITE -> 0xF0F0F0;
        };
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
