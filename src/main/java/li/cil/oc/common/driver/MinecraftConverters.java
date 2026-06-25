package li.cil.oc.common.driver;

import li.cil.oc.api.driver.Converter;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.component.LinkedNetwork;
import li.cil.oc.common.item.LinkedCardItem;
import li.cil.oc.common.item.NanomachineItemData;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class MinecraftConverters {
    private static final String ITEM_DRIVER_DATA_TAG = "oc:data";

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
            convertItemStackCustomData(stack, output);
            convertItemStackLore(stack, output);
            convertItemStackEnchantments(stack, output);
        }
    };

    public static final Converter LINKED_CARD = (value, output) -> {
        if (value instanceof ItemStack stack && stack.is(ModItems.LINKED_CARD.get())) {
            final CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            final CompoundTag data = root.getCompound(ITEM_DRIVER_DATA_TAG);
            output.put("linkChannel", LinkedNetwork.normalizeChannel(data.getString(LinkedCardItem.TUNNEL_TAG)));
        }
    };

    public static final Converter NANOMACHINES = (value, output) -> {
        if (value instanceof ItemStack stack && stack.is(ModItems.NANOMACHINES.get())) {
            final CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            final String uuid = NanomachineItemData.uuid(root);
            if (!uuid.isEmpty()) {
                output.put("nanomachines", uuid);
            }
        }
    };

    public static final Converter FLUID_CONTAINER_ITEM = (value, output) -> {
        if (value instanceof ItemStack stack) {
            final IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
            if (handler != null) {
                output.put("capacity", totalCapacity(handler));
                if (handler.getTanks() == 1) {
                    output.put("fluid", handler.getFluidInTank(0));
                } else {
                    final Object[] tanks = new Object[handler.getTanks()];
                    for (int tank = 0; tank < tanks.length; tank++) {
                        tanks[tank] = fluidTankMap(handler, tank);
                    }
                    output.put("fluid", tanks);
                }
            }
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
            convertFluidStack(stack, output);
        }
    };

    public static final Converter FLUID_TANK = (value, output) -> {
        if (value instanceof IFluidTank tank) {
            output.put("capacity", tank.getCapacity());
            convertFluidStack(tank.getFluid(), output);
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

    private static void convertFluidStack(final FluidStack stack, final Map<Object, Object> output) {
        output.put("amount", stack.getAmount());
        output.put("hasTag", !stack.isComponentsPatchEmpty());
        if (!stack.isEmpty()) {
            final ResourceLocation id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
            output.put("name", id == null ? "minecraft:empty" : id.toString());
            output.put("label", stack.getHoverName().getString());
        }
    }

    private static int totalCapacity(final IFluidHandler handler) {
        int capacity = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            capacity += handler.getTankCapacity(tank);
        }
        return capacity;
    }

    private static Map<Object, Object> fluidTankMap(final IFluidHandler handler, final int tank) {
        final Map<Object, Object> output = new LinkedHashMap<>();
        output.put("capacity", handler.getTankCapacity(tank));
        convertFluidStack(handler.getFluidInTank(tank), output);
        return output;
    }

    private static void convertItemStackCustomData(final ItemStack stack, final Map<Object, Object> output) {
        if (!stack.has(DataComponents.CUSTOM_DATA)) {
            return;
        }
        final CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains("advDmg", Tag.TAG_INT)) {
            output.put("customDamage", tag.getInt("advDmg"));
        }
        if (tag.contains("Energy", Tag.TAG_INT)) {
            output.put("Energy", tag.getInt("Energy"));
        }
        if (ModSettings.allowItemStackNbtTags()) {
            output.put("tag", saveTag(tag));
        }
    }

    private static void convertItemStackLore(final ItemStack stack, final Map<Object, Object> output) {
        final ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            output.put("lore", lore.lines().stream()
                .map(Component::getString)
                .collect(Collectors.joining("\n")));
        }
    }

    private static void convertItemStackEnchantments(final ItemStack stack, final Map<Object, Object> output) {
        final ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (enchantments.isEmpty()) {
            return;
        }
        output.put("enchantments", enchantments.entrySet().stream()
            .map(entry -> enchantmentMap(entry.getKey(), entry.getIntValue()))
            .toArray());
    }

    private static Map<String, Object> enchantmentMap(final Holder<Enchantment> enchantment, final int level) {
        final Map<String, Object> output = new LinkedHashMap<>();
        output.put("name", enchantment.unwrapKey()
            .map(key -> key.location().toString())
            .orElse(enchantment.value().toString()));
        output.put("label", Enchantment.getFullname(enchantment, level).getString());
        output.put("level", level);
        return output;
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

    private static byte[] saveTag(final CompoundTag tag) {
        try {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compress item stack custom data.", e);
        }
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
