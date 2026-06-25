package li.cil.oc.common.driver;

import li.cil.oc.api.driver.Converter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.LinkedHashMap;
import java.util.Map;

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
