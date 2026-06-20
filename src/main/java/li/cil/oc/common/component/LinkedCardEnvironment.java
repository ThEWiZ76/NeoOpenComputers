package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.nbt.CompoundTag;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LinkedCardEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "tunnel";
    private static final String TUNNEL_TAG = "oc:tunnel";
    private static final String WAKE_MESSAGE_TAG = "wakeMessage";
    private static final String WAKE_MESSAGE_FUZZY_TAG = "wakeMessageFuzzy";
    private static final String MODEM_MESSAGE_SIGNAL = "modem_message";
    private static final int MAX_PACKET_SIZE = 8192;
    private static final int MAX_PACKET_PARTS = 8;
    private static final Map<String, List<LinkedCardEnvironment>> CHANNELS = new LinkedHashMap<>();
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Network,
        DeviceInfo.DeviceAttribute.Description, "Quantumnet controller",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "HyperLink IV: Ender Edition",
        DeviceInfo.DeviceAttribute.Capacity, Integer.toString(MAX_PACKET_SIZE),
        DeviceInfo.DeviceAttribute.Width, Integer.toString(MAX_PACKET_PARTS)
    );

    private final EnvironmentHost host;
    private String channel;
    private String wakeMessage;
    private boolean wakeMessageFuzzy;

    public LinkedCardEnvironment(final EnvironmentHost host, final String channel) {
        this.host = host;
        this.channel = normalizeChannel(channel);
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Neighbors).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
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
        for (LinkedCardEnvironment endpoint : List.copyOf(CHANNELS.getOrDefault(channel, List.of()))) {
            if (endpoint != this) {
                endpoint.receivePacket(packet);
            }
        }
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function():number -- Gets the maximum packet size.")
    public Object[] maxPacketSize(final Context context, final Arguments args) {
        return new Object[]{MAX_PACKET_SIZE};
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
            CHANNELS.computeIfAbsent(channel, ignored -> new ArrayList<>()).add(this);
        }
    }

    @Override
    public void onDisconnect(final Node node) {
        if (node == node()) {
            removeFromChannel();
        }
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        removeFromChannel();
        if (nbt.contains(TUNNEL_TAG)) {
            channel = normalizeChannel(nbt.getString(TUNNEL_TAG));
        }
        if (nbt.contains(WAKE_MESSAGE_TAG)) {
            wakeMessage = nbt.getString(WAKE_MESSAGE_TAG);
        }
        wakeMessageFuzzy = nbt.getBoolean(WAKE_MESSAGE_FUZZY_TAG);
        if (node() != null && node().network() != null) {
            CHANNELS.computeIfAbsent(channel, ignored -> new ArrayList<>()).add(this);
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

    private void receivePacket(final Packet packet) {
        if (!(host instanceof MachineHost machineHost) || machineHost.machine() == null || node() == null) {
            return;
        }
        if (node().address() != null && node().address().equals(packet.source())) {
            return;
        }
        final Object[] data = packet.data();
        final Object[] signalArgs = new Object[3 + data.length];
        signalArgs[0] = packet.source();
        signalArgs[1] = packet.port();
        signalArgs[2] = 0D;
        System.arraycopy(data, 0, signalArgs, 3, data.length);
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

    private void removeFromChannel() {
        final List<LinkedCardEnvironment> endpoints = CHANNELS.get(channel);
        if (endpoints != null) {
            endpoints.remove(this);
            if (endpoints.isEmpty()) {
                CHANNELS.remove(channel);
            }
        }
    }

    private static String normalizeChannel(final String value) {
        return value == null || value.isBlank() ? "creative" : value;
    }
}
