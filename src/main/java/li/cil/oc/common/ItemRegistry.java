package li.cil.oc.common;

import li.cil.oc.api.detail.ItemAPI;
import li.cil.oc.api.detail.ItemInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public final class ItemRegistry implements ItemAPI {
    public static final String EEPROM_CODE_TAG = "oc:code";
    public static final String EEPROM_DATA_SECTION_TAG = "oc:dataSection";
    public static final String EEPROM_DATA_TAG = "oc:data";
    public static final String EEPROM_LABEL_TAG = "oc:label";
    public static final String EEPROM_READONLY_TAG = "oc:readonly";

    private final Map<String, RegisteredItemInfo> infosByName = new LinkedHashMap<>();

    RegisteredItemInfo register(final String name, final Block block, final Item item) {
        final RegisteredItemInfo info = new RegisteredItemInfo(name, block, item);
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
        return null;
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
        return stack;
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
}
