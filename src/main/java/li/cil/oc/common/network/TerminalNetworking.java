package li.cil.oc.common.network;

import li.cil.oc.common.component.TerminalScreenSnapshot;
import li.cil.oc.common.menu.TerminalMenu;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

public final class TerminalNetworking {
    public static final String NETWORK_VERSION = "1";

    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
            .playToClient(
                TerminalScreenSnapshotPayload.TYPE,
                TerminalScreenSnapshotPayload.STREAM_CODEC,
                TerminalNetworking::handleScreenSnapshot);
        event.registrar(NETWORK_VERSION)
            .playToClient(
                TerminalScreenDeltaPayload.TYPE,
                TerminalScreenDeltaPayload.STREAM_CODEC,
                TerminalNetworking::handleScreenDelta);
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

    static void applyScreenDelta(final AbstractContainerMenu containerMenu, final TerminalScreenDeltaPayload payload) {
        if (containerMenu instanceof TerminalMenu menu && menu.containerId == payload.containerId()) {
            menu.updateSnapshot(payload.delta().applyTo(menu.snapshot()));
        }
    }

    public static void sendToPlayerIfSupported(final ServerPlayer player, final CustomPacketPayload payload) {
        if (player == null || payload == null) {
            return;
        }
        if (player.connection instanceof ICommonPacketListener listener && NetworkRegistry.hasChannel(listener, payload.type().id())) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    static void applyTerminalKey(final AbstractContainerMenu containerMenu, final TerminalKeyPayload payload, final Player player) {
        if (!(containerMenu instanceof TerminalMenu menu) || menu.containerId != payload.containerId()) {
            return;
        }
        if (!acceptsTerminalMenu(menu, player)) {
            return;
        }
        if (!acceptsTerminalInput(liveSnapshot(menu))) {
            return;
        }
        final ScreenBlockEntity physicalScreen = menu.physicalScreen();
        if (payload.pressed()) {
            if (menu.terminalServer() != null) {
                menu.terminalServer().screen().keyDown((char) payload.character(), payload.keyCode(), player);
            } else if (physicalScreen != null) {
                physicalScreen.keyDown((char) payload.character(), payload.keyCode(), player);
            }
        } else {
            if (menu.terminalServer() != null) {
                menu.terminalServer().screen().keyUp((char) payload.character(), payload.keyCode(), player);
            } else if (physicalScreen != null) {
                physicalScreen.keyUp((char) payload.character(), payload.keyCode(), player);
            }
        }
    }

    static void applyTerminalClipboard(final AbstractContainerMenu containerMenu, final TerminalClipboardPayload payload, final Player player) {
        if (!(containerMenu instanceof TerminalMenu menu) || menu.containerId != payload.containerId()) {
            return;
        }
        if (!acceptsTerminalMenu(menu, player)) {
            return;
        }
        if (!acceptsTerminalInput(liveSnapshot(menu))) {
            return;
        }
        if (menu.terminalServer() != null) {
            menu.terminalServer().screen().clipboard(payload.value(), player);
        } else if (menu.physicalScreen() != null) {
            menu.physicalScreen().clipboard(payload.value(), player);
        }
    }

    static void applyTerminalMouse(final AbstractContainerMenu containerMenu, final TerminalMousePayload payload, final Player player) {
        if (!(containerMenu instanceof TerminalMenu menu) || menu.containerId != payload.containerId()) {
            return;
        }
        if (!acceptsTerminalMenu(menu, player)) {
            return;
        }
        if (!acceptsTerminalMouse(menu, liveSnapshot(menu), payload)) {
            return;
        }
        final ScreenBlockEntity physicalScreen = menu.physicalScreen();
        if (menu.terminalServer() != null) {
            switch (payload.kind()) {
                case TerminalMousePayload.MOUSE_DOWN -> menu.terminalServer().screen().mouseDown(payload.x(), payload.y(), payload.buttonOrDelta(), player);
                case TerminalMousePayload.MOUSE_DRAG -> menu.terminalServer().screen().mouseDrag(payload.x(), payload.y(), payload.buttonOrDelta(), player);
                case TerminalMousePayload.MOUSE_UP -> menu.terminalServer().screen().mouseUp(payload.x(), payload.y(), payload.buttonOrDelta(), player);
                case TerminalMousePayload.MOUSE_SCROLL -> menu.terminalServer().screen().mouseScroll(payload.x(), payload.y(), payload.buttonOrDelta(), player);
                default -> {
                }
            }
        } else if (physicalScreen != null) {
            switch (payload.kind()) {
                case TerminalMousePayload.MOUSE_DOWN -> physicalScreen.mouseDown(payload.x(), payload.y(), payload.buttonOrDelta(), player);
                case TerminalMousePayload.MOUSE_DRAG -> physicalScreen.mouseDrag(payload.x(), payload.y(), payload.buttonOrDelta(), player);
                case TerminalMousePayload.MOUSE_UP -> physicalScreen.mouseUp(payload.x(), payload.y(), payload.buttonOrDelta(), player);
                case TerminalMousePayload.MOUSE_SCROLL -> physicalScreen.mouseScroll(payload.x(), payload.y(), payload.buttonOrDelta(), player);
                default -> {
                }
            }
        }
    }

    private static TerminalScreenSnapshot liveSnapshot(final TerminalMenu menu) {
        if (menu.terminalServer() != null) {
            return menu.terminalServer().screenSnapshot();
        }
        if (menu.physicalScreen() != null) {
            return menu.physicalScreen().terminalSnapshot();
        }
        return menu.snapshot();
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

    static boolean acceptsTerminalMouse(final TerminalScreenSnapshot snapshot, final TerminalMousePayload payload) {
        return acceptsTerminalInput(snapshot)
            && (mouseInside(snapshot, payload)
            || payload.kind() == TerminalMousePayload.MOUSE_UP
            && payload.x() == -1.0D
            && payload.y() == -1.0D);
    }

    static boolean acceptsTerminalMouse(final TerminalMenu menu, final TerminalScreenSnapshot snapshot, final TerminalMousePayload payload) {
        return menu != null && menu.supportsMouseInput() && acceptsTerminalMouse(snapshot, payload);
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

    private static void handleScreenDelta(final TerminalScreenDeltaPayload payload, final IPayloadContext context) {
        applyScreenDelta(context.player().containerMenu, payload);
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
