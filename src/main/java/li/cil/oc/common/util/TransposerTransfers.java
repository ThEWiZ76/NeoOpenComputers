package li.cil.oc.common.util;

import li.cil.oc.common.ModSettings;

public final class TransposerTransfers {
    private TransposerTransfers() {
    }

    public static double pauseSeconds(final int millibuckets) {
        return millibuckets / (double) ModSettings.transposerFluidTransferRate();
    }
}
