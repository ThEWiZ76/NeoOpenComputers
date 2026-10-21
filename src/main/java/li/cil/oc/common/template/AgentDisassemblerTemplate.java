package li.cil.oc.common.template;

import com.mojang.serialization.DynamicOps;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.block.MicrocontrollerBlock;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/** Agent chassis and installed hardware are all ordinary paid disassembly parts. */
final class AgentDisassemblerTemplate implements DisassemblerTemplate {
    enum Kind { ROBOT, DRONE, MICROCONTROLLER }
    private final Kind kind;
    AgentDisassemblerTemplate(Kind kind) { this.kind = kind; }
    @Override public String name() { return kind.name().toLowerCase(Locale.ROOT); }
    @Override public boolean matches(ItemStack stack) {
        return switch (kind) {
            case ROBOT -> stack.is(ModItems.ROBOT.get());
            case DRONE -> stack.getItem() instanceof DroneItem;
            case MICROCONTROLLER -> stack.getItem() instanceof BlockItem item && item.getBlock() instanceof MicrocontrollerBlock;
        };
    }

    @Override public ItemStack[] disassemble(ItemStack stack) { return plan(stack, new ItemStack[0], null).queued(); }
    @Override public DisassemblyResult plan(ItemStack stack, ItemStack[] ingredients) { return plan(stack, ingredients, null); }

    @Override public DisassemblyResult plan(ItemStack stack, ItemStack[] ingredients, HolderLookup.Provider registries) {
        final var data = hardware(stack);
        if (data.chassis.isEmpty()) return DisassemblyResult.EMPTY;
        final var parts = new ArrayList<ItemStack>(); parts.add(data.chassis);
        final var slots = new ItemStack[data.capacity]; Arrays.fill(slots, ItemStack.EMPTY);
        final var occupied = new boolean[data.capacity];
        final DynamicOps<Tag> ops = registries == null ? NbtOps.INSTANCE : registries.createSerializationContext(NbtOps.INSTANCE);
        for (int index = 0; index < data.items.size(); index++) {
            final var entry = data.items.getCompound(index);
            final int slot = entry.getByte(data.nested ? DroneItem.SLOT_TAG : "Slot") & 255;
            // Preserve the assembled input when malformed or unavailable hardware cannot be recovered exactly.
            if (slot >= slots.length || occupied[slot]) return DisassemblyResult.EMPTY;
            final var parsed = ItemStack.OPTIONAL_CODEC.parse(ops, data.nested ? entry.getCompound(DroneItem.STACK_TAG) : entry).result();
            if (parsed.isEmpty()) return DisassemblyResult.EMPTY;
            occupied[slot] = true; slots[slot] = parsed.get();
        }
        for (var part : slots) if (!part.isEmpty()) parts.add(part);
        return new DisassemblyResult(parts.toArray(ItemStack[]::new), null);
    }

    private Hardware hardware(ItemStack stack) {
        if (kind == Kind.DRONE) {
            final var item = (DroneItem) stack.getItem(); final int tier = item.tier(stack);
            final var chassis = switch (tier) {
                case 0 -> new ItemStack(ModItems.DRONE_CASE_TIER1.get());
                case 1 -> new ItemStack(ModItems.DRONE_CASE_TIER2.get());
                case 2, 3 -> new ItemStack(ModItems.DRONE_CASE_CREATIVE.get());
                default -> ItemStack.EMPTY;
            };
            return new Hardware(chassis, item.components(stack), DroneEntity.slotCount(tier), true);
        }
        final var data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
        if (kind == Kind.ROBOT) {
            final int tier = data.getInt(RobotBlockEntity.TAG_TIER);
            final var chassis = switch (tier) {
                case 0 -> new ItemStack(ModBlocks.COMPUTER_CASE_TIER1.get());
                case 1 -> new ItemStack(ModBlocks.COMPUTER_CASE_TIER2.get());
                case 2 -> new ItemStack(ModBlocks.COMPUTER_CASE_TIER3.get());
                default -> ItemStack.EMPTY;
            };
            return new Hardware(chassis, data.getList(RobotBlockEntity.TAG_HARDWARE, Tag.TAG_COMPOUND), RobotBlockEntity.slotCount(tier), false);
        }
        final int tier = ((MicrocontrollerBlock) ((BlockItem) stack.getItem()).getBlock()).tier();
        final var chassis = switch (tier) {
            case 0 -> new ItemStack(ModItems.MICROCONTROLLER_CASE_TIER1.get());
            case 1 -> new ItemStack(ModItems.MICROCONTROLLER_CASE_TIER2.get());
            case 3 -> new ItemStack(ModItems.MICROCONTROLLER_CASE_CREATIVE.get());
            default -> ItemStack.EMPTY;
        };
        return new Hardware(chassis, data.getList("Items", Tag.TAG_COMPOUND), MicrocontrollerBlockEntity.slotCount(tier), false);
    }

    private record Hardware(ItemStack chassis, ListTag items, int capacity, boolean nested) { }
}
