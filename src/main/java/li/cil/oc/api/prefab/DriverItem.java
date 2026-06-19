package li.cil.oc.api.prefab;

import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public abstract class DriverItem implements li.cil.oc.api.driver.DriverItem {
    private static final String TAG_DATA = "oc:data";

    protected final ItemStack[] items;

    protected DriverItem(final ItemStack... items) {
        this.items = items == null ? new ItemStack[0] : items.clone();
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        for (final ItemStack item : items) {
            if (item != null && !item.isEmpty() && ItemStack.isSameItem(item, stack)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int tier(final ItemStack stack) {
        return 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public CompoundTag dataTag(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }

        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            final CompoundTag root = new CompoundTag();
            root.put(TAG_DATA, new CompoundTag());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
            data = stack.get(DataComponents.CUSTOM_DATA);
        }

        final CompoundTag root = data.getUnsafe();
        if (!root.contains(TAG_DATA, Tag.TAG_COMPOUND)) {
            root.put(TAG_DATA, new CompoundTag());
        }
        return root.getCompound(TAG_DATA);
    }

    protected boolean isAdapter(final Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Adapter.class.isAssignableFrom(host);
    }

    protected boolean isComputer(final Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Case.class.isAssignableFrom(host);
    }

    protected boolean isRobot(final Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Robot.class.isAssignableFrom(host);
    }

    protected boolean isRotatable(final Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Rotatable.class.isAssignableFrom(host);
    }

    protected boolean isServer(final Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Server.class.isAssignableFrom(host);
    }

    protected boolean isTablet(final Class<? extends EnvironmentHost> host) {
        return li.cil.oc.api.internal.Tablet.class.isAssignableFrom(host);
    }
}
