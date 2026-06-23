package li.cil.oc.common.util;

import li.cil.oc.common.ModSettings;

public final class DisassemblerWork {
    private DisassemblerWork() {
    }

    public static double energyToApply(final double buffer) {
        return canReleaseOutput(buffer) ? 0D : ModSettings.disassemblerTickAmount();
    }

    public static boolean canReleaseOutput(final double buffer) {
        return buffer >= ModSettings.disassemblerItemCost();
    }

    public static double remainingBufferAfterRelease(final double buffer) {
        return Math.max(0D, buffer - ModSettings.disassemblerItemCost());
    }
}
