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
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.Map;

public class TankControllerEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "tank_controller";

    private final EnvironmentHost host;

    public TankControllerEnvironment(final EnvironmentHost host) {
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
            DeviceInfo.DeviceAttribute.Description, "Tank controller",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
            DeviceInfo.DeviceAttribute.Product, "Tank Controller Upgrade"
        );
    }

    @Callback(doc = "function(side:number):number -- Get the number of tanks exposed by the fluid handler on the specified side.")
    public Object[] getTankCount(final Context context, final Arguments arguments) {
        return new Object[]{handler(arguments.checkInteger(0)).getTanks()};
    }

    @Callback(doc = "function(side:number, tank:number):number -- Get the fluid amount in the specified tank.")
    public Object[] getTankLevel(final Context context, final Arguments arguments) {
        final IFluidHandler handler = handler(arguments.checkInteger(0));
        final FluidStack stack = handler.getFluidInTank(checkTank(handler, arguments.checkInteger(1)));
        return new Object[]{stack.isEmpty() ? 0 : stack.getAmount()};
    }

    @Callback(doc = "function(side:number, tank:number):number -- Get the capacity of the specified tank.")
    public Object[] getTankCapacity(final Context context, final Arguments arguments) {
        final IFluidHandler handler = handler(arguments.checkInteger(0));
        return new Object[]{handler.getTankCapacity(checkTank(handler, arguments.checkInteger(1)))};
    }

    @Callback(doc = "function(side:number, tank:number):string, number, number -- Get fluid id, amount, and capacity for the specified tank.")
    public Object[] getFluidInTank(final Context context, final Arguments arguments) {
        final IFluidHandler handler = handler(arguments.checkInteger(0));
        final int tank = checkTank(handler, arguments.checkInteger(1));
        final FluidStack stack = handler.getFluidInTank(tank);
        return new Object[]{
            stack.isEmpty() ? "" : BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString(),
            stack.isEmpty() ? 0 : stack.getAmount(),
            handler.getTankCapacity(tank)
        };
    }

    private IFluidHandler handler(final int side) {
        if (side < 0 || side > 5) {
            throw new IllegalArgumentException("invalid side");
        }
        if (host == null || host.world() == null) {
            throw new IllegalStateException("no world");
        }

        final Direction direction = Direction.from3DDataValue(side);
        final BlockPos hostPos = BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition());
        final IFluidHandler handler = host.world().getCapability(Capabilities.FluidHandler.BLOCK, hostPos.relative(direction), direction.getOpposite());
        if (handler != null) {
            return handler;
        }
        throw new IllegalArgumentException("no tank");
    }

    private static int checkTank(final IFluidHandler handler, final int tank) {
        final int index = tank - 1;
        if (index < 0 || index >= handler.getTanks()) {
            throw new IllegalArgumentException("tank index out of bounds");
        }
        return index;
    }
}
