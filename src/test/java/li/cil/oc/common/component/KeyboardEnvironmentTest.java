package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
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

final class KeyboardEnvironmentTest {
    @Test
    void createsKeyboardComponentNode() {
        OpenComputersApi.initialize();
        TestEnvironment host = new TestEnvironment();

        Node node = KeyboardEnvironment.createNode(host);

        Component component = assertInstanceOf(Component.class, node);
        assertSame(host, component.host());
        assertEquals("keyboard", component.name());
        assertEquals(Visibility.Network, component.reachability());
        assertEquals(Visibility.Network, component.visibility());
    }

    @Test
    void exposesDeviceInfoMetadata() {
        Map<String, String> metadata = KeyboardEnvironment.deviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Input, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Keyboard", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("Fancytyper MX-Stone", metadata.get(DeviceInfo.DeviceAttribute.Product));
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
