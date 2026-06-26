package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.util.FluidDescriptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

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
        if (!ModSettings.allowItemStackInspection()) {
            return notEnabled();
        }
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

    protected static Object[] noTank() {
        return new Object[]{null, "no tank"};
    }

    protected static Object[] notEnabled() {
        return new Object[]{null, "not enabled in config"};
    }

    private static int checkTank(final IFluidHandler handler, final int tank) {
        final int index = tank - 1;
        if (index < 0 || index >= handler.getTanks()) {
            throw new IllegalArgumentException("invalid tank index");
        }
        return index;
    }

    public static class AgentTankControllerEnvironment extends TankControllerEnvironment {
        private final Agent agent;

        public AgentTankControllerEnvironment(final Agent agent) {
            super(agent);
            this.agent = agent;
        }

        @Callback(doc = "function([slot:number]):number -- Get the amount of fluid in the tank item in the specified internal slot or selected slot.")
        public Object[] getTankLevelInSlot(final Context context, final Arguments arguments) {
            final IFluidHandlerItem handler = itemFluidHandler(checkInternalSlot(arguments, 0));
            return handler == null ? notFluidContainer() : new Object[]{handler.getFluidInTank(0).getAmount()};
        }

        @Callback(doc = "function([slot:number]):number -- Get the capacity of the tank item in the specified internal slot or selected slot.")
        public Object[] getTankCapacityInSlot(final Context context, final Arguments arguments) {
            final IFluidHandlerItem handler = itemFluidHandler(checkInternalSlot(arguments, 0));
            return handler == null ? notFluidContainer() : new Object[]{handler.getTankCapacity(0)};
        }

        @Callback(doc = "function([slot:number]):table -- Get fluid info for the tank item in the specified internal slot or selected slot.")
        public Object[] getFluidInTankInSlot(final Context context, final Arguments arguments) {
            if (!ModSettings.allowItemStackInspection()) {
                return notEnabled();
            }
            final IFluidHandlerItem handler = itemFluidHandler(checkInternalSlot(arguments, 0));
            return handler == null ? notFluidContainer() : FluidDescriptions.describe(handler, 0);
        }

        @Callback(doc = "function([tank:number]):table -- Get fluid info for the specified internal tank or selected tank.")
        public Object[] getFluidInInternalTank(final Context context, final Arguments arguments) {
            if (!ModSettings.allowItemStackInspection()) {
                return notEnabled();
            }
            final IFluidTank tank = internalTank(checkInternalTank(arguments, 0));
            return tank == null ? noTank() : describe(tank);
        }

        @Callback(doc = "function([amount:number]):boolean, number or string -- Transfer fluid from the selected inventory item into the selected tank.")
        public Object[] drain(final Context context, final Arguments arguments) {
            final IFluidTank into = internalTank(agent.selectedTank());
            if (into == null) {
                return noTank();
            }
            final IFluidHandlerItem handler = itemFluidHandler(agent.selectedSlot());
            if (handler == null) {
                return notFluidContainer();
            }
            final FluidStack drained = handler.drain(fluidAmount(arguments, 0), FluidAction.SIMULATE);
            final int transferred = into.fill(drained, FluidAction.EXECUTE);
            if (transferred <= 0) {
                return incompatible();
            }
            handler.drain(transferred, FluidAction.EXECUTE);
            updateSelectedContainer(handler.getContainer());
            return new Object[]{true, transferred};
        }

        @Callback(doc = "function([amount:number]):boolean, number or string -- Transfer fluid from the selected tank into the selected inventory item.")
        public Object[] fill(final Context context, final Arguments arguments) {
            final IFluidTank from = internalTank(agent.selectedTank());
            if (from == null) {
                return noTank();
            }
            final IFluidHandlerItem handler = itemFluidHandler(agent.selectedSlot());
            if (handler == null) {
                return notFluidContainer();
            }
            final FluidStack drained = from.drain(fluidAmount(arguments, 0), FluidAction.SIMULATE);
            final int transferred = handler.fill(drained, FluidAction.EXECUTE);
            if (transferred <= 0) {
                return incompatible();
            }
            from.drain(transferred, FluidAction.EXECUTE);
            updateSelectedContainer(handler.getContainer());
            return new Object[]{true, transferred};
        }

        private int checkInternalSlot(final Arguments arguments, final int index) {
            final Container inventory = agent.mainInventory();
            final int slot = arguments.count() > index && arguments.checkAny(index) != null
                ? arguments.checkInteger(index) - 1
                : agent.selectedSlot();
            if (inventory == null || slot < 0 || slot >= inventory.getContainerSize()) {
                throw new IllegalArgumentException("invalid slot");
            }
            return slot;
        }

        private int checkInternalTank(final Arguments arguments, final int index) {
            final int tank = arguments.count() > index && arguments.checkAny(index) != null
                ? arguments.checkInteger(index) - 1
                : agent.selectedTank();
            final var tanks = agent.tank();
            if (tanks == null || tank < 0 || tank >= tanks.tankCount()) {
                throw new IllegalArgumentException("invalid tank index");
            }
            return tank;
        }

        private IFluidHandlerItem itemFluidHandler(final int slot) {
            final Container inventory = agent.mainInventory();
            if (inventory == null || slot < 0 || slot >= inventory.getContainerSize()) {
                return null;
            }
            final ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.isEmpty()) {
                return null;
            }
            final IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
            return handler != null && handler.getTanks() > 0 ? handler : null;
        }

        private IFluidTank internalTank(final int index) {
            final var tanks = agent.tank();
            if (tanks == null || index < 0 || index >= tanks.tankCount()) {
                return null;
            }
            return tanks.getFluidTank(index);
        }

        private void updateSelectedContainer(final ItemStack stack) {
            final Container inventory = agent.mainInventory();
            if (inventory != null) {
                inventory.setItem(agent.selectedSlot(), stack);
                inventory.setChanged();
            }
            agent.markChanged();
        }

        private static int fluidAmount(final Arguments arguments, final int index) {
            return Math.max(0, arguments.count() > index && arguments.checkAny(index) != null ? arguments.checkInteger(index) : 1000);
        }

        private static Object[] describe(final IFluidTank tank) {
            final FluidStack stack = tank.getFluid();
            return new Object[]{
                stack.isEmpty() ? "" : BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString(),
                stack.isEmpty() ? 0 : stack.getAmount(),
                tank.getCapacity()
            };
        }

        private static Object[] incompatible() {
            return new Object[]{null, "incompatible or no fluid"};
        }

        private static Object[] notFluidContainer() {
            return new Object[]{null, "item is not a fluid container"};
        }
    }
}
