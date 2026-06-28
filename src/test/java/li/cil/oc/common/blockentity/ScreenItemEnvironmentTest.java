package li.cil.oc.common.blockentity;

import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.network.Connector;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class ScreenItemEnvironmentTest {
    @Test
    void usesConfiguredScreenResolutionTiers() throws Exception {
        OpenComputersApi.initialize();

        withCachedConfig(ModSettings.SCREEN_WIDTHS_BY_TIER, List.of(7, 9, 11), () ->
            withCachedConfig(ModSettings.SCREEN_HEIGHTS_BY_TIER, List.of(3, 5, 13), () -> {
                final ScreenItemEnvironment tierOne = new ScreenItemEnvironment(null, 0);
                final ScreenItemEnvironment tierThree = new ScreenItemEnvironment(null, 2);

                assertEquals(7, tierOne.getMaximumWidth());
                assertEquals(3, tierOne.getMaximumHeight());
                assertEquals(7, tierOne.getWidth());
                assertEquals(3, tierOne.getHeight());
                assertEquals(11, tierThree.getMaximumWidth());
                assertEquals(13, tierThree.getMaximumHeight());
            }));
    }

    @Test
    void usesConfiguredScreenDepthTiers() throws Exception {
        OpenComputersApi.initialize();

        withCachedConfig(ModSettings.SCREEN_DEPTHS_BY_TIER, List.of(8, 4, 1), () -> {
            final ScreenItemEnvironment tierOne = new ScreenItemEnvironment(null, 0);
            final ScreenItemEnvironment tierThree = new ScreenItemEnvironment(null, 2);

            assertEquals(TextBuffer.ColorDepth.EightBit, tierOne.getMaximumColorDepth());
            assertEquals(TextBuffer.ColorDepth.EightBit, tierOne.getColorDepth());
            assertEquals(TextBuffer.ColorDepth.OneBit, tierThree.getMaximumColorDepth());
            assertEquals(TextBuffer.ColorDepth.OneBit, tierThree.getColorDepth());
        });
    }

    @Test
    void preciseModeIsSupportedOnlyByTierThreeScreensLikeUpstream() throws Exception {
        OpenComputersApi.initialize();

        withCachedConfig(ModSettings.SCREEN_DEPTHS_BY_TIER, List.of(1, 4, 8), () -> {
            final ScreenItemEnvironment tierOne = new ScreenItemEnvironment(null, 0);
            final ScreenItemEnvironment tierTwo = new ScreenItemEnvironment(null, 1);
            final ScreenItemEnvironment tierThree = new ScreenItemEnvironment(null, 2);

            assertArrayEquals(new Object[]{null, "unsupported operation"}, tierOne.setPrecise(null, new TestArguments(true)));
            assertArrayEquals(new Object[]{false}, tierOne.isPrecise(null, new TestArguments()));
            assertArrayEquals(new Object[]{null, "unsupported operation"}, tierTwo.setPrecise(null, new TestArguments(true)));
            assertArrayEquals(new Object[]{false}, tierTwo.isPrecise(null, new TestArguments()));
            assertArrayEquals(new Object[]{false}, tierThree.setPrecise(null, new TestArguments(true)));
            assertArrayEquals(new Object[]{true}, tierThree.isPrecise(null, new TestArguments()));
        });
    }

    @Test
    void usesConfiguredScreenEnergyCostLikeUpstream() throws Exception {
        OpenComputersApi.initialize();

        withCachedConfig(ModSettings.SCREEN_COST, 0.2D, () ->
            withCachedConfig(ModSettings.SCREEN_WIDTHS_BY_TIER, List.of(50, 80, 160), () ->
            withCachedConfig(ModSettings.SCREEN_HEIGHTS_BY_TIER, List.of(16, 25, 50), () -> {
                final ScreenItemEnvironment tierThree = new ScreenItemEnvironment(null, 2);

                assertEquals(0.2D, tierThree.getEnergyCostPerTick(), 0.000_001D);
                assertEquals(2D, tierThree.fullyLitEnergyCostPerTick(), 0.000_001D);
            })));
    }

    @Test
    void litScreenConsumesConfiguredPowerOnUpdateLikeUpstream() {
        OpenComputersApi.initialize();
        ScreenItemEnvironment screen = new ScreenItemEnvironment(null, 0);
        screen.setEnergyCostPerTick(1D);
        screen.setResolution(1, 1);
        screen.set(0, 0, "X", false);
        Connector connector = (Connector) screen.node();
        connector.setLocalBufferSize(20D);
        connector.changeBuffer(20D);

        assertEquals(true, screen.canUpdate());
        for (int tick = 0; tick < ModSettings.mfuTickFrequency(); tick++) {
            screen.update();
        }

        assertEquals(10D, connector.localBuffer(), 0.000_001D);
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
        @Override public Iterator<Object> iterator() { return Arrays.asList(values).iterator(); }
    }
}
