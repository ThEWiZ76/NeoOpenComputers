package li.cil.oc.common.blockentity;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.common.item.data.PrintData;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrinterBlockEntityTest {
    @Test
    void setCollidableValidatesBothFlagsBeforeMutationLikeUpstream() throws Exception {
        PrinterBlockEntity printer = allocatePrinter();
        printer.setCollidable(null, new TestArguments(false, true));

        assertThrows(RuntimeException.class, () -> printer.setCollidable(null, new TestArguments(true, "bad")));

        assertArrayEquals(new Object[]{false, true}, printer.isCollidable(null, new TestArguments()));
    }

    @Test
    void syncsPrintPreviewDataToClientRenderer() throws Exception {
        final Method updateTag = PrinterBlockEntity.class.getDeclaredMethod("getUpdateTag", net.minecraft.core.HolderLookup.Provider.class);
        final Method updatePacket = PrinterBlockEntity.class.getDeclaredMethod("getUpdatePacket");
        final Method previewStack = PrinterBlockEntity.class.getDeclaredMethod("previewStack");
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/blockentity/PrinterBlockEntity.java"));

        assertEquals(net.minecraft.nbt.CompoundTag.class, updateTag.getReturnType());
        assertEquals(net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.class, updatePacket.getReturnType());
        assertEquals(net.minecraft.world.item.ItemStack.class, previewStack.getReturnType());
        assertTrue(source.contains("ClientboundBlockEntityDataPacket.create(this)"));
        assertTrue(source.contains("TAG_VISUAL_DATA"));
        assertTrue(source.contains("data.save"));
        assertTrue(source.contains("data.load"));
    }

    private static PrinterBlockEntity allocatePrinter() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        PrinterBlockEntity printer = (PrinterBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(PrinterBlockEntity.class);
        setField(printer, "data", new PrintData());
        return printer;
    }

    private static void setField(final PrinterBlockEntity printer, final String name, final Object value) throws Exception {
        Field field = PrinterBlockEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(printer, value);
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
