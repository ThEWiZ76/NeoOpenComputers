package li.cil.oc.common.item;

import li.cil.oc.api.API;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.concurrent.Callable;

public class FloppyItem extends Item implements DriverItem {
    public FloppyItem(final Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(final ItemStack stack) {
        final CompoundTag data = dataTag(stack);
        if (data.contains(ItemRegistry.FLOPPY_LABEL_TAG)) {
            return Component.literal(data.getString(ItemRegistry.FLOPPY_LABEL_TAG));
        }
        return super.getName(stack);
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        return stack != null && stack.getItem() == this;
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        if (!(API.items instanceof ItemRegistry registry)) {
            return null;
        }
        final Callable<li.cil.oc.api.fs.FileSystem> factory = registry.floppyFactory(stack);
        if (factory == null) {
            return null;
        }
        try {
            final li.cil.oc.api.fs.FileSystem fileSystem = factory.call();
            if (fileSystem == null) {
                return null;
            }
            final String label = dataTag(stack).getString(ItemRegistry.FLOPPY_LABEL_TAG);
            final ManagedEnvironment environment = FileSystem.asManagedEnvironment(fileSystem, label.isEmpty() ? null : label, host, null);
            if (environment != null && environment.node() instanceof li.cil.oc.api.network.Component component) {
                component.setVisibility(Visibility.Network);
            }
            return environment;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.Floppy;
    }

    @Override
    public int tier(final ItemStack stack) {
        return 0;
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        if (stack == null) {
            return new CompoundTag();
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? new CompoundTag() : customData.copyTag();
    }
}
