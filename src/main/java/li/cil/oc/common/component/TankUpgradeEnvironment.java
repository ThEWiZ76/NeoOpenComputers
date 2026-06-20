package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.Map;

public class TankUpgradeEnvironment extends AbstractManagedEnvironment implements IFluidTank, DeviceInfo {
    public static final int CAPACITY = 16000;

    private static final String FLUID_TAG = "fluid";
    private static final String AMOUNT_TAG = "amount";

    private final EnvironmentHost owner;
    private final FluidTank tank = new FluidTank(CAPACITY);

    public TankUpgradeEnvironment(final EnvironmentHost owner) {
        this.owner = owner;
        final var builder = Network.newNode(this, Visibility.None);
        if (builder != null) {
            setNode(builder.create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Tank upgrade",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
            DeviceInfo.DeviceAttribute.Product, "Superblubb V10",
            DeviceInfo.DeviceAttribute.Capacity, Integer.toString(CAPACITY)
        );
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        final String fluidId = nbt.getString(FLUID_TAG);
        final int amount = nbt.getInt(AMOUNT_TAG);
        if (!fluidId.isEmpty() && amount > 0) {
            final var fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidId));
            tank.setFluid(new FluidStack(fluid, Math.min(amount, CAPACITY)));
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        final FluidStack fluid = tank.getFluid();
        if (!fluid.isEmpty()) {
            nbt.putString(FLUID_TAG, BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
            nbt.putInt(AMOUNT_TAG, fluid.getAmount());
        }
    }

    @Override
    public FluidStack getFluid() {
        return tank.getFluid();
    }

    @Override
    public int getFluidAmount() {
        return tank.getFluidAmount();
    }

    @Override
    public int getCapacity() {
        return tank.getCapacity();
    }

    @Override
    public boolean isFluidValid(final FluidStack stack) {
        return tank.isFluidValid(stack);
    }

    @Override
    public int fill(final FluidStack stack, final FluidAction action) {
        final int amount = tank.fill(stack, action);
        if (action.execute() && amount > 0) {
            signalChanged(amount);
        }
        return amount;
    }

    @Override
    public FluidStack drain(final int maxDrain, final FluidAction action) {
        final FluidStack drained = tank.drain(maxDrain, action);
        if (action.execute() && !drained.isEmpty() && drained.getAmount() > 0) {
            signalChanged(-drained.getAmount());
        }
        return drained;
    }

    @Override
    public FluidStack drain(final FluidStack stack, final FluidAction action) {
        final FluidStack drained = tank.drain(stack, action);
        if (action.execute() && !drained.isEmpty() && drained.getAmount() > 0) {
            signalChanged(-drained.getAmount());
        }
        return drained;
    }

    private void signalChanged(final int amount) {
        if (node() != null) {
            node().sendToVisible("computer.signal", "tank_changed", tankIndex(), amount);
        }
    }

    private int tankIndex() {
        if (owner instanceof Agent agent && agent.tank() != null) {
            for (int index = 0; index < agent.tank().tankCount(); index++) {
                if (agent.tank().getFluidTank(index) == this) {
                    return index + 1;
                }
            }
        }
        return 1;
    }
}
