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
import li.cil.oc.mixin.BrewingStandBlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

import java.util.Map;

public final class BrewingStandBlockDriver implements DriverBlock {
    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        return world != null && pos != null && world.getBlockEntity(pos) instanceof BrewingStandBlockEntity;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        if (world != null && pos != null && world.getBlockEntity(pos) instanceof BrewingStandBlockEntity brewingStand) {
            return new Environment(brewingStand);
        }
        return null;
    }

    public static final class Environment extends AbstractManagedEnvironment implements NamedBlock, DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Brewing Stand",
            DeviceInfo.DeviceAttribute.Vendor, "Minecraft",
            DeviceInfo.DeviceAttribute.Product, "Brewing Stand"
        );

        private final BrewingStandBlockEntityAccessor brewingStand;

        public Environment(final BrewingStandBlockEntity brewingStand) {
            this.brewingStand = (BrewingStandBlockEntityAccessor) brewingStand;
            setNode(Network.newNode(this, Visibility.Network)
                .withComponent("brewing_stand", Visibility.Network)
                .create());
        }

        @Override
        public String preferredName() {
            return "brewing_stand";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }

        @Callback(doc = "function():number -- Get the number of ticks remaining of the current brewing operation.")
        public Object[] getBrewTime(final Context context, final Arguments arguments) {
            return new Object[]{brewingStand.neoopencomputers$getBrewTime()};
        }
    }
}
