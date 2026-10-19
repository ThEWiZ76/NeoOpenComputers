package li.cil.oc.common.integration;

import li.cil.oc.common.component.RedstoneControllerHost;
import net.minecraft.core.Direction;

/** Combines available transports using upstream's per-channel maximum. */
public final class BundledRedstone {
    public static boolean isAvailable() {
        return ComputerCraftBundledRedstone.isAvailable() || ProjectRedBundledRedstone.isAvailable();
    }

    public static int[] input(final RedstoneControllerHost host, final Direction side) {
        final int mask = ComputerCraftBundledRedstone.input(host, side);
        final var projectRed = ProjectRedBundledRedstone.input(host, side);
        final var signal = new int[16];
        for (int color = 0; color < 16; color++) {
            final int cc = (mask & (1 << color)) == 0 ? 0 : 15;
            signal[color] = Math.max(cc, projectRed != null && color < projectRed.length ? projectRed[color] & 255 : 0);
        }
        return signal;
    }

    private BundledRedstone() {}
}
