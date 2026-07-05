package li.cil.oc.common.item;

import li.cil.oc.api.internal.Tiered;
import li.cil.oc.common.ModEntities;
import li.cil.oc.common.entity.DroneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

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

    public List<ItemStack> componentStacks(final ItemStack stack) {
        final List<ItemStack> result = new ArrayList<>();
        final ListTag components = components(stack);
        for (int index = 0; index < components.size(); index++) {
            final CompoundTag entry = components.getCompound(index);
            final ItemStack component = decodeStack(entry.getCompound(STACK_TAG));
            if (!component.isEmpty()) {
                result.add(component);
            }
        }
        return result;
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final ItemStack stack = context.getItemInHand();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        final DroneEntity drone = ModEntities.DRONE.get().create(level);
        if (drone == null) {
            return InteractionResult.FAIL;
        }
        final BlockPos clicked = context.getClickedPos();
        final Vec3 position = Vec3.atCenterOf(clicked.relative(context.getClickedFace())).add(0D, -0.3125D, 0D);
        drone.moveTo(position.x, position.y, position.z, context.getHorizontalDirection().toYRot(), 0F);
        drone.loadFromItemStack(stack, context.getPlayer());
        level.addFreshEntity(drone);
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
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

    private static ItemStack decodeStack(final CompoundTag tag) {
        return ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, tag)
            .result()
            .orElse(ItemStack.EMPTY);
    }
}
