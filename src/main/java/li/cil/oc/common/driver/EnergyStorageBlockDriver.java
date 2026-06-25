package li.cil.oc.common.driver;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.Map;

public final class EnergyStorageBlockDriver implements DriverBlock {
    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        return world != null
            && pos != null
            && world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        if (world == null || pos == null) {
            return null;
        }
        final IEnergyStorage storage = world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
        return storage == null ? null : new Environment(storage);
    }

    public static final class Environment extends AbstractManagedEnvironment implements NamedBlock, DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Energy storage",
            DeviceInfo.DeviceAttribute.Vendor, "Minecraft",
            DeviceInfo.DeviceAttribute.Product, "Energy storage"
        );

        private final IEnergyStorage storage;

        public Environment(final IEnergyStorage storage) {
            this.storage = storage;
            setNode(Network.newNode(this, Visibility.Network)
                .withComponent("energy_device", Visibility.Network)
                .create());
        }

        @Override
        public String preferredName() {
            return "energy_device";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }

        @Callback(doc = "function():number -- Returns the amount of stored energy on the connected side.")
        public Object[] getEnergyStored(final Context context, final Arguments args) {
            return new Object[]{storage.getEnergyStored()};
        }

        @Callback(doc = "function():number -- Returns the maximum amount of energy that can be stored on the connected side.")
        public Object[] getMaxEnergyStored(final Context context, final Arguments args) {
            return new Object[]{storage.getMaxEnergyStored()};
        }

        @Callback(doc = "function():boolean -- Returns whether energy can be extracted from the connected side.")
        public Object[] canExtract(final Context context, final Arguments args) {
            return new Object[]{storage.canExtract()};
        }

        @Callback(doc = "function():boolean -- Returns whether energy can be received by the connected side.")
        public Object[] canReceive(final Context context, final Arguments args) {
            return new Object[]{storage.canReceive()};
        }
    }
}
