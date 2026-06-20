package li.cil.oc.common.component;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class KeyboardInputStateTest {
    @Test
    void forwardsKeyDownAndMatchingKeyUpSignals() {
        CapturingNode node = new CapturingNode();
        KeyboardInputState state = new KeyboardInputState();

        state.onMessage(node, new TestMessage("keyboard.keyDown", null, 'a', 30), player -> true);
        state.onMessage(node, new TestMessage("keyboard.keyUp", null, 'a', 30), player -> true);

        assertEquals(2, node.signals.size());
        assertEquals(List.of("computer.checked_signal", "key_down", "keyboard", (int) 'a', 30), node.signals.get(0));
        assertEquals(List.of("computer.checked_signal", "key_up", "keyboard", (int) 'a', 30), node.signals.get(1));
    }

    @Test
    void ignoresKeyUpWithoutMatchingKeyDown() {
        CapturingNode node = new CapturingNode();
        KeyboardInputState state = new KeyboardInputState();

        state.onMessage(node, new TestMessage("keyboard.keyUp", null, 'a', 30), player -> true);

        assertEquals(0, node.signals.size());
    }

    @Test
    void forwardsClipboardLines() {
        CapturingNode node = new CapturingNode();
        KeyboardInputState state = new KeyboardInputState();

        state.onMessage(node, new TestMessage("keyboard.clipboard", null, "alpha\nbeta"), player -> true);

        assertEquals(2, node.signals.size());
        assertEquals(List.of("computer.checked_signal", "clipboard", "keyboard", "alpha"), node.signals.get(0));
        assertEquals(List.of("computer.checked_signal", "clipboard", "keyboard", "beta"), node.signals.get(1));
    }

    private record TestMessage(String name, Object... data) implements Message {
        @Override
        public Node source() {
            return null;
        }

        @Override
        public void cancel() {
        }
    }

    private static final class CapturingNode implements Node {
        private final List<List<Object>> signals = new ArrayList<>();

        @Override
        public Environment host() {
            return null;
        }

        @Override
        public Visibility reachability() {
            return Visibility.Network;
        }

        @Override
        public String address() {
            return "keyboard";
        }

        @Override
        public Network network() {
            return null;
        }

        @Override
        public boolean isNeighborOf(final Node other) {
            return false;
        }

        @Override
        public boolean canBeReachedFrom(final Node other) {
            return false;
        }

        @Override
        public Iterable<Node> neighbors() {
            return List.of();
        }

        @Override
        public Iterable<Node> reachableNodes() {
            return List.of();
        }

        @Override
        public void connect(final Node node) {
        }

        @Override
        public void disconnect(final Node node) {
        }

        @Override
        public void remove() {
        }

        @Override
        public void sendToAddress(final String target, final String name, final Object... data) {
        }

        @Override
        public void sendToNeighbors(final String name, final Object... data) {
        }

        @Override
        public void sendToReachable(final String name, final Object... data) {
            List<Object> signal = new ArrayList<>();
            signal.add(name);
            signal.addAll(List.of(data));
            signals.add(signal);
        }

        @Override
        public void sendToVisible(final String name, final Object... data) {
        }

        @Override
        public void load(final CompoundTag nbt) {
        }

        @Override
        public void save(final CompoundTag nbt) {
        }
    }

    @FunctionalInterface
    interface Usable {
        boolean test(Player player);
    }
}
