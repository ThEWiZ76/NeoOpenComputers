package li.cil.oc.common.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class DebugNetworking {
    public static final String NETWORK_VERSION = "1";

    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
            .playToClient(
                DebugClipboardPayload.TYPE,
                DebugClipboardPayload.STREAM_CODEC,
                DebugNetworking::handleClipboard);
    }

    static void applyClipboard(final DebugClipboardPayload payload) {
        DebugClipboardState.accept(payload);
    }

    private static void handleClipboard(final DebugClipboardPayload payload, final IPayloadContext context) {
        applyClipboard(payload);
    }

    private DebugNetworking() {
    }
}
