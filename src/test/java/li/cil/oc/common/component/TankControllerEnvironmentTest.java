package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TankControllerEnvironmentTest {
    @Test
    void exposesTankControllerCallbacks() throws NoSuchMethodException {
        assertCallback("getTankCount");
        assertCallback("getTankLevel");
        assertCallback("getTankCapacity");
        assertCallback("getFluidInTank");
    }

    @Test
    void reportsUpstreamDeviceInfo() {
        OpenComputersApi.initialize();
        TankControllerEnvironment controller = new TankControllerEnvironment(new TestHost());

        DeviceInfo info = controller;

        assertEquals(DeviceInfo.DeviceClass.Generic, info.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Tank controller", info.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("FlowCheckDX", info.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Product));
    }

    @Test
    void returnsNoTankWhenNoFluidHandlerExists() {
        OpenComputersApi.initialize();
        TankControllerEnvironment controller = new TankControllerEnvironment(new TestHost());

        assertArrayEquals(new Object[]{null, "no tank"}, controller.getTankCount(null, new TestArguments(0)));
        assertArrayEquals(new Object[]{null, "no tank"}, controller.getTankLevel(null, new TestArguments(0)));
        assertArrayEquals(new Object[]{null, "no tank"}, controller.getTankCapacity(null, new TestArguments(0)));
        assertArrayEquals(new Object[]{null, "no tank"}, controller.getFluidInTank(null, new TestArguments(0)));
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = TankControllerEnvironment.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }

    private static final class TestHost implements EnvironmentHost {
        @Override public Level world() { return null; }
        @Override public double xPosition() { return 0.5D; }
        @Override public double yPosition() { return 0.5D; }
        @Override public double zPosition() { return 0.5D; }
        @Override public void markChanged() { }
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
