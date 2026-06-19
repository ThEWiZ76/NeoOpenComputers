package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GraphicsCardEnvironmentTest {
    @Test
    void exposesGpuCallbacks() throws NoSuchMethodException {
        assertCallback("bind");
        assertCallback("getScreen");
        assertCallback("getResolution");
        assertCallback("setResolution");
        assertCallback("maxResolution");
        assertCallback("getViewport");
        assertCallback("setViewport");
        assertCallback("getBackground");
        assertCallback("setBackground");
        assertCallback("getForeground");
        assertCallback("setForeground");
        assertCallback("getPaletteColor");
        assertCallback("setPaletteColor");
        assertCallback("getDepth");
        assertCallback("setDepth");
        assertCallback("maxDepth");
        assertCallback("get");
        assertCallback("set");
        assertCallback("copy");
        assertCallback("fill");
    }

    @Test
    void createsGpuComponentConnectorNode() {
        OpenComputersApi.initialize();

        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);

        assertNotNull(gpu.node());
        assertInstanceOf(ComponentConnector.class, gpu.node());
    }

    @Test
    void exposesDeviceInfoMetadata() {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);

        DeviceInfo info = assertInstanceOf(DeviceInfo.class, gpu);
        Map<String, String> metadata = info.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Display, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Graphics controller", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MPG1000 GTZ", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("800", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
        assertEquals("1", metadata.get(DeviceInfo.DeviceAttribute.Width));
    }

    @Test
    void reportsNoScreenBeforeBind() {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);

        assertArrayEquals(new Object[]{null, "no screen"}, gpu.getScreen(null, new TestArguments()));
    }

    @Test
    void bindsConnectedScreenByAddress() {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        FakeTextBuffer screen = new FakeTextBuffer();
        Network.joinNewNetwork(gpu.node());
        gpu.node().connect(screen.node());

        assertArrayEquals(new Object[]{true}, gpu.bind(null, new TestArguments(screen.node().address(), true)));
        assertArrayEquals(new Object[]{screen.node().address()}, gpu.getScreen(null, new TestArguments()));
        assertArrayEquals(new Object[]{50, 16}, gpu.getResolution(null, new TestArguments()));
    }

    @Test
    void delegatesPaletteColorCallbacksToScreen() {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        FakeTextBuffer screen = new FakeTextBuffer();
        Network.joinNewNetwork(gpu.node());
        gpu.node().connect(screen.node());
        gpu.bind(null, new TestArguments(screen.node().address(), true));
        screen.setPaletteColor(2, 0x112233);

        assertArrayEquals(new Object[]{0x112233}, gpu.getPaletteColor(null, new TestArguments(2)));
        assertArrayEquals(new Object[]{0x112233}, gpu.setPaletteColor(null, new TestArguments(2, 0x445566)));
        assertArrayEquals(new Object[]{0x445566}, gpu.getPaletteColor(null, new TestArguments(2)));
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = GraphicsCardEnvironment.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }

    private static final class FakeTextBuffer extends AbstractManagedEnvironment implements TextBuffer {
        private int width = 40;
        private int height = 16;
        private int viewportWidth = 40;
        private int viewportHeight = 16;
        private int foreground = 0xFFFFFF;
        private int background = 0x000000;
        private ColorDepth depth = ColorDepth.OneBit;
        private final int[] palette = new int[16];

        private FakeTextBuffer() {
            setNode(ScreenEnvironment.createNode(this));
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
            return 80;
        }

        @Override
        public int getMaximumHeight() {
            return 25;
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
            this.width = width;
            this.height = height;
            viewportWidth = Math.min(viewportWidth, width);
            viewportHeight = Math.min(viewportHeight, height);
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
            viewportWidth = width;
            viewportHeight = height;
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
            return ColorDepth.OneBit;
        }

        @Override
        public boolean setColorDepth(final ColorDepth depth) {
            this.depth = depth;
            return true;
        }

        @Override
        public ColorDepth getColorDepth() {
            return depth;
        }

        @Override
        public void setPaletteColor(final int index, final int color) {
            if (index >= 0 && index < palette.length) {
                palette[index] = color;
            }
        }

        @Override
        public int getPaletteColor(final int index) {
            return index >= 0 && index < palette.length ? palette[index] : 0;
        }

        @Override
        public void setForegroundColor(final int color) {
            foreground = color;
        }

        @Override
        public void setForegroundColor(final int color, final boolean isFromPalette) {
            foreground = color;
        }

        @Override
        public int getForegroundColor() {
            return foreground;
        }

        @Override
        public boolean isForegroundFromPalette() {
            return false;
        }

        @Override
        public void setBackgroundColor(final int color) {
            background = color;
        }

        @Override
        public void setBackgroundColor(final int color, final boolean isFromPalette) {
            background = color;
        }

        @Override
        public int getBackgroundColor() {
            return background;
        }

        @Override
        public boolean isBackgroundFromPalette() {
            return false;
        }

        @Override
        public void copy(final int column, final int row, final int width, final int height, final int horizontalTranslation, final int verticalTranslation) {
        }

        @Override
        public void fill(final int column, final int row, final int width, final int height, final char value) {
        }

        @Override
        public void fill(final int column, final int row, final int width, final int height, final int value) {
        }

        @Override
        public void set(final int column, final int row, final String value, final boolean vertical) {
        }

        @Override
        public char get(final int column, final int row) {
            return ' ';
        }

        @Override
        public int getCodePoint(final int column, final int row) {
            return ' ';
        }

        @Override
        public int getForegroundColor(final int column, final int row) {
            return foreground;
        }

        @Override
        public boolean isForegroundFromPalette(final int column, final int row) {
            return false;
        }

        @Override
        public int getBackgroundColor(final int column, final int row) {
            return background;
        }

        @Override
        public boolean isBackgroundFromPalette(final int column, final int row) {
            return false;
        }

        @Override
        public void rawSetText(final int column, final int row, final char[][] text) {
        }

        @Override
        public void rawSetText(final int column, final int row, final int[][] text) {
        }

        @Override
        public void rawSetForeground(final int column, final int row, final int[][] color) {
        }

        @Override
        public void rawSetBackground(final int column, final int row, final int[][] color) {
        }

        @Override
        public boolean renderText() {
            return true;
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
            return true;
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
    }

    private record TestArguments(Object... values) implements Arguments {
        @Override
        public int count() {
            return values.length;
        }

        @Override
        public Object checkAny(final int index) {
            return values[index];
        }

        @Override
        public boolean checkBoolean(final int index) {
            return (Boolean) values[index];
        }

        @Override
        public int checkInteger(final int index) {
            return ((Number) values[index]).intValue();
        }

        @Override
        public long checkLong(final int index) {
            return ((Number) values[index]).longValue();
        }

        @Override
        public double checkDouble(final int index) {
            return ((Number) values[index]).doubleValue();
        }

        @Override
        public String checkString(final int index) {
            return (String) values[index];
        }

        @Override
        public byte[] checkByteArray(final int index) {
            return (byte[]) values[index];
        }

        @Override
        public Map checkTable(final int index) {
            return (Map) values[index];
        }

        @Override
        public ItemStack checkItemStack(final int index) {
            return (ItemStack) values[index];
        }

        @Override
        public Object optAny(final int index, final Object def) {
            return index < values.length ? values[index] : def;
        }

        @Override
        public boolean optBoolean(final int index, final boolean def) {
            return index < values.length ? checkBoolean(index) : def;
        }

        @Override
        public int optInteger(final int index, final int def) {
            return index < values.length ? checkInteger(index) : def;
        }

        @Override
        public long optLong(final int index, final long def) {
            return index < values.length ? checkLong(index) : def;
        }

        @Override
        public double optDouble(final int index, final double def) {
            return index < values.length ? checkDouble(index) : def;
        }

        @Override
        public String optString(final int index, final String def) {
            return index < values.length ? checkString(index) : def;
        }

        @Override
        public byte[] optByteArray(final int index, final byte[] def) {
            return index < values.length ? checkByteArray(index) : def;
        }

        @Override
        public Map optTable(final int index, final Map def) {
            return index < values.length ? checkTable(index) : def;
        }

        @Override
        public ItemStack optItemStack(final int index, final ItemStack def) {
            return index < values.length ? checkItemStack(index) : def;
        }

        @Override
        public boolean isBoolean(final int index) {
            return values[index] instanceof Boolean;
        }

        @Override
        public boolean isInteger(final int index) {
            return values[index] instanceof Integer;
        }

        @Override
        public boolean isLong(final int index) {
            return values[index] instanceof Long;
        }

        @Override
        public boolean isDouble(final int index) {
            return values[index] instanceof Double;
        }

        @Override
        public boolean isString(final int index) {
            return values[index] instanceof String;
        }

        @Override
        public boolean isByteArray(final int index) {
            return values[index] instanceof byte[];
        }

        @Override
        public boolean isTable(final int index) {
            return values[index] instanceof Map;
        }

        @Override
        public boolean isItemStack(final int index) {
            return values[index] instanceof ItemStack;
        }

        @Override
        public Object[] toArray() {
            return values;
        }

        @Override
        public Iterator<Object> iterator() {
            return java.util.Arrays.asList(values).iterator();
        }
    }
}
