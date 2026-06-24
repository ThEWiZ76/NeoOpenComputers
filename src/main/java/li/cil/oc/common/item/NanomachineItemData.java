package li.cil.oc.common.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class NanomachineItemData {
    public static final String UUID_TAG = "oc:uuid";
    public static final String CONFIGURATION_TAG = "oc:configuration";

    private NanomachineItemData() {
    }

    public static void save(final CompoundTag root, final String uuid, final CompoundTag configuration) {
        if (root == null) {
            return;
        }
        root.putString(UUID_TAG, uuid == null ? "" : uuid);
        if (configuration != null && !configuration.isEmpty()) {
            root.put(CONFIGURATION_TAG, configuration.copy());
        }
    }

    public static String uuid(final CompoundTag root) {
        return root == null ? "" : root.getString(UUID_TAG);
    }

    public static boolean hasConfiguration(final CompoundTag root) {
        return root != null && root.contains(CONFIGURATION_TAG, CompoundTag.TAG_COMPOUND);
    }

    public static boolean hasConfiguration(final ItemStack stack) {
        return stack != null && !stack.isEmpty() && hasConfiguration(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
    }

    public static CompoundTag configuration(final CompoundTag root) {
        return hasConfiguration(root) ? root.getCompound(CONFIGURATION_TAG).copy() : new CompoundTag();
    }

    @SuppressWarnings("deprecation")
    public static CompoundTag dataTag(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
            customData = stack.get(DataComponents.CUSTOM_DATA);
        }
        return customData.getUnsafe();
    }
}
