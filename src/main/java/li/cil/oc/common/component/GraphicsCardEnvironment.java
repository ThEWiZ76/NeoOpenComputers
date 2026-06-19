package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.nbt.CompoundTag;

public class GraphicsCardEnvironment extends AbstractManagedEnvironment {
    private static final String COMPONENT_NAME = "gpu";
    private static final String SCREEN_TAG = "screen";

    private final int maxWidth;
    private final int maxHeight;
    private final TextBuffer.ColorDepth maxDepth;
    private String screenAddress;
    private TextBuffer screen;

    public GraphicsCardEnvironment(final int tier) {
        final int clampedTier = Math.max(0, Math.min(2, tier));
        maxWidth = switch (clampedTier) {
            case 0 -> 50;
            case 1 -> 80;
            default -> 160;
        };
        maxHeight = switch (clampedTier) {
            case 0 -> 16;
            case 1 -> 25;
            default -> 50;
        };
        maxDepth = switch (clampedTier) {
            case 0 -> TextBuffer.ColorDepth.OneBit;
            case 1 -> TextBuffer.ColorDepth.FourBit;
            default -> TextBuffer.ColorDepth.EightBit;
        };

        final var builder = Network.newNode(this, Visibility.Neighbors);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Neighbors).withConnector().create());
        }
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
        return withScreen(buffer -> new Object[]{buffer.getWidth(), buffer.getHeight()});
    }

    @Callback(doc = "function(width:number, height:number):boolean -- Sets the current screen resolution.")
    public Object[] setResolution(final Context context, final Arguments args) {
        final int width = args.checkInteger(0);
        final int height = args.checkInteger(1);
        checkSize(width, height, maxWidth, maxHeight, "unsupported resolution");
        return withScreen(buffer -> new Object[]{buffer.setResolution(width, height)});
    }

    @Callback(direct = true, doc = "function():number, number -- Returns the maximum screen resolution.")
    public Object[] maxResolution(final Context context, final Arguments args) {
        return withScreen(buffer -> new Object[]{
            Math.min(maxWidth, buffer.getMaximumWidth()),
            Math.min(maxHeight, buffer.getMaximumHeight())
        });
    }

    @Callback(direct = true, doc = "function():number, number -- Returns the current viewport resolution.")
    public Object[] getViewport(final Context context, final Arguments args) {
        return withScreen(buffer -> new Object[]{buffer.getViewportWidth(), buffer.getViewportHeight()});
    }

    @Callback(doc = "function(width:number, height:number):boolean -- Sets the current viewport resolution.")
    public Object[] setViewport(final Context context, final Arguments args) {
        final int width = args.checkInteger(0);
        final int height = args.checkInteger(1);
        checkSize(width, height, maxWidth, maxHeight, "unsupported viewport size");
        return withScreen(buffer -> {
            if (width > buffer.getWidth() || height > buffer.getHeight()) {
                throw new IllegalArgumentException("unsupported viewport size");
            }
            return new Object[]{buffer.setViewport(width, height)};
        });
    }

    @Callback(direct = true, doc = "function():number, boolean -- Returns the background color.")
    public Object[] getBackground(final Context context, final Arguments args) {
        return withScreen(buffer -> new Object[]{buffer.getBackgroundColor(), buffer.isBackgroundFromPalette()});
    }

    @Callback(direct = true, doc = "function(value:number[, palette:boolean]):number, boolean -- Sets the background color.")
    public Object[] setBackground(final Context context, final Arguments args) {
        final int color = args.checkInteger(0);
        final boolean palette = args.optBoolean(1, false);
        return withScreen(buffer -> {
            final int previous = buffer.getBackgroundColor();
            final boolean wasPalette = buffer.isBackgroundFromPalette();
            buffer.setBackgroundColor(color, palette);
            return new Object[]{previous, wasPalette};
        });
    }

    @Callback(direct = true, doc = "function():number, boolean -- Returns the foreground color.")
    public Object[] getForeground(final Context context, final Arguments args) {
        return withScreen(buffer -> new Object[]{buffer.getForegroundColor(), buffer.isForegroundFromPalette()});
    }

    @Callback(direct = true, doc = "function(value:number[, palette:boolean]):number, boolean -- Sets the foreground color.")
    public Object[] setForeground(final Context context, final Arguments args) {
        final int color = args.checkInteger(0);
        final boolean palette = args.optBoolean(1, false);
        return withScreen(buffer -> {
            final int previous = buffer.getForegroundColor();
            final boolean wasPalette = buffer.isForegroundFromPalette();
            buffer.setForegroundColor(color, palette);
            return new Object[]{previous, wasPalette};
        });
    }

    @Callback(direct = true, doc = "function():number -- Returns the current color depth.")
    public Object[] getDepth(final Context context, final Arguments args) {
        return withScreen(buffer -> new Object[]{bits(buffer.getColorDepth())});
    }

    @Callback(doc = "function(depth:number):number -- Sets the current color depth.")
    public Object[] setDepth(final Context context, final Arguments args) {
        final TextBuffer.ColorDepth depth = depth(args.checkInteger(0));
        if (depth.ordinal() > maxDepth.ordinal()) {
            throw new IllegalArgumentException("unsupported depth");
        }
        return withScreen(buffer -> {
            final int previous = bits(buffer.getColorDepth());
            if (!buffer.setColorDepth(depth)) {
                throw new IllegalArgumentException("unsupported depth");
            }
            return new Object[]{previous};
        });
    }

    @Callback(direct = true, doc = "function():number -- Returns the maximum supported color depth.")
    public Object[] maxDepth(final Context context, final Arguments args) {
        return withScreen(buffer -> new Object[]{bits(minDepth(maxDepth, buffer.getMaximumColorDepth()))});
    }

    @Callback(direct = true, doc = "function(x:number, y:number):string, number, number, boolean, boolean -- Gets a screen cell.")
    public Object[] get(final Context context, final Arguments args) {
        final int x = args.checkInteger(0) - 1;
        final int y = args.checkInteger(1) - 1;
        return withScreen(buffer -> new Object[]{
            new String(Character.toChars(buffer.getCodePoint(x, y))),
            buffer.getForegroundColor(x, y),
            buffer.getBackgroundColor(x, y),
            buffer.isForegroundFromPalette(x, y),
            buffer.isBackgroundFromPalette(x, y)
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, value:string[, vertical:boolean]):boolean -- Writes text to the screen.")
    public Object[] set(final Context context, final Arguments args) {
        final int x = args.checkInteger(0) - 1;
        final int y = args.checkInteger(1) - 1;
        final String value = args.checkString(2);
        final boolean vertical = args.optBoolean(3, false);
        return withScreen(buffer -> {
            buffer.set(x, y, value, vertical);
            return new Object[]{true};
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, width:number, height:number, tx:number, ty:number):boolean -- Copies screen text.")
    public Object[] copy(final Context context, final Arguments args) {
        final int x = args.checkInteger(0) - 1;
        final int y = args.checkInteger(1) - 1;
        final int width = Math.max(0, args.checkInteger(2));
        final int height = Math.max(0, args.checkInteger(3));
        final int tx = args.checkInteger(4);
        final int ty = args.checkInteger(5);
        return withScreen(buffer -> {
            buffer.copy(x, y, width, height, tx, ty);
            return new Object[]{true};
        });
    }

    @Callback(direct = true, doc = "function(x:number, y:number, width:number, height:number, char:string):boolean -- Fills screen text.")
    public Object[] fill(final Context context, final Arguments args) {
        final int x = args.checkInteger(0) - 1;
        final int y = args.checkInteger(1) - 1;
        final int width = Math.max(0, args.checkInteger(2));
        final int height = Math.max(0, args.checkInteger(3));
        final String value = args.checkString(4);
        if (value.codePointCount(0, value.length()) != 1) {
            throw new IllegalArgumentException("invalid fill value");
        }
        return withScreen(buffer -> {
            buffer.fill(x, y, width, height, value.codePointAt(0));
            return new Object[]{true};
        });
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
            screen = null;
        }
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        screenAddress = nbt.contains(SCREEN_TAG) ? nbt.getString(SCREEN_TAG) : null;
        screen = null;
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        if (screenAddress != null) {
            nbt.putString(SCREEN_TAG, screenAddress);
        }
    }

    private void resetScreen(final TextBuffer buffer) {
        buffer.setResolution(Math.min(maxWidth, buffer.getMaximumWidth()), Math.min(maxHeight, buffer.getMaximumHeight()));
        buffer.setColorDepth(minDepth(maxDepth, buffer.getMaximumColorDepth()));
        buffer.setForegroundColor(0xFFFFFF);
        buffer.setBackgroundColor(0x000000);
    }

    private Object[] withScreen(final ScreenOperation operation) {
        if (screen == null) {
            return noScreen();
        }
        return operation.apply(screen);
    }

    private static Object[] noScreen() {
        return new Object[]{null, "no screen"};
    }

    private static void checkSize(final int width, final int height, final int maxWidth, final int maxHeight, final String message) {
        if (width < 1 || height < 1 || width > maxWidth || height > maxHeight || (long) width * height > (long) maxWidth * maxHeight) {
            throw new IllegalArgumentException(message);
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

    @FunctionalInterface
    private interface ScreenOperation {
        Object[] apply(TextBuffer buffer);
    }
}
