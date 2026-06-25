package li.cil.oc.common.driver;

import li.cil.oc.api.driver.Converter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.fluids.FluidStack;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class MinecraftConverters {
    public static final Converter ITEM_STACK = (value, output) -> {
        if (value instanceof ItemStack stack) {
            final ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            output.put("damage", stack.getDamageValue());
            output.put("maxDamage", stack.getMaxDamage());
            output.put("size", stack.getCount());
            output.put("maxSize", stack.getMaxStackSize());
            output.put("hasTag", stack.has(DataComponents.CUSTOM_DATA));
            output.put("name", id == null ? "minecraft:air" : id.toString());
            output.put("label", stack.getHoverName().getString());
        }
    };

    public static final Converter BLOCK = (value, output) -> {
        if (value instanceof Block block) {
            final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            output.put("name", id == null ? "minecraft:air" : id.toString());
        }
    };

    public static final Converter BLOCK_STATE = (value, output) -> {
        if (value instanceof BlockState state) {
            final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            output.put("name", id == null ? "minecraft:air" : id.toString());
            if (!state.getValues().isEmpty()) {
                final Map<String, String> properties = new LinkedHashMap<>();
                for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
                    properties.put(entry.getKey().getName(), propertyName(entry.getKey(), entry.getValue()));
                }
                output.put("properties", properties);
            }
        }
    };

    public static final Converter NBT = (value, output) -> {
        if (value instanceof CompoundTag tag) {
            output.put("oc:flatten", convertTag(tag));
        }
    };

    public static final Converter FLUID_STACK = (value, output) -> {
        if (value instanceof FluidStack stack) {
            output.put("amount", stack.getAmount());
            output.put("hasTag", !stack.isComponentsPatchEmpty());
            if (!stack.isEmpty()) {
                final ResourceLocation id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
                output.put("name", id == null ? "minecraft:empty" : id.toString());
                output.put("label", stack.getHoverName().getString());
            }
        }
    };

    public static final Converter LEVEL = (value, output) -> {
        if (value instanceof Level level) {
            output.put("id", levelId(level));
            output.put("name", level.dimension().location().toString());
        }
    };

    private static String levelId(final Level level) {
        final long seed = level instanceof ServerLevel serverLevel ? serverLevel.getSeed() : 0L;
        final int dimension = legacyDimensionId(level);
        final byte[] bytes = ByteBuffer.allocate(Long.BYTES + Integer.BYTES)
            .putLong(seed)
            .putInt(dimension)
            .array();
        return UUID.nameUUIDFromBytes(bytes).toString();
    }

    private static int legacyDimensionId(final Level level) {
        if (Level.OVERWORLD.equals(level.dimension())) {
            return 0;
        }
        if (Level.NETHER.equals(level.dimension())) {
            return -1;
        }
        if (Level.END.equals(level.dimension())) {
            return 1;
        }
        return level.dimension().location().hashCode();
    }

    private static Object convertTag(final Tag tag) {
        if (tag instanceof NumericTag numericTag) {
            return numericTag.getAsNumber();
        }
        if (tag instanceof StringTag stringTag) {
            return stringTag.getAsString();
        }
        if (tag instanceof ByteArrayTag byteArrayTag) {
            return byteArrayTag.getAsByteArray();
        }
        if (tag instanceof IntArrayTag intArrayTag) {
            return intArrayTag.getAsIntArray();
        }
        if (tag instanceof LongArrayTag longArrayTag) {
            return longArrayTag.getAsLongArray();
        }
        if (tag instanceof ListTag listTag) {
            Object[] values = new Object[listTag.size()];
            for (int index = 0; index < listTag.size(); index++) {
                values[index] = convertTag(listTag.get(index));
            }
            return values;
        }
        if (tag instanceof CompoundTag compoundTag) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (String key : compoundTag.getAllKeys()) {
                values.put(key, convertTag(compoundTag.get(key)));
            }
            return values;
        }
        return null;
    }

    private static <T extends Comparable<T>> String propertyName(final Property<T> property, final Comparable<?> value) {
        return property.getName(valueClassCast(value));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> T valueClassCast(final Comparable<?> value) {
        return (T) value;
    }

    private MinecraftConverters() {
    }
}
