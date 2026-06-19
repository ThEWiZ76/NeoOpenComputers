package li.cil.oc.api.machine;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MachineHostTest {
    @Test
    void exposesMachineHostContextAndDefaultPositionText() {
        MachineHost host = new TestMachineHost();

        host.onMachineConnect(null);
        host.onMachineDisconnect(null);

        assertInstanceOf(EnvironmentHost.class, host);
        assertNull(host.machine());
        assertEquals(1, host.internalComponents().spliterator().getExactSizeIfKnown());
        assertEquals(4, host.componentSlot("component"));
        assertTrue(host.machinePosition().contains("1.50000"));
        assertTrue(host.machinePosition().contains("2.50000"));
        assertTrue(host.machinePosition().contains("3.50000"));
    }

    private static final class TestMachineHost implements MachineHost {
        @Override
        public Level world() {
            return null;
        }

        @Override
        public double xPosition() {
            return 1.5;
        }

        @Override
        public double yPosition() {
            return 2.5;
        }

        @Override
        public double zPosition() {
            return 3.5;
        }

        @Override
        public void markChanged() {
        }

        @Override
        public Machine machine() {
            return null;
        }

        @Override
        public Iterable<ItemStack> internalComponents() {
            return Collections.singletonList(null);
        }

        @Override
        public int componentSlot(final String address) {
            return "component".equals(address) ? 4 : -1;
        }

        @Override
        public void onMachineConnect(final Node node) {
        }

        @Override
        public void onMachineDisconnect(final Node node) {
        }
    }
}
