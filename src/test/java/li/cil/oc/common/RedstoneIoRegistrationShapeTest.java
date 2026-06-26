package li.cil.oc.common;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.block.RedstoneIoBlock;
import li.cil.oc.common.blockentity.RedstoneIoBlockEntity;
import li.cil.oc.common.component.RedstoneControllerHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RedstoneIoRegistrationShapeTest {
    @Test
    void redstoneIoBlockIsEntityBlockAndSignalSource() throws NoSuchMethodException {
        final Constructor<RedstoneIoBlock> constructor = RedstoneIoBlock.class.getConstructor(BlockBehaviour.Properties.class);
        final Method getSignal = RedstoneIoBlock.class.getDeclaredMethod("getSignal", BlockState.class, net.minecraft.world.level.BlockGetter.class, BlockPos.class, Direction.class);

        assertTrue(Block.class.isAssignableFrom(RedstoneIoBlock.class));
        assertTrue(EntityBlock.class.isAssignableFrom(RedstoneIoBlock.class));
        assertArrayEquals(new Class<?>[]{BlockBehaviour.Properties.class}, constructor.getParameterTypes());
        assertEquals(int.class, getSignal.getReturnType());
    }

    @Test
    void redstoneIoBlockEntityExposesComponentShape() throws NoSuchMethodException {
        final Constructor<RedstoneIoBlockEntity> constructor = RedstoneIoBlockEntity.class.getConstructor(BlockPos.class, BlockState.class);

        assertTrue(BlockEntity.class.isAssignableFrom(RedstoneIoBlockEntity.class));
        assertTrue(Environment.class.isAssignableFrom(RedstoneIoBlockEntity.class));
        assertTrue(EnvironmentHost.class.isAssignableFrom(RedstoneIoBlockEntity.class));
        assertTrue(RedstoneControllerHost.class.isAssignableFrom(RedstoneIoBlockEntity.class));
        assertTrue(DeviceInfo.class.isAssignableFrom(RedstoneIoBlockEntity.class));
        assertArrayEquals(new Class<?>[]{BlockPos.class, BlockState.class}, constructor.getParameterTypes());
        assertCallback("getInput");
        assertCallback("getOutput");
        assertCallback("setOutput");
        assertCallback("getComparatorInput");
    }

    @Test
    void redstoneIoBlockEntityExposesUpstreamDeviceInfoMetadata() throws Exception {
        final RedstoneIoBlockEntity redstone = allocateRedstoneIo();

        Map<String, String> metadata = redstone.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Communication, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Redstone controller", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("Rs100-V", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("16", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
        assertEquals("1", metadata.get(DeviceInfo.DeviceAttribute.Width));
    }

    @Test
    void setOutputUsesConfiguredRedstoneDelayLikeUpstream() throws Exception {
        withCachedConfig(ModSettings.REDSTONE_DELAY, 0.25D, () -> {
            final RedstoneIoBlockEntity redstone = allocateRedstoneIo();
            initializeArrays(redstone);
            final RecordingContext context = new RecordingContext();

            assertArrayEquals(new Object[]{0}, redstone.setOutput(context, new TestArguments(2, 15)));

            assertEquals(0.25D, context.pauseSeconds, 0.000_001D);
        });
    }

    @Test
    void getInputAndOutputIgnoreSideWhenExtraArgumentsExistLikeUpstream() throws Exception {
        final RedstoneIoBlockEntity redstone = allocateRedstoneIo();
        final int[] inputs = new int[6];
        final int[] outputs = new int[6];
        inputs[Direction.NORTH.get3DDataValue()] = 7;
        outputs[Direction.NORTH.get3DDataValue()] = 11;
        setField(redstone, "inputs", inputs);
        setField(redstone, "outputs", outputs);

        assertEquals(Map.of(0, 0, 1, 0, 2, 7, 3, 0, 4, 0, 5, 0),
            redstone.getInput(null, new TestArguments(2, "ignored"))[0]);
        assertEquals(Map.of(0, 0, 1, 0, 2, 11, 3, 0, 4, 0, 5, 0),
            redstone.getOutput(null, new TestArguments(2, "ignored"))[0]);
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = RedstoneIoBlockEntity.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }

    private static RedstoneIoBlockEntity allocateRedstoneIo() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (RedstoneIoBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(RedstoneIoBlockEntity.class);
    }

    private static void initializeArrays(final RedstoneIoBlockEntity redstone) throws Exception {
        setField(redstone, "outputs", new int[6]);
        setField(redstone, "inputs", new int[6]);
    }

    private static void setField(final RedstoneIoBlockEntity redstone, final String name, final Object value) throws Exception {
        final Field field = RedstoneIoBlockEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(redstone, value);
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

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class RecordingContext implements Context {
        private double pauseSeconds = -1D;

        @Override public Node node() { return null; }
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
