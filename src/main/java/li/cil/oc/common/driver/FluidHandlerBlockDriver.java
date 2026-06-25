package li.cil.oc.common.driver;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FluidHandlerBlockDriver implements DriverBlock {
    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        return ModSettings.enableTankDriver()
            && world != null
            && pos != null
            && world.getCapability(Capabilities.FluidHandler.BLOCK, pos, side) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        if (!ModSettings.enableTankDriver() || world == null || pos == null) {
            return null;
        }
        final IFluidHandler handler = world.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
        return handler == null ? null : new Environment(handler);
    }

    static Map<Object, Object> tankInfo(final IFluidHandler handler, final int tank) {
        final FluidStack stack = handler.getFluidInTank(tank);
        final Map<Object, Object> info = new LinkedHashMap<>();
        info.put("capacity", handler.getTankCapacity(tank));
        info.put("amount", stack.getAmount());
        info.put("hasTag", !stack.isComponentsPatchEmpty());
        if (!stack.isEmpty()) {
            final ResourceLocation id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
            info.put("name", id == null ? "minecraft:empty" : id.toString());
            info.put("label", stack.getHoverName().getString());
        }
        return info;
    }

    public static final class Environment extends AbstractManagedEnvironment implements DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Fluid handler",
            DeviceInfo.DeviceAttribute.Vendor, "Minecraft",
            DeviceInfo.DeviceAttribute.Product, "Fluid handler"
        );

        private final IFluidHandler handler;

        public Environment(final IFluidHandler handler) {
            this.handler = handler;
            setNode(Network.newNode(this, Visibility.Network)
                .withComponent("fluid_handler", Visibility.Network)
                .create());
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }

        @Callback(doc = "function():table -- Get information about the tank accessible from the specified side.")
        public Object[] getTankInfo(final Context context, final Arguments args) {
            final Object[] result = new Object[handler.getTanks()];
            for (int tank = 0; tank < result.length; tank++) {
                result[tank] = tankInfo(handler, tank);
            }
            return result;
        }
    }
}
