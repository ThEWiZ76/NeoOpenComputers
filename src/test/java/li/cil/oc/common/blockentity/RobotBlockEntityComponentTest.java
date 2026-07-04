package li.cil.oc.common.blockentity;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RobotBlockEntityComponentTest {
    @Test
    void robotComponentCallbacksAreExposed() throws NoSuchMethodException {
        assertCallback("name");
        assertCallback("getLightColor");
        assertCallback("setLightColor");
        assertCallback("inventorySize");
        assertCallback("select");
        assertCallback("count");
        assertCallback("space");
        assertCallback("compareTo");
        assertCallback("transferTo");
        assertCallback("move");
        assertCallback("turn");
    }

    @Test
    void movementCallbacksAreSynchronizedLikeUpstream() throws NoSuchMethodException {
        assertFalse(RobotBlockEntity.class.getMethod("move", Context.class, Arguments.class).getAnnotation(Callback.class).direct());
        assertFalse(RobotBlockEntity.class.getMethod("turn", Context.class, Arguments.class).getAnnotation(Callback.class).direct());
    }

    @Test
    void robotOwnsSeparateComponentNode() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/RobotBlockEntity.java"));

        assertTrue(source.contains("TAG_ROBOT_NODE"));
        assertTrue(source.contains("withComponent(\"robot\""));
        assertTrue(source.contains("connectMachineNode"));
        assertTrue(source.contains("robotNode.save"));
        assertTrue(source.contains("robotNode.load"));
        assertTrue(source.contains("new RobotMoveEvent.Post(eventRobot, direction)"));
    }

    @Test
    void robotNodeIsRobotComponentNode() throws Exception {
        final RobotBlockEntity robot = allocateRobot();
        final TestComponentNode node = new TestComponentNode();
        setField(robot, "robotNode", node);

        assertEquals(node, robot.node());
        assertEquals("robot", ((Component) robot.node()).name());
    }

    @Test
    void localMovementSidesMapThroughRobotFacing() {
        assertEquals(Direction.SOUTH, RobotBlockEntity.movementDirection(Direction.NORTH, 3));
        assertEquals(Direction.NORTH, RobotBlockEntity.movementDirection(Direction.NORTH, 2));
        assertEquals(Direction.EAST, RobotBlockEntity.movementDirection(Direction.NORTH, 5));
        assertEquals(Direction.WEST, RobotBlockEntity.movementDirection(Direction.NORTH, 4));
        assertEquals(Direction.EAST, RobotBlockEntity.movementDirection(Direction.EAST, 3));
        assertEquals(Direction.NORTH, RobotBlockEntity.movementDirection(Direction.EAST, 5));
        assertEquals(Direction.UP, RobotBlockEntity.movementDirection(Direction.WEST, 1));
        assertEquals(Direction.DOWN, RobotBlockEntity.movementDirection(Direction.WEST, 0));
    }

    @Test
    void turnUpdatesFacing() {
        assertEquals(Direction.EAST, RobotBlockEntity.turnedFacing(Direction.NORTH, true));
        assertEquals(Direction.WEST, RobotBlockEntity.turnedFacing(Direction.NORTH, false));
        assertEquals(Direction.SOUTH, RobotBlockEntity.turnedFacing(Direction.EAST, true));
    }

    @Test
    void inventoryCallbacksUseOneBasedSelectedSlots() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/RobotBlockEntity.java"));

        assertTrue(source.contains("return new Object[]{selectedSlot + 1}"));
        assertTrue(source.contains("setSelectedSlot(checkRobotSlot(arguments.checkInteger(0)))"));
        assertTrue(source.contains("return new Object[]{getItem(callbackSlot(arguments, 0)).getCount()}"));
        assertTrue(source.contains("return arguments.count() > index"));
        assertTrue(source.contains("final int zeroBased = slot - 1"));
    }

    @Test
    void transferToUsesSelectedRobotSlotAndMergesStacks() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/RobotBlockEntity.java"));

        assertTrue(source.contains("checkRobotSlot(arguments.checkInteger(0))"));
        assertTrue(source.contains("if (targetSlot == selectedSlot)"));
        assertTrue(source.contains("ItemStack.isSameItemSameComponents(source, target)"));
        assertTrue(source.contains("source.copyWithCount(moved)"));
        assertTrue(source.contains("target.grow(moved)"));
        assertTrue(source.contains("source.shrink(moved)"));
    }

    @Test
    void transferToRejectsEmptySelectedSlot() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/RobotBlockEntity.java"));

        assertTrue(source.contains("if (source.isEmpty())"));
        assertTrue(source.contains("return new Object[]{false}"));
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        final Method method = RobotBlockEntity.class.getMethod(methodName, Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class), methodName + " must be a robot component callback");
    }

    private static RobotBlockEntity allocateRobot() throws Exception {
        return (RobotBlockEntity) unsafe().allocateInstance(RobotBlockEntity.class);
    }

    private static Unsafe unsafe() throws Exception {
        final Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        Field field = RobotBlockEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
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

    private static final class TestComponentNode implements Component {
        @Override public String name() { return "robot"; }
        @Override public li.cil.oc.api.network.Visibility visibility() { return li.cil.oc.api.network.Visibility.Neighbors; }
        @Override public void setVisibility(final li.cil.oc.api.network.Visibility value) {}
        @Override public boolean canBeSeenFrom(final Node other) { return true; }
        @Override public java.util.Collection<String> methods() { return java.util.Set.of(); }
        @Override public Callback annotation(final String method) { return null; }
        @Override public Object[] invoke(final String method, final Context context, final Object... arguments) { return new Object[0]; }
        @Override public li.cil.oc.api.network.Environment host() { return null; }
        @Override public li.cil.oc.api.network.Visibility reachability() { return li.cil.oc.api.network.Visibility.Network; }
        @Override public String address() { return "robot-node"; }
        @Override public li.cil.oc.api.network.Network network() { return null; }
        @Override public boolean isNeighborOf(final Node other) { return false; }
        @Override public boolean canBeReachedFrom(final Node other) { return false; }
        @Override public Iterable<Node> neighbors() { return java.util.List.of(); }
        @Override public Iterable<Node> reachableNodes() { return java.util.List.of(); }
        @Override public void connect(final Node node) {}
        @Override public void disconnect(final Node node) {}
        @Override public void remove() {}
        @Override public void sendToAddress(final String target, final String name, final Object... data) {}
        @Override public void sendToNeighbors(final String name, final Object... data) {}
        @Override public void sendToReachable(final String name, final Object... data) {}
        @Override public void sendToVisible(final String name, final Object... data) {}
        @Override public void load(final net.minecraft.nbt.CompoundTag nbt) {}
        @Override public void save(final net.minecraft.nbt.CompoundTag nbt) {}
    }
}
