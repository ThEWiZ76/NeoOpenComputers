package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

final class ScreenEnvironmentTest {
    @Test
    void createsScreenComponentNode() {
        OpenComputersApi.initialize();
        TestEnvironment host = new TestEnvironment();

        Node node = ScreenEnvironment.createNode(host);

        Component component = assertInstanceOf(Component.class, node);
        assertSame(host, component.host());
        assertEquals("screen", component.name());
        assertEquals(Visibility.Neighbors, component.reachability());
        assertEquals(Visibility.Neighbors, component.visibility());
    }

    @Test
    void exposesDeviceInfoMetadata() {
        Map<String, String> metadata = ScreenEnvironment.deviceInfo(40, 16, TextBuffer.ColorDepth.OneBit);

        assertEquals(DeviceInfo.DeviceClass.Display, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Text buffer", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("Text Screen V0", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("640", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
        assertEquals("1", metadata.get(DeviceInfo.DeviceAttribute.Width));
    }

    private static final class TestEnvironment implements ManagedEnvironment {
        @Override
        public Node node() {
            return null;
        }

        @Override
        public void onConnect(final Node node) {
        }

        @Override
        public void onDisconnect(final Node node) {
        }

        @Override
        public void onMessage(final Message message) {
        }

        @Override
        public boolean canUpdate() {
            return false;
        }

        @Override
        public void update() {
        }

        @Override
        public void load(final CompoundTag nbt) {
        }

        @Override
        public void save(final CompoundTag nbt) {
        }
    }
}
