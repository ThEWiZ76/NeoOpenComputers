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
import li.cil.oc.mixin.AbstractFurnaceBlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

import java.util.Map;

public final class FurnaceBlockDriver implements DriverBlock {
    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        return world != null && pos != null && world.getBlockEntity(pos) instanceof FurnaceBlockEntity;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        if (world != null && pos != null && world.getBlockEntity(pos) instanceof FurnaceBlockEntity furnace) {
            return new Environment(furnace);
        }
        return null;
    }

    public static final class Environment extends AbstractManagedEnvironment implements NamedBlock, DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Furnace",
            DeviceInfo.DeviceAttribute.Vendor, "Minecraft",
            DeviceInfo.DeviceAttribute.Product, "Furnace"
        );

        private final AbstractFurnaceBlockEntityAccessor furnace;

        public Environment(final AbstractFurnaceBlockEntity furnace) {
            this.furnace = (AbstractFurnaceBlockEntityAccessor) furnace;
            setNode(Network.newNode(this, Visibility.Network)
                .withComponent("furnace", Visibility.Network)
                .create());
        }

        @Override
        public String preferredName() {
            return "furnace";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }

        @Callback(doc = "function():number -- The number of ticks that the furnace will keep burning from the last consumed fuel.")
        public Object[] getBurnTime(final Context context, final Arguments arguments) {
            return new Object[]{furnace.neoopencomputers$getLitTime()};
        }

        @Callback(doc = "function():number -- The number of ticks that the currently burning fuel lasts in total.")
        public Object[] getCurrentItemBurnTime(final Context context, final Arguments arguments) {
            return new Object[]{furnace.neoopencomputers$getLitDuration()};
        }

        @Callback(doc = "function():number -- The number of ticks that the current item has been cooking for.")
        public Object[] getCookTime(final Context context, final Arguments arguments) {
            return new Object[]{furnace.neoopencomputers$getCookingProgress()};
        }

        @Callback(doc = "function():number -- The number of ticks that the current item needs to cook.")
        public Object[] getTotalCookTime(final Context context, final Arguments arguments) {
            return new Object[]{furnace.neoopencomputers$getCookingTotalTime()};
        }

        @Callback(doc = "function():boolean -- Get whether the furnace is currently active.")
        public Object[] isBurning(final Context context, final Arguments arguments) {
            return new Object[]{furnace.neoopencomputers$getLitTime() > 0};
        }
    }
}
