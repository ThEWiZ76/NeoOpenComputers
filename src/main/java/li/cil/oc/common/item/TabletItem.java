package li.cil.oc.common.item;

import li.cil.oc.api.driver.item.Chargeable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class TabletItem extends Item implements Chargeable {
    private static final String DATA_TAG = "oc:tablet";
    private static final String ENERGY_TAG = "energy";
    private static final String MAX_ENERGY_TAG = "maxEnergy";

    public TabletItem(final Properties properties) {
        super(properties);
    }

    @Override
    public boolean canCharge(final ItemStack stack) {
        return stack != null && stack.getItem() == this;
    }

    @Override
    public double charge(final ItemStack stack, final double amount, final boolean simulate) {
        if (!canCharge(stack)) {
            return 0D;
        }
        if (amount < 0D) {
            return amount;
        }
        final double stored = getCharge(stack);
        final double accepted = Math.min(amount, Math.max(0D, maxCharge(stack) - stored));
        if (!simulate && accepted > 0D) {
            setCharge(stack, stored + accepted);
        }
        return accepted;
    }

    @Override
    public boolean isBarVisible(final ItemStack stack) {
        return canCharge(stack);
    }

    @Override
    public int getBarWidth(final ItemStack stack) {
        final double maxCharge = maxCharge(stack);
        if (maxCharge <= 0D) {
            return 0;
        }
        return Mth.clamp((int) Math.round(13D * getCharge(stack) / maxCharge), 0, 13);
    }

    @Override
    public int getBarColor(final ItemStack stack) {
        final double maxCharge = maxCharge(stack);
        if (maxCharge <= 0D) {
            return 0xFF0000;
        }
        return Mth.hsvToRgb(Math.max(0F, (float) (getCharge(stack) / maxCharge) / 3F), 1F, 1F);
    }

    public double getCharge(final ItemStack stack) {
        return Math.max(0D, Math.min(readData(stack).getDouble(ENERGY_TAG), maxCharge(stack)));
    }

    public void setCharge(final ItemStack stack, final double amount) {
        final CompoundTag data = readData(stack);
        data.putDouble(ENERGY_TAG, Math.max(0D, Math.min(amount, maxCharge(stack))));
        writeData(stack, data);
    }

    public double maxCharge(final ItemStack stack) {
        return Math.max(0D, readData(stack).getDouble(MAX_ENERGY_TAG));
    }

    public void setMaxCharge(final ItemStack stack, final double amount) {
        final CompoundTag data = readData(stack);
        final double maxCharge = Math.max(0D, amount);
        data.putDouble(MAX_ENERGY_TAG, maxCharge);
        data.putDouble(ENERGY_TAG, Math.max(0D, Math.min(data.getDouble(ENERGY_TAG), maxCharge)));
        writeData(stack, data);
    }

    private static CompoundTag readData(final ItemStack stack) {
        if (stack == null) {
            return new CompoundTag();
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return new CompoundTag();
        }
        return customData.copyTag().getCompound(DATA_TAG);
    }

    private static void writeData(final ItemStack stack, final CompoundTag data) {
        if (stack == null) {
            return;
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag root = customData == null ? new CompoundTag() : customData.copyTag();
        root.put(DATA_TAG, data.copy());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
}
