package li.cil.oc.api.network;

import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.TestNodes;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComponentTest {
    @Test
    void componentExtendsNodeAndExposesCallbackMetadata() throws Exception {
        var component = new TestComponent();
        Method method = TestComponent.class.getDeclaredMethod("ping", Context.class);
        Callback callback = method.getAnnotation(Callback.class);

        component.setVisibility(Visibility.Network);

        assertSame(component, (Node) component);
        assertEquals("test_component", component.name());
        assertEquals(Visibility.Network, component.visibility());
        assertTrue(component.canBeSeenFrom(TestNodes.node("computer")));
        assertEquals(List.of("ping"), component.methods());
        assertNotNull(component.annotation("ping"));
        assertSame(callback, component.annotation("ping"));
        assertArrayEquals(new Object[]{"pong"}, component.invoke("ping", null));
    }

    @Test
    void componentConnectorCombinesComponentAndConnectorContracts() {
        ComponentConnector connector = new TestComponentConnector();

        assertSame(connector, (Component) connector);
        assertSame(connector, (Connector) connector);
    }

    private static class TestComponent implements Component {
        private Visibility visibility = Visibility.Neighbors;

        @Callback
        Object[] ping(final Context context) {
            return new Object[]{"pong"};
        }

        @Override
        public String name() {
            return "test_component";
        }

        @Override
        public Visibility visibility() {
            return visibility;
        }

        @Override
        public void setVisibility(final Visibility value) {
            visibility = value;
        }

        @Override
        public boolean canBeSeenFrom(final Node other) {
            return true;
        }

        @Override
        public Collection<String> methods() {
            return List.of("ping");
        }

        @Override
        public Callback annotation(final String method) {
            try {
                return TestComponent.class.getDeclaredMethod(method, Context.class).getAnnotation(Callback.class);
            } catch (NoSuchMethodException e) {
                return null;
            }
        }

        @Override
        public Object[] invoke(final String method, final Context context, final Object... arguments) throws Exception {
            if ("ping".equals(method)) {
                return ping(context);
            }
            throw new NoSuchMethodException(method);
        }

        @Override
        public Environment host() {
            return TestNodes.node("component").host();
        }

        @Override
        public Visibility reachability() {
            return Visibility.Network;
        }

        @Override
        public String address() {
            return "component";
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
            return true;
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

    private static final class TestComponentConnector extends TestComponent implements ComponentConnector {
        @Override
        public double localBuffer() {
            return 0;
        }

        @Override
        public double localBufferSize() {
            return 0;
        }

        @Override
        public double globalBuffer() {
            return 0;
        }

        @Override
        public double globalBufferSize() {
            return 0;
        }

        @Override
        public double changeBuffer(final double delta) {
            return 0;
        }

        @Override
        public boolean tryChangeBuffer(final double delta) {
            return true;
        }

        @Override
        public void setLocalBufferSize(final double size) {
        }
    }
}
