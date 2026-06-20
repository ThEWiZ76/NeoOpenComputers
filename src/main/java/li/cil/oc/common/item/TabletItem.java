package li.cil.oc.common.item;

import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.api.internal.Tiered;
import li.cil.oc.common.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class TabletItem extends Item implements Chargeable {
    public static final int COMPONENT_SLOTS = 32;
    public static final double DEFAULT_MAX_CHARGE = 10000D;

    private static final String DATA_TAG = "oc:tablet";
    private static final String ENERGY_TAG = "energy";
    private static final String MAX_ENERGY_TAG = "maxEnergy";
    private static final String TIER_TAG = "tier";
    private static final String RUNNING_TAG = "running";
    private static final String CONTAINER_TAG = "container";
    private static final String COMPONENTS_TAG = "components";
    private static final String SLOT_TAG = "slot";
    private static final String STACK_TAG = "stack";

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

    public ItemStack assembleFromCase(final ItemStack caseStack, final ItemStack container, final ItemStack... components) {
        final ItemStack stack = new ItemStack(this);
        setTier(stack, caseTier(caseStack));
        setContainer(stack, container);
        setMaxCharge(stack, DEFAULT_MAX_CHARGE);
        setCharge(stack, DEFAULT_MAX_CHARGE);
        setComponent(stack, 0, new ItemStack(ModItems.SCREEN_TIER1.get()));
        if (components != null) {
            for (int index = 0; index < components.length && index + 1 < COMPONENT_SLOTS; index++) {
                setComponent(stack, index + 1, components[index]);
            }
        }
        return stack;
    }

    public int tier(final ItemStack stack) {
        return Math.max(0, readData(stack).getInt(TIER_TAG));
    }

    public void setTier(final ItemStack stack, final int tier) {
        final CompoundTag data = readData(stack);
        data.putInt(TIER_TAG, Math.max(0, tier));
        writeData(stack, data);
    }

    public boolean isRunning(final ItemStack stack) {
        return readData(stack).getBoolean(RUNNING_TAG);
    }

    public void setRunning(final ItemStack stack, final boolean running) {
        final CompoundTag data = readData(stack);
        data.putBoolean(RUNNING_TAG, running);
        writeData(stack, data);
    }

    public ItemStack getContainer(final ItemStack stack) {
        return decodeStack(readData(stack).getCompound(CONTAINER_TAG));
    }

    public void setContainer(final ItemStack stack, final ItemStack container) {
        final CompoundTag data = readData(stack);
        if (container == null || container.isEmpty()) {
            data.remove(CONTAINER_TAG);
        } else {
            data.put(CONTAINER_TAG, encodeStack(container));
        }
        writeData(stack, data);
    }

    public ItemStack getComponent(final ItemStack stack, final int slot) {
        if (slot < 0 || slot >= COMPONENT_SLOTS) {
            return ItemStack.EMPTY;
        }
        final ListTag components = readData(stack).getList(COMPONENTS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < components.size(); index++) {
            final CompoundTag entry = components.getCompound(index);
            if (entry.getInt(SLOT_TAG) == slot) {
                return decodeStack(entry.getCompound(STACK_TAG));
            }
        }
        return ItemStack.EMPTY;
    }

    public void setComponent(final ItemStack stack, final int slot, final ItemStack component) {
        if (slot < 0 || slot >= COMPONENT_SLOTS) {
            return;
        }
        final CompoundTag data = readData(stack);
        final ListTag oldComponents = data.getList(COMPONENTS_TAG, Tag.TAG_COMPOUND);
        final ListTag newComponents = new ListTag();
        for (int index = 0; index < oldComponents.size(); index++) {
            final CompoundTag entry = oldComponents.getCompound(index);
            if (entry.getInt(SLOT_TAG) != slot) {
                newComponents.add(entry.copy());
            }
        }
        if (component != null && !component.isEmpty()) {
            final CompoundTag entry = new CompoundTag();
            entry.putInt(SLOT_TAG, slot);
            entry.put(STACK_TAG, encodeStack(component));
            newComponents.add(entry);
        }
        if (newComponents.isEmpty()) {
            data.remove(COMPONENTS_TAG);
        } else {
            data.put(COMPONENTS_TAG, newComponents);
        }
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

    private static CompoundTag encodeStack(final ItemStack stack) {
        return ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, stack)
            .result()
            .filter(tag -> tag instanceof CompoundTag)
            .map(tag -> (CompoundTag) tag)
            .orElseGet(CompoundTag::new);
    }

    private static ItemStack decodeStack(final CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(ItemStack.EMPTY);
    }

    private static int caseTier(final ItemStack caseStack) {
        if (caseStack != null && caseStack.getItem() instanceof Tiered tiered) {
            return tiered.tier();
        }
        return 0;
    }
}
