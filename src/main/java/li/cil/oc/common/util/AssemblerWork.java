package li.cil.oc.common.util;

import li.cil.oc.common.ModSettings;

public final class AssemblerWork {
    private AssemblerWork() {
    }

    public static double energyToApply(final double remainingEnergy) {
        return Math.min(remainingEnergy, ModSettings.assemblerTickAmount());
    }

    public static double energyConsumed(final double requestedEnergy, final double remainingDelta) {
        return Math.clamp(requestedEnergy + remainingDelta, 0D, requestedEnergy);
    }
}
