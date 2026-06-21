package li.cil.oc.common.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class TerminalNetworking {
    public static final String NETWORK_VERSION = "1";

    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
            .playToClient(
                TerminalScreenSnapshotPayload.TYPE,
                TerminalScreenSnapshotPayload.STREAM_CODEC,
                TerminalNetworking::handleScreenSnapshot);
    }

    private static void handleScreenSnapshot(final TerminalScreenSnapshotPayload payload, final IPayloadContext context) {
    }

    private TerminalNetworking() {
    }
}
