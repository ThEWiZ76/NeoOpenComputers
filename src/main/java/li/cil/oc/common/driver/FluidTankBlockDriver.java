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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FluidTankBlockDriver implements DriverBlock {
    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        return ModSettings.enableTankDriver()
            && world != null
            && pos != null
            && world.getBlockEntity(pos) instanceof IFluidTank;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        if (worksWith(world, pos, side) && world.getBlockEntity(pos) instanceof IFluidTank tank) {
            return new Environment(tank);
        }
        return null;
    }

    public static final class Environment extends AbstractManagedEnvironment implements DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Fluid tank",
            DeviceInfo.DeviceAttribute.Vendor, "Minecraft",
            DeviceInfo.DeviceAttribute.Product, "Fluid tank"
        );

        private final IFluidTank tank;

        public Environment(final IFluidTank tank) {
            this.tank = tank;
            setNode(Network.newNode(this, Visibility.Network)
                .withComponent("fluid_tank", Visibility.Network)
                .create());
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }

        @Callback(doc = "function():table -- Get information about this tank.")
        public Object[] getInfo(final Context context, final Arguments args) {
            final FluidStack stored = tank.getFluid();
            final FluidStack stack = stored == null ? FluidStack.EMPTY : stored;
            final Map<Object, Object> info = new LinkedHashMap<>();
            info.put("capacity", tank.getCapacity());
            info.put("amount", stack.getAmount());
            info.put("hasTag", !stack.isComponentsPatchEmpty());
            if (!stack.isEmpty()) {
                final ResourceLocation id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
                info.put("name", id == null ? "minecraft:empty" : id.toString());
                info.put("label", stack.getHoverName().getString());
            }
            return new Object[]{info};
        }
    }
}
