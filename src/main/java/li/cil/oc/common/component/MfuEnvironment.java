package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;

public final class MfuEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    public static final int TARGET_TAG_LENGTH = 4;

    private static final String TARGET_TAG = "oc:target";
    private static final String SIDE_TAG = "oc:side";
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Bus,
        DeviceInfo.DeviceAttribute.Description, "Remote Adapter",
        DeviceInfo.DeviceAttribute.Vendor, "Scummtech, Inc.",
        DeviceInfo.DeviceAttribute.Product, "ERR NAME NOT FOUND"
    );

    private final BlockPos target;
    private final Direction side;

    public MfuEnvironment(final EnvironmentHost host, final BlockPos target, final Direction side) {
        this.target = target;
        this.side = side == null ? Direction.NORTH : side;
        final var builder = Network.newNode(this, Visibility.None);
        if (builder != null) {
            setNode(builder.withConnector().create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    public BlockPos target() {
        return target;
    }

    public Direction side() {
        return side;
    }

    @Override
    public void save(final CompoundTag tag) {
        super.save(tag);
        tag.putIntArray(TARGET_TAG, new int[]{target.getX(), target.getY(), target.getZ()});
        tag.putInt(SIDE_TAG, side.ordinal());
    }
}
