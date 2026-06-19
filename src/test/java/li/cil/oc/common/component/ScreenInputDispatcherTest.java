package li.cil.oc.common.component;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ScreenInputDispatcherTest {
    @Test
    void sendsKeyboardEventsToNeighborKeyboards() {
        CapturingNode node = new CapturingNode();
        ScreenInputDispatcher dispatcher = new ScreenInputDispatcher();

        dispatcher.keyDown(node, 'a', 30, null);
        dispatcher.keyUp(node, 'a', 30, null);
        dispatcher.clipboard(node, "text", null);

        assertEquals(Arrays.asList("keyboard.keyDown", null, 'a', 30), node.neighborMessages.get(0));
        assertEquals(Arrays.asList("keyboard.keyUp", null, 'a', 30), node.neighborMessages.get(1));
        assertEquals(Arrays.asList("keyboard.clipboard", null, "text"), node.neighborMessages.get(2));
    }

    @Test
    void sendsMouseEventsToReachableComputers() {
        CapturingNode node = new CapturingNode();
        ScreenInputDispatcher dispatcher = new ScreenInputDispatcher();

        dispatcher.mouseDown(node, 1.2, 2.8, 0, null);
        dispatcher.mouseDrag(node, 3.0, 4.0, 1, null);
        dispatcher.mouseUp(node, 5.0, 6.0, 2, null);
        dispatcher.mouseScroll(node, 7.0, 8.0, -1, null);

        assertEquals(Arrays.asList("computer.checked_signal", null, "touch", 2, 3, 0), node.reachableMessages.get(0));
        assertEquals(Arrays.asList("computer.checked_signal", null, "drag", 4, 5, 1), node.reachableMessages.get(1));
        assertEquals(Arrays.asList("computer.checked_signal", null, "drop", 6, 7, 2), node.reachableMessages.get(2));
        assertEquals(Arrays.asList("computer.checked_signal", null, "scroll", 8, 9, -1), node.reachableMessages.get(3));
    }

    private static final class CapturingNode implements Node {
        private final List<List<Object>> neighborMessages = new ArrayList<>();
        private final List<List<Object>> reachableMessages = new ArrayList<>();

        @Override
        public Environment host() {
            return null;
        }

        @Override
        public Visibility reachability() {
            return Visibility.Neighbors;
        }

        @Override
        public String address() {
            return "screen";
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
            List<Object> message = new ArrayList<>();
            message.add(name);
            for (Object value : data) {
                message.add(value);
            }
            neighborMessages.add(message);
        }

        @Override
        public void sendToReachable(final String name, final Object... data) {
            List<Object> message = new ArrayList<>();
            message.add(name);
            for (Object value : data) {
                message.add(value);
            }
            reachableMessages.add(message);
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
}
