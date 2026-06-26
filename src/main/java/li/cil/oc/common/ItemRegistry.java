package li.cil.oc.common;

import li.cil.oc.api.detail.ItemAPI;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.fs.FileSystem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public final class ItemRegistry implements ItemAPI {
    public static final String FLOPPY_COLOR_TAG = "oc:color";
    public static final String FLOPPY_FACTORY_ID_TAG = "oc:factory";
    public static final String FLOPPY_LABEL_TAG = "oc:label";
    public static final String FLOPPY_RECIPE_CYCLING_TAG = "oc:recipeCycling";
    public static final String EEPROM_CODE_TAG = "oc:code";
    public static final String EEPROM_DATA_SECTION_TAG = "oc:dataSection";
    public static final String EEPROM_DATA_TAG = "oc:data";
    public static final String EEPROM_LABEL_TAG = "oc:label";
    public static final String EEPROM_READONLY_TAG = "oc:readonly";

    private final Map<String, ItemInfo> infosByName = new LinkedHashMap<>();
    private final Map<String, Callable<FileSystem>> floppyFactoriesById = new LinkedHashMap<>();
    private final List<Supplier<ItemStack>> creativeStackSuppliers = new ArrayList<>();

    RegisteredItemInfo register(final String name, final Block block, final Item item) {
        final RegisteredItemInfo info = new RegisteredItemInfo(name, block, item);
        infosByName.put(name, info);
        return info;
    }

    ItemInfo registerStack(final String name, final Supplier<ItemStack> stackSupplier) {
        final ItemInfo info = new RegisteredStackInfo(name, stackSupplier);
        infosByName.put(name, info);
        return info;
    }

    @Override
    public ItemInfo get(final String name) {
        return infosByName.get(name);
    }

    @Override
    public ItemInfo get(final ItemStack stack) {
        if (stack == null) {
            return null;
        }
        final Item item = stack.getItem();
        return infosByName.values().stream()
            .filter(info -> info.item() == item)
            .findFirst()
            .orElse(null);
    }

    @Override
    public ItemStack registerFloppy(final String name, final DyeColor color, final Callable<li.cil.oc.api.fs.FileSystem> factory, final boolean doRecipeCycling) {
        return registerFloppy(name, color, UUID.randomUUID().toString(), factory, doRecipeCycling);
    }

    ItemStack registerFloppy(final String name, final DyeColor color, final String factoryId, final Callable<li.cil.oc.api.fs.FileSystem> factory, final boolean doRecipeCycling) {
        if (factory == null) {
            return null;
        }
        registerFloppyFactory(factoryId, factory);
        final ItemInfo info = get(ModContentIds.FLOPPY);
        if (info == null) {
            return null;
        }
        final ItemStack stack = info.createItemStack(1);
        if (stack == null) {
            return null;
        }
        final CompoundTag floppyData = createFloppyData(name, color, factoryId, doRecipeCycling);
        if (name != null) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(floppyData));
        rememberCreativeStackSupplier(stack::copy);
        return stack;
    }

    @Override
    public ItemStack registerEEPROM(final String name, final byte[] code, final byte[] data, final boolean readonly) {
        final ItemInfo info = get(ModContentIds.EEPROM);
        if (info == null) {
            return null;
        }
        final ItemStack stack = info.createItemStack(1);
        if (stack == null) {
            return null;
        }
        final CompoundTag eepromData = createEepromData(name, code, data, readonly);
        if (name != null) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }

        final CompoundTag root = new CompoundTag();
        root.put(EEPROM_DATA_TAG, eepromData);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        rememberCreativeStackSupplier(stack::copy);
        return stack;
    }

    void rememberCreativeStackSupplier(final Supplier<ItemStack> stackSupplier) {
        if (stackSupplier != null) {
            creativeStackSuppliers.add(stackSupplier);
        }
    }

    List<Supplier<ItemStack>> creativeStackSuppliers() {
        return List.copyOf(creativeStackSuppliers);
    }

    static CompoundTag createEepromData(final String name, final byte[] code, final byte[] data, final boolean readonly) {
        final CompoundTag eepromData = new CompoundTag();
        if (name != null) {
            eepromData.putString(EEPROM_LABEL_TAG, name);
        }
        eepromData.putByteArray(EEPROM_CODE_TAG, copyBytes(code));
        eepromData.putByteArray(EEPROM_DATA_SECTION_TAG, copyBytes(data));
        eepromData.putBoolean(EEPROM_READONLY_TAG, readonly);
        return eepromData;
    }

    public Callable<FileSystem> floppyFactory(final ItemStack stack) {
        if (stack == null) {
            return null;
        }
        final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        final String factoryId = data.copyTag().getString(FLOPPY_FACTORY_ID_TAG);
        return floppyFactory(factoryId);
    }

    void registerFloppyFactory(final String factoryId, final Callable<FileSystem> factory) {
        if (factoryId != null && !factoryId.isEmpty() && factory != null) {
            floppyFactoriesById.put(factoryId, factory);
        }
    }

    public Callable<FileSystem> floppyFactory(final String factoryId) {
        return factoryId == null || factoryId.isEmpty() ? null : floppyFactoriesById.get(factoryId);
    }

    static CompoundTag createFloppyData(final String name, final DyeColor color, final String factoryId, final boolean doRecipeCycling) {
        final CompoundTag floppyData = new CompoundTag();
        if (name != null) {
            floppyData.putString(FLOPPY_LABEL_TAG, name);
        }
        if (color != null) {
            floppyData.putString(FLOPPY_COLOR_TAG, color.getName());
        }
        if (factoryId != null) {
            floppyData.putString(FLOPPY_FACTORY_ID_TAG, factoryId);
        }
        floppyData.putBoolean(FLOPPY_RECIPE_CYCLING_TAG, doRecipeCycling);
        return floppyData;
    }

    private static byte[] copyBytes(final byte[] value) {
        return value == null ? new byte[0] : Arrays.copyOf(value, value.length);
    }

    private record RegisteredItemInfo(String name, Block block, Item item) implements ItemInfo {
        @Override
        public ItemStack createItemStack(final int size) {
            if (item == null) {
                return null;
            }
            return new ItemStack(item, size);
        }
    }

    private record RegisteredStackInfo(String name, Supplier<ItemStack> stackSupplier) implements ItemInfo {
        @Override
        public Block block() {
            return null;
        }

        @Override
        public Item item() {
            final ItemStack stack = createItemStack(1);
            return stack == null || stack.isEmpty() ? null : stack.getItem();
        }

        @Override
        public ItemStack createItemStack(final int size) {
            if (stackSupplier == null) {
                return null;
            }
            final ItemStack stack = stackSupplier.get();
            if (stack == null) {
                return null;
            }
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            final ItemStack copy = stack.copy();
            copy.setCount(size);
            return copy;
        }
    }
}
