package li.cil.oc.common.component;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChunkloaderUpgradeEnvironmentTest {
    @Test
    void activeChunkloaderConsumesOwnConnectorEnergyLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        withCachedConfig(ModSettings.CHUNKLOADER_COST, 0.25D, () ->
            withCachedConfig(ModSettings.MFU_TICK_FREQUENCY, 4, () -> {
                ChunkloaderUpgradeEnvironment environment = new ChunkloaderUpgradeEnvironment(null);
                ComponentConnector connector = assertInstanceOf(ComponentConnector.class, environment.node());
                connector.setLocalBufferSize(2D);
                connector.changeBuffer(2D);

                assertTrue(environment.canUpdate());
                assertArrayEquals(new Object[]{true}, environment.setActive(null, new TestArguments(true)));

                environment.tickPowerCost();

                assertEquals(1D, connector.localBuffer(), 0.000_001D);
                assertArrayEquals(new Object[]{true}, environment.isActive(null, new TestArguments()));

                assertTrue(connector.tryChangeBuffer(-0.75D));
                environment.tickPowerCost();

                assertEquals(0.25D, connector.localBuffer(), 0.000_001D);
                assertArrayEquals(new Object[]{false}, environment.isActive(null, new TestArguments()));
            }));
    }

    @Test
    void inactiveChunkloaderDoesNotDrainEnergy() throws Exception {
        OpenComputersApi.initialize();
        withCachedConfig(ModSettings.CHUNKLOADER_COST, 0.25D, () ->
            withCachedConfig(ModSettings.MFU_TICK_FREQUENCY, 4, () -> {
                ChunkloaderUpgradeEnvironment environment = new ChunkloaderUpgradeEnvironment(null);
                ComponentConnector connector = assertInstanceOf(ComponentConnector.class, environment.node());
                connector.setLocalBufferSize(2D);
                connector.changeBuffer(2D);

                assertFalse((Boolean) environment.isActive(null, new TestArguments())[0]);
                environment.tickPowerCost();

                assertEquals(2D, connector.localBuffer(), 0.000_001D);
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

    private record TestArguments(Object... values) implements Arguments {
        @Override public int count() { return values.length; }
        @Override public boolean isInteger(final int index) { return index < values.length && values[index] instanceof Number; }
        @Override public boolean isLong(final int index) { return index < values.length && values[index] instanceof Number; }
        @Override public boolean isBoolean(final int index) { return index < values.length && values[index] instanceof Boolean; }
        @Override public boolean isDouble(final int index) { return index < values.length && values[index] instanceof Number; }
        @Override public boolean isString(final int index) { return index < values.length && values[index] instanceof String; }
        @Override public boolean isByteArray(final int index) { return index < values.length && values[index] instanceof byte[]; }
        @Override public boolean isTable(final int index) { return index < values.length && values[index] instanceof Map; }
        @Override public boolean isItemStack(final int index) { return index < values.length && values[index] instanceof ItemStack; }
        @Override public int checkInteger(final int index) { return ((Number) values[index]).intValue(); }
        @Override public long checkLong(final int index) { return ((Number) values[index]).longValue(); }
        @Override public boolean checkBoolean(final int index) { return (Boolean) values[index]; }
        @Override public double checkDouble(final int index) { return ((Number) values[index]).doubleValue(); }
        @Override public String checkString(final int index) { return (String) values[index]; }
        @Override public byte[] checkByteArray(final int index) { return (byte[]) values[index]; }
        @Override public Map checkTable(final int index) { return (Map) values[index]; }
        @Override public ItemStack checkItemStack(final int index) { return (ItemStack) values[index]; }
        @Override public Object checkAny(final int index) { return values[index]; }
        @Override public Object optAny(final int index, final Object def) { return index < values.length ? values[index] : def; }
        @Override public int optInteger(final int index, final int def) { return index < values.length ? checkInteger(index) : def; }
        @Override public long optLong(final int index, final long def) { return index < values.length ? checkLong(index) : def; }
        @Override public boolean optBoolean(final int index, final boolean def) { return index < values.length ? checkBoolean(index) : def; }
        @Override public double optDouble(final int index, final double def) { return index < values.length ? checkDouble(index) : def; }
        @Override public String optString(final int index, final String def) { return index < values.length ? checkString(index) : def; }
        @Override public byte[] optByteArray(final int index, final byte[] def) { return index < values.length ? checkByteArray(index) : def; }
        @Override public Map optTable(final int index, final Map def) { return index < values.length ? checkTable(index) : def; }
        @Override public ItemStack optItemStack(final int index, final ItemStack def) { return index < values.length ? checkItemStack(index) : def; }
        @Override public Object[] toArray() { return Arrays.copyOf(values, values.length); }
        @Override public Iterator<Object> iterator() { return Arrays.asList(values).iterator(); }
    }
}
