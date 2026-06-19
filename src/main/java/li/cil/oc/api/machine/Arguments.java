package li.cil.oc.api.machine;

import net.minecraft.world.item.ItemStack;

import java.util.Map;

public interface Arguments extends Iterable<Object> {
    int count();

    Object checkAny(int index);

    boolean checkBoolean(int index);

    int checkInteger(int index);

    long checkLong(int index);

    double checkDouble(int index);

    String checkString(int index);

    byte[] checkByteArray(int index);

    Map checkTable(int index);

    ItemStack checkItemStack(int index);

    Object optAny(int index, Object def);

    boolean optBoolean(int index, boolean def);

    int optInteger(int index, int def);

    long optLong(int index, long def);

    double optDouble(int index, double def);

    String optString(int index, String def);

    byte[] optByteArray(int index, byte[] def);

    Map optTable(int index, Map def);

    ItemStack optItemStack(int index, ItemStack def);

    boolean isBoolean(int index);

    boolean isInteger(int index);

    boolean isLong(int index);

    boolean isDouble(int index);

    boolean isString(int index);

    boolean isByteArray(int index);

    boolean isTable(int index);

    boolean isItemStack(int index);

    Object[] toArray();
}
