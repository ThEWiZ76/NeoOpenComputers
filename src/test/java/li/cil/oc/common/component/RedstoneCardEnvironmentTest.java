package li.cil.oc.common.component;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RedstoneCardEnvironmentTest {
    @Test
    void exposesVanillaRedstoneCallbacks() throws NoSuchMethodException {
        assertCallback("getInput");
        assertCallback("getOutput");
        assertCallback("setOutput");
        assertCallback("getComparatorInput");
    }

    @Test
    void getsAllSidesAndSetsTableOutputs() {
        OpenComputersApi.initialize();
        TestRedstoneHost host = new TestRedstoneHost();
        RedstoneCardEnvironment card = new RedstoneCardEnvironment(host);
        RecordingContext context = new RecordingContext(card.node());
        host.inputs[Direction.NORTH.get3DDataValue()] = 7;

        assertEquals(Map.of(0, 0, 1, 0, 2, 7, 3, 0, 4, 0, 5, 0), card.getInput(null, new TestArguments())[0]);
        assertEquals(Map.of(0, 0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 0), card.getOutput(null, new TestArguments())[0]);

        assertArrayEquals(new Object[]{0}, card.setOutput(context, new TestArguments(2, 15)));
        assertEquals(15, host.redstoneOutput(Direction.NORTH));
        assertEquals(0.1D, context.pauseSeconds, 0.000_001D);

        Map<Integer, Integer> values = new HashMap<>();
        values.put(2, 4);
        values.put(5, 9);
        Object[] result = card.setOutput(context, new TestArguments(values));

        assertEquals(Map.of(0, 0, 1, 0, 2, 15, 3, 0, 4, 0, 5, 0), result[0]);
        assertEquals(4, host.redstoneOutput(Direction.NORTH));
        assertEquals(9, host.redstoneOutput(Direction.EAST));
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = RedstoneCardEnvironment.class.getMethod(methodName, Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }

    private static final class TestRedstoneHost implements RedstoneControllerHost {
        private final int[] inputs = new int[6];
        private final int[] outputs = new int[6];

        @Override public int redstoneOutput(final Direction direction) { return outputs[direction.get3DDataValue()]; }
        @Override public int redstoneInput(final Direction direction) { return inputs[direction.get3DDataValue()]; }
        @Override public void setRedstoneOutput(final Direction direction, final int value) { outputs[direction.get3DDataValue()] = Math.clamp(value, 0, 15); }
        @Override public Direction toGlobal(final Direction direction) { return direction; }
        @Override public Level world() { return null; }
        @Override public double xPosition() { return 0.5D; }
        @Override public double yPosition() { return 0.5D; }
        @Override public double zPosition() { return 0.5D; }
        @Override public void markChanged() { }
    }

    private static final class RecordingContext implements Context {
        private final Node node;
        private double pauseSeconds = -1D;

        private RecordingContext(final Node node) {
            this.node = node;
        }

        @Override public Node node() { return node; }
        @Override public boolean canInteract(final String player) { return true; }
        @Override public boolean isRunning() { return true; }
        @Override public boolean isPaused() { return false; }
        @Override public boolean start() { return true; }
        @Override public boolean pause(final double seconds) { pauseSeconds = seconds; return true; }
        @Override public boolean stop() { return true; }
        @Override public void consumeCallBudget(final double callCost) { }
        @Override public boolean signal(final String name, final Object... args) { return true; }
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
