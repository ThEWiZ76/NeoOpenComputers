package li.cil.oc.common.component;

import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class KeyboardInputState {
    private static final String SIGNAL_MESSAGE = "computer.checked_signal";
    private static final String KEY_DOWN_MESSAGE = "keyboard.keyDown";
    private static final String KEY_UP_MESSAGE = "keyboard.keyUp";
    private static final String CLIPBOARD_MESSAGE = "keyboard.clipboard";

    private final Map<Player, Map<Integer, Character>> pressedKeys = new HashMap<>();

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
                node.sendToReachable(SIGNAL_MESSAGE, "key_down", node.address(), (int) character, code);
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
                node.sendToReachable(SIGNAL_MESSAGE, "key_up", node.address(), (int) character, code);
            }
        } else if (CLIPBOARD_MESSAGE.equals(message.name()) && data.length >= 2) {
            final Player player = (Player) data[0];
            if (isUsable.test(player)) {
                final String value = String.valueOf(data[1]);
                value.lines().forEach(line -> node.sendToReachable(SIGNAL_MESSAGE, "clipboard", node.address(), line));
            }
        }
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
