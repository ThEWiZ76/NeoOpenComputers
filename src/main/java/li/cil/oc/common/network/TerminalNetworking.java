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
        event.registrar(NETWORK_VERSION)
            .playToServer(
                TerminalKeyPayload.TYPE,
                TerminalKeyPayload.STREAM_CODEC,
                TerminalNetworking::handleTerminalKey);
    }

    static void applyScreenSnapshot(final AbstractContainerMenu containerMenu, final TerminalScreenSnapshotPayload payload) {
        if (containerMenu instanceof TerminalMenu menu && menu.containerId == payload.containerId()) {
            menu.updateSnapshot(payload.snapshot());
        }
    }

    static void applyTerminalKey(final AbstractContainerMenu containerMenu, final TerminalKeyPayload payload, final net.minecraft.world.entity.player.Player player) {
        if (!(containerMenu instanceof TerminalMenu menu) || menu.containerId != payload.containerId() || menu.terminalServer() == null) {
            return;
        }
        if (payload.pressed()) {
            menu.terminalServer().screen().keyDown((char) payload.character(), payload.keyCode(), player);
        } else {
            menu.terminalServer().screen().keyUp((char) payload.character(), payload.keyCode(), player);
        }
    }

    private static void handleScreenSnapshot(final TerminalScreenSnapshotPayload payload, final IPayloadContext context) {
        applyScreenSnapshot(context.player().containerMenu, payload);
    }

    private static void handleTerminalKey(final TerminalKeyPayload payload, final IPayloadContext context) {
        applyTerminalKey(context.player().containerMenu, payload, context.player());
    }

    private TerminalNetworking() {
    }
}
