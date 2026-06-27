package li.cil.oc.common;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DriveEnvironmentTest {
    @AfterEach
    void resetApi() {
        li.cil.oc.api.API.network = null;
    }

    @Test
    void rawDriveSeekUsesUpstreamDefaultThreshold() {
        OpenComputersApi.initialize();
        final DriveEnvironment drive = new DriveEnvironment(512 * 129, 1, null, null, null, 0, null);
        final RecordingContext context = new RecordingContext();

        drive.readSector(context, new TestArguments(129));

        assertEquals(-1D, context.pauseSeconds, 0.000_001D);
    }

    @Test
    void rawDriveSeekUsesConfiguredThresholdAndTime() throws Exception {
        OpenComputersApi.initialize();
        withCachedConfig(ModSettings.SECTOR_SEEK_THRESHOLD, 4, () ->
            withCachedConfig(ModSettings.SECTOR_SEEK_TIME, 0.25D, () -> {
                final DriveEnvironment drive = new DriveEnvironment(512 * 6, 1, null, null, null, 0, null);
                final RecordingContext context = new RecordingContext();

                drive.readSector(context, new TestArguments(6));

                assertEquals(0.25D, context.pauseSeconds, 0.000_001D);
            }));
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

    private static final class RecordingContext implements Context {
        private double pauseSeconds = -1;

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
        @Override public Object checkAny(final int index) { return value(index); }
        @Override public boolean checkBoolean(final int index) { return (Boolean) value(index); }
        @Override public int checkInteger(final int index) { return ((Number) value(index)).intValue(); }
        @Override public long checkLong(final int index) { return ((Number) value(index)).longValue(); }
        @Override public double checkDouble(final int index) { return ((Number) value(index)).doubleValue(); }
        @Override public String checkString(final int index) { return (String) value(index); }
        @Override public byte[] checkByteArray(final int index) { return (byte[]) value(index); }
        @Override public Map checkTable(final int index) { return (Map) value(index); }
        @Override public ItemStack checkItemStack(final int index) { return (ItemStack) value(index); }
        @Override public Object optAny(final int index, final Object def) { return index < values.length ? values[index] : def; }
        @Override public boolean optBoolean(final int index, final boolean def) { return index < values.length ? (Boolean) values[index] : def; }
        @Override public int optInteger(final int index, final int def) { return index < values.length ? ((Number) values[index]).intValue() : def; }
        @Override public long optLong(final int index, final long def) { return index < values.length ? ((Number) values[index]).longValue() : def; }
        @Override public double optDouble(final int index, final double def) { return index < values.length ? ((Number) values[index]).doubleValue() : def; }
        @Override public String optString(final int index, final String def) { return index < values.length ? (String) values[index] : def; }
        @Override public byte[] optByteArray(final int index, final byte[] def) { return index < values.length ? (byte[]) values[index] : def; }
        @Override public Map optTable(final int index, final Map def) { return index < values.length ? (Map) values[index] : def; }
        @Override public ItemStack optItemStack(final int index, final ItemStack def) { return index < values.length ? (ItemStack) values[index] : def; }
        @Override public boolean isBoolean(final int index) { return index < values.length && values[index] instanceof Boolean; }
        @Override public boolean isInteger(final int index) { return index < values.length && values[index] instanceof Integer; }
        @Override public boolean isLong(final int index) { return index < values.length && values[index] instanceof Long; }
        @Override public boolean isDouble(final int index) { return index < values.length && values[index] instanceof Double; }
        @Override public boolean isString(final int index) { return index < values.length && values[index] instanceof String; }
        @Override public boolean isByteArray(final int index) { return index < values.length && values[index] instanceof byte[]; }
        @Override public boolean isTable(final int index) { return index < values.length && values[index] instanceof Map; }
        @Override public boolean isItemStack(final int index) { return index < values.length && values[index] instanceof ItemStack; }
        @Override public Object[] toArray() { return values.clone(); }
        @Override public Iterator<Object> iterator() { return java.util.Arrays.asList(values).iterator(); }

        private Object value(final int index) {
            if (index < 0 || index >= values.length) {
                throw new IllegalArgumentException("missing argument " + index);
            }
            return values[index];
        }
    }
}
