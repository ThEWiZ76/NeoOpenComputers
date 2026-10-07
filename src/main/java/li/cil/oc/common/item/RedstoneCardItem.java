package li.cil.oc.common.item;

import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.RedstoneCardEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class RedstoneCardItem extends Item implements HostAware {
    private final int tier;

    public RedstoneCardItem(final Properties properties) {
        this(properties, 0);
    }

    public RedstoneCardItem(final Properties properties, final int tier) {
        super(properties);
        this.tier = Math.clamp(tier, 0, 1);
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
        if (ItemDriverData.isClientSide(host)) {
            return null;
        }
        if (!(host instanceof li.cil.oc.common.component.RedstoneControllerHost)) return null;
        // Both upstream tiers use vanilla redstone when no bundled/wireless integration is available.
        final var environment = new RedstoneCardEnvironment(host) {
            @Override
            public void save(final CompoundTag data) {
                super.save(data);
                ItemDriverData.writeDataTag(stack, data);
            }
        };
        environment.load(dataTag(stack));
        return environment;
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.Card;
    }

    @Override
    public int tier(final ItemStack stack) {
        return tier;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        return ItemDriverData.dataTag(stack);
    }
}
