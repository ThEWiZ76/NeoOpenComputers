package li.cil.oc.common.component;

import li.cil.oc.common.integration.ComputerCraftBundledRedstone;
import net.minecraft.core.Direction;

import java.util.Arrays;
import java.util.function.Consumer;

/** Server-tick input cache; direct Lua callbacks never query the world from a machine thread. */
public final class BundledRedstoneInputs {
    private final int[][] values = new int[6][16];

    public BundledRedstoneInputs() {
        for (int[] side : values) Arrays.fill(side, -1);
    }

    public synchronized int get(final Direction side, final int color) {
        return Math.max(0, values[side.get3DDataValue()][color]);
    }

    public synchronized void update(final RedstoneControllerHost host, final Consumer<Change> changed) {
        if (!ComputerCraftBundledRedstone.isAvailable()) return;
        for (Direction side : Direction.values()) {
            final int mask = ComputerCraftBundledRedstone.input(host, side);
            for (int color = 0; color < 16; color++) {
                final int oldValue = values[side.get3DDataValue()][color];
                final int newValue = (mask & (1 << color)) == 0 ? 0 : 15;
                values[side.get3DDataValue()][color] = newValue;
                // Upstream suppresses changes on the first sample after placement/load.
                if (oldValue >= 0 && oldValue != newValue) changed.accept(new Change(side, color, oldValue, newValue));
            }
        }
    }

    public record Change(Direction side, int color, int oldValue, int newValue) {}
}
