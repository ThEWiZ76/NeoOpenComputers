package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.nbt.CompoundTag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class NetworkCardEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    protected static final String COMPONENT_NAME = "modem";
    private static final String OPEN_PORTS_TAG = "openPorts";
    private static final String WAKE_MESSAGE_TAG = "wakeMessage";
    private static final String WAKE_MESSAGE_FUZZY_TAG = "wakeMessageFuzzy";
    private static final int MAX_OPEN_PORTS = 16;
    private static final int MAX_PACKET_SIZE = 8192;
    private static final int MAX_PACKET_PARTS = 8;
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;
    protected static final String NETWORK_MESSAGE = "network.message";
    private static final String MODEM_MESSAGE_SIGNAL = "modem_message";
    private static final Map<String, String> WIRED_DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Network,
        DeviceInfo.DeviceAttribute.Description, "Ethernet controller",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "42i520 (MPN-01)",
        DeviceInfo.DeviceAttribute.Version, "1.0",
        DeviceInfo.DeviceAttribute.Capacity, Integer.toString(MAX_PACKET_SIZE),
        DeviceInfo.DeviceAttribute.Size, Integer.toString(MAX_OPEN_PORTS),
        DeviceInfo.DeviceAttribute.Width, Integer.toString(MAX_PACKET_PARTS)
    );

    protected final EnvironmentHost host;
    private final Set<Integer> openPorts = new LinkedHashSet<>();
    private String wakeMessage;
    private boolean wakeMessageFuzzy;

    public NetworkCardEnvironment(final EnvironmentHost host) {
        this.host = host;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Neighbors).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return WIRED_DEVICE_INFO;
    }

    @Callback(doc = "function(port:number):boolean -- Opens the specified port.")
    public Object[] open(final Context context, final Arguments args) throws IOException {
        final int port = checkPort(args.checkInteger(0));
        if (openPorts.contains(port)) {
            return new Object[]{false};
        }
        if (openPorts.size() >= maxOpenPorts()) {
            throw new IOException("too many open ports");
        }
        openPorts.add(port);
        markChanged();
        return new Object[]{true};
    }

    @Callback(doc = "function([port:number]):boolean -- Closes the specified port, or all ports if omitted.")
    public Object[] close(final Context context, final Arguments args) {
        final boolean changed;
        if (args.count() == 0) {
            changed = !openPorts.isEmpty();
            openPorts.clear();
        } else {
            changed = openPorts.remove(checkPort(args.checkInteger(0)));
        }
        if (changed) {
            markChanged();
        }
        return new Object[]{changed};
    }

    @Callback(direct = true, doc = "function(port:number):boolean -- Returns whether the specified port is open.")
    public Object[] isOpen(final Context context, final Arguments args) {
        return new Object[]{openPorts.contains(checkPort(args.checkInteger(0)))};
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether this modem is wireless.")
    public Object[] isWireless(final Context context, final Arguments args) {
        return new Object[]{false};
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether this modem is wired.")
    public Object[] isWired(final Context context, final Arguments args) {
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function():string, boolean -- Get the current wake-up message.")
    public Object[] getWakeMessage(final Context context, final Arguments args) {
        return new Object[]{wakeMessage, wakeMessageFuzzy};
    }

    @Callback(doc = "function(message:string[, fuzzy:boolean]):string, boolean -- Set the wake-up message and whether to ignore additional data/parameters.")
    public Object[] setWakeMessage(final Context context, final Arguments args) {
        final String oldMessage = wakeMessage;
        final boolean oldFuzzy = wakeMessageFuzzy;

        if (args.optAny(0, null) == null) {
            wakeMessage = null;
        } else {
            wakeMessage = args.checkString(0);
        }
        wakeMessageFuzzy = args.optBoolean(1, wakeMessageFuzzy);
        markChanged();
        return new Object[]{oldMessage, oldFuzzy};
    }

    @Callback(doc = "function(address:string, port:number, ...):boolean -- Sends a packet to the specified address.")
    public Object[] send(final Context context, final Arguments args) throws IOException {
        final String address = args.checkString(0);
        final int port = checkPort(args.checkInteger(1));
        if (node() == null) {
            return new Object[]{false};
        }

        final Packet packet = Network.newPacket(node().address(), address, port, remaining(args, 2));
        if (packet == null) {
            return new Object[]{false};
        }

        doSend(context, address, packet);
        return new Object[]{true};
    }

    @Callback(doc = "function(port:number, ...):boolean -- Broadcasts a packet on the specified port.")
    public Object[] broadcast(final Context context, final Arguments args) throws IOException {
        final int port = checkPort(args.checkInteger(0));
        if (node() == null) {
            return new Object[]{false};
        }

        final Packet packet = Network.newPacket(node().address(), null, port, remaining(args, 1));
        if (packet == null) {
            return new Object[]{false};
        }

        doBroadcast(context, packet);
        return new Object[]{true};
    }

    @Override
    public void onMessage(final Message message) {
        if ("computer.started".equals(message.name()) || "computer.stopped".equals(message.name())) {
            if (isOwnComputerMessage(message) && !openPorts.isEmpty()) {
                openPorts.clear();
                markChanged();
            }
        } else if (NETWORK_MESSAGE.equals(message.name())) {
            receiveWiredPacket(message);
        }
    }

    @Override
    public void onDisconnect(final Node node) {
        if (node == node() && !openPorts.isEmpty()) {
            openPorts.clear();
            markChanged();
        }
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        openPorts.clear();
        for (int port : nbt.getIntArray(OPEN_PORTS_TAG)) {
            if (isValidPort(port)) {
                openPorts.add(port);
            }
        }
        wakeMessage = nbt.contains(WAKE_MESSAGE_TAG) ? nbt.getString(WAKE_MESSAGE_TAG) : null;
        wakeMessageFuzzy = nbt.getBoolean(WAKE_MESSAGE_FUZZY_TAG);
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        nbt.putIntArray(OPEN_PORTS_TAG, openPorts.stream().mapToInt(Integer::intValue).toArray());
        if (wakeMessage != null) {
            nbt.putString(WAKE_MESSAGE_TAG, wakeMessage);
        }
        nbt.putBoolean(WAKE_MESSAGE_FUZZY_TAG, wakeMessageFuzzy);
    }

    protected void markChanged() {
        if (host != null) {
            host.markChanged();
        }
    }

    protected int maxOpenPorts() {
        return MAX_OPEN_PORTS;
    }

    private boolean isOwnComputerMessage(final Message message) {
        if (!(host instanceof MachineHost machineHost)) {
            return false;
        }
        final var machine = machineHost.machine();
        return machine != null && message.source() == machine.node();
    }

    protected void doSend(final Context context, final String address, final Packet packet) throws IOException {
        node().sendToAddress(address, NETWORK_MESSAGE, packet);
    }

    protected void doBroadcast(final Context context, final Packet packet) throws IOException {
        node().sendToReachable(NETWORK_MESSAGE, packet);
    }

    private void receiveWiredPacket(final Message message) {
        if (message.data().length == 0 || !(message.data()[0] instanceof Packet packet)) {
            return;
        }
        receivePacket(packet, 0D);
    }

    protected void receivePacket(final Packet packet, final double distance) {
        if (!(host instanceof MachineHost machineHost) || node() == null) {
            return;
        }
        final var machine = machineHost.machine();
        if (machine == null) {
            return;
        }
        final String localAddress = node().address();
        if ((localAddress != null && localAddress.equals(packet.source())) ||
            (packet.destination() != null && (localAddress == null || !localAddress.equals(packet.destination())))) {
            return;
        }

        final Object[] packetData = packet.data();
        if (openPorts.contains(packet.port())) {
            final Object[] signalArgs = new Object[4 + packetData.length];
            signalArgs[0] = node().address();
            signalArgs[1] = packet.source();
            signalArgs[2] = packet.port();
            signalArgs[3] = distance;
            System.arraycopy(packetData, 0, signalArgs, 4, packetData.length);
            machine.signal(MODEM_MESSAGE_SIGNAL, signalArgs);
        }
        if (isWakePacket(packetData)) {
            machine.start();
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

    private static Object[] remaining(final Arguments args, final int offset) {
        final Object[] values = args.toArray();
        if (values.length <= offset) {
            return new Object[0];
        }
        return Arrays.copyOfRange(values, offset, values.length);
    }

    private static int checkPort(final int port) {
        if (!isValidPort(port)) {
            throw new IllegalArgumentException("invalid port number");
        }
        return port;
    }

    private static boolean isValidPort(final int port) {
        return port >= MIN_PORT && port <= MAX_PORT;
    }
}
