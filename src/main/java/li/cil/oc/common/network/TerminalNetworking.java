package li.cil.oc.common.network;

import li.cil.oc.common.component.TerminalScreenSnapshot;
import li.cil.oc.common.menu.TerminalMenu;
import net.minecraft.world.entity.player.Player;
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
        event.registrar(NETWORK_VERSION)
            .playToServer(
                TerminalClipboardPayload.TYPE,
                TerminalClipboardPayload.STREAM_CODEC,
                TerminalNetworking::handleTerminalClipboard);
        event.registrar(NETWORK_VERSION)
            .playToServer(
                TerminalMousePayload.TYPE,
                TerminalMousePayload.STREAM_CODEC,
                TerminalNetworking::handleTerminalMouse);
    }

    static void applyScreenSnapshot(final AbstractContainerMenu containerMenu, final TerminalScreenSnapshotPayload payload) {
        if (containerMenu instanceof TerminalMenu menu && menu.containerId == payload.containerId()) {
            menu.updateSnapshot(payload.snapshot());
        }
    }

    static void applyTerminalKey(final AbstractContainerMenu containerMenu, final TerminalKeyPayload payload, final Player player) {
        if (!(containerMenu instanceof TerminalMenu menu) || menu.containerId != payload.containerId() || menu.terminalServer() == null) {
            return;
        }
        if (!acceptsTerminalMenu(menu, player)) {
            return;
        }
        if (!acceptsTerminalInput(menu.terminalServer().screenSnapshot())) {
            return;
        }
        if (payload.pressed()) {
            menu.terminalServer().screen().keyDown((char) payload.character(), payload.keyCode(), player);
        } else {
            menu.terminalServer().screen().keyUp((char) payload.character(), payload.keyCode(), player);
        }
    }

    static void applyTerminalClipboard(final AbstractContainerMenu containerMenu, final TerminalClipboardPayload payload, final Player player) {
        if (!(containerMenu instanceof TerminalMenu menu) || menu.containerId != payload.containerId() || menu.terminalServer() == null) {
            return;
        }
        if (!acceptsTerminalMenu(menu, player)) {
            return;
        }
        if (!acceptsTerminalInput(menu.terminalServer().screenSnapshot())) {
            return;
        }
        menu.terminalServer().screen().clipboard(payload.value(), player);
    }

    static void applyTerminalMouse(final AbstractContainerMenu containerMenu, final TerminalMousePayload payload, final Player player) {
        if (!(containerMenu instanceof TerminalMenu menu) || menu.containerId != payload.containerId() || menu.terminalServer() == null) {
            return;
        }
        if (!acceptsTerminalMenu(menu, player)) {
            return;
        }
        if (!mouseInside(menu.terminalServer().screenSnapshot(), payload)) {
            return;
        }
        switch (payload.kind()) {
            case TerminalMousePayload.MOUSE_DOWN -> menu.terminalServer().screen().mouseDown(payload.x(), payload.y(), payload.buttonOrDelta(), player);
            case TerminalMousePayload.MOUSE_DRAG -> menu.terminalServer().screen().mouseDrag(payload.x(), payload.y(), payload.buttonOrDelta(), player);
            case TerminalMousePayload.MOUSE_UP -> menu.terminalServer().screen().mouseUp(payload.x(), payload.y(), payload.buttonOrDelta(), player);
            case TerminalMousePayload.MOUSE_SCROLL -> menu.terminalServer().screen().mouseScroll(payload.x(), payload.y(), payload.buttonOrDelta(), player);
            default -> {
            }
        }
    }

    static boolean mouseInside(final TerminalScreenSnapshot snapshot, final TerminalMousePayload payload) {
        return snapshot != null
            && snapshot.width() > 0
            && snapshot.height() > 0
            && payload.x() >= 0
            && payload.y() >= 0
            && payload.x() < snapshot.width()
            && payload.y() < snapshot.height();
    }

    static boolean acceptsTerminalInput(final TerminalScreenSnapshot snapshot) {
        return snapshot != null
            && snapshot.width() > 0
            && snapshot.height() > 0;
    }

    static boolean acceptsTerminalMenu(final TerminalMenu menu, final Player player) {
        return menu != null && menu.stillValid(player);
    }

    private static void handleScreenSnapshot(final TerminalScreenSnapshotPayload payload, final IPayloadContext context) {
        applyScreenSnapshot(context.player().containerMenu, payload);
    }

    private static void handleTerminalKey(final TerminalKeyPayload payload, final IPayloadContext context) {
        applyTerminalKey(context.player().containerMenu, payload, context.player());
    }

    private static void handleTerminalClipboard(final TerminalClipboardPayload payload, final IPayloadContext context) {
        applyTerminalClipboard(context.player().containerMenu, payload, context.player());
    }

    private static void handleTerminalMouse(final TerminalMousePayload payload, final IPayloadContext context) {
        applyTerminalMouse(context.player().containerMenu, payload, context.player());
    }

    private TerminalNetworking() {
    }
}
