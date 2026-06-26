package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

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
    void agentControllerExposesInternalTankCallbacksLikeUpstream() {
        assertCallback(TankControllerEnvironment.AgentTankControllerEnvironment.class, "getTankLevelInSlot");
        assertCallback(TankControllerEnvironment.AgentTankControllerEnvironment.class, "getTankCapacityInSlot");
        assertCallback(TankControllerEnvironment.AgentTankControllerEnvironment.class, "getFluidInTankInSlot");
        assertCallback(TankControllerEnvironment.AgentTankControllerEnvironment.class, "getFluidInInternalTank");
        assertCallback(TankControllerEnvironment.AgentTankControllerEnvironment.class, "drain");
        assertCallback(TankControllerEnvironment.AgentTankControllerEnvironment.class, "fill");
    }

    @Test
    void reportsUpstreamDeviceInfo() {
        OpenComputersApi.initialize();
        TankControllerEnvironment controller = new TankControllerEnvironment(new TestHost());

        DeviceInfo info = controller;

        assertEquals(DeviceInfo.DeviceClass.Generic, info.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Tank controller", info.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", info.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Vendor));
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

    @Test
    void fluidInfoCallbackHonorsInspectionConfigLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        withCachedConfig(ModSettings.ALLOW_ITEM_STACK_INSPECTION, false, () -> {
            TankControllerEnvironment controller = new TankControllerEnvironment(new TestHost());

            assertArrayEquals(new Object[]{null, "not enabled in config"}, controller.getFluidInTank(null, new TestArguments(0)));
        });
    }

    @Test
    void internalFluidInfoCallbacksHonorInspectionConfigLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        withCachedConfig(ModSettings.ALLOW_ITEM_STACK_INSPECTION, false, () -> {
            final var controller = new TankControllerEnvironment.AgentTankControllerEnvironment(testAgent());

            assertArrayEquals(new Object[]{null, "not enabled in config"},
                invokeIfPresent(controller, "getFluidInTankInSlot", new TestArguments(1)));
            assertArrayEquals(new Object[]{null, "not enabled in config"},
                invokeIfPresent(controller, "getFluidInInternalTank", new TestArguments(1)));
        });
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        assertCallback(TankControllerEnvironment.class, methodName);
    }

    private static void assertCallback(final Class<?> type, final String methodName) {
        final Method method;
        try {
            method = type.getMethod(methodName, Context.class, Arguments.class);
        } catch (final NoSuchMethodException e) {
            throw new AssertionError("Missing callback " + methodName, e);
        }
        assertTrue(method.isAnnotationPresent(Callback.class));
    }

    private static Object[] invokeIfPresent(final Object target, final String methodName, final Arguments arguments) throws Exception {
        try {
            final Method method = target.getClass().getMethod(methodName, Context.class, Arguments.class);
            return (Object[]) method.invoke(target, null, arguments);
        } catch (final NoSuchMethodException ignored) {
            return new Object[]{"missing callback"};
        }
    }

    private static li.cil.oc.api.internal.Agent testAgent() {
        final InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "mainInventory", "equipmentInventory", "tank", "player", "machine", "world" -> null;
            case "selectedSlot", "selectedTank", "componentSlot" -> 0;
            case "setSelectedSlot", "setSelectedTank", "markChanged", "onMachineConnect", "onMachineDisconnect", "setName" -> null;
            case "internalComponents" -> java.util.List.<ItemStack>of();
            case "facing", "toGlobal", "toLocal" -> net.minecraft.core.Direction.NORTH;
            case "xPosition", "yPosition", "zPosition" -> 0D;
            case "name", "ownerName" -> "";
            case "ownerUUID" -> new UUID(0L, 0L);
            default -> throw new UnsupportedOperationException(method.toString());
        };
        return (li.cil.oc.api.internal.Agent) Proxy.newProxyInstance(
            TankControllerEnvironmentTest.class.getClassLoader(),
            new Class<?>[]{li.cil.oc.api.internal.Agent.class},
            handler);
    }

    private static <T> void withCachedConfig(final ModConfigSpec.ConfigValue<T> value, final T override, final ThrowingRunnable action) throws Exception {
        final Field cachedValue = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cachedValue.setAccessible(true);
        final Object previous = cachedValue.get(value);
        cachedValue.set(value, override);
        try {
            action.run();
        } finally {
            cachedValue.set(value, previous);
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
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
