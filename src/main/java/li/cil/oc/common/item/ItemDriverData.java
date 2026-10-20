package li.cil.oc.common.item;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.driver.DriverItem;
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

    public static void writeDataTag(final ItemStack stack, final CompoundTag data) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag root = customData == null ? new CompoundTag() : customData.copyTag();
        root.put(TAG_DATA, data.copy());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static CompoundTag driverDataTag(final DriverItem driver, final ItemStack stack) {
        final CompoundTag data = driver.dataTag(stack);
        return data == null ? dataTag(stack) : data;
    }

    public static boolean isClientSide(final EnvironmentHost host) {
        return host != null && host.world() != null && host.world().isClientSide;
    }

    public static void writeDriverDataTag(final DriverItem driver, final ItemStack stack, final CompoundTag data) {
        final var detached = stack.copy();
        final var original = detached.get(DataComponents.CUSTOM_DATA);
        if (original != null) detached.set(DataComponents.CUSTOM_DATA, CustomData.of(original.copyTag()));
        final var saved = data.copy();
        final var target = driverDataTag(driver, detached);
        for (final var key : java.util.Set.copyOf(target.getAllKeys())) target.remove(key);
        target.merge(saved);
        final var updated = detached.get(DataComponents.CUSTOM_DATA);
        if (updated != null) stack.set(DataComponents.CUSTOM_DATA, CustomData.of(updated.copyTag()));
    }
}
