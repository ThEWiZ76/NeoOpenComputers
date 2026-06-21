package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NavigationUpgradeEnvironmentTest {
    @Test
    void exposesNavigationCallbacks() throws NoSuchMethodException {
        assertCallback("getPosition");
        assertCallback("getFacing");
        assertCallback("getRange");
        assertCallback("findWaypoints");
    }

    @Test
    void reportsHostPositionFacingAndRange() {
        OpenComputersApi.initialize();
        NavigationUpgradeEnvironment navigation = new NavigationUpgradeEnvironment(new TestHost());

        assertArrayEquals(new Object[]{10.5D, 64.0D, -4.5D}, navigation.getPosition(null, new TestArguments()));
        assertArrayEquals(new Object[]{Direction.EAST.get3DDataValue()}, navigation.getFacing(null, new TestArguments()));
        assertArrayEquals(new Object[]{64.0D}, navigation.getRange(null, new TestArguments()));
        DeviceInfo info = (DeviceInfo) navigation;
        assertEquals("PathFinder v3", info.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("128", info.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Capacity));
    }

    @Test
    void findWaypointsConsumesEnergyBeforePausing() {
        OpenComputersApi.initialize();
        NavigationUpgradeEnvironment navigation = new NavigationUpgradeEnvironment(new TestHost());
        ComponentConnector connector = assertInstanceOf(ComponentConnector.class, navigation.node());
        connector.setLocalBufferSize(1D);
        TestEnvironment machineEnvironment = new TestEnvironment();
        Node machineNode = Network.newNode(machineEnvironment, li.cil.oc.api.network.Visibility.None).withConnector(1D).create();
        machineEnvironment.node = machineNode;
        Connector machineConnector = assertInstanceOf(Connector.class, machineNode);
        RecordingContext context = new RecordingContext(machineNode);

        assertArrayEquals(new Object[]{null, "not enough energy"}, navigation.findWaypoints(context, new TestArguments(8D)));
        assertEquals(-1D, context.pauseSeconds, 0.000_001D);

        connector.changeBuffer(1D);
        Object[] result = navigation.findWaypoints(context, new TestArguments(8D));

        assertEquals(0, ((Map[]) result[0]).length);
        assertEquals(0.5D, context.pauseSeconds, 0.000_001D);
        assertEquals(0.9D, connector.localBuffer(), 0.000_001D);
        assertEquals(0D, machineConnector.localBuffer(), 0.000_001D);
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = NavigationUpgradeEnvironment.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
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

    private static final class TestEnvironment implements Environment {
        private Node node;

        @Override public Node node() { return node; }
        @Override public void onConnect(final Node node) { }
        @Override public void onDisconnect(final Node node) { }
        @Override public void onMessage(final Message message) { }
    }

    private static final class TestHost implements EnvironmentHost, li.cil.oc.api.internal.Rotatable {
        @Override public Level world() { return null; }
        @Override public double xPosition() { return 10.5D; }
        @Override public double yPosition() { return 64.0D; }
        @Override public double zPosition() { return -4.5D; }
        @Override public void markChanged() {}
        @Override public Direction facing() { return Direction.EAST; }
        @Override public Direction toGlobal(final Direction value) { return value; }
        @Override public Direction toLocal(final Direction value) { return value; }
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
