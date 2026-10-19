package li.cil.oc.common.component;

import li.cil.oc.common.integration.ComputerCraftBundledRedstone;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.function.Consumer;

/** Server-tick input cache; direct Lua callbacks never query the world from a machine thread. */
public final class BundledRedstoneInputs {
    private static final String TAG_INPUTS = "oc:bundledRedstoneInputs";
    private final int[][] values = new int[6][16];

    public BundledRedstoneInputs() {
        for (int[] side : values) Arrays.fill(side, -1);
    }

    public synchronized int get(final Direction side, final int color) {
        return Math.max(0, values[side.get3DDataValue()][color]);
    }

    public synchronized void save(final CompoundTag tag) {
        final int[] saved = new int[96];
        for (int side = 0; side < 6; side++) System.arraycopy(values[side], 0, saved, side * 16, 16);
        tag.putIntArray(TAG_INPUTS, saved);
    }

    public synchronized void load(final CompoundTag tag) {
        final int[] saved = tag.getIntArray(TAG_INPUTS);
        for (int side = 0; side < 6; side++) {
            for (int color = 0; color < 16; color++) {
                final int index = side * 16 + color;
                values[side][color] = index < saved.length ? Math.clamp(saved[index], -1, 255) : -1;
            }
        }
    }

    public synchronized void update(final RedstoneControllerHost host, final Consumer<Change> changed) {
        boolean dirty = false;
        for (Direction side : Direction.values()) {
            final int mask = ComputerCraftBundledRedstone.input(host, side);
            for (int color = 0; color < 16; color++) {
                final int oldValue = values[side.get3DDataValue()][color];
                final int newValue = (mask & (1 << color)) == 0 ? 0 : 15;
                values[side.get3DDataValue()][color] = newValue;
                dirty |= oldValue != newValue;
                // Only never-sampled cells suppress events; restored samples must report offline changes.
                if (oldValue >= 0 && oldValue != newValue) changed.accept(new Change(side, color, oldValue, newValue));
            }
        }
        if (dirty) host.markChanged();
    }

    public record Change(Direction side, int color, int oldValue, int newValue) {}
}
