package li.cil.oc.common.network;

import java.util.List;

public final class NanomachineClientState {
    private static boolean installed;
    private static double buffer;
    private static double maxBuffer;
    private static int activeInputs;
    private static int totalInputs;
    private static List<String> activeParticleEffects = List.of();

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
        totalInputs = Math.max(0, payload.totalInputs());
        activeInputs = Math.min(Math.max(0, payload.activeInputs()), totalInputs);
        activeParticleEffects = List.copyOf(payload.activeParticleEffects());
    }

    public static void clear() {
        installed = false;
        buffer = 0D;
        maxBuffer = 0D;
        activeInputs = 0;
        totalInputs = 0;
        activeParticleEffects = List.of();
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

    public static int activeInputs() {
        return activeInputs;
    }

    public static int totalInputs() {
        return totalInputs;
    }

    public static List<String> activeParticleEffects() {
        return activeParticleEffects;
    }

    public static double fill() {
        return installed && maxBuffer > 0D ? Math.clamp(buffer / maxBuffer, 0D, 1D) : 0D;
    }
}
