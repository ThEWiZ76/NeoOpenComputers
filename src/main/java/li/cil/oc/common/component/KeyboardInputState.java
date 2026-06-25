package li.cil.oc.common.component;

import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModSettings;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public final class KeyboardInputState {
    private static final String SIGNAL_MESSAGE = "computer.checked_signal";
    private static final String KEY_DOWN_MESSAGE = "keyboard.keyDown";
    private static final String KEY_UP_MESSAGE = "keyboard.keyUp";
    private static final String CLIPBOARD_MESSAGE = "keyboard.clipboard";

    private final Map<Player, Map<Integer, Character>> pressedKeys = new HashMap<>();
    private final Function<Player, String> usernameProvider;

    public KeyboardInputState() {
        this(player -> player == null ? null : player.getName().getString());
    }

    KeyboardInputState(final Function<Player, String> usernameProvider) {
        this.usernameProvider = usernameProvider;
    }

    public void onMessage(final Node node, final Message message, final Predicate<Player> isUsable) {
        if (node == null || message == null) {
            return;
        }

        final Object[] data = message.data();
        if (KEY_DOWN_MESSAGE.equals(message.name()) && data.length >= 3) {
            final Player player = (Player) data[0];
            if (isUsable.test(player)) {
                final char character = toCharacter(data[1]);
                final int code = toInteger(data[2]);
                pressedKeys.computeIfAbsent(player, ignored -> new HashMap<>()).put(code, character);
                sendInputSignal(node, player, "key_down", (int) character, code);
            }
        } else if (KEY_UP_MESSAGE.equals(message.name()) && data.length >= 3) {
            final Player player = (Player) data[0];
            final int code = toInteger(data[2]);
            final Map<Integer, Character> playerKeys = pressedKeys.get(player);
            if (playerKeys != null && playerKeys.containsKey(code)) {
                final char character = toCharacter(data[1]);
                playerKeys.remove(code);
                if (playerKeys.isEmpty()) {
                    pressedKeys.remove(player);
                }
                if (!isUsable.test(player)) {
                    return;
                }
                sendInputSignal(node, player, "key_up", (int) character, code);
            }
        } else if (CLIPBOARD_MESSAGE.equals(message.name()) && data.length >= 2) {
            final Player player = (Player) data[0];
            if (isUsable.test(player)) {
                final String value = String.valueOf(data[1]);
                linesWithSeparators(value).forEach(line -> sendInputSignal(node, player, "clipboard", line));
            }
        }
    }

    private void sendInputSignal(final Node node, final Player player, final String signal, final Object... data) {
        final String username = ModSettings.inputUsername() ? usernameProvider.apply(player) : null;
        final boolean includeUsername = username != null;
        final Object[] payload = includeUsername
            ? new Object[data.length + 3]
            : new Object[data.length + 2];
        payload[0] = player;
        payload[1] = signal;
        System.arraycopy(data, 0, payload, 2, data.length);
        if (includeUsername) {
            payload[payload.length - 1] = username;
        }
        node.sendToReachable(SIGNAL_MESSAGE, payload);
    }

    private static List<String> linesWithSeparators(final String value) {
        final List<String> lines = new ArrayList<>();
        int start = 0;
        for (int index = 0; index < value.length(); index++) {
            final char character = value.charAt(index);
            if (character == '\n' || character == '\r') {
                int end = index + 1;
                if (character == '\r' && end < value.length() && value.charAt(end) == '\n') {
                    end++;
                }
                lines.add(value.substring(start, end));
                start = end;
                index = end - 1;
            }
        }
        if (start < value.length()) {
            lines.add(value.substring(start));
        }
        return lines;
    }

    private static char toCharacter(final Object value) {
        if (value instanceof Character character) {
            return character;
        }
        if (value instanceof Number number) {
            return (char) number.intValue();
        }
        final String string = String.valueOf(value);
        return string.isEmpty() ? 0 : string.charAt(0);
    }

    private static int toInteger(final Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
