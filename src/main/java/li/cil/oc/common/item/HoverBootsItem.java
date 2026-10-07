package li.cil.oc.common.item;

import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class HoverBootsItem extends ArmorItem implements Chargeable {
    private static final String CHARGE_TAG = "oc:charge";

    public HoverBootsItem(final Properties properties) {
        super(ArmorMaterials.DIAMOND, Type.BOOTS, properties.setNoRepair().durability((int) Math.min(Integer.MAX_VALUE, ModSettings.hoverBootsBuffer())));
    }

    public ItemStack createChargedStack() {
        final var stack = new ItemStack(this);
        setCharge(stack, maxCharge(stack));
        return stack;
    }

    @Override
    public boolean onEntityItemUpdate(final ItemStack stack, final net.minecraft.world.entity.item.ItemEntity entity) {
        if (!entity.level().isClientSide && stack.has(DataComponents.DYED_COLOR)) {
            final var pos = entity.blockPosition();
            final var state = entity.level().getBlockState(pos);
            if (state.is(net.minecraft.world.level.block.Blocks.WATER_CAULDRON)) {
                stack.remove(DataComponents.DYED_COLOR);
                net.minecraft.world.level.block.LayeredCauldronBlock.lowerFillLevel(state, entity.level(), pos);
                return true;
            }
        }
        return false;
    }

    public double maxCharge(final ItemStack stack) {
        return ModSettings.hoverBootsBuffer();
    }

    public double getCharge(final ItemStack stack) {
        final var custom = stack.get(DataComponents.CUSTOM_DATA);
        final double stored = custom == null ? 0 : custom.copyTag().getDouble(CHARGE_TAG);
        return Double.isFinite(stored) ? Math.max(0, Math.min(maxCharge(stack), stored)) : 0;
    }

    public void setCharge(final ItemStack stack, final double value) {
        final var custom = stack.get(DataComponents.CUSTOM_DATA);
        final var tag = custom == null ? new CompoundTag() : custom.copyTag();
        tag.putDouble(CHARGE_TAG, Double.isFinite(value) ? Math.max(0, Math.min(maxCharge(stack), value)) : 0);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public boolean canCharge(final ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == this;
    }

    @Override
    public double charge(final ItemStack stack, final double amount, final boolean simulate) {
        if (!canCharge(stack) || !Double.isFinite(amount)) return 0;
        final double stored = getCharge(stack);
        // The port's Chargeable contract returns accepted energy, including a negative accepted discharge.
        final double accepted = amount >= 0 ? Math.min(amount, maxCharge(stack) - stored) : Math.max(amount, -stored);
        if (!simulate && accepted != 0) setCharge(stack, stored + accepted);
        return accepted;
    }

    @Override
    public void setDamage(final ItemStack stack, final int damage) {
        charge(stack, -Math.max(0, damage), false);
        stack.set(DataComponents.DAMAGE, 0);
    }

    @Override
    public <T extends net.minecraft.world.entity.LivingEntity> int damageItem(final ItemStack stack, final int amount,
            final T entity, final java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        if (entity == null || !entity.hasInfiniteMaterials()) charge(stack, -Math.max(0, amount), false);
        // Keep Minecraft's armor damage dispatch, but never reach its durability break threshold.
        return 0;
    }

    @Override
    public int getMaxDamage(final ItemStack stack) {
        return (int) Math.min(Integer.MAX_VALUE, maxCharge(stack));
    }

    @Override
    public boolean isDamageable(final ItemStack stack) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(final ItemStack stack, final ItemStack ingredient) {
        return false;
    }

    @Override
    public boolean isBarVisible(final ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(final ItemStack stack) {
        return (int) Math.round(13 * getCharge(stack) / maxCharge(stack));
    }

    @Override
    public int getBarColor(final ItemStack stack) {
        return Mth.hsvToRgb((float) (getCharge(stack) / maxCharge(stack) / 3), 1, 1);
    }
}
