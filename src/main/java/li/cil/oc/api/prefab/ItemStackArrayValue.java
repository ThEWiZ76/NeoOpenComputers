package li.cil.oc.api.prefab;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.TreeMap;

public class ItemStackArrayValue extends AbstractValue {
    private static final String ARRAY_KEY = "Array";
    private static final String INDEX_KEY = "Index";
    private static final HashMap<Object, Object> EMPTY_MAP = new HashMap<>();

    private ItemStack[] array;
    private int iteratorIndex;

    public ItemStackArrayValue(final ItemStack[] array) {
        if (array != null) {
            this.array = new ItemStack[array.length];
            for (int index = 0; index < array.length; index++) {
                this.array[index] = array[index] != null ? array[index].copy() : null;
            }
        }
        iteratorIndex = 0;
    }

    public ItemStackArrayValue() {
        this(null);
    }

    @Override
    public Object[] call(final Context context, final Arguments arguments) {
        if (array == null || iteratorIndex >= array.length) {
            return null;
        }

        final ItemStack stack = array[iteratorIndex++];
        if (stack == null || stack.isEmpty()) {
            return new Object[]{EMPTY_MAP};
        }
        return new Object[]{stack};
    }

    @Override
    public Object apply(final Context context, final Arguments arguments) {
        if (arguments.count() == 0 || array == null) {
            return null;
        }

        if (arguments.isInteger(0)) {
            final int luaIndex = arguments.checkInteger(0);
            if (luaIndex < 1 || luaIndex > array.length) {
                return null;
            }
            return array[luaIndex - 1];
        }

        if (arguments.isString(0) && "n".equals(arguments.checkString(0))) {
            return array.length;
        }

        return null;
    }

    @Override
    public void load(final CompoundTag nbt) {
        if (nbt.contains(ARRAY_KEY, Tag.TAG_LIST)) {
            final ListTag list = nbt.getList(ARRAY_KEY, Tag.TAG_COMPOUND);
            array = new ItemStack[list.size()];
            for (int index = 0; index < list.size(); index++) {
                final CompoundTag stackTag = list.getCompound(index);
                array[index] = stackTag.isEmpty()
                        ? ItemStack.EMPTY
                        : ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, stackTag).result().orElse(null);
            }
        } else {
            array = null;
        }
        iteratorIndex = nbt.getInt(INDEX_KEY);
    }

    @Override
    public void save(final CompoundTag nbt) {
        if (array != null) {
            final ListTag list = new ListTag();
            for (final ItemStack stack : array) {
                list.add(stack != null && !stack.isEmpty()
                        ? ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, stack).result().orElseGet(CompoundTag::new)
                        : new CompoundTag());
            }
            nbt.put(ARRAY_KEY, list);
        }
        nbt.putInt(INDEX_KEY, iteratorIndex);
    }

    @Callback(doc = "function():nil -- Reset the iterator index so that the next call will return the first element.")
    public Object[] reset(final Context context, final Arguments arguments) {
        iteratorIndex = 0;
        return null;
    }

    @Callback(doc = "function():number -- Returns the number of elements in the array.")
    public Object[] count(final Context context, final Arguments arguments) {
        return new Object[]{array != null ? array.length : 0};
    }

    @Callback(doc = "function():table -- Returns all stacks in the array. Memory intensive.")
    public Object[] getAll(final Context context, final Arguments arguments) {
        final TreeMap<Integer, Object> map = new TreeMap<>();
        if (array != null) {
            for (int index = 0; index < array.length; index++) {
                final ItemStack stack = array[index];
                map.put(index + 1, stack != null ? stack : EMPTY_MAP);
            }
        }
        return new Object[]{map};
    }

    @Override
    public String toString() {
        return "{ItemStack Array}";
    }
}
