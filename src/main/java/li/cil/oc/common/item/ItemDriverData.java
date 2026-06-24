package li.cil.oc.common.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ItemDriverData {
    private static final String TAG_DATA = "oc:data";

    private ItemDriverData() {
    }

    @SuppressWarnings("deprecation")
    public static CompoundTag dataTag(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            final CompoundTag root = new CompoundTag();
            root.put(TAG_DATA, new CompoundTag());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
            customData = stack.get(DataComponents.CUSTOM_DATA);
        }

        final CompoundTag root = customData.getUnsafe();
        if (!root.contains(TAG_DATA, Tag.TAG_COMPOUND)) {
            root.put(TAG_DATA, new CompoundTag());
        }
        return root.getCompound(TAG_DATA);
    }
}
