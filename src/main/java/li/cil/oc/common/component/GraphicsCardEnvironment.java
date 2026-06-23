package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;

public class GraphicsCardEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "gpu";
    private static final String SCREEN_TAG = "screen";
    private static final String ACTIVE_BUFFER_TAG = "bufferIndex";
    private static final String VIDEO_RAM_TAG = "videoRam";
    private static final String PAGES_TAG = "pages";
    private static final String PAGE_INDEX_TAG = "pageIndex";
    private static final String PAGE_DATA_TAG = "pageData";
    private static final int SCREEN_INDEX = 0;
    private static final double[] SET_BACKGROUND_COSTS = {1.0D / 32.0D, 1.0D / 64.0D, 1.0D / 128.0D};
    private static final double[] SET_FOREGROUND_COSTS = {1.0D / 32.0D, 1.0D / 64.0D, 1.0D / 128.0D};
    private static final double[] SET_PALETTE_COLOR_COSTS = {1.0D / 2.0D, 1.0D / 8.0D, 1.0D / 16.0D};
    private static final double[] SET_COSTS = {1.0D / 64.0D, 1.0D / 128.0D, 1.0D / 256.0D};
    private static final double[] COPY_COSTS = {1.0D / 16.0D, 1.0D / 32.0D, 1.0D / 64.0D};
    private static final double[] FILL_COSTS = {1.0D / 32.0D, 1.0D / 64.0D, 1.0D / 128.0D};

    private final int tier;
    private final int maxWidth;
    private final int maxHeight;
    private final TextBuffer.ColorDepth maxDepth;
    private final int totalVideoMemory;
    private final Map<Integer, VideoBuffer> videoBuffers = new LinkedHashMap<>();
    private String screenAddress;
    private TextBuffer screen;
    private int activeBufferIndex = SCREEN_INDEX;

    public GraphicsCardEnvironment(final int tier) {
        final int clampedTier = Math.max(0, Math.min(2, tier));
        this.tier = clampedTier;
        maxWidth = ModSettings.screenWidthByTier(clampedTier);
        maxHeight = ModSettings.screenHeightByTier(clampedTier);
        maxDepth = ModSettings.screenDepthByTier(clampedTier);
        totalVideoMemory = maxWidth * maxHeight * 4;

        final var builder = Network.newNode(this, Visibility.Neighbors);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Neighbors).withConnector().create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Display,
            DeviceInfo.DeviceAttribute.Description, "Graphics controller",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
            DeviceInfo.DeviceAttribute.Product, "MPG" + ((tier + 1) * 1000) + " GTZ",
            DeviceInfo.DeviceAttribute.Capacity, Integer.toString(maxWidth * maxHeight),
            DeviceInfo.DeviceAttribute.Width, Integer.toString(bits(maxDepth)),
            DeviceInfo.DeviceAttribute.Clock, clockInfo(tier)
        );
    }

    @Callback(doc = "function(address:string[, reset:boolean=true]):boolean -- Binds this GPU to a screen.")
    public Object[] bind(final Context context, final Arguments args) {
        final String address = args.checkString(0);
        final boolean reset = args.optBoolean(1, true);
        if (node() == null || node().network() == null) {
            return new Object[]{null, "invalid address"};
        }

        final Node target = node().network().node(address);
        if (target == null) {
            return new Object[]{null, "invalid address"};
        }
        if (!(target.host() instanceof TextBuffer buffer)) {
            return new Object[]{null, "not a screen"};
        }

        screenAddress = address;
        screen = buffer;
        if (reset) {
            resetScreen(buffer);
        } else if (context != null) {
            context.pause(0);
        }
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function():string -- Returns the bound screen address.")
    public Object[] getScreen(final Context context, final Arguments args) {
        return screen == null || screen.node() == null ? noScreen() : new Object[]{screen.node().address()};
    }

    @Callback(direct = true, doc = "function():number, number -- Returns the current screen resolution.")
    public Object[] getResolution(final Context context, final Arguments args) {
        return withActiveBuffer(buffer -> new Object[]{buffer.getWidth(), buffer.getHeight()});
    }

    @Callback(doc = "function(width:number, height:number):boolean -- Sets the current screen resolution.")
    public Object[] setResolution(final Context context, final Arguments args) {
        final int width = args.checkInteger(0);
        final int height = args.checkInteger(1);
        checkSize(width, height, maxWidth, maxHeight, "unsupported resolution");
        return withActiveBuffer(buffer -> new Object[]{buffer.setResolution(width, height)});
    }

    @Callback(direct = true, doc = "function():number, number -- Returns the maximum screen resolution.")
    public Object[] maxResolution(final Context context, final Arguments args) {
        return withActiveBuffer(buffer -> new Object[]{
            Math.min(maxWidth, buffer.getMaximumWidth()),
            Math.min(maxHeight, buffer.getMaximumHeight())
        });
    }

    @Callback(direct = true, doc = "function():number, number -- Returns the current viewport resolution.")
    public Object[] getViewport(final Context context, final Arguments args) {
        return withActiveBuffer(buffer -> new Object[]{buffer.getViewportWidth(), buffer.getViewportHeight()});
    }

    @Callback(doc = "function(width:number, height:number):boolean -- Sets the current viewport resolution.")
    public Object[] setViewport(final Context context, final Arguments args) {
        final int width = args.checkInteger(0);
        final int height = args.checkInteger(1);
        checkSize(width, height, maxWidth, maxHeight, "unsupported viewport size");
        return withActiveBuffer(buffer -> {
            if (width > buffer.getWidth() || height > buffer.getHeight()) {
                throw new IllegalArgumentException("unsupported viewport size");
            }
            return new Object[]{buffer.setViewport(width, height)};
        });
    }

    @Callback(direct = true, doc = "function():number, boolean -- Returns the background color.")
    public Object[] getBackground(final Context context, final Arguments args) {
        return withActiveBuffer(buffer -> new Object[]{buffer.getBackgroundColor(), buffer.isBackgroundFromPalette()});
    }

    @Callback(direct = true, doc = "function(value:number[, palette:boolean]):number, number or nil -- Sets the background color.")
    public Object[] setBackground(final Context context, final Arguments args) throws LimitReachedException {
        final int color = args.checkInteger(0);
        final boolean palette = args.optBoolean(1, false);
        consumeScreenCallBudget(context, SET_BACKGROUND_COSTS[tier]);
        return withActiveBuffer(buffer -> {
            final int previous = buffer.getBackgroundColor();
            final boolean wasPalette = buffer.isBackgroundFromPalette();
            final Object[] result = previousColorResult(buffer, previous, wasPalette);
            buffer.setBackgroundColor(color, palette);
            return result;
        });
    }

    @Callback(direct = true, doc = "function():number, boolean -- Returns the foreground color.")
    public Object[] getForeground(final Context context, final Arguments args) {
        return withActiveBuffer(buffer -> new Object[]{buffer.getForegroundColor(), buffer.isForegroundFromPalette()});
    }

    @Callback(direct = true, doc = "function(value:number[, palette:boolean]):number, number or nil -- Sets the foreground color.")
    public Object[] setForeground(final Context context, final Arguments args) throws LimitReachedException {
        final int color = args.checkInteger(0);
        final boolean palette = args.optBoolean(1, false);
        consumeScreenCallBudget(context, SET_FOREGROUND_COSTS[tier]);
        return withActiveBuffer(buffer -> {
            final int previous = buffer.getForegroundColor();
            final boolean wasPalette = buffer.isForegroundFromPalette();
            final Object[] result = previousColorResult(buffer, previous, wasPalette);
            buffer.setForegroundColor(color, palette);
            return result;
        });
    }

    @Callback(direct = true, doc = "function(index:number):number -- Gets a palette color.")
    public Object[] getPaletteColor(final Context context, final Arguments args) {
        final int index = args.checkInteger(0);
        checkPaletteIndex(index);
        return withActiveBuffer(buffer -> new Object[]{buffer.getPaletteColor(index)});
    }

    @Callback(direct = true, doc = "function(index:number, color:number):number -- Sets a palette color and returns the previous value.")
    public Object[] setPaletteColor(final Context context, final Arguments args) throws LimitReachedException {
        final int index = args.checkInteger(0);
        final int color = args.checkInteger(1);
        checkPaletteIndex(index);
        consumeScreenCallBudget(context, SET_PALETTE_COLOR_COSTS[tier]);
        if (context != null && activeBufferIndex == SCREEN_INDEX) {
            context.pause(0.1D);
        }
        return withActiveBuffer(buffer -> {
            final int previous = buffer.getPaletteColor(index);
            buffer.setPaletteColor(index, color);
            return new Object[]{previous};
        });
    }

    @Callback(direct = true, doc = "function():number -- Returns the active buffer index. Zero is the screen.")
    public Object[] getActiveBuffer(final Context context, final Arguments args) {
        return new Object[]{activeBufferIndex};
    }

    @Callback(direct = true, doc = "function(index:number):number -- Sets the active buffer index and returns the previous index.")
    public Object[] setActiveBuffer(final Context context, final Arguments args) {
        final int index = args.checkInteger(0);
        if (index != SCREEN_INDEX && !videoBuffers.containsKey(index)) {
            return invalidBufferIndex();
        }
        final int previous = activeBufferIndex;
        activeBufferIndex = index;
        return new Object[]{previous};
    }

    @Callback(direct = true, doc = "function():table -- Returns allocated buffer indexes.")
    public Object[] buffers(final Context context, final Arguments args) {
        return new Object[]{videoBuffers.keySet().stream().mapToInt(Integer::intValue).toArray()};
    }

    @Callback(direct = true, doc = "function([width:number, height:number]):number -- Allocates a video memory buffer.")
    public Object[] allocateBuffer(final Context context, final Arguments args) {
        final int width = args.optInteger(0, maxWidth);
        final int height = args.optInteger(1, maxHeight);
        if (width <= 0 || height <= 0) {
            return new Object[]{null, "invalid page dimensions: must be greater than zero"};
        }
        final int size = width * height;
        if (size > freeVideoMemory()) {
            return new Object[]{null, "not enough video memory"};
        }
        final int index = nextBufferIndex();
        videoBuffers.put(index, new VideoBuffer(width, height, maxDepth));
        return new Object[]{index};
    }

    @Callback(direct = true, doc = "function([index:number]):boolean -- Frees a video memory buffer.")
    public Object[] freeBuffer(final Context context, final Arguments args) {
        final int index = args.optInteger(0, activeBufferIndex);
        if (videoBuffers.remove(index) == null) {
            return new Object[]{null, "no buffer at index"};
        }
        if (activeBufferIndex == index) {
            activeBufferIndex = SCREEN_INDEX;
        }
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function():number -- Frees all video memory buffers and returns the count.")
    public Object[] freeAllBuffers(final Context context, final Arguments args) {
        final int count = videoBuffers.size();
        videoBuffers.clear();
        activeBufferIndex = SCREEN_INDEX;
        return new Object[]{count};
    }

    @Callback(direct = true, doc = "function():number -- Returns total video memory available for buffers.")
    public Object[] totalMemory(final Context context, final Arguments args) {
        return new Object[]{totalVideoMemory};
    }

    @Callback(direct = true, doc = "function():number -- Returns free video memory available for buffers.")
    public Object[] freeMemory(final Context context, final Arguments args) {
        return new Object[]{freeVideoMemory()};
    }

    @Callback(direct = true, doc = "function([index:number]):number, number -- Returns buffer dimensions.")
    public Object[] getBufferSize(final Context context, final Arguments args) {
        final int index = args.optInteger(0, activeBufferIndex);
        return withBuffer(index, buffer -> new Object[]{buffer.getWidth(), buffer.getHeight()});
    }

    @Callback(direct = true, doc = "function():number -- Returns the current color depth.")
    public Object[] getDepth(final Context context, final Arguments args) {
        return withActiveBuffer(buffer -> new Object[]{bits(buffer.getColorDepth())});
    }

    @Callback(doc = "function(depth:number):number -- Sets the current color depth.")
    public Object[] setDepth(final Context context, final Arguments args) {
        final TextBuffer.ColorDepth depth = depth(args.checkInteger(0));
        if (depth.ordinal() > maxDepth.ordinal()) {
            throw new IllegalArgumentException("unsupported depth");
        }
        return withActiveBuffer(buffer -> {
            final int previous = bits(buffer.getColorDepth());
            if (!buffer.setColorDepth(depth)) {
                throw new IllegalArgumentException("unsupported depth");
            }
            return new Object[]{previous};
        });
    }

    @Callback(direct = true, doc = "function():number -- Returns the maximum supported color depth.")
    public Object[] maxDepth(final Context context, final Arguments args) {
        return withActiveBuffer(buffer -> new Object[]{bits(minDepth(maxDepth, buffer.getMaximumColorDepth()))});
    }

    @Callback(direct = true, doc = "function(x:number, y:number):string, number, number, number or nil, number or nil -- Gets a screen cell.")
    public Object[] get(final Context context, final Arguments args) {
        final int x = args.checkInteger(0) - 1;
        final int y = args.checkInteger(1) - 1;
        return withActiveBuffer(buffer -> {
            final Object[] foreground = previousColorResult(buffer, buffer.getForegroundColor(x, y), buffer.isForegroundFromPalette(x, y));
            final Object[] background = previousColorResult(buffer, buffer.getBackgroundColor(x, y), buffer.isBackgroundFromPalette(x, y));
            return new Object[]{
                new String(Character.toChars(buffer.getCodePoint(x, y))),
                foreground[0],
                background[0],
                foreground[1],
                background[1]
            };
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, value:string[, vertical:boolean]):boolean -- Writes text to the screen.")
    public Object[] set(final Context context, final Arguments args) throws LimitReachedException {
        final int x = args.checkInteger(0) - 1;
        final int y = args.checkInteger(1) - 1;
        final String value = args.checkString(2);
        final boolean vertical = args.optBoolean(3, false);
        consumeScreenCallBudget(context, SET_COSTS[tier]);
        return withActiveBuffer(buffer -> {
            if (!consumeScreenEnergy(value.codePointCount(0, value.length()), ModSettings.gpuSetCost())) {
                return notEnoughEnergy();
            }
            buffer.set(x, y, value, vertical);
            return new Object[]{true};
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, width:number, height:number, tx:number, ty:number):boolean -- Copies screen text.")
    public Object[] copy(final Context context, final Arguments args) throws LimitReachedException {
        final int x = args.checkInteger(0) - 1;
        final int y = args.checkInteger(1) - 1;
        final int width = Math.max(0, args.checkInteger(2));
        final int height = Math.max(0, args.checkInteger(3));
        final int tx = args.checkInteger(4);
        final int ty = args.checkInteger(5);
        consumeScreenCallBudget(context, COPY_COSTS[tier]);
        return withActiveBuffer(buffer -> {
            if (!consumeScreenEnergy(width * height, ModSettings.gpuCopyCost())) {
                return notEnoughEnergy();
            }
            buffer.copy(x, y, width, height, tx, ty);
            return new Object[]{true};
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, width:number, height:number, char:string):boolean -- Fills screen text.")
    public Object[] fill(final Context context, final Arguments args) throws LimitReachedException {
        final int x = args.checkInteger(0) - 1;
        final int y = args.checkInteger(1) - 1;
        final int width = Math.max(0, args.checkInteger(2));
        final int height = Math.max(0, args.checkInteger(3));
        final String value = args.checkString(4);
        if (value.codePointCount(0, value.length()) != 1) {
            throw new IllegalArgumentException("invalid fill value");
        }
        consumeScreenCallBudget(context, FILL_COSTS[tier]);
        return withActiveBuffer(buffer -> {
            final int codePoint = value.codePointAt(0);
            final double cost = codePoint == ' ' ? ModSettings.gpuClearCost() : ModSettings.gpuFillCost();
            if (!consumeScreenEnergy(width * height, cost)) {
                return notEnoughEnergy();
            }
            buffer.fill(x, y, width, height, codePoint);
            return new Object[]{true};
        });
    }

    @Callback(direct = true, doc = "function([dst:number, x:number, y:number, width:number, height:number, src:number, fromX:number, fromY:number]):boolean -- Copies between video buffers and screens.")
    public Object[] bitblt(final Context context, final Arguments args) throws LimitReachedException {
        final int dstIndex = args.optInteger(0, SCREEN_INDEX);
        final TextBuffer dst = buffer(dstIndex);
        if (dst == null) {
            return dstIndex == SCREEN_INDEX ? noScreen() : invalidBufferIndex();
        }
        final int x = args.optInteger(1, 1) - 1;
        final int y = args.optInteger(2, 1) - 1;
        final int width = Math.max(0, args.optInteger(3, dst.getWidth()));
        final int height = Math.max(0, args.optInteger(4, dst.getHeight()));
        final int srcIndex = args.optInteger(5, activeBufferIndex);
        final TextBuffer src = buffer(srcIndex);
        if (src == null) {
            return srcIndex == SCREEN_INDEX ? noScreen() : invalidBufferIndex();
        }
        final int fromX = args.optInteger(6, 1) - 1;
        final int fromY = args.optInteger(7, 1) - 1;
        consumeBitbltCallBudget(context, dstIndex, src);
        if (!consumeScreenEnergy(dstIndex, width * height, ModSettings.gpuCopyCost() / 15D)) {
            return notEnoughEnergy();
        }
        dst.rawSetText(x, y, textSnapshot(src, fromX, fromY, width, height));
        dst.rawSetForeground(x, y, foregroundSnapshot(src, fromX, fromY, width, height));
        dst.rawSetBackground(x, y, backgroundSnapshot(src, fromX, fromY, width, height));
        return new Object[]{true};
    }

    @Override
    public void onConnect(final Node node) {
        if (screen == null && screenAddress != null && screenAddress.equals(node.address()) && node.host() instanceof TextBuffer buffer) {
            screen = buffer;
        }
    }

    @Override
    public void onDisconnect(final Node node) {
        if (node == node() || screenAddress != null && screenAddress.equals(node.address())) {
            screen = null;
        }
    }

    @Override
    public void onMessage(final Message message) {
        if (node() != null && message.source() != null && node().isNeighborOf(message.source()) &&
            ("computer.started".equals(message.name()) || "computer.stopped".equals(message.name()))) {
            activeBufferIndex = SCREEN_INDEX;
            videoBuffers.clear();
            if ("computer.stopped".equals(message.name()) && screen != null) {
                resetScreen(screen);
                screen.fill(0, 0, screen.getWidth(), screen.getHeight(), ' ');
            }
        }
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        screenAddress = nbt.contains(SCREEN_TAG) ? nbt.getString(SCREEN_TAG) : null;
        screen = null;
        activeBufferIndex = nbt.contains(ACTIVE_BUFFER_TAG) ? nbt.getInt(ACTIVE_BUFFER_TAG) : SCREEN_INDEX;
        videoBuffers.clear();
        if (nbt.contains(VIDEO_RAM_TAG)) {
            final ListTag pages = nbt.getCompound(VIDEO_RAM_TAG).getList(PAGES_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < pages.size(); i++) {
                final CompoundTag page = pages.getCompound(i);
                final int index = page.getInt(PAGE_INDEX_TAG);
                if (index > SCREEN_INDEX && page.contains(PAGE_DATA_TAG)) {
                    videoBuffers.put(index, VideoBuffer.load(page.getCompound(PAGE_DATA_TAG), maxDepth));
                }
            }
        }
        if (activeBufferIndex != SCREEN_INDEX && !videoBuffers.containsKey(activeBufferIndex)) {
            activeBufferIndex = SCREEN_INDEX;
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        if (screenAddress != null) {
            nbt.putString(SCREEN_TAG, screenAddress);
        }
        nbt.putInt(ACTIVE_BUFFER_TAG, activeBufferIndex);
        final CompoundTag videoRam = new CompoundTag();
        final ListTag pages = new ListTag();
        for (Map.Entry<Integer, VideoBuffer> entry : videoBuffers.entrySet()) {
            final CompoundTag page = new CompoundTag();
            page.putInt(PAGE_INDEX_TAG, entry.getKey());
            final CompoundTag data = new CompoundTag();
            entry.getValue().save(data);
            page.put(PAGE_DATA_TAG, data);
            pages.add(page);
        }
        videoRam.put(PAGES_TAG, pages);
        nbt.put(VIDEO_RAM_TAG, videoRam);
    }

    private void resetScreen(final TextBuffer buffer) {
        buffer.setResolution(Math.min(maxWidth, buffer.getMaximumWidth()), Math.min(maxHeight, buffer.getMaximumHeight()));
        buffer.setColorDepth(minDepth(maxDepth, buffer.getMaximumColorDepth()));
        buffer.setForegroundColor(0xFFFFFF);
        buffer.setBackgroundColor(0x000000);
    }

    private Object[] withActiveBuffer(final ScreenOperation operation) {
        return withBuffer(activeBufferIndex, operation);
    }

    private Object[] withBuffer(final int index, final ScreenOperation operation) {
        final TextBuffer buffer = buffer(index);
        if (buffer == null) {
            return index == SCREEN_INDEX ? noScreen() : invalidBufferIndex();
        }
        return operation.apply(buffer);
    }

    private TextBuffer buffer(final int index) {
        return index == SCREEN_INDEX ? screen : videoBuffers.get(index);
    }

    private static Object[] noScreen() {
        return new Object[]{null, "no screen"};
    }

    private static Object[] invalidBufferIndex() {
        return new Object[]{null, "invalid buffer index"};
    }

    private static Object[] notEnoughEnergy() {
        return new Object[]{null, "not enough energy"};
    }

    private void consumeScreenCallBudget(final Context context, final double cost) throws LimitReachedException {
        if (context != null && activeBufferIndex == SCREEN_INDEX) {
            context.consumeCallBudget(cost);
        }
    }

    private void consumeBitbltCallBudget(final Context context, final int dstIndex, final TextBuffer src) throws LimitReachedException {
        if (context != null && dstIndex == SCREEN_INDEX && src instanceof VideoBuffer buffer) {
            final double cost = buffer.isDirty()
                ? ModSettings.gpuBitbltCost() * Math.pow(2D, tier) * buffer.size() / ((double) maxWidth * (double) maxHeight)
                : 0.001D;
            context.consumeCallBudget(cost);
        }
    }

    private boolean consumeScreenEnergy(final double units, final double cost) {
        return consumeScreenEnergy(activeBufferIndex, units, cost);
    }

    private boolean consumeScreenEnergy(final int bufferIndex, final double units, final double cost) {
        if (bufferIndex != SCREEN_INDEX || units <= 0D || cost <= 0D) {
            return true;
        }
        return !(node() instanceof Connector connector) || connector.tryChangeBuffer(-units * cost);
    }

    private static Object[] previousColorResult(final TextBuffer buffer, final int previous, final boolean wasPalette) {
        return wasPalette
            ? new Object[]{buffer.getPaletteColor(previous), previous}
            : new Object[]{previous, null};
    }

    private static int[][] textSnapshot(final TextBuffer source, final int column, final int row, final int width, final int height) {
        final int[][] snapshot = new int[Math.max(0, height)][Math.max(0, width)];
        for (int y = 0; y < snapshot.length; y++) {
            for (int x = 0; x < snapshot[y].length; x++) {
                snapshot[y][x] = source.getCodePoint(column + x, row + y);
            }
        }
        return snapshot;
    }

    private static int[][] foregroundSnapshot(final TextBuffer source, final int column, final int row, final int width, final int height) {
        final int[][] snapshot = new int[Math.max(0, height)][Math.max(0, width)];
        for (int y = 0; y < snapshot.length; y++) {
            for (int x = 0; x < snapshot[y].length; x++) {
                snapshot[y][x] = source.getForegroundColor(column + x, row + y);
            }
        }
        return snapshot;
    }

    private static int[][] backgroundSnapshot(final TextBuffer source, final int column, final int row, final int width, final int height) {
        final int[][] snapshot = new int[Math.max(0, height)][Math.max(0, width)];
        for (int y = 0; y < snapshot.length; y++) {
            for (int x = 0; x < snapshot[y].length; x++) {
                snapshot[y][x] = source.getBackgroundColor(column + x, row + y);
            }
        }
        return snapshot;
    }

    private int nextBufferIndex() {
        int index = 1;
        while (videoBuffers.containsKey(index)) {
            index++;
        }
        return index;
    }

    private int usedVideoMemory() {
        return videoBuffers.values().stream().mapToInt(VideoBuffer::size).sum();
    }

    private int freeVideoMemory() {
        return totalVideoMemory - usedVideoMemory();
    }

    private static void checkSize(final int width, final int height, final int maxWidth, final int maxHeight, final String message) {
        if (width < 1 || height < 1 || width > maxWidth || height > maxHeight || (long) width * height > (long) maxWidth * maxHeight) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void checkPaletteIndex(final int index) {
        if (index < 0 || index >= 16) {
            throw new IllegalArgumentException("invalid palette index");
        }
    }

    private static TextBuffer.ColorDepth minDepth(final TextBuffer.ColorDepth first, final TextBuffer.ColorDepth second) {
        return first.ordinal() <= second.ordinal() ? first : second;
    }

    private static TextBuffer.ColorDepth depth(final int bits) {
        return switch (bits) {
            case 1 -> TextBuffer.ColorDepth.OneBit;
            case 4 -> TextBuffer.ColorDepth.FourBit;
            case 8 -> TextBuffer.ColorDepth.EightBit;
            default -> throw new IllegalArgumentException("unsupported depth");
        };
    }

    private static int bits(final TextBuffer.ColorDepth depth) {
        return switch (depth) {
            case OneBit -> 1;
            case FourBit -> 4;
            case EightBit -> 8;
        };
    }

    private static String clockInfo(final int tier) {
        return switch (tier) {
            case 0 -> "640/640/40/1280/320/640";
            case 1 -> "1280/1280/160/2560/640/1280";
            default -> "2560/2560/320/5120/1280/2560";
        };
    }

    @FunctionalInterface
    private interface ScreenOperation {
        Object[] apply(TextBuffer buffer);
    }

    private static final class VideoBuffer extends AbstractManagedEnvironment implements TextBuffer {
        private static final String WIDTH_TAG = "width";
        private static final String HEIGHT_TAG = "height";
        private static final String MAXIMUM_WIDTH_TAG = "maximumWidth";
        private static final String MAXIMUM_HEIGHT_TAG = "maximumHeight";
        private static final String VIEWPORT_WIDTH_TAG = "viewportWidth";
        private static final String VIEWPORT_HEIGHT_TAG = "viewportHeight";
        private static final String FOREGROUND_TAG = "foreground";
        private static final String BACKGROUND_TAG = "background";
        private static final String FOREGROUND_PALETTE_TAG = "foregroundPalette";
        private static final String BACKGROUND_PALETTE_TAG = "backgroundPalette";
        private static final String DEPTH_TAG = "depth";
        private static final String PALETTE_TAG = "palette";
        private static final String TEXT_TAG = "text";
        private static final String CELL_FOREGROUND_TAG = "cellForeground";
        private static final String CELL_BACKGROUND_TAG = "cellBackground";

        private final ColorDepth maximumDepth;
        private final int maximumWidth;
        private final int maximumHeight;
        private final int[] palette = new int[16];
        private int width;
        private int height;
        private int viewportWidth;
        private int viewportHeight;
        private int foregroundColor = 0xFFFFFF;
        private int backgroundColor = 0x000000;
        private boolean foregroundFromPalette;
        private boolean backgroundFromPalette;
        private ColorDepth colorDepth;
        private int[][] text;
        private int[][] foreground;
        private int[][] background;
        private boolean dirty = true;

        private VideoBuffer(final int width, final int height, final ColorDepth maximumDepth) {
            this.maximumWidth = Math.max(1, width);
            this.maximumHeight = Math.max(1, height);
            this.maximumDepth = maximumDepth;
            this.colorDepth = maximumDepth == null ? ColorDepth.OneBit : maximumDepth;
            resize(this.maximumWidth, this.maximumHeight);
        }

        private static VideoBuffer load(final CompoundTag nbt, final ColorDepth maximumDepth) {
            final int maximumWidth = nbt.contains(MAXIMUM_WIDTH_TAG) ? nbt.getInt(MAXIMUM_WIDTH_TAG) : nbt.getInt(WIDTH_TAG);
            final int maximumHeight = nbt.contains(MAXIMUM_HEIGHT_TAG) ? nbt.getInt(MAXIMUM_HEIGHT_TAG) : nbt.getInt(HEIGHT_TAG);
            final VideoBuffer buffer = new VideoBuffer(maximumWidth, maximumHeight, maximumDepth);
            buffer.load(nbt);
            return buffer;
        }

        private int size() {
            return width * height;
        }

        private boolean isDirty() {
            return dirty;
        }

        @Override
        public void setEnergyCostPerTick(final double value) {
        }

        @Override
        public double getEnergyCostPerTick() {
            return 0;
        }

        @Override
        public void setPowerState(final boolean value) {
        }

        @Override
        public boolean getPowerState() {
            return true;
        }

        @Override
        public void setMaximumResolution(final int width, final int height) {
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
        }

        @Override
        public double getAspectRatio() {
            return 1;
        }

        @Override
        public boolean setResolution(final int width, final int height) {
            if (width < 1 || height < 1 || width > maximumWidth || height > maximumHeight) {
                return false;
            }
            resize(width, height);
            dirty = true;
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
            dirty = true;
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
        }

        @Override
        public ColorDepth getMaximumColorDepth() {
            return maximumDepth;
        }

        @Override
        public boolean setColorDepth(final ColorDepth depth) {
            if (depth == null || depth.ordinal() > maximumDepth.ordinal()) {
                return false;
            }
            colorDepth = depth;
            dirty = true;
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
                dirty = true;
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
            dirty = true;
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
            dirty = true;
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
            final int[][] textSnapshot = snapshot(text, column, row, width, height);
            final int[][] foregroundSnapshot = snapshot(foreground, column, row, width, height);
            final int[][] backgroundSnapshot = snapshot(background, column, row, width, height);
            rawSetText(column + horizontalTranslation, row + verticalTranslation, textSnapshot);
            rawSetForeground(column + horizontalTranslation, row + verticalTranslation, foregroundSnapshot);
            rawSetBackground(column + horizontalTranslation, row + verticalTranslation, backgroundSnapshot);
            dirty = true;
        }

        @Override
        public void fill(final int column, final int row, final int width, final int height, final char value) {
            fill(column, row, width, height, (int) value);
        }

        @Override
        public void fill(final int column, final int row, final int width, final int height, final int value) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    put(column + x, row + y, value, foregroundColor, backgroundColor);
                }
            }
            dirty = true;
        }

        @Override
        public void set(final int column, final int row, final String value, final boolean vertical) {
            if (value == null) {
                return;
            }
            final int[] codePoints = value.codePoints().toArray();
            for (int index = 0; index < codePoints.length; index++) {
                final int x = vertical ? column : column + index;
                final int y = vertical ? row + index : row;
                put(x, y, codePoints[index], foregroundColor, backgroundColor);
            }
            dirty = true;
        }

        @Override
        public char get(final int column, final int row) {
            return (char) getCodePoint(column, row);
        }

        @Override
        public int getCodePoint(final int column, final int row) {
            return isInside(column, row) ? text[row][column] : ' ';
        }

        @Override
        public int getForegroundColor(final int column, final int row) {
            return isInside(column, row) ? foreground[row][column] : foregroundColor;
        }

        @Override
        public boolean isForegroundFromPalette(final int column, final int row) {
            return foregroundFromPalette;
        }

        @Override
        public int getBackgroundColor(final int column, final int row) {
            return isInside(column, row) ? background[row][column] : backgroundColor;
        }

        @Override
        public boolean isBackgroundFromPalette(final int column, final int row) {
            return backgroundFromPalette;
        }

        @Override
        public void rawSetText(final int column, final int row, final char[][] text) {
            if (text == null) {
                return;
            }
            for (int y = 0; y < text.length; y++) {
                for (int x = 0; x < text[y].length; x++) {
                    putText(column + x, row + y, text[y][x]);
                }
            }
            dirty = true;
        }

        @Override
        public void rawSetText(final int column, final int row, final int[][] text) {
            if (text == null) {
                return;
            }
            for (int y = 0; y < text.length; y++) {
                for (int x = 0; x < text[y].length; x++) {
                    putText(column + x, row + y, text[y][x]);
                }
            }
            dirty = true;
        }

        @Override
        public void rawSetForeground(final int column, final int row, final int[][] color) {
            rawSetColor(foreground, column, row, color);
            dirty = true;
        }

        @Override
        public void rawSetBackground(final int column, final int row, final int[][] color) {
            rawSetColor(background, column, row, color);
            dirty = true;
        }

        @Override
        public boolean renderText() {
            return false;
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
        }

        @Override
        public boolean isRenderingEnabled() {
            return false;
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
        public void load(final CompoundTag nbt) {
            resize(
                nbt.contains(WIDTH_TAG) ? Math.min(maximumWidth, Math.max(1, nbt.getInt(WIDTH_TAG))) : maximumWidth,
                nbt.contains(HEIGHT_TAG) ? Math.min(maximumHeight, Math.max(1, nbt.getInt(HEIGHT_TAG))) : maximumHeight
            );
            viewportWidth = nbt.contains(VIEWPORT_WIDTH_TAG) ? Math.min(width, Math.max(1, nbt.getInt(VIEWPORT_WIDTH_TAG))) : width;
            viewportHeight = nbt.contains(VIEWPORT_HEIGHT_TAG) ? Math.min(height, Math.max(1, nbt.getInt(VIEWPORT_HEIGHT_TAG))) : height;
            foregroundColor = nbt.getInt(FOREGROUND_TAG);
            backgroundColor = nbt.getInt(BACKGROUND_TAG);
            foregroundFromPalette = nbt.getBoolean(FOREGROUND_PALETTE_TAG);
            backgroundFromPalette = nbt.getBoolean(BACKGROUND_PALETTE_TAG);
            if (nbt.contains(DEPTH_TAG)) {
                final int depth = nbt.getInt(DEPTH_TAG);
                if (depth >= 0 && depth < ColorDepth.values().length && depth <= maximumDepth.ordinal()) {
                    colorDepth = ColorDepth.values()[depth];
                }
            }
            final int[] loadedPalette = nbt.getIntArray(PALETTE_TAG);
            System.arraycopy(loadedPalette, 0, palette, 0, Math.min(loadedPalette.length, palette.length));
            loadRows(nbt.getList(TEXT_TAG, Tag.TAG_INT_ARRAY), text);
            loadRows(nbt.getList(CELL_FOREGROUND_TAG, Tag.TAG_INT_ARRAY), foreground);
            loadRows(nbt.getList(CELL_BACKGROUND_TAG, Tag.TAG_INT_ARRAY), background);
            dirty = true;
        }

        @Override
        public void save(final CompoundTag nbt) {
            nbt.putInt(WIDTH_TAG, width);
            nbt.putInt(HEIGHT_TAG, height);
            nbt.putInt(MAXIMUM_WIDTH_TAG, maximumWidth);
            nbt.putInt(MAXIMUM_HEIGHT_TAG, maximumHeight);
            nbt.putInt(VIEWPORT_WIDTH_TAG, viewportWidth);
            nbt.putInt(VIEWPORT_HEIGHT_TAG, viewportHeight);
            nbt.putInt(FOREGROUND_TAG, foregroundColor);
            nbt.putInt(BACKGROUND_TAG, backgroundColor);
            nbt.putBoolean(FOREGROUND_PALETTE_TAG, foregroundFromPalette);
            nbt.putBoolean(BACKGROUND_PALETTE_TAG, backgroundFromPalette);
            nbt.putInt(DEPTH_TAG, colorDepth.ordinal());
            nbt.putIntArray(PALETTE_TAG, palette);
            nbt.put(TEXT_TAG, saveRows(text));
            nbt.put(CELL_FOREGROUND_TAG, saveRows(foreground));
            nbt.put(CELL_BACKGROUND_TAG, saveRows(background));
            dirty = false;
        }

        private void resize(final int width, final int height) {
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            viewportWidth = this.width;
            viewportHeight = this.height;
            text = new int[this.height][this.width];
            foreground = new int[this.height][this.width];
            background = new int[this.height][this.width];
            fill(0, 0, this.width, this.height, ' ');
        }

        private void put(final int column, final int row, final int value, final int foregroundColor, final int backgroundColor) {
            if (isInside(column, row)) {
                text[row][column] = value;
                foreground[row][column] = foregroundColor;
                background[row][column] = backgroundColor;
            }
        }

        private void putText(final int column, final int row, final int value) {
            if (isInside(column, row)) {
                text[row][column] = value;
            }
        }

        private void rawSetColor(final int[][] target, final int column, final int row, final int[][] color) {
            if (color == null) {
                return;
            }
            for (int y = 0; y < color.length; y++) {
                for (int x = 0; x < color[y].length; x++) {
                    final int targetX = column + x;
                    final int targetY = row + y;
                    if (isInside(targetX, targetY)) {
                        target[targetY][targetX] = color[y][x];
                    }
                }
            }
        }

        private int[][] snapshot(final int[][] source, final int column, final int row, final int width, final int height) {
            final int[][] snapshot = new int[Math.max(0, height)][Math.max(0, width)];
            for (int y = 0; y < snapshot.length; y++) {
                for (int x = 0; x < snapshot[y].length; x++) {
                    final int sourceX = column + x;
                    final int sourceY = row + y;
                    snapshot[y][x] = isInside(sourceX, sourceY) ? source[sourceY][sourceX] : 0;
                }
            }
            return snapshot;
        }

        private ListTag saveRows(final int[][] source) {
            final ListTag rows = new ListTag();
            for (int y = 0; y < height; y++) {
                rows.add(new IntArrayTag(source[y]));
            }
            return rows;
        }

        private void loadRows(final ListTag rows, final int[][] target) {
            for (int y = 0; y < Math.min(rows.size(), height); y++) {
                final int[] row = rows.getIntArray(y);
                for (int x = 0; x < Math.min(row.length, width); x++) {
                    target[y][x] = row[x];
                }
            }
        }

        private boolean isInside(final int column, final int row) {
            return column >= 0 && row >= 0 && column < width && row < height;
        }
    }
}
