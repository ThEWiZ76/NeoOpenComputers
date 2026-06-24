package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class HologramBlockEntityTest {
    @Test
    void exposesUpstreamDeviceInfoMetadata() throws Exception {
        HologramBlockEntity hologram = allocateHologram(1);

        Map<String, String> metadata = hologram.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Display, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Holographic projector", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("VirtualViewer H1-2", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals(Integer.toString(HologramBlockEntity.WIDTH * HologramBlockEntity.WIDTH * HologramBlockEntity.HEIGHT), metadata.get(DeviceInfo.DeviceAttribute.Capacity));
        assertEquals("3", metadata.get(DeviceInfo.DeviceAttribute.Width));
    }

    @Test
    void storesVoxelValues() throws Exception {
        HologramBlockEntity hologram = allocateHologram(1);

        assertArrayEquals(new Object[]{0}, hologram.get(null, new TestArguments(1, 1, 1)));
        hologram.set(null, new TestArguments(1, 1, 1, 3));
        assertArrayEquals(new Object[]{3}, hologram.get(null, new TestArguments(1, 1, 1)));
        hologram.clear(null, new TestArguments());
        assertArrayEquals(new Object[]{0}, hologram.get(null, new TestArguments(1, 1, 1)));
    }

    @Test
    void fillsVoxelColumns() throws Exception {
        HologramBlockEntity hologram = allocateHologram(1);

        hologram.fill(null, new TestArguments(2, 3, 4, 6, 2));

        assertArrayEquals(new Object[]{0}, hologram.get(null, new TestArguments(2, 3, 3)));
        assertArrayEquals(new Object[]{2}, hologram.get(null, new TestArguments(2, 4, 3)));
        assertArrayEquals(new Object[]{2}, hologram.get(null, new TestArguments(2, 6, 3)));
        assertArrayEquals(new Object[]{0}, hologram.get(null, new TestArguments(2, 7, 3)));
    }

    @Test
    void exposesTieredPaletteAndScale() throws Exception {
        HologramBlockEntity tierOne = allocateHologram(0);
        HologramBlockEntity tierTwo = allocateHologram(1);

        assertArrayEquals(new Object[]{1}, tierOne.maxDepth(null, new TestArguments()));
        assertArrayEquals(new Object[]{2}, tierTwo.maxDepth(null, new TestArguments()));
        assertArrayEquals(new Object[]{0x00FF00}, tierOne.getPaletteColor(null, new TestArguments(1)));
        assertArrayEquals(new Object[]{0xFF0000}, tierTwo.getPaletteColor(null, new TestArguments(1)));
        assertArrayEquals(new Object[]{0x0000FF}, tierTwo.setPaletteColor(null, new TestArguments(1, 0x123456)));
        assertArrayEquals(new Object[]{0x123456}, tierTwo.getPaletteColor(null, new TestArguments(1)));

        tierOne.setScale(null, new TestArguments(8.0D));
        tierTwo.setScale(null, new TestArguments(8.0D));

        assertArrayEquals(new Object[]{3.0D}, tierOne.getScale(null, new TestArguments()));
        assertArrayEquals(new Object[]{4.0D}, tierTwo.getScale(null, new TestArguments()));
    }

    @Test
    void usesConfiguredScaleTranslationAndRawDelay() throws Exception {
        withCachedConfig(ModSettings.HOLOGRAM_MAX_SCALE, List.of(5D, 6D), () ->
            withCachedConfig(ModSettings.HOLOGRAM_MAX_TRANSLATION, List.of(2D, 3D), () ->
                withCachedConfig(ModSettings.HOLOGRAM_SET_RAW_DELAY, 0.125D, () -> {
                    HologramBlockEntity tierTwo = allocateHologram(1);
                    TrackingContext context = new TrackingContext();

                    tierTwo.setScale(null, new TestArguments(9.0D));
                    tierTwo.setTranslation(null, new TestArguments(-4D, 8D, 4D));
                    tierTwo.setRaw(context, new TestArguments(new byte[HologramBlockEntity.WIDTH * HologramBlockEntity.WIDTH * HologramBlockEntity.HEIGHT]));

                    assertArrayEquals(new Object[]{6.0D}, tierTwo.getScale(null, new TestArguments()));
                    assertArrayEquals(new Object[]{-3.0D, 6.0D, 3.0D}, tierTwo.getTranslation(null, new TestArguments()));
                    assertEquals(0.125D, context.pauseSeconds, 0.000_001D);
                })));
    }

    @Test
    void loadedScalePreservesSavedValueLikeUpstream() throws Exception {
        HologramBlockEntity hologram = allocateHologram(1);
        CompoundTag tag = new CompoundTag();
        tag.putDouble("oc:scale", 9.0D);

        hologram.loadAdditional(tag, null);

        assertArrayEquals(new Object[]{9.0D}, hologram.getScale(null, new TestArguments()));
    }

    private static HologramBlockEntity allocateHologram(final int tier) throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        HologramBlockEntity hologram = (HologramBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(HologramBlockEntity.class);
        setField(hologram, "volume", new int[HologramBlockEntity.WIDTH * HologramBlockEntity.WIDTH * 2]);
        setField(hologram, "tier", tier);
        setField(hologram, "colors", tier == 0 ? new int[]{0x00FF00} : new int[]{0x0000FF, 0x00FF00, 0xFF0000});
        setField(hologram, "scale", 1.0D);
        return hologram;
    }

    private static void setField(final HologramBlockEntity hologram, final String name, final Object value) throws Exception {
        Field field = HologramBlockEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(hologram, value);
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

    private static final class TrackingContext implements Context {
        private double pauseSeconds = -1D;

        @Override public Node node() { return null; }
        @Override public boolean canInteract(final String player) { return true; }
        @Override public boolean isRunning() { return true; }
        @Override public boolean isPaused() { return false; }
        @Override public boolean start() { return true; }
        @Override public boolean pause(final double seconds) {
            pauseSeconds = seconds;
            return true;
        }
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
