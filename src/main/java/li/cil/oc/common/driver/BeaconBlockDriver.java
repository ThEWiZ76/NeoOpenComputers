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
import li.cil.oc.mixin.BeaconBlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

import javax.annotation.Nullable;
import java.util.Map;

public final class BeaconBlockDriver implements DriverBlock {
    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        return world != null && pos != null && world.getBlockEntity(pos) instanceof BeaconBlockEntity;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        if (world != null && pos != null && world.getBlockEntity(pos) instanceof BeaconBlockEntity beacon) {
            return new Environment(beacon);
        }
        return null;
    }

    public static final class Environment extends AbstractManagedEnvironment implements NamedBlock, DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Beacon",
            DeviceInfo.DeviceAttribute.Vendor, "Minecraft",
            DeviceInfo.DeviceAttribute.Product, "Beacon"
        );

        private final BeaconBlockEntityAccessor beacon;

        public Environment(final BeaconBlockEntity beacon) {
            this.beacon = (BeaconBlockEntityAccessor) beacon;
            setNode(Network.newNode(this, Visibility.Network)
                .withComponent("beacon", Visibility.Network)
                .create());
        }

        @Override
        public String preferredName() {
            return "beacon";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }

        @Callback(doc = "function():number -- Get the number of levels for this beacon.")
        public Object[] getLevels(final Context context, final Arguments arguments) {
            return new Object[]{beacon.neoopencomputers$getLevels()};
        }

        @Callback(doc = "function():string -- Get the name of the active primary effect.")
        public Object[] getPrimaryEffect(final Context context, final Arguments arguments) {
            return new Object[]{effectName(beacon.neoopencomputers$getPrimaryPower())};
        }

        @Callback(doc = "function():string -- Get the name of the active secondary effect.")
        public Object[] getSecondaryEffect(final Context context, final Arguments arguments) {
            return new Object[]{effectName(beacon.neoopencomputers$getSecondaryPower())};
        }

        @Nullable
        private static String effectName(@Nullable final Holder<MobEffect> effect) {
            return effect == null ? null : effect.value().getDescriptionId();
        }
    }
}
