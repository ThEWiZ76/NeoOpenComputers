package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;

public class InventoryControllerEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "inventory_controller";

    private final EnvironmentHost host;

    public InventoryControllerEnvironment(final EnvironmentHost host) {
        this.host = host;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Network).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Inventory controller",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
            DeviceInfo.DeviceAttribute.Product, "Inventory Controller Upgrade"
        );
    }

    @Callback(doc = "function(side:number):number -- Get the number of slots in the inventory on the specified side.")
    public Object[] getInventorySize(final Context context, final Arguments arguments) {
        return new Object[]{container(arguments.checkInteger(0)).getContainerSize()};
    }

    @Callback(doc = "function(side:number, slot:number):number -- Get the stack size of the item stack in the specified inventory slot.")
    public Object[] getSlotStackSize(final Context context, final Arguments arguments) {
        final Container container = container(arguments.checkInteger(0));
        final ItemStack stack = container.getItem(checkSlot(container, arguments.checkInteger(1)));
        return new Object[]{stack.isEmpty() ? 0 : stack.getCount()};
    }

    private Container container(final int side) {
        if (side < 0 || side > 5) {
            throw new IllegalArgumentException("invalid side");
        }
        if (host == null || host.world() == null) {
            throw new IllegalStateException("no world");
        }

        final BlockPos hostPos = BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition());
        final BlockEntity blockEntity = host.world().getBlockEntity(hostPos.relative(Direction.from3DDataValue(side)));
        if (blockEntity instanceof Container container) {
            return container;
        }
        throw new IllegalArgumentException("no inventory");
    }

    private static int checkSlot(final Container container, final int slot) {
        final int index = slot - 1;
        if (index < 0 || index >= container.getContainerSize()) {
            throw new IllegalArgumentException("slot index out of bounds");
        }
        return index;
    }
}
