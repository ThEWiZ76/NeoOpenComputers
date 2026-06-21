package li.cil.oc.common.item;

import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.DatabaseEnvironment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

public class DatabaseUpgradeItem extends Item implements li.cil.oc.api.driver.DriverItem, HostAware {
    private static final int[] SLOTS_BY_TIER = {9, 25, 81};
    private static final String DATABASE_DATA_TAG = "oc:database";
    private final int tier;

    public DatabaseUpgradeItem(final Properties properties) {
        this(properties, 0);
    }

    public DatabaseUpgradeItem(final Properties properties, final int tier) {
        super(properties);
        this.tier = Math.max(0, Math.min(SLOTS_BY_TIER.length - 1, tier));
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        return stack != null && stack.getItem() == this;
    }

    @Override
    public boolean worksWith(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
        return worksWith(stack);
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        return createEnvironment(tier(stack), dataTag(stack), saved -> writeDataTag(stack, saved), host);
    }

    static ManagedEnvironment createEnvironment(final CompoundTag data, final Consumer<CompoundTag> saveData, final EnvironmentHost host) {
        return createEnvironment(0, data, saveData, host);
    }

    static ManagedEnvironment createEnvironment(final int tier, final CompoundTag data, final Consumer<CompoundTag> saveData, final EnvironmentHost host) {
        final DatabaseEnvironment environment = new DatabaseEnvironment(slotsForTier(tier), saveData);
        if (data != null && !data.isEmpty()) {
            environment.load(data);
        }
        return environment;
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int tier(final ItemStack stack) {
        return tier;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        if (stack == null) {
            return new CompoundTag();
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return new CompoundTag();
        }
        return customData.copyTag().getCompound(DATABASE_DATA_TAG);
    }

    private static void writeDataTag(final ItemStack stack, final CompoundTag data) {
        if (stack == null) {
            return;
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag root = customData == null ? new CompoundTag() : customData.copyTag();
        root.put(DATABASE_DATA_TAG, data.copy());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static int slotsForTier(final int tier) {
        final int clampedTier = Math.max(0, Math.min(SLOTS_BY_TIER.length - 1, tier));
        return SLOTS_BY_TIER[clampedTier];
    }
}
