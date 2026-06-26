package li.cil.oc.common.item;

import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.component.EepromEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.LevelReader;

public class EepromItem extends Item implements DriverItem {
    private static final String EEPROM_SLOT = "eeprom";

    public EepromItem(final Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(final ItemStack stack) {
        final CompoundTag data = dataTag(stack);
        if (data.contains(ItemRegistry.EEPROM_LABEL_TAG)) {
            return Component.literal(data.getString(ItemRegistry.EEPROM_LABEL_TAG));
        }
        return super.getName(stack);
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        return stack != null && stack.getItem() == this;
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        if (ItemDriverData.isClientSide(host)) {
            return null;
        }
        final CompoundTag data = dataTag(stack);
        return new EepromEnvironment(data, () -> writeDataTag(stack, data));
    }

    @Override
    public String slot(final ItemStack stack) {
        return EEPROM_SLOT;
    }

    @Override
    public int tier(final ItemStack stack) {
        return 0;
    }

    @Override
    public boolean doesSneakBypassUse(final ItemStack stack, final LevelReader level, final BlockPos pos, final Player player) {
        return true;
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
        return customData.getUnsafe().getCompound(ItemRegistry.EEPROM_DATA_TAG);
    }

    private static void writeDataTag(final ItemStack stack, final CompoundTag data) {
        if (stack == null) {
            return;
        }
        final CompoundTag root = new CompoundTag();
        root.put(ItemRegistry.EEPROM_DATA_TAG, data.copy());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
}
