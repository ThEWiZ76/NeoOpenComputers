package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.util.FluidDescriptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
            final Visibility visibility = host instanceof Adapter ? Visibility.Network : Visibility.Neighbors;
            setNode(builder.withComponent(COMPONENT_NAME, visibility).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Tank controller",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "FlowCheckDX"
        );
    }

    @Callback(doc = "function(side:number):number -- Get the number of tanks exposed by the fluid handler on the specified side.")
    public Object[] getTankCount(final Context context, final Arguments arguments) {
        final IFluidHandler handler = handler(arguments.checkInteger(0));
        return handler == null ? noTank() : new Object[]{handler.getTanks()};
    }

    @Callback(doc = "function(side:number[, tank:number]):number -- Get the fluid amount in the specified tank, or the total amount in all tanks.")
    public Object[] getTankLevel(final Context context, final Arguments arguments) {
        final IFluidHandler handler = handler(arguments.checkInteger(0));
        if (handler == null) {
            return noTank();
        }
        if (arguments.count() > 1 && arguments.checkAny(1) != null) {
            final FluidStack stack = handler.getFluidInTank(checkTank(handler, arguments.checkInteger(1)));
            return new Object[]{stack.isEmpty() ? 0 : stack.getAmount()};
        }
        int amount = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            final FluidStack stack = handler.getFluidInTank(tank);
            if (!stack.isEmpty()) {
                amount += stack.getAmount();
            }
        }
        return new Object[]{amount};
    }

    @Callback(doc = "function(side:number[, tank:number]):number -- Get the capacity of the specified tank, or the maximum capacity on the side.")
    public Object[] getTankCapacity(final Context context, final Arguments arguments) {
        final IFluidHandler handler = handler(arguments.checkInteger(0));
        if (handler == null) {
            return noTank();
        }
        if (arguments.count() > 1 && arguments.checkAny(1) != null) {
            return new Object[]{handler.getTankCapacity(checkTank(handler, arguments.checkInteger(1)))};
        }
        int capacity = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            capacity = Math.max(capacity, handler.getTankCapacity(tank));
        }
        return new Object[]{capacity};
    }

    @Callback(doc = "function(side:number[, tank:number]):table -- Get fluid info for the specified tank, or all tanks on the side.")
    public Object[] getFluidInTank(final Context context, final Arguments arguments) {
        final IFluidHandler handler = handler(arguments.checkInteger(0));
        if (handler == null) {
            return noTank();
        }
        if (arguments.count() > 1 && arguments.checkAny(1) != null) {
            return FluidDescriptions.describe(handler, checkTank(handler, arguments.checkInteger(1)));
        }
        return new Object[]{FluidDescriptions.describeAll(handler)};
    }

    private IFluidHandler handler(final int side) {
        if (side < 0 || side > 5) {
            throw new IllegalArgumentException("invalid side");
        }
        if (host == null || host.world() == null) {
            return null;
        }

        final Direction direction = host instanceof Robot robot ? robot.toGlobal(Direction.from3DDataValue(side)) : Direction.from3DDataValue(side);
        final BlockPos hostPos = BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition());
        final IFluidHandler handler = host.world().getCapability(Capabilities.FluidHandler.BLOCK, hostPos.relative(direction), direction.getOpposite());
        if (handler != null) {
            return handler;
        }
        return null;
    }

    private static Object[] noTank() {
        return new Object[]{null, "no tank"};
    }

    private static int checkTank(final IFluidHandler handler, final int tank) {
        final int index = tank - 1;
        if (index < 0 || index >= handler.getTanks()) {
            throw new IllegalArgumentException("invalid tank index");
        }
        return index;
    }
}
