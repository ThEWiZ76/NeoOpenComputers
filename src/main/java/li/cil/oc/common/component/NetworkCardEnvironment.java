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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class NetworkCardEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "modem";
    private static final String OPEN_PORTS_TAG = "openPorts";
    private static final int MAX_OPEN_PORTS = 16;
    private static final int MAX_PACKET_SIZE = 8192;
    private static final int MAX_PACKET_PARTS = 8;
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;
    private static final String NETWORK_MESSAGE = "network.message";
    private static final String MODEM_MESSAGE_SIGNAL = "modem_message";
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Network,
        DeviceInfo.DeviceAttribute.Description, "Ethernet controller",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "42i520 (MPN-01)",
        DeviceInfo.DeviceAttribute.Version, "1.0",
        DeviceInfo.DeviceAttribute.Capacity, Integer.toString(MAX_PACKET_SIZE),
        DeviceInfo.DeviceAttribute.Size, Integer.toString(MAX_OPEN_PORTS),
        DeviceInfo.DeviceAttribute.Width, Integer.toString(MAX_PACKET_PARTS)
    );

    private final EnvironmentHost host;
    private final Set<Integer> openPorts = new LinkedHashSet<>();

    public NetworkCardEnvironment(final EnvironmentHost host) {
        this.host = host;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Neighbors).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    @Callback(doc = "function(port:number):boolean -- Opens the specified port.")
    public Object[] open(final Context context, final Arguments args) throws IOException {
        final int port = checkPort(args.checkInteger(0));
        if (openPorts.contains(port)) {
            return new Object[]{false};
        }
        if (openPorts.size() >= MAX_OPEN_PORTS) {
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

    @Callback(doc = "function(address:string, port:number, ...):boolean -- Sends a packet to the specified address.")
    public Object[] send(final Context context, final Arguments args) {
        final String address = args.checkString(0);
        final int port = checkPort(args.checkInteger(1));
        if (node() == null) {
            return new Object[]{false};
        }

        final Packet packet = Network.newPacket(node().address(), address, port, remaining(args, 2));
        if (packet == null) {
            return new Object[]{false};
        }

        node().sendToAddress(address, NETWORK_MESSAGE, packet);
        return new Object[]{true};
    }

    @Callback(doc = "function(port:number, ...):boolean -- Broadcasts a packet on the specified port.")
    public Object[] broadcast(final Context context, final Arguments args) {
        final int port = checkPort(args.checkInteger(0));
        if (node() == null) {
            return new Object[]{false};
        }

        final Packet packet = Network.newPacket(node().address(), null, port, remaining(args, 1));
        if (packet == null) {
            return new Object[]{false};
        }

        node().sendToReachable(NETWORK_MESSAGE, packet);
        return new Object[]{true};
    }

    @Override
    public void onMessage(final Message message) {
        if ("computer.started".equals(message.name()) || "computer.stopped".equals(message.name())) {
            if (!openPorts.isEmpty()) {
                openPorts.clear();
                markChanged();
            }
        } else if (NETWORK_MESSAGE.equals(message.name())) {
            receivePacket(message);
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
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        nbt.putIntArray(OPEN_PORTS_TAG, openPorts.stream().mapToInt(Integer::intValue).toArray());
    }

    private void markChanged() {
        if (host != null) {
            host.markChanged();
        }
    }

    private void receivePacket(final Message message) {
        if (message.data().length == 0 || !(message.data()[0] instanceof Packet packet) || !openPorts.contains(packet.port())) {
            return;
        }
        if (!(host instanceof MachineHost machineHost) || machineHost.machine() == null || node() == null) {
            return;
        }
        final String localAddress = node().address();
        if ((localAddress != null && localAddress.equals(packet.source())) ||
            (packet.destination() != null && (localAddress == null || !localAddress.equals(packet.destination())))) {
            return;
        }
        final Object[] signalArgs = new Object[4 + packet.data().length];
        signalArgs[0] = node().address();
        signalArgs[1] = packet.source();
        signalArgs[2] = packet.port();
        signalArgs[3] = 0D;
        System.arraycopy(packet.data(), 0, signalArgs, 4, packet.data().length);
        machineHost.machine().signal(MODEM_MESSAGE_SIGNAL, signalArgs);
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
