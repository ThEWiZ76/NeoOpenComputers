package li.cil.oc.common.item;

import li.cil.oc.api.internal.Tiered;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class DroneItem extends Item implements Tiered {
    public static final String DATA_TAG = "oc:drone";
    public static final String TIER_TAG = "tier";
    public static final String COMPONENTS_TAG = "components";
    public static final String SLOT_TAG = "slot";
    public static final String STACK_TAG = "stack";

    public DroneItem(final Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public int tier() {
        return 0;
    }

    public int tier(final ItemStack stack) {
        return Math.max(0, readData(stack).getInt(TIER_TAG));
    }

    public ItemStack assembleFromCase(final ItemStack caseStack, final ItemStack... components) {
        final ItemStack stack = new ItemStack(this);
        final CompoundTag data = new CompoundTag();
        data.putInt(TIER_TAG, caseTier(caseStack));
        final ListTag componentTags = new ListTag();
        if (components != null) {
            for (int slot = 0; slot < components.length; slot++) {
                final ItemStack component = components[slot];
                if (component == null || component.isEmpty()) {
                    continue;
                }
                final CompoundTag entry = new CompoundTag();
                entry.putByte(SLOT_TAG, (byte) slot);
                entry.put(STACK_TAG, encodeStack(component));
                componentTags.add(entry);
            }
        }
        data.put(COMPONENTS_TAG, componentTags);
        writeData(stack, data);
        return stack;
    }

    public ListTag components(final ItemStack stack) {
        return readData(stack).getList(COMPONENTS_TAG, Tag.TAG_COMPOUND);
    }

    private static int caseTier(final ItemStack stack) {
        if (stack.getItem() instanceof DroneCaseItem item) {
            return item.tier();
        }
        return 0;
    }

    private static CompoundTag readData(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? new CompoundTag() : customData.getUnsafe().getCompound(DATA_TAG).copy();
    }

    private static void writeData(final ItemStack stack, final CompoundTag data) {
        final CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(DATA_TAG, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static CompoundTag encodeStack(final ItemStack stack) {
        return ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, stack)
            .result()
            .filter(CompoundTag.class::isInstance)
            .map(CompoundTag.class::cast)
            .orElseGet(CompoundTag::new);
    }
}
