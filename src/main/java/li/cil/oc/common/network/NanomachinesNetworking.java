package li.cil.oc.common.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NanomachinesNetworking {
    public static final String NETWORK_VERSION = "1";

    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
            .playToClient(
                NanomachinePowerPayload.TYPE,
                NanomachinePowerPayload.STREAM_CODEC,
                NanomachinesNetworking::handlePower);
    }

    static void applyPower(final NanomachinePowerPayload payload) {
        NanomachineClientState.apply(payload);
    }

    private static void handlePower(final NanomachinePowerPayload payload, final IPayloadContext context) {
        applyPower(payload);
    }

    private NanomachinesNetworking() {
    }
}
