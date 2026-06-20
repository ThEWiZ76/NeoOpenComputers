package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.WirelessEndpoint;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.Map;

public class WirelessNetworkCardEnvironment extends NetworkCardEnvironment implements WirelessEndpoint {
    private static final String STRENGTH_TAG = "strength";
    private static final double[] MAX_RANGE_BY_TIER = {16D, 400D};
    private static final Map<String, String> TIER1_DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Network,
        DeviceInfo.DeviceAttribute.Description, "Wireless ethernet controller",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "39i110 (LPPW-01)",
        DeviceInfo.DeviceAttribute.Version, "1.0",
        DeviceInfo.DeviceAttribute.Capacity, "8192",
        DeviceInfo.DeviceAttribute.Size, "16",
        DeviceInfo.DeviceAttribute.Width, "16.0"
    );
    private static final Map<String, String> TIER2_DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Network,
        DeviceInfo.DeviceAttribute.Description, "Wireless ethernet controller",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "62i230 (MPW-01)",
        DeviceInfo.DeviceAttribute.Version, "2.0",
        DeviceInfo.DeviceAttribute.Capacity, "8192",
        DeviceInfo.DeviceAttribute.Size, "16",
        DeviceInfo.DeviceAttribute.Width, "400.0"
    );

    private final int tier;
    private double strength;

    public WirelessNetworkCardEnvironment(final EnvironmentHost host, final int tier) {
        super(host);
        this.tier = Math.max(0, Math.min(tier, MAX_RANGE_BY_TIER.length - 1));
        strength = maxWirelessRange();
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return tier == 0 ? TIER1_DEVICE_INFO : TIER2_DEVICE_INFO;
    }

    @Override
    public int x() {
        return (int) Math.floor(host == null ? 0D : host.xPosition());
    }

    @Override
    public int y() {
        return (int) Math.floor(host == null ? 0D : host.yPosition());
    }

    @Override
    public int z() {
        return (int) Math.floor(host == null ? 0D : host.zPosition());
    }

    @Override
    public Level world() {
        return host == null ? null : host.world();
    }

    @Override
    public void receivePacket(final Packet packet, final WirelessEndpoint sender) {
        final double distance = distanceTo(sender);
        if (distance <= maxWirelessRange() && (distance > 0D || isWiredTier())) {
            receivePacket(packet, distance);
        }
    }

    @Callback(direct = true, doc = "function():number -- Get the signal strength (range) used when sending messages.")
    public Object[] getStrength(final Context context, final Arguments args) {
        return new Object[]{strength};
    }

    @Callback(doc = "function(strength:number):number -- Set the signal strength (range) used when sending messages.")
    public Object[] setStrength(final Context context, final Arguments args) {
        strength = Math.max(0D, Math.min(args.checkDouble(0), maxWirelessRange()));
        markChanged();
        return new Object[]{strength};
    }

    @Override
    public Object[] isWireless(final Context context, final Arguments args) {
        return new Object[]{true};
    }

    @Override
    public Object[] isWired(final Context context, final Arguments args) {
        return new Object[]{isWiredTier()};
    }

    @Override
    protected void doSend(final String address, final Packet packet) {
        if (strength > 0D) {
            Network.sendWirelessPacket(this, strength, packet);
        }
        if (isWiredTier()) {
            super.doSend(address, packet);
        }
    }

    @Override
    protected void doBroadcast(final Packet packet) {
        if (strength > 0D) {
            Network.sendWirelessPacket(this, strength, packet);
        }
        if (isWiredTier()) {
            super.doBroadcast(packet);
        }
    }

    @Override
    public void onConnect(final Node node) {
        super.onConnect(node);
        if (node == node()) {
            Network.joinWirelessNetwork(this);
        }
    }

    @Override
    public void onDisconnect(final Node node) {
        super.onDisconnect(node);
        if (node == node()) {
            Network.leaveWirelessNetwork(this);
        }
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains(STRENGTH_TAG)) {
            strength = Math.max(0D, Math.min(nbt.getDouble(STRENGTH_TAG), maxWirelessRange()));
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        nbt.putDouble(STRENGTH_TAG, strength);
    }

    private boolean isWiredTier() {
        return tier >= 1;
    }

    private double maxWirelessRange() {
        return MAX_RANGE_BY_TIER[tier];
    }

    private double distanceTo(final WirelessEndpoint sender) {
        if (sender == null || host == null) {
            return 0D;
        }
        final double dx = (sender.x() + 0.5D) - host.xPosition();
        final double dy = (sender.y() + 0.5D) - host.yPosition();
        final double dz = (sender.z() + 0.5D) - host.zPosition();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
