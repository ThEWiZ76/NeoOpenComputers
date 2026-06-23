package li.cil.oc.common.util;

import li.cil.oc.common.ModSettings;

public final class HologramPower {
    private HologramPower() {
    }

    public static double energyCost(final double litRatio, final double scale) {
        return ModSettings.hologramCost() * Math.max(0D, litRatio) * Math.max(0D, scale);
    }
}
