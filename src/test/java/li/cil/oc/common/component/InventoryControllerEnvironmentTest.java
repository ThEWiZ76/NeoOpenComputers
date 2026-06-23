package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class InventoryControllerEnvironmentTest {
    @Test
    void reportsUpstreamDeviceInfo() {
        OpenComputersApi.initialize();

        final InventoryControllerEnvironment controller = new InventoryControllerEnvironment(null);
        final Map<String, String> metadata = controller.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Generic, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Inventory controller", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("Item Cataloguer R1", metadata.get(DeviceInfo.DeviceAttribute.Product));
    }

    @Test
    void nonAdapterControllerIsNeighborVisibleLikeUpstreamRobotAndDroneHosts() {
        OpenComputersApi.initialize();

        final InventoryControllerEnvironment controller = new InventoryControllerEnvironment(new TestEnvironmentHost());
        final Component component = assertInstanceOf(Component.class, controller.node());

        assertEquals(Visibility.Neighbors, component.visibility());
    }

    private static final class TestEnvironmentHost implements EnvironmentHost {
        @Override
        public Level world() {
            return null;
        }

        @Override
        public double xPosition() {
            return 0;
        }

        @Override
        public double yPosition() {
            return 0;
        }

        @Override
        public double zPosition() {
            return 0;
        }

        @Override
        public void markChanged() {
        }
    }
}
