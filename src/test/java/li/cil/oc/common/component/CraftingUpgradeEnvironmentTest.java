package li.cil.oc.common.component;

import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class CraftingUpgradeEnvironmentTest {
    @Test
    void componentIsNetworkVisibleLikeUpstreamCraftingUpgrade() {
        OpenComputersApi.initialize();

        final CraftingUpgradeEnvironment environment = new CraftingUpgradeEnvironment(new TestAgent());
        final Component component = assertInstanceOf(Component.class, environment.node());

        assertEquals(Visibility.Network, component.visibility());
    }

    private static final class TestAgent implements Agent {
        @Override public Container equipmentInventory() { return null; }
        @Override public Container mainInventory() { return null; }
        @Override public MultiTank tank() { return null; }
        @Override public int selectedSlot() { return 0; }
        @Override public void setSelectedSlot(final int index) { }
        @Override public int selectedTank() { return 0; }
        @Override public void setSelectedTank(final int index) { }
        @Override public Player player() { return null; }
        @Override public String name() { return "test"; }
        @Override public void setName(final String name) { }
        @Override public String ownerName() { return "test"; }
        @Override public UUID ownerUUID() { return new UUID(0L, 0L); }
        @Override public Machine machine() { return null; }
        @Override public Iterable<ItemStack> internalComponents() { return List.of(); }
        @Override public int componentSlot(final String address) { return -1; }
        @Override public void onMachineConnect(final Node node) { }
        @Override public void onMachineDisconnect(final Node node) { }
        @Override public Level world() { return null; }
        @Override public double xPosition() { return 0; }
        @Override public double yPosition() { return 0; }
        @Override public double zPosition() { return 0; }
        @Override public void markChanged() { }
        @Override public Direction facing() { return Direction.NORTH; }
        @Override public Direction toGlobal(final Direction value) { return value; }
        @Override public Direction toLocal(final Direction value) { return value; }
    }
}
