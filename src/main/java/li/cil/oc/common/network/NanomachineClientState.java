package li.cil.oc.common.network;

public final class NanomachineClientState {
    private static boolean installed;
    private static double buffer;
    private static double maxBuffer;

    private NanomachineClientState() {
    }

    public static void apply(final NanomachinePowerPayload payload) {
        if (payload == null || !payload.installed()) {
            clear();
            return;
        }
        installed = true;
        buffer = Math.max(0D, payload.buffer());
        maxBuffer = Math.max(0D, payload.maxBuffer());
        if (maxBuffer > 0D) {
            buffer = Math.min(buffer, maxBuffer);
        }
    }

    public static void clear() {
        installed = false;
        buffer = 0D;
        maxBuffer = 0D;
    }

    public static boolean installed() {
        return installed;
    }

    public static double buffer() {
        return buffer;
    }

    public static double maxBuffer() {
        return maxBuffer;
    }

    public static double fill() {
        return installed && maxBuffer > 0D ? Math.clamp(buffer / maxBuffer, 0D, 1D) : 0D;
    }
}
