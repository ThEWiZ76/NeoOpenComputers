package li.cil.oc.common;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.random.RandomGenerator;

/** Descriptor weights without allocating one stack reference per weight unit. */
final class LootDiskPool {
    record Entry(ItemStack stack, int weight) {
        Entry {
            stack = stack.copy();
            if (!stack.isEmpty()) stack.setCount(1);
            weight = Math.max(0, weight);
        }
        @Override public ItemStack stack() { return stack.copy(); }
    }

    private final List<Entry> entries;
    private final long weight;

    LootDiskPool(List<Entry> entries) {
        this.entries = entries.stream().filter(entry -> entry.weight() > 0 && !entry.stack().isEmpty())
            .map(entry -> new Entry(entry.stack(), entry.weight())).toList();
        this.weight = this.entries.stream().mapToLong(Entry::weight).sum();
    }

    long weight() { return weight; }

    ItemStack sample(RandomSource random) {
        if (weight == 0) return ItemStack.EMPTY;
        final long ticket = weight <= Integer.MAX_VALUE ? random.nextInt((int) weight) : ((RandomGenerator) random::nextLong).nextLong(weight);
        return at(ticket);
    }

    ItemStack at(long ticket) {
        if (ticket < 0 || ticket >= weight) throw new IllegalArgumentException("invalid loot ticket");
        for (final var entry : entries) {
            if (ticket < entry.weight()) return entry.stack();
            ticket -= entry.weight();
        }
        throw new IllegalStateException("loot weight changed");
    }
}
