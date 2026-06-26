package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.common.ModSettings;

import java.util.Map;

public final class ApuEnvironment extends GraphicsCardEnvironment {
    private final int tier;
    private final int maxWidth;
    private final int maxHeight;
    private final TextBuffer.ColorDepth maxDepth;

    public ApuEnvironment(final int tier) {
        super(tier);
        final int clampedTier = Math.max(0, Math.min(2, tier));
        this.tier = clampedTier;
        maxWidth = ModSettings.screenWidthByTier(clampedTier);
        maxHeight = ModSettings.screenHeightByTier(clampedTier);
        maxDepth = ModSettings.screenDepthByTier(clampedTier);
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Processor,
            DeviceInfo.DeviceAttribute.Description, "APU",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "FlexiArch " + (tier + 1) + " Processor (Builtin Graphics)",
            DeviceInfo.DeviceAttribute.Capacity, Integer.toString(maxWidth * maxHeight),
            DeviceInfo.DeviceAttribute.Width, Integer.toString(bits(maxDepth)),
            DeviceInfo.DeviceAttribute.Clock, ((int) (ModSettings.callBudget(tier) * 1000D)) + "+" + clockInfo(tier)
        );
    }

    private static int bits(final TextBuffer.ColorDepth depth) {
        return switch (depth) {
            case OneBit -> 1;
            case FourBit -> 4;
            case EightBit -> 8;
        };
    }

    private static String clockInfo(final int tier) {
        return switch (tier) {
            case 0 -> "640/640/40/1280/320/640";
            case 1 -> "1280/1280/160/2560/640/1280";
            default -> "2560/2560/320/5120/1280/2560";
        };
    }
}
