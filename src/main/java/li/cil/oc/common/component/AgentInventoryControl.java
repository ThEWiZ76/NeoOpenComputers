package li.cil.oc.common.component;

import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.machine.Arguments;
import net.minecraft.world.item.ItemStack;

/** Internal inventory operations from upstream InventoryControl. */
public final class AgentInventoryControl {
    private AgentInventoryControl() { }

    private static int slot(Agent agent, Arguments args, boolean optional) {
        if (optional && (args.count() == 0 || args.checkAny(0) == null)) return agent.selectedSlot();
        final int slot = args.checkInteger(0) - 1;
        if (slot < 0 || slot >= agent.mainInventory().getContainerSize()) throw new IllegalArgumentException("invalid slot");
        return slot;
    }

    public static Object[] select(Agent agent, Arguments args) {
        agent.setSelectedSlot(slot(agent, args, true));
        return new Object[]{agent.selectedSlot() + 1};
    }

    public static Object[] count(Agent agent, Arguments args) {
        return new Object[]{agent.mainInventory().getItem(slot(agent, args, true)).getCount()};
    }

    public static Object[] space(Agent agent, Arguments args) {
        final var inventory = agent.mainInventory();
        final var stack = inventory.getItem(slot(agent, args, true));
        return new Object[]{stack.isEmpty() ? inventory.getMaxStackSize()
            : Math.min(inventory.getMaxStackSize(), stack.getMaxStackSize()) - stack.getCount()};
    }

    public static Object[] compare(Agent agent, Arguments args) {
        final var inventory = agent.mainInventory();
        final var from = inventory.getItem(agent.selectedSlot());
        final var to = inventory.getItem(slot(agent, args, false));
        if (from.isEmpty() || to.isEmpty()) return new Object[]{from.isEmpty() && to.isEmpty()};
        return new Object[]{args.optBoolean(1, false) ? ItemStack.isSameItemSameComponents(from, to) : ItemStack.isSameItem(from, to)};
    }

    public static Object[] transfer(Agent agent, Arguments args) {
        final var inventory = agent.mainInventory();
        final int target = slot(agent, args, false);
        final int selected = agent.selectedSlot();
        final int count = Math.clamp(args.optInteger(1, 64), 0, 64);
        if (target == selected || count == 0) return new Object[]{true};
        final var from = inventory.getItem(selected);
        final var to = inventory.getItem(target);
        if (from.isEmpty()) return new Object[]{false};
        if (!to.isEmpty() && !ItemStack.isSameItemSameComponents(from, to)) {
            if (count < from.getCount()) return new Object[]{false};
            inventory.setItem(selected, to);
            inventory.setItem(target, from);
        } else {
            final int limit = Math.min(inventory.getMaxStackSize(), from.getMaxStackSize());
            final int moved = Math.min(count, Math.min(from.getCount(), limit - to.getCount()));
            if (moved <= 0) return new Object[]{false};
            if (to.isEmpty()) inventory.setItem(target, from.copyWithCount(moved));
            else to.grow(moved);
            from.shrink(moved);
            if (from.isEmpty()) inventory.setItem(selected, ItemStack.EMPTY);
        }
        inventory.setChanged();
        return new Object[]{true};
    }
}
