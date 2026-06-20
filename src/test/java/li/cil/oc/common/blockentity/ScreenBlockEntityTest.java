package li.cil.oc.common.blockentity;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.component.KeyboardEnvironment;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenBlockEntityTest {
    @Test
    void exposesScreenPowerCallbacks() throws NoSuchMethodException {
        assertCallback("isOn");
        assertCallback("turnOn");
        assertCallback("turnOff");
        assertCallback("getAspectRatio");
        assertCallback("getKeyboards");
        assertCallback("isTouchModeInverted");
        assertCallback("setTouchModeInverted");
        assertCallback("isPrecise");
        assertCallback("setPrecise");
    }

    @Test
    void togglesPreciseMode() throws Exception {
        ScreenBlockEntity screen = allocateScreen();

        assertArrayEquals(new Object[]{false}, screen.isPrecise(null, new TestArguments()));
        assertArrayEquals(new Object[]{false}, screen.setPrecise(null, new TestArguments(true)));
        assertArrayEquals(new Object[]{true}, screen.isPrecise(null, new TestArguments()));
        assertArrayEquals(new Object[]{true}, screen.setPrecise(null, new TestArguments(false)));
        assertArrayEquals(new Object[]{false}, screen.isPrecise(null, new TestArguments()));
    }

    @Test
    void togglesTouchModeInversion() throws Exception {
        ScreenBlockEntity screen = allocateScreen();

        assertArrayEquals(new Object[]{false}, screen.isTouchModeInverted(null, new TestArguments()));
        assertArrayEquals(new Object[]{false}, screen.setTouchModeInverted(null, new TestArguments(true)));
        assertArrayEquals(new Object[]{true}, screen.isTouchModeInverted(null, new TestArguments()));
        assertArrayEquals(new Object[]{true}, screen.setTouchModeInverted(null, new TestArguments(false)));
        assertArrayEquals(new Object[]{false}, screen.isTouchModeInverted(null, new TestArguments()));
    }

    @Test
    void persistsPreciseMode() throws Exception {
        OpenComputersApi.initialize();
        ScreenBlockEntity saved = allocateScreen();
        initializeBuffer(saved);
        saved.setPrecise(null, new TestArguments(true));
        CompoundTag tag = new CompoundTag();

        saved.save(tag);
        ScreenBlockEntity loaded = allocateScreen();
        initializeBuffer(loaded);
        loaded.load(tag);

        assertArrayEquals(new Object[]{true}, loaded.isPrecise(null, new TestArguments()));
    }

    @Test
    void persistsTouchModeInversion() throws Exception {
        OpenComputersApi.initialize();
        ScreenBlockEntity saved = allocateScreen();
        initializeBuffer(saved);
        saved.setTouchModeInverted(null, new TestArguments(true));
        CompoundTag tag = new CompoundTag();

        saved.save(tag);
        ScreenBlockEntity loaded = allocateScreen();
        initializeBuffer(loaded);
        loaded.load(tag);

        assertArrayEquals(new Object[]{true}, loaded.isTouchModeInverted(null, new TestArguments()));
    }

    @Test
    void listsNeighborKeyboardAddresses() throws Exception {
        OpenComputersApi.initialize();
        ScreenBlockEntity screen = allocateScreen();
        Node screenNode = screen.node();
        li.cil.oc.api.Network.joinNewNetwork(screenNode);
        TestKeyboard keyboard = new TestKeyboard();
        keyboard.node = KeyboardEnvironment.createNode(keyboard);

        screenNode.connect(keyboard.node);
        Object[] result = screen.getKeyboards(null, new TestArguments());

        assertEquals(1, result.length);
        assertArrayEquals(new String[]{keyboard.node.address()}, (String[]) result[0]);
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = ScreenBlockEntity.class.getMethod(methodName, Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }

    private static ScreenBlockEntity allocateScreen() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (ScreenBlockEntity) ((Unsafe) field.get(null)).allocateInstance(ScreenBlockEntity.class);
    }

    private static void initializeBuffer(final ScreenBlockEntity screen) throws Exception {
        Field field = ScreenBlockEntity.class.getDeclaredField("buffer");
        field.setAccessible(true);
        field.set(screen, new TextBufferState(1, 1));
    }

    private static final class TestKeyboard implements li.cil.oc.api.internal.Keyboard {
        private Node node;

        @Override public void setUsableOverride(final UsabilityChecker callback) {}
        @Override public Node node() { return node; }
        @Override public void onConnect(final Node node) {}
        @Override public void onDisconnect(final Node node) {}
        @Override public void onMessage(final Message message) {}
        @Override public void load(final CompoundTag nbt) {}
        @Override public void save(final CompoundTag nbt) {}
    }

    private record TestArguments(Object... values) implements Arguments {
        @Override public int count() { return values.length; }
        @Override public Object checkAny(final int index) { return values[index]; }
        @Override public boolean checkBoolean(final int index) { return (Boolean) values[index]; }
        @Override public int checkInteger(final int index) { return ((Number) values[index]).intValue(); }
        @Override public long checkLong(final int index) { return ((Number) values[index]).longValue(); }
        @Override public double checkDouble(final int index) { return ((Number) values[index]).doubleValue(); }
        @Override public String checkString(final int index) { return (String) values[index]; }
        @Override public byte[] checkByteArray(final int index) { return (byte[]) values[index]; }
        @Override public Map checkTable(final int index) { return (Map) values[index]; }
        @Override public ItemStack checkItemStack(final int index) { return (ItemStack) values[index]; }
        @Override public Object optAny(final int index, final Object def) { return index < values.length ? values[index] : def; }
        @Override public boolean optBoolean(final int index, final boolean def) { return index < values.length ? checkBoolean(index) : def; }
        @Override public int optInteger(final int index, final int def) { return index < values.length ? checkInteger(index) : def; }
        @Override public long optLong(final int index, final long def) { return index < values.length ? checkLong(index) : def; }
        @Override public double optDouble(final int index, final double def) { return index < values.length ? checkDouble(index) : def; }
        @Override public String optString(final int index, final String def) { return index < values.length ? checkString(index) : def; }
        @Override public byte[] optByteArray(final int index, final byte[] def) { return index < values.length ? checkByteArray(index) : def; }
        @Override public Map optTable(final int index, final Map def) { return index < values.length ? checkTable(index) : def; }
        @Override public ItemStack optItemStack(final int index, final ItemStack def) { return index < values.length ? checkItemStack(index) : def; }
        @Override public boolean isBoolean(final int index) { return values[index] instanceof Boolean; }
        @Override public boolean isInteger(final int index) { return values[index] instanceof Integer; }
        @Override public boolean isLong(final int index) { return values[index] instanceof Long; }
        @Override public boolean isDouble(final int index) { return values[index] instanceof Double; }
        @Override public boolean isString(final int index) { return values[index] instanceof String; }
        @Override public boolean isByteArray(final int index) { return values[index] instanceof byte[]; }
        @Override public boolean isTable(final int index) { return values[index] instanceof Map; }
        @Override public boolean isItemStack(final int index) { return values[index] instanceof ItemStack; }
        @Override public Object[] toArray() { return values; }
        @Override public Iterator<Object> iterator() { return java.util.Arrays.asList(values).iterator(); }
    }
}
