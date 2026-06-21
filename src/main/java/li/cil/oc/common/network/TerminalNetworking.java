package li.cil.oc.common.network;

import li.cil.oc.common.menu.TerminalMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
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

    static void applyScreenSnapshot(final AbstractContainerMenu containerMenu, final TerminalScreenSnapshotPayload payload) {
        if (containerMenu instanceof TerminalMenu menu && menu.containerId == payload.containerId()) {
            menu.updateSnapshot(payload.snapshot());
        }
    }

    private static void handleScreenSnapshot(final TerminalScreenSnapshotPayload payload, final IPayloadContext context) {
        applyScreenSnapshot(context.player().containerMenu, payload);
    }

    private TerminalNetworking() {
    }
}
