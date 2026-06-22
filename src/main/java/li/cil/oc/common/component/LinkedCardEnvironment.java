package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModSettings;
import net.minecraft.nbt.CompoundTag;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LinkedCardEnvironment extends AbstractManagedEnvironment implements DeviceInfo, LinkedNetwork.Endpoint {
    private static final String COMPONENT_NAME = "tunnel";
    private static final String TUNNEL_TAG = "oc:tunnel";
    private static final String WAKE_MESSAGE_TAG = "wakeMessage";
    private static final String WAKE_MESSAGE_FUZZY_TAG = "wakeMessageFuzzy";
    private static final String MODEM_MESSAGE_SIGNAL = "modem_message";
    private static final double LINKED_CARD_BASE_COST = 0.05D * 400D * 5D;

    private final EnvironmentHost host;
    private String channel;
    private String wakeMessage;
    private boolean wakeMessageFuzzy;

    public LinkedCardEnvironment(final EnvironmentHost host, final String channel) {
        this.host = host;
        this.channel = LinkedNetwork.normalizeChannel(channel);
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Neighbors).withConnector().create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Network,
            DeviceInfo.DeviceAttribute.Description, "Quantumnet controller",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
            DeviceInfo.DeviceAttribute.Product, "HyperLink IV: Ender Edition",
            DeviceInfo.DeviceAttribute.Capacity, Integer.toString(ModSettings.maxNetworkPacketSize()),
            DeviceInfo.DeviceAttribute.Width, Integer.toString(ModSettings.maxNetworkPacketParts())
        );
    }

    @Callback(doc = "function(data...):boolean -- Sends the specified data to linked cards on this channel.")
    public Object[] send(final Context context, final Arguments args) {
        if (node() == null) {
            return new Object[]{false};
        }
        final Packet packet = Network.newPacket(node().address(), null, 0, args.toArray());
        if (packet == null) {
            return new Object[]{false};
        }
        if (!consumeEnergy(context, packet)) {
            return new Object[]{null, "not enough energy"};
        }
        LinkedNetwork.send(channel, this, packet);
        return new Object[]{true};
    }

    private static boolean consumeEnergy(final Context context, final Packet packet) {
        if (context == null || !(context.node() instanceof Connector connector)) {
            return true;
        }
        final double cost = packet.size() / 32.0D + LINKED_CARD_BASE_COST;
        return connector.tryChangeBuffer(-cost);
    }

    @Callback(direct = true, doc = "function():number -- Gets the maximum packet size.")
    public Object[] maxPacketSize(final Context context, final Arguments args) {
        return new Object[]{ModSettings.maxNetworkPacketSize()};
    }

    @Callback(direct = true, doc = "function():string -- Gets this linked card's shared channel.")
    public Object[] getChannel(final Context context, final Arguments args) {
        return new Object[]{channel};
    }

    @Callback(direct = true, doc = "function():string, boolean -- Get the current wake-up message.")
    public Object[] getWakeMessage(final Context context, final Arguments args) {
        return new Object[]{wakeMessage, wakeMessageFuzzy};
    }

    @Callback(doc = "function(message:string[, fuzzy:boolean]):string, boolean -- Set the wake-up message.")
    public Object[] setWakeMessage(final Context context, final Arguments args) {
        final String oldMessage = wakeMessage;
        final boolean oldFuzzy = wakeMessageFuzzy;
        wakeMessage = args.optAny(0, null) == null ? null : args.checkString(0);
        wakeMessageFuzzy = args.optBoolean(1, wakeMessageFuzzy);
        if (host != null) {
            host.markChanged();
        }
        return new Object[]{oldMessage, oldFuzzy};
    }

    @Override
    public void onConnect(final Node node) {
        if (node == node()) {
            LinkedNetwork.add(this);
        }
    }

    @Override
    public void onDisconnect(final Node node) {
        if (node == node()) {
            LinkedNetwork.remove(this);
        }
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        LinkedNetwork.remove(this);
        if (nbt.contains(TUNNEL_TAG)) {
            channel = LinkedNetwork.normalizeChannel(nbt.getString(TUNNEL_TAG));
        }
        if (nbt.contains(WAKE_MESSAGE_TAG)) {
            wakeMessage = nbt.getString(WAKE_MESSAGE_TAG);
        }
        wakeMessageFuzzy = nbt.getBoolean(WAKE_MESSAGE_FUZZY_TAG);
        if (node() != null && node().network() != null) {
            LinkedNetwork.add(this);
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        nbt.putString(TUNNEL_TAG, channel);
        if (wakeMessage != null) {
            nbt.putString(WAKE_MESSAGE_TAG, wakeMessage);
        }
        nbt.putBoolean(WAKE_MESSAGE_FUZZY_TAG, wakeMessageFuzzy);
    }

    @Override
    public String linkedChannel() {
        return channel;
    }

    @Override
    public void receiveLinkedPacket(final Packet packet) {
        if (!(host instanceof MachineHost machineHost) || machineHost.machine() == null || node() == null) {
            return;
        }
        if (node().address() != null && node().address().equals(packet.source())) {
            return;
        }
        final Object[] data = packet.data();
        final Object[] signalArgs = new Object[4 + data.length];
        signalArgs[0] = node().address();
        signalArgs[1] = packet.source();
        signalArgs[2] = packet.port();
        signalArgs[3] = 0D;
        System.arraycopy(data, 0, signalArgs, 4, data.length);
        machineHost.machine().signal(MODEM_MESSAGE_SIGNAL, signalArgs);
        if (isWakePacket(data)) {
            machineHost.machine().start();
        }
    }

    private boolean isWakePacket(final Object[] packetData) {
        if (wakeMessage == null || packetData.length == 0 || (!wakeMessageFuzzy && packetData.length != 1)) {
            return false;
        }
        final Object message = packetData[0];
        if (message instanceof String value) {
            return wakeMessage.equals(value);
        }
        if (message instanceof byte[] value) {
            return wakeMessage.equals(new String(value, StandardCharsets.UTF_8));
        }
        return false;
    }
}
