package li.cil.oc.common.component;

import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModSettings;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.function.Function;

public final class ScreenInputDispatcher {
    private static final String SIGNAL_MESSAGE = "computer.checked_signal";

    private final Function<Player, String> usernameProvider;

    public ScreenInputDispatcher() {
        this(player -> player == null ? null : player.getName().getString());
    }

    ScreenInputDispatcher(final Function<Player, String> usernameProvider) {
        this.usernameProvider = usernameProvider;
    }

    public void keyDown(final Node node, final char character, final int code, final Player player) {
        sendToKeyboard(node, "keyboard.keyDown", player, character, code);
    }

    public void keyUp(final Node node, final char character, final int code, final Player player) {
        sendToKeyboard(node, "keyboard.keyUp", player, character, code);
    }

    public void clipboard(final Node node, final String value, final Player player) {
        sendToKeyboard(node, "keyboard.clipboard", player, value);
    }

    public void mouseDown(final Node node, final double x, final double y, final int button, final Player player) {
        mouseDown(node, x, y, button, player, false);
    }

    public void mouseDown(final Node node, final double x, final double y, final int button, final Player player, final boolean precise) {
        sendMouseEvent(node, player, "touch", x, y, button, precise);
    }

    public void mouseDrag(final Node node, final double x, final double y, final int button, final Player player) {
        mouseDrag(node, x, y, button, player, false);
    }

    public void mouseDrag(final Node node, final double x, final double y, final int button, final Player player, final boolean precise) {
        sendMouseEvent(node, player, "drag", x, y, button, precise);
    }

    public void mouseUp(final Node node, final double x, final double y, final int button, final Player player) {
        mouseUp(node, x, y, button, player, false);
    }

    public void mouseUp(final Node node, final double x, final double y, final int button, final Player player, final boolean precise) {
        sendMouseEvent(node, player, "drop", x, y, button, precise);
    }

    public void mouseScroll(final Node node, final double x, final double y, final int delta, final Player player) {
        mouseScroll(node, x, y, delta, player, false);
    }

    public void mouseScroll(final Node node, final double x, final double y, final int delta, final Player player, final boolean precise) {
        sendMouseEvent(node, player, "scroll", x, y, delta, precise);
    }

    private static void sendToKeyboard(final Node node, final String name, final Object... data) {
        if (node != null) {
            node.sendToReachable(name, data);
        }
    }

    private void sendMouseEvent(final Node node, final Player player, final String name, final double x, final double y, final int data, final boolean precise) {
        if (node == null) {
            return;
        }
        final Object[] payload = precise
            ? new Object[]{player, name, x, y, data}
            : new Object[]{player, name, (int) x + 1, (int) y + 1, data};
        final String username = ModSettings.inputUsername() ? usernameProvider.apply(player) : null;
        if (username == null) {
            node.sendToReachable(SIGNAL_MESSAGE, payload);
        } else {
            final Object[] payloadWithUsername = Arrays.copyOf(payload, payload.length + 1);
            payloadWithUsername[payload.length] = username;
            node.sendToReachable(SIGNAL_MESSAGE, payloadWithUsername);
        }
    }
}
