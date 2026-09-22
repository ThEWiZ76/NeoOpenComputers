package li.cil.oc.api.machine;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import net.minecraft.world.item.ItemStack;

public interface MachineHost extends EnvironmentHost {
    Machine machine();

    Iterable<ItemStack> internalComponents();

    int componentSlot(String address);

    void onMachineConnect(Node node);

    /** Called for an installed component, with the exact stack that created its environment. */
    default void onMachineConnect(Node node, ItemStack stack) {
        onMachineConnect(node);
    }

    void onMachineDisconnect(Node node);

    default String machinePosition() {
        if (world() != null) {
            return String.format("(%g, %g, %g, %s)", xPosition(), yPosition(), zPosition(), world().dimension().location());
        }
        return String.format("(%g, %g, %g)", xPosition(), yPosition(), zPosition());
    }
}
