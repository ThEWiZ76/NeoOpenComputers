package li.cil.oc.common.item;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.Tiered;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.ServerRackMountableEnvironment;
import li.cil.oc.common.menu.ServerRackMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class ServerItem extends Item implements DriverItem, HostAware, Tiered {
    private static final String RACK_MOUNTABLE_DATA_TAG = "oc:rackMountable";

    private final int tier;

    public ServerItem(final Properties properties, final int tier) {
        super(properties.stacksTo(1));
        this.tier = Math.max(0, Math.min(2, tier));
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == this;
    }

    @Override
    public boolean worksWith(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
        return worksWith(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand usedHand) {
        final ItemStack stack = player.getItemInHand(usedHand);
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                final CompoundTag data = dataTag(stack);
                final ServerRackMountableEnvironment server = new ServerRackMountableEnvironment(player, tier, data);
                player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, menuPlayer) -> new ServerRackMenu(containerId, playerInventory, server, stack),
                    ServerRackMenu.serverTitle()));
            }
            player.swing(usedHand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        if (host instanceof Rack rack) {
            return new ServerRackMountableEnvironment(rack, findSlot(rack, stack), tier);
        }
        return new ServerRackMountableEnvironment(null, -1, tier);
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.RackMountable;
    }

    @Override
    public int tier(final ItemStack stack) {
        return tier;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
            customData = stack.get(DataComponents.CUSTOM_DATA);
        }
        final CompoundTag root = customData.getUnsafe();
        if (root.contains(RACK_MOUNTABLE_DATA_TAG, CompoundTag.TAG_COMPOUND)) {
            final CompoundTag legacyData = root.getCompound(RACK_MOUNTABLE_DATA_TAG).copy();
            root.remove(RACK_MOUNTABLE_DATA_TAG);
            root.merge(legacyData);
        }
        return root;
    }

    private static int findSlot(final Rack rack, final ItemStack stack) {
        for (int slot = 0; slot < rack.getContainerSize(); slot++) {
            final ItemStack candidate = rack.getItem(slot);
            if (candidate == stack || ItemStack.matches(candidate, stack)) {
                return slot;
            }
        }
        return -1;
    }
}
