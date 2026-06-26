package li.cil.oc.common;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class ArgumentUtils {
    public static ItemStack checkItemStack(final int index, final Object value) {
        if (value instanceof ItemStack stack) {
            return stack;
        }
        if (value instanceof Map<?, ?> map) {
            return itemStackFromMap(map);
        }
        throw new IllegalArgumentException("bad argument #" + (index + 1) + " (item stack expected)");
    }

    public static boolean isItemStack(final Object value) {
        if (value instanceof ItemStack) {
            return true;
        }
        if (value instanceof Map<?, ?> map) {
            final Object name = get(map, "name");
            return name instanceof String || name instanceof byte[];
        }
        return false;
    }

    private static ItemStack itemStackFromMap(final Map<?, ?> map) {
        final Object nameValue = get(map, "name");
        final String name;
        if (nameValue instanceof String string) {
            name = string;
        } else if (nameValue instanceof byte[] bytes) {
            name = new String(bytes, StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException("invalid item stack");
        }
        final ResourceLocation id = ResourceLocation.tryParse(name);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            throw new IllegalArgumentException("invalid item stack");
        }
        final Item item = BuiltInRegistries.ITEM.get(id);
        final ItemStack stack = new ItemStack(item, 1);
        final Object damage = get(map, "damage");
        if (damage instanceof Number number) {
            stack.setDamageValue(number.intValue());
        }
        final CompoundTag tag = itemStackTag(get(map, "tag"));
        if (tag != null) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return stack;
    }

    private static Object get(final Map<?, ?> map, final String key) {
        final Object direct = map.get(key);
        if (direct != null || map.containsKey(key)) {
            return direct;
        }
        final byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof byte[] candidate && java.util.Arrays.equals(candidate, bytes)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static CompoundTag itemStackTag(final Object value) {
        final byte[] bytes;
        if (value instanceof byte[] data) {
            bytes = data;
        } else if (value instanceof String data) {
            bytes = data.getBytes(StandardCharsets.UTF_8);
        } else {
            return null;
        }
        try {
            return NbtIo.readCompressed(new ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap());
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private ArgumentUtils() {
    }
}
