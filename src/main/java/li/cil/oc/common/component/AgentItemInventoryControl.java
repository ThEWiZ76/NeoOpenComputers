package li.cil.oc.common.component;

import li.cil.oc.api.Driver;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.machine.Arguments;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import java.util.function.BiFunction;

/** Item-provider and capability transfers from upstream ItemInventoryControl. */
public final class AgentItemInventoryControl {
    private AgentItemInventoryControl() { }

    public static Object[] size(Agent agent, Arguments args) {
        final int carrierSlot = internalSlot(agent, args.checkInteger(0));
        final var handler = Driver.itemHandlerFor(agent.mainInventory().getItem(carrierSlot), agent.player());
        return handler == null ? missing() : new Object[]{handler.getSlots()};
    }

    public static Object[] transfer(Agent agent, Arguments args, boolean intoItem) {
        final var inventory = agent.mainInventory();
        final int carrierSlot = internalSlot(agent, args.checkInteger(0));
        final var carrier = inventory.getItem(carrierSlot);
        final var item = Driver.itemHandlerFor(carrier, agent.player());
        if (item == null) return missing();
        final int slot = args.checkInteger(1) - 1;
        if (slot < 0 || slot >= item.getSlots()) throw new IllegalArgumentException("invalid slot");
        final int count = args.count() <= 2 || args.checkAny(2) == null ? 64 : Math.clamp(args.checkInteger(2), 0, 64);
        final var internal = new InvWrapper(inventory);
        final int selected = agent.selectedSlot();
        if (count == 0 || intoItem && selected == carrierSlot) return new Object[]{0};
        if (selected < 0 || selected >= inventory.getContainerSize()) return new Object[]{0};
        final IItemHandler source = intoItem ? internal : item;
        final int sourceSlot = intoItem ? selected : slot;
        final BiFunction<ItemStack, Boolean, ItemStack> insert = intoItem
            ? (stack, simulate) -> item.insertItem(slot, stack, simulate)
            : (stack, simulate) -> insertCargo(internal, selected, carrierSlot, stack, simulate);
        final var previewItem = carrier.getCount() > 1 ? Driver.itemHandlerFor(carrier.copyWithCount(1), agent.player()) : item;
        if (previewItem == null) return new Object[]{0};
        final var preview = (intoItem ? internal : previewItem).extractItem(sourceSlot, count, true);
        final int accepted = preview.getCount() - (intoItem ? previewItem.insertItem(slot, preview, true)
            : insertCargo(internal, selected, carrierSlot, preview, true)).getCount();
        if (accepted <= 0) return new Object[]{0};
        // A stack of identical container items shares one component map. Mutate
        // one carrier only; otherwise splitting the stack would copy new contents.
        if (carrier.getCount() > 1) {
            int free = -1;
            for (int index = 0; index < inventory.getContainerSize(); index++) {
                if (index != carrierSlot && index != selected && inventory.getItem(index).isEmpty()) { free = index; break; }
            }
            if (free < 0) return new Object[]{0};
            inventory.setItem(free, carrier.copyWithCount(carrier.getCount() - 1));
            carrier.setCount(1);
        }
        final var extracted = source.extractItem(sourceSlot, accepted, false);
        final var remainder = insert.apply(extracted, false);
        final int moved = extracted.getCount() - remainder.getCount();
        if (!remainder.isEmpty()) {
            final var returned = ItemHandlerHelper.insertItemStacked(source,
                source.insertItem(sourceSlot, remainder, false), false);
            if (!returned.isEmpty()) {
                final var cargoRemainder = insertCargo(internal, selected, carrierSlot, returned, false);
                if (!cargoRemainder.isEmpty()) agent.world().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                    agent.world(), agent.xPosition(), agent.yPosition(), agent.zPosition(), cargoRemainder));
            }
        }
        // Capability providers update the carrier's components. Reassign it as well
        // so hosts whose inventory returns copies receive the final item state.
        inventory.setItem(carrierSlot, carrier);
        inventory.setChanged();
        return new Object[]{moved};
    }

    private static ItemStack insertCargo(IItemHandler cargo, int selected, int carrier, ItemStack stack, boolean simulate) {
        ItemStack remainder = stack.copy();
        for (int offset = 0; offset < cargo.getSlots() && !remainder.isEmpty(); offset++) {
            final int slot = (selected + offset) % cargo.getSlots();
            if (slot != carrier) remainder = cargo.insertItem(slot, remainder, simulate);
        }
        return remainder;
    }

    private static int internalSlot(Agent agent, int slot) {
        if (slot < 1 || slot > agent.mainInventory().getContainerSize()) throw new IllegalArgumentException("invalid slot");
        return slot - 1;
    }

    private static Object[] missing() { return new Object[]{0, "no item inventory"}; }
}
