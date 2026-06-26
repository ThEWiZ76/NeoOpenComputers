package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.event.NetworkActivityEvent;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NetworkCardEnvironmentTest {
    @Test
    void exposesModemCallbacks() throws NoSuchMethodException {
        assertCallback("open");
        assertCallback("close");
        assertCallback("isOpen");
        assertCallback("isWired");
        assertCallback("isWireless");
        assertCallback("send");
        assertCallback("broadcast");
        assertCallback("getWakeMessage");
        assertCallback("setWakeMessage");
    }

    @Test
    void exposesWirelessModemCallbacks() throws NoSuchMethodException {
        Method getStrength = WirelessNetworkCardEnvironment.class.getMethod("getStrength", li.cil.oc.api.machine.Context.class, Arguments.class);
        Method setStrength = WirelessNetworkCardEnvironment.class.getMethod("setStrength", li.cil.oc.api.machine.Context.class, Arguments.class);

        assertTrue(getStrength.isAnnotationPresent(Callback.class));
        assertTrue(setStrength.isAnnotationPresent(Callback.class));
    }

    @Test
    void opensAndClosesPorts() throws Exception {
        OpenComputersApi.initialize();
        NetworkCardEnvironment card = new NetworkCardEnvironment(new TestHost());

        assertArrayEquals(new Object[]{true}, card.open(null, new TestArguments(123)));
        assertArrayEquals(new Object[]{true}, card.isOpen(null, new TestArguments(123)));
        assertArrayEquals(new Object[]{false}, card.open(null, new TestArguments(123)));
        assertArrayEquals(new Object[]{true}, card.close(null, new TestArguments(123)));
        assertArrayEquals(new Object[]{false}, card.isOpen(null, new TestArguments(123)));
    }

    @Test
    void clearsOpenPortsWhenCardDisconnects() throws Exception {
        OpenComputersApi.initialize();
        NetworkCardEnvironment card = new NetworkCardEnvironment(new TestHost());
        card.open(null, new TestArguments(123));

        card.onDisconnect(card.node());

        assertArrayEquals(new Object[]{false}, card.isOpen(null, new TestArguments(123)));
    }

    @Test
    void keepsOpenPortsWhenRemoteComputerStarts() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost receiverHost = new TestMachineHost();
        TestMachineHost senderHost = new TestMachineHost();
        NetworkCardEnvironment receiver = new NetworkCardEnvironment(receiverHost);
        receiver.open(null, new TestArguments(123));

        receiver.onMessage(new TestMessage(senderHost.machine.node(), "computer.started", new Object[0]));

        assertArrayEquals(new Object[]{true}, receiver.isOpen(null, new TestArguments(123)));
    }

    @Test
    void clearsOpenPortsWhenOwnComputerStarts() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost host = new TestMachineHost();
        NetworkCardEnvironment card = new NetworkCardEnvironment(host);
        Network.joinNewNetwork(host.machine.node());
        host.machine.node().connect(card.node());
        card.open(null, new TestArguments(123));

        card.onMessage(new TestMessage(host.machine.node(), "computer.started", new Object[0]));

        assertArrayEquals(new Object[]{false}, card.isOpen(null, new TestArguments(123)));
    }

    @Test
    void rackNetworkCardClearsOpenPortsWhenNeighborComputerStartsLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        NetworkCardEnvironment card = new NetworkCardEnvironment(rackHost());
        RecordingEnvironment computer = new RecordingEnvironment();
        Network.joinNewNetwork(card.node());
        card.node().connect(computer.node());
        card.open(null, new TestArguments(123));

        card.onMessage(new TestMessage(computer.node(), "computer.started", new Object[0]));

        assertArrayEquals(new Object[]{false}, card.isOpen(null, new TestArguments(123)));
    }

    @Test
    void reportsWiredOnly() {
        OpenComputersApi.initialize();
        NetworkCardEnvironment card = new NetworkCardEnvironment(new TestHost());

        assertArrayEquals(new Object[]{true}, card.isWired(null, new TestArguments()));
        assertArrayEquals(new Object[]{false}, card.isWireless(null, new TestArguments()));
        assertNotNull(card.node());
    }

    @Test
    void rackNetworkCardsUseNeighborReachabilityLikeUpstream() {
        OpenComputersApi.initialize();
        NetworkCardEnvironment wired = new NetworkCardEnvironment(rackHost());
        WirelessNetworkCardEnvironment wireless = new WirelessNetworkCardEnvironment(rackHost(), 1);

        assertEquals(Visibility.Neighbors, wired.node().reachability());
        assertEquals(Visibility.Neighbors, wireless.node().reachability());
    }

    @Test
    void tierOneWirelessCardReportsWirelessOnlyAndClampsStrength() {
        OpenComputersApi.initialize();
        WirelessNetworkCardEnvironment card = new WirelessNetworkCardEnvironment(new TestHost(), 0);

        assertArrayEquals(new Object[]{false}, card.isWired(null, new TestArguments()));
        assertArrayEquals(new Object[]{true}, card.isWireless(null, new TestArguments()));
        assertArrayEquals(new Object[]{16D}, card.getStrength(null, new TestArguments()));
        assertArrayEquals(new Object[]{8D}, card.setStrength(null, new TestArguments(8D)));
        assertArrayEquals(new Object[]{16D}, card.setStrength(null, new TestArguments(900D)));
    }

    @Test
    void tierOneWirelessCardAllowsOnlyOneOpenPort() throws Exception {
        OpenComputersApi.initialize();
        WirelessNetworkCardEnvironment card = new WirelessNetworkCardEnvironment(new TestHost(), 0);

        assertArrayEquals(new Object[]{true}, card.open(null, new TestArguments(100)));
        IOException error = assertThrows(IOException.class, () -> card.open(null, new TestArguments(101)));

        assertEquals("too many open ports", error.getMessage());
        assertEquals("1", card.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Size));
    }

    @Test
    void networkCardsUseConfiguredOpenPortLimits() throws Exception {
        OpenComputersApi.initialize();

        withCachedConfig(ModSettings.MAX_OPEN_PORTS, List.of(2, 3, 4), () -> {
            NetworkCardEnvironment wired = new NetworkCardEnvironment(new TestHost());
            WirelessNetworkCardEnvironment tierOne = new WirelessNetworkCardEnvironment(new TestHost(), 0);
            WirelessNetworkCardEnvironment tierTwo = new WirelessNetworkCardEnvironment(new TestHost(), 1);

            assertEquals("2", wired.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Size));
            assertEquals("3", tierOne.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Size));
            assertEquals("4", tierTwo.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Size));
            openPorts(wired, 100, 2);
            IOException wiredError = assertThrows(IOException.class, () -> wired.open(null, new TestArguments(102)));
            assertEquals("too many open ports", wiredError.getMessage());
            openPorts(tierOne, 200, 3);
            IOException wirelessError = assertThrows(IOException.class, () -> tierOne.open(null, new TestArguments(203)));
            assertEquals("too many open ports", wirelessError.getMessage());
        });
    }

    @Test
    void tierTwoWirelessCardReportsWirelessAndWired() {
        OpenComputersApi.initialize();
        WirelessNetworkCardEnvironment card = new WirelessNetworkCardEnvironment(new TestHost(), 1);

        assertArrayEquals(new Object[]{true}, card.isWired(null, new TestArguments()));
        assertArrayEquals(new Object[]{true}, card.isWireless(null, new TestArguments()));
        assertArrayEquals(new Object[]{400D}, card.getStrength(null, new TestArguments()));
    }

    @Test
    void wirelessCardsUseConfiguredRanges() throws Exception {
        OpenComputersApi.initialize();

        withCachedConfig(ModSettings.MAX_WIRELESS_RANGE, List.of(12D, 24D), () -> {
            WirelessNetworkCardEnvironment tierOne = new WirelessNetworkCardEnvironment(new TestHost(), 0);
            WirelessNetworkCardEnvironment tierTwo = new WirelessNetworkCardEnvironment(new TestHost(), 1);

            assertArrayEquals(new Object[]{12D}, tierOne.getStrength(null, new TestArguments()));
            assertArrayEquals(new Object[]{12D}, tierOne.setStrength(null, new TestArguments(99D)));
            assertEquals("12.0", tierOne.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Width));
            assertArrayEquals(new Object[]{24D}, tierTwo.getStrength(null, new TestArguments()));
            assertArrayEquals(new Object[]{24D}, tierTwo.setStrength(null, new TestArguments(99D)));
            assertEquals("24.0", tierTwo.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Width));
        });
    }

    @Test
    void getsAndSetsWakeMessage() {
        OpenComputersApi.initialize();
        NetworkCardEnvironment card = new NetworkCardEnvironment(new TestHost());

        assertArrayEquals(new Object[]{null, false}, card.getWakeMessage(null, new TestArguments()));
        assertArrayEquals(new Object[]{null, false}, card.setWakeMessage(null, new TestArguments("boot", true)));
        assertArrayEquals(new Object[]{"boot", true}, card.getWakeMessage(null, new TestArguments()));
        assertArrayEquals(new Object[]{"boot", true}, card.setWakeMessage(null, new TestArguments(null, false)));
        assertArrayEquals(new Object[]{null, false}, card.getWakeMessage(null, new TestArguments()));
    }

    @Test
    void persistsWakeMessage() {
        OpenComputersApi.initialize();
        NetworkCardEnvironment saved = new NetworkCardEnvironment(new TestHost());
        saved.setWakeMessage(null, new TestArguments("boot", true));
        CompoundTag tag = new CompoundTag();

        saved.save(tag);
        NetworkCardEnvironment loaded = new NetworkCardEnvironment(new TestHost());
        loaded.load(tag);

        assertArrayEquals(new Object[]{"boot", true}, loaded.getWakeMessage(null, new TestArguments()));
    }

    @Test
    void wakeMessageStartsMachineEvenWhenPortClosed() {
        OpenComputersApi.initialize();
        TestMachineHost host = new TestMachineHost();
        NetworkCardEnvironment card = new NetworkCardEnvironment(host);
        Network.joinNewNetwork(card.node());
        card.setWakeMessage(null, new TestArguments("boot", false));

        card.onMessage(new TestMessage(null, "network.message", new Object[]{
            new TestPacket("remote", card.node().address(), 123, new Object[]{"boot"})
        }));

        assertEquals(1, host.machine.starts);
        assertEquals(List.of(), host.signals);
    }

    @Test
    void fuzzyWakeMessageAllowsAdditionalPacketData() {
        OpenComputersApi.initialize();
        TestMachineHost host = new TestMachineHost();
        NetworkCardEnvironment card = new NetworkCardEnvironment(host);
        Network.joinNewNetwork(card.node());
        card.setWakeMessage(null, new TestArguments("boot", true));

        card.onMessage(new TestMessage(null, "network.message", new Object[]{
            new TestPacket("remote", card.node().address(), 123, new Object[]{"boot", "extra"})
        }));

        assertEquals(1, host.machine.starts);
    }

    @Test
    void wakeMessageAcceptsUtf8ByteArrayPayload() {
        OpenComputersApi.initialize();
        TestMachineHost host = new TestMachineHost();
        NetworkCardEnvironment card = new NetworkCardEnvironment(host);
        Network.joinNewNetwork(card.node());
        card.setWakeMessage(null, new TestArguments("boot", false));

        card.onMessage(new TestMessage(null, "network.message", new Object[]{
            new TestPacket("remote", card.node().address(), 123, new Object[]{"boot".getBytes(StandardCharsets.UTF_8)})
        }));

        assertEquals(1, host.machine.starts);
    }

    @Test
    void rackWakeMessageSendsComputerStartToNeighborsLikeUpstream() {
        OpenComputersApi.initialize();
        NetworkCardEnvironment card = new NetworkCardEnvironment(rackHost());
        RecordingEnvironment computer = new RecordingEnvironment();
        Network.joinNewNetwork(card.node());
        card.node().connect(computer.node());
        card.setWakeMessage(null, new TestArguments("boot", false));

        card.onMessage(new TestMessage(null, "network.message", new Object[]{
            new TestPacket("remote", card.node().address(), 123, new Object[]{"boot"})
        }));

        assertEquals(List.of("computer.start"), computer.messages.stream().map(Message::name).toList());
    }

    @Test
    void rackModemMessageSendsComputerSignalToReachableNeighborsLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        NetworkCardEnvironment card = new NetworkCardEnvironment(rackHost());
        RecordingEnvironment computer = new RecordingEnvironment();
        Network.joinNewNetwork(card.node());
        card.node().connect(computer.node());
        card.open(null, new TestArguments(123));

        card.onMessage(new TestMessage(null, "network.message", new Object[]{
            new TestPacket("remote", card.node().address(), 123, new Object[]{"payload"})
        }));

        assertEquals(1, computer.messages.size());
        Message message = computer.messages.get(0);
        assertEquals("computer.signal", message.name());
        assertArrayEquals(new Object[]{"modem_message", "remote", 123, 0D, "payload"}, message.data());
    }

    @Test
    void exposesDeviceInfoMetadata() {
        OpenComputersApi.initialize();
        NetworkCardEnvironment card = new NetworkCardEnvironment(new TestHost());

        DeviceInfo info = assertInstanceOf(DeviceInfo.class, card);
        Map<String, String> metadata = info.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Network, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Ethernet controller", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", metadata.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("42i520 (MPN-01)", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("1.0", metadata.get(DeviceInfo.DeviceAttribute.Version));
        assertEquals("8192", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
        assertEquals("16", metadata.get(DeviceInfo.DeviceAttribute.Size));
        assertEquals("8", metadata.get(DeviceInfo.DeviceAttribute.Width));
    }

    @Test
    void wirelessDeviceInfoExposesUpstreamMetadata() {
        OpenComputersApi.initialize();

        Map<String, String> tierOne = new WirelessNetworkCardEnvironment(new TestHost(), 0).getDeviceInfo();
        Map<String, String> tierTwo = new WirelessNetworkCardEnvironment(new TestHost(), 1).getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Network, tierOne.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Wireless ethernet controller", tierOne.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", tierOne.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("39i110 (LPPW-01)", tierOne.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("1.0", tierOne.get(DeviceInfo.DeviceAttribute.Version));
        assertEquals("8192", tierOne.get(DeviceInfo.DeviceAttribute.Capacity));
        assertEquals("1", tierOne.get(DeviceInfo.DeviceAttribute.Size));
        assertEquals("16.0", tierOne.get(DeviceInfo.DeviceAttribute.Width));

        assertEquals(DeviceInfo.DeviceClass.Network, tierTwo.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Wireless ethernet controller", tierTwo.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MightyPirates GmbH & Co. KG", tierTwo.get(DeviceInfo.DeviceAttribute.Vendor));
        assertEquals("62i230 (MPW-01)", tierTwo.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("2.0", tierTwo.get(DeviceInfo.DeviceAttribute.Version));
        assertEquals("8192", tierTwo.get(DeviceInfo.DeviceAttribute.Capacity));
        assertEquals("16", tierTwo.get(DeviceInfo.DeviceAttribute.Size));
        assertEquals("400.0", tierTwo.get(DeviceInfo.DeviceAttribute.Width));
    }

    @Test
    void deviceInfoReportsConfiguredPacketSize() throws Exception {
        OpenComputersApi.initialize();

        withCachedConfig(ModSettings.MAX_NETWORK_PACKET_SIZE, 32, () -> {
            assertEquals("32", new NetworkCardEnvironment(new TestHost()).getDeviceInfo().get(DeviceInfo.DeviceAttribute.Capacity));
            assertEquals("32", new WirelessNetworkCardEnvironment(new TestHost(), 0).getDeviceInfo().get(DeviceInfo.DeviceAttribute.Capacity));
            assertEquals("32", new WirelessNetworkCardEnvironment(new TestHost(), 1).getDeviceInfo().get(DeviceInfo.DeviceAttribute.Capacity));
        });
    }

    @Test
    void wiredDeviceInfoReportsConfiguredPacketPartCount() throws Exception {
        OpenComputersApi.initialize();

        withCachedConfig(ModSettings.MAX_NETWORK_PACKET_PARTS, 4, () ->
            assertEquals("4", new NetworkCardEnvironment(new TestHost()).getDeviceInfo().get(DeviceInfo.DeviceAttribute.Width)));
    }

    @Test
    void sendAndAcceptedReceivePostNetworkActivityEventsLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost senderHost = new TestMachineHost(1, 2, 3);
        TestMachineHost receiverHost = new TestMachineHost(4, 5, 6);
        NetworkCardEnvironment sender = new NetworkCardEnvironment(senderHost);
        NetworkCardEnvironment receiver = new NetworkCardEnvironment(receiverHost);
        List<NetworkActivityEvent.Server> events = new ArrayList<>();
        Network.joinNewNetwork(sender.node());
        sender.node().connect(receiver.node());
        receiver.open(null, new TestArguments(123));
        NeoForge.EVENT_BUS.start();
        NeoForge.EVENT_BUS.addListener(NetworkActivityEvent.Server.class, event -> {
            if (event.getNode() == sender.node() || event.getNode() == receiver.node()) {
                events.add(event);
            }
        });

        assertArrayEquals(new Object[]{true}, sender.send(null, new TestArguments(receiver.node().address(), 123, "payload")));

        assertEquals(2, events.size());
        assertNetworkActivity(events, sender.node(), 1.5D, 2.5D, 3.5D);
        assertNetworkActivity(events, receiver.node(), 4.5D, 5.5D, 6.5D);
    }

    @Test
    void receivedPacketOnOpenPortQueuesModemMessageSignal() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost host = new TestMachineHost();
        NetworkCardEnvironment card = new NetworkCardEnvironment(host);
        card.open(null, new TestArguments(123));

        card.onMessage(new TestMessage(null, "network.message", new Object[]{
            new TestPacket("remote", card.node().address(), 123, new Object[]{"payload"})
        }));

        assertEquals(List.of(Arrays.asList("modem_message", "remote", 123, 0D, "payload")), host.signals);
    }

    @Test
    void ignoresPacketsAddressedToAnotherNode() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost host = new TestMachineHost();
        NetworkCardEnvironment card = new NetworkCardEnvironment(host);
        Network.joinNewNetwork(card.node());
        card.open(null, new TestArguments(123));

        card.onMessage(new TestMessage(null, "network.message", new Object[]{
            new TestPacket("remote", "other-node", 123, new Object[]{"payload"})
        }));

        assertEquals(List.of(), host.signals);
    }

    @Test
    void ignoresPacketsSentByItself() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost host = new TestMachineHost();
        NetworkCardEnvironment card = new NetworkCardEnvironment(host);
        Network.joinNewNetwork(card.node());
        card.open(null, new TestArguments(123));

        card.onMessage(new TestMessage(null, "network.message", new Object[]{
            new TestPacket(card.node().address(), card.node().address(), 123, new Object[]{"payload"})
        }));

        assertEquals(List.of(), host.signals);
    }

    @Test
    void sendDeliversPacketToReachableModemAddress() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost senderHost = new TestMachineHost();
        TestMachineHost receiverHost = new TestMachineHost();
        NetworkCardEnvironment sender = new NetworkCardEnvironment(senderHost);
        NetworkCardEnvironment receiver = new NetworkCardEnvironment(receiverHost);
        Network.joinNewNetwork(sender.node());
        sender.node().connect(receiver.node());
        sender.open(null, new TestArguments(123));
        receiver.open(null, new TestArguments(123));

        assertArrayEquals(new Object[]{true}, sender.send(null, new TestArguments(receiver.node().address(), 123, "payload")));

        assertEquals(List.of(Arrays.asList("modem_message", sender.node().address(), 123, 0D, "payload")), receiverHost.signals);
    }

    @Test
    void wiredSendPublishesAddressedPacketToReachableNodesLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost receiverHost = new TestMachineHost();
        NetworkCardEnvironment sender = new NetworkCardEnvironment(new TestMachineHost());
        NetworkCardEnvironment receiver = new NetworkCardEnvironment(receiverHost);
        RecordingEnvironment observer = new RecordingEnvironment();
        Network.joinNewNetwork(sender.node());
        sender.node().connect(receiver.node());
        sender.node().connect(observer.node());
        receiver.open(null, new TestArguments(123));

        assertArrayEquals(new Object[]{true}, sender.send(null, new TestArguments(receiver.node().address(), 123, "payload")));

        assertEquals(1, observer.messages.size());
        Message message = observer.messages.get(0);
        assertEquals("network.message", message.name());
        Packet packet = assertInstanceOf(Packet.class, message.data()[0]);
        assertEquals(sender.node().address(), packet.source());
        assertEquals(receiver.node().address(), packet.destination());
        assertEquals(123, packet.port());
        assertArrayEquals(new Object[]{"payload"}, packet.data());
        assertEquals(List.of(Arrays.asList("modem_message", sender.node().address(), 123, 0D, "payload")), receiverHost.signals);
    }

    @Test
    void sendDoesNotRequireSenderPortToBeOpen() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost receiverHost = new TestMachineHost();
        NetworkCardEnvironment sender = new NetworkCardEnvironment(new TestMachineHost());
        NetworkCardEnvironment receiver = new NetworkCardEnvironment(receiverHost);
        Network.joinNewNetwork(sender.node());
        sender.node().connect(receiver.node());
        receiver.open(null, new TestArguments(123));

        assertArrayEquals(new Object[]{true}, sender.send(null, new TestArguments(receiver.node().address(), 123, "payload")));

        assertEquals(List.of(Arrays.asList("modem_message", sender.node().address(), 123, 0D, "payload")), receiverHost.signals);
    }

    @Test
    void broadcastDoesNotRequireSenderPortToBeOpen() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost receiverHost = new TestMachineHost();
        NetworkCardEnvironment sender = new NetworkCardEnvironment(new TestMachineHost());
        NetworkCardEnvironment receiver = new NetworkCardEnvironment(receiverHost);
        Network.joinNewNetwork(sender.node());
        sender.node().connect(receiver.node());
        receiver.open(null, new TestArguments(123));

        assertArrayEquals(new Object[]{true}, sender.broadcast(null, new TestArguments(123, "payload")));

        assertEquals(List.of(Arrays.asList("modem_message", sender.node().address(), 123, 0D, "payload")), receiverHost.signals);
    }

    @Test
    void rackNetworkCardSendDoesNotReachNonNeighborModems() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost receiverHost = new TestMachineHost();
        NetworkCardEnvironment sender = new NetworkCardEnvironment(rackHost());
        NetworkCardEnvironment receiver = new NetworkCardEnvironment(receiverHost);
        Node bridge = Network.newNode(new BridgeEnvironment(), Visibility.Network).create();
        Network.joinNewNetwork(sender.node());
        sender.node().connect(bridge);
        bridge.connect(receiver.node());
        receiver.open(null, new TestArguments(123));

        assertArrayEquals(new Object[]{true}, sender.send(null, new TestArguments(receiver.node().address(), 123, "payload")));

        assertEquals(List.of(), receiverHost.signals);
    }

    @Test
    void rackNetworkCardBroadcastDoesNotReachNonNeighborModems() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost receiverHost = new TestMachineHost();
        NetworkCardEnvironment sender = new NetworkCardEnvironment(rackHost());
        NetworkCardEnvironment receiver = new NetworkCardEnvironment(receiverHost);
        Node bridge = Network.newNode(new BridgeEnvironment(), Visibility.Network).create();
        Network.joinNewNetwork(sender.node());
        sender.node().connect(bridge);
        bridge.connect(receiver.node());
        receiver.open(null, new TestArguments(123));

        assertArrayEquals(new Object[]{true}, sender.broadcast(null, new TestArguments(123, "payload")));

        assertEquals(List.of(), receiverHost.signals);
    }

    @Test
    void wirelessBroadcastDeliversWithinStrengthAndReportsDistance() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost senderHost = new TestMachineHost(0, 0, 0);
        TestMachineHost receiverHost = new TestMachineHost(3, 4, 0);
        WirelessNetworkCardEnvironment sender = new WirelessNetworkCardEnvironment(senderHost, 0);
        WirelessNetworkCardEnvironment receiver = new WirelessNetworkCardEnvironment(receiverHost, 0);
        Network.joinNewNetwork(sender.node());
        Network.joinNewNetwork(receiver.node());
        receiver.open(null, new TestArguments(123));

        sender.setStrength(null, new TestArguments(5D));
        assertArrayEquals(new Object[]{true}, sender.broadcast(null, new TestArguments(123, "payload")));

        assertEquals(List.of(Arrays.asList("modem_message", sender.node().address(), 123, 5D, "payload")), receiverHost.signals);
    }

    @Test
    void tierOneWirelessBroadcastIgnoresZeroDistanceLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost senderHost = new TestMachineHost(0, 0, 0);
        TestMachineHost receiverHost = new TestMachineHost(0, 0, 0);
        WirelessNetworkCardEnvironment sender = new WirelessNetworkCardEnvironment(senderHost, 0);
        WirelessNetworkCardEnvironment receiver = new WirelessNetworkCardEnvironment(receiverHost, 0);
        Network.joinNewNetwork(sender.node());
        Network.joinNewNetwork(receiver.node());
        receiver.open(null, new TestArguments(123));

        sender.setStrength(null, new TestArguments(5D));
        assertArrayEquals(new Object[]{true}, sender.broadcast(null, new TestArguments(123, "payload")));

        assertEquals(List.of(), receiverHost.signals);
    }

    @Test
    void tierTwoWirelessBroadcastDeliversAtZeroDistanceViaWiredModeLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost senderHost = new TestMachineHost(0, 0, 0);
        TestMachineHost receiverHost = new TestMachineHost(0, 0, 0);
        WirelessNetworkCardEnvironment sender = new WirelessNetworkCardEnvironment(senderHost, 1);
        WirelessNetworkCardEnvironment receiver = new WirelessNetworkCardEnvironment(receiverHost, 1);
        Network.joinNewNetwork(sender.node());
        Network.joinNewNetwork(receiver.node());
        receiver.open(null, new TestArguments(123));

        sender.setStrength(null, new TestArguments(5D));
        assertArrayEquals(new Object[]{true}, sender.broadcast(null, new TestArguments(123, "payload")));

        assertEquals(List.of(Arrays.asList("modem_message", sender.node().address(), 123, 0D, "payload")), receiverHost.signals);
    }

    @Test
    void wirelessCardUpdateRefreshesWirelessNetworkLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost receiverHost = new TestMachineHost();
        WirelessNetworkCardEnvironment sender = new WirelessNetworkCardEnvironment(new TestHost(1, 0, 0), 1);
        WirelessNetworkCardEnvironment receiver = new WirelessNetworkCardEnvironment(receiverHost, 0);
        receiver.open(null, new TestArguments(123));
        Network.joinWirelessNetwork(sender);

        Packet packet = Network.newPacket("remote", null, 123, new Object[]{"payload"});
        Network.sendWirelessPacket(sender, 16D, packet);
        assertTrue(receiverHost.signals.isEmpty());

        assertTrue(receiver.canUpdate());
        receiver.update();
        Network.sendWirelessPacket(sender, 16D, packet);

        assertEquals(List.of(List.of("modem_message", "remote", 123, 1D, "payload")), receiverHost.signals);

        Network.leaveWirelessNetwork(sender);
        Network.leaveWirelessNetwork(receiver);
    }

    @Test
    void wirelessBroadcastRequiresEnergyAndConsumesBuffer() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost senderHost = new TestMachineHost(0, 0, 0);
        TestMachineHost receiverHost = new TestMachineHost(3, 4, 0);
        WirelessNetworkCardEnvironment sender = new WirelessNetworkCardEnvironment(senderHost, 0);
        WirelessNetworkCardEnvironment receiver = new WirelessNetworkCardEnvironment(receiverHost, 0);
        ComponentConnector connector = assertInstanceOf(ComponentConnector.class, sender.node());
        RecordingContext context = new RecordingContext(sender.node());
        Network.joinNewNetwork(sender.node());
        Network.joinNewNetwork(receiver.node());
        receiver.open(null, new TestArguments(123));
        sender.setStrength(null, new TestArguments(5D));

        Exception error = assertThrows(Exception.class, () -> sender.broadcast(context, new TestArguments(123, "payload")));
        assertEquals("not enough energy", error.getMessage());
        assertEquals(List.of(), receiverHost.signals);

        connector.setLocalBufferSize(1D);
        connector.changeBuffer(1D);

        assertArrayEquals(new Object[]{true}, sender.broadcast(context, new TestArguments(123, "payload")));

        assertEquals(0.75D, connector.localBuffer(), 0.000_001D);
        assertEquals(List.of(Arrays.asList("modem_message", sender.node().address(), 123, 5D, "payload")), receiverHost.signals);
    }

    @Test
    void wirelessBroadcastConsumesCardConnectorWithMachineContextLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost senderHost = new TestMachineHost(0, 0, 0);
        TestMachineHost receiverHost = new TestMachineHost(3, 4, 0);
        WirelessNetworkCardEnvironment sender = new WirelessNetworkCardEnvironment(senderHost, 0);
        WirelessNetworkCardEnvironment receiver = new WirelessNetworkCardEnvironment(receiverHost, 0);
        ComponentConnector connector = assertInstanceOf(ComponentConnector.class, sender.node());
        RecordingContext context = new RecordingContext(senderHost.machine.node());
        Network.joinNewNetwork(sender.node());
        Network.joinNewNetwork(receiver.node());
        receiver.open(null, new TestArguments(123));
        sender.setStrength(null, new TestArguments(5D));
        connector.setLocalBufferSize(1D);
        connector.changeBuffer(1D);

        assertArrayEquals(new Object[]{true}, sender.broadcast(context, new TestArguments(123, "payload")));

        assertEquals(0.75D, connector.localBuffer(), 0.000_001D);
        assertEquals(List.of(Arrays.asList("modem_message", sender.node().address(), 123, 5D, "payload")), receiverHost.signals);
    }

    @Test
    void wirelessBroadcastUsesConfiguredRangeCost() throws Exception {
        OpenComputersApi.initialize();

        withCachedConfig(ModSettings.WIRELESS_COST_PER_RANGE, List.of(0.1D, 0.2D), () -> {
            WirelessNetworkCardEnvironment sender = new WirelessNetworkCardEnvironment(new TestMachineHost(0, 0, 0), 0);
            WirelessNetworkCardEnvironment receiver = new WirelessNetworkCardEnvironment(new TestMachineHost(3, 4, 0), 0);
            ComponentConnector connector = assertInstanceOf(ComponentConnector.class, sender.node());
            RecordingContext context = new RecordingContext(sender.node());
            Network.joinNewNetwork(sender.node());
            Network.joinNewNetwork(receiver.node());
            receiver.open(null, new TestArguments(123));
            sender.setStrength(null, new TestArguments(5D));
            connector.setLocalBufferSize(1D);
            connector.changeBuffer(1D);

            assertArrayEquals(new Object[]{true}, sender.broadcast(context, new TestArguments(123, "payload")));

            assertEquals(0.5D, connector.localBuffer(), 0.000_001D);
        });
    }

    @Test
    void wirelessBroadcastDoesNotDeliverBeyondStrength() throws Exception {
        OpenComputersApi.initialize();
        TestMachineHost senderHost = new TestMachineHost(0, 0, 0);
        TestMachineHost receiverHost = new TestMachineHost(6, 0, 0);
        WirelessNetworkCardEnvironment sender = new WirelessNetworkCardEnvironment(senderHost, 0);
        WirelessNetworkCardEnvironment receiver = new WirelessNetworkCardEnvironment(receiverHost, 0);
        Network.joinNewNetwork(sender.node());
        Network.joinNewNetwork(receiver.node());
        receiver.open(null, new TestArguments(123));

        sender.setStrength(null, new TestArguments(5D));
        assertArrayEquals(new Object[]{true}, sender.broadcast(null, new TestArguments(123, "payload")));

        assertEquals(List.of(), receiverHost.signals);
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = NetworkCardEnvironment.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }

    private static void assertNetworkActivity(
        final List<NetworkActivityEvent.Server> events,
        final Node node,
        final double x,
        final double y,
        final double z
    ) {
        for (NetworkActivityEvent.Server event : events) {
            if (event.getNode() == node) {
                assertEquals(x, event.getX());
                assertEquals(y, event.getY());
                assertEquals(z, event.getZ());
                return;
            }
        }
        throw new AssertionError("Missing network activity event for " + node);
    }

    private record RecordingContext(Node node) implements li.cil.oc.api.machine.Context {
        @Override public boolean canInteract(final String player) { return true; }
        @Override public boolean isRunning() { return true; }
        @Override public boolean isPaused() { return false; }
        @Override public boolean start() { return true; }
        @Override public boolean pause(final double seconds) { return true; }
        @Override public boolean stop() { return true; }
        @Override public void consumeCallBudget(final double callCost) { }
        @Override public boolean signal(final String name, final Object... args) { return true; }
    }

    private static class TestHost implements EnvironmentHost {
        private final int x;
        private final int y;
        private final int z;

        private TestHost() {
            this(0, 0, 0);
        }

        private TestHost(final int x, final int y, final int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public Level world() {
            return null;
        }

        @Override
        public double xPosition() {
            return x + 0.5D;
        }

        @Override
        public double yPosition() {
            return y + 0.5D;
        }

        @Override
        public double zPosition() {
            return z + 0.5D;
        }

        @Override
        public void markChanged() {
        }
    }

    private static final class TestMachineHost extends TestHost implements MachineHost {
        private final List<List<Object>> signals = new ArrayList<>();
        private final TestMachine machine = new TestMachine(signals);

        private TestMachineHost() {
            super();
        }

        private TestMachineHost(final int x, final int y, final int z) {
            super(x, y, z);
        }

        @Override
        public Machine machine() {
            return machine;
        }

        @Override
        public Iterable<ItemStack> internalComponents() {
            return List.of();
        }

        @Override
        public int componentSlot(final String address) {
            return -1;
        }

        @Override
        public void onMachineConnect(final Node node) {
        }

        @Override
        public void onMachineDisconnect(final Node node) {
        }
    }

    private static final class TestMachine implements Machine {
        private final List<List<Object>> signals;
        private final Node node;
        private int starts;

        private TestMachine(final List<List<Object>> signals) {
            this.signals = signals;
            node = Network.newNode(this, Visibility.Network).create();
        }

        @Override public boolean signal(final String name, final Object... args) {
            final List<Object> values = new ArrayList<>();
            values.add(name);
            values.addAll(Arrays.asList(args));
            signals.add(values);
            return true;
        }
        @Override public MachineHost host() { return null; }
        @Override public void onHostChanged() {}
        @Override public Architecture architecture() { return null; }
        @Override public Map<String, String> components() { return Map.of(); }
        @Override public int componentCount() { return 0; }
        @Override public int maxComponents() { return 0; }
        @Override public double getCostPerTick() { return 0; }
        @Override public void setCostPerTick(final double value) {}
        @Override public String tmpAddress() { return null; }
        @Override public String lastError() { return null; }
        @Override public long worldTime() { return 0; }
        @Override public double upTime() { return 0; }
        @Override public double cpuTime() { return 0; }
        @Override public void beep(final short frequency, final short duration) {}
        @Override public void beep(final String pattern) {}
        @Override public boolean crash(final String message) { return false; }
        @Override public Signal popSignal() { return null; }
        @Override public Map<String, Callback> methods(final Object value) { return Map.of(); }
        @Override public Object[] invoke(final String address, final String method, final Object[] args) { return new Object[0]; }
        @Override public Object[] invoke(final li.cil.oc.api.machine.Value value, final String method, final Object[] args) { return new Object[0]; }
        @Override public String[] users() { return new String[0]; }
        @Override public void addUser(final String name) {}
        @Override public boolean removeUser(final String name) { return false; }
        @Override public boolean canInteract(final String player) { return true; }
        @Override public boolean isRunning() { return false; }
        @Override public boolean isPaused() { return false; }
        @Override public boolean start() {
            starts++;
            return true;
        }
        @Override public boolean pause(final double seconds) { return false; }
        @Override public boolean stop() { return false; }
        @Override public void consumeCallBudget(final double callCost) {}
        @Override public boolean canUpdate() { return false; }
        @Override public void update() {}
        @Override public Node node() { return node; }
        @Override public void onConnect(final Node node) {}
        @Override public void onDisconnect(final Node node) {}
        @Override public void onMessage(final Message message) {}
        @Override public void load(final CompoundTag nbt) {}
        @Override public void save(final CompoundTag nbt) {}
    }

    private static final class BridgeEnvironment implements li.cil.oc.api.network.Environment {
        @Override public Node node() { return null; }
        @Override public void onConnect(final Node node) {}
        @Override public void onDisconnect(final Node node) {}
        @Override public void onMessage(final Message message) {}
    }

    private static final class RecordingEnvironment implements li.cil.oc.api.network.Environment {
        private final List<Message> messages = new ArrayList<>();
        private final Node node = Network.newNode(this, Visibility.Network).create();

        @Override public Node node() { return node; }
        @Override public void onConnect(final Node node) {}
        @Override public void onDisconnect(final Node node) {}
        @Override public void onMessage(final Message message) { messages.add(message); }
    }

    private record TestMessage(Node source, String name, Object[] data) implements Message {
        @Override
        public void cancel() {
        }
    }

    private record TestPacket(String source, String destination, int port, Object[] data) implements Packet {
        @Override public int size() { return data.length; }
        @Override public int ttl() { return 16; }
        @Override public Packet hop() { return this; }
        @Override public void save(final CompoundTag nbt) {}
    }

    private static EnvironmentHost rackHost() {
        return (EnvironmentHost) Proxy.newProxyInstance(
            Rack.class.getClassLoader(),
            new Class<?>[]{Rack.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "test-rack";
                default -> defaultValue(method.getReturnType());
            });
    }

    private static Object defaultValue(final Class<?> type) {
        if (!type.isPrimitive() || type == Void.TYPE) {
            return null;
        }
        if (type == Boolean.TYPE) {
            return false;
        }
        if (type == Character.TYPE) {
            return '\0';
        }
        if (type == Byte.TYPE) {
            return (byte) 0;
        }
        if (type == Short.TYPE) {
            return (short) 0;
        }
        if (type == Integer.TYPE) {
            return 0;
        }
        if (type == Long.TYPE) {
            return 0L;
        }
        if (type == Float.TYPE) {
            return 0F;
        }
        return 0D;
    }

    private static void openPorts(final NetworkCardEnvironment card, final int firstPort, final int count) throws Exception {
        for (int offset = 0; offset < count; offset++) {
            assertArrayEquals(new Object[]{true}, card.open(null, new TestArguments(firstPort + offset)));
        }
    }

    private static <T> void withCachedConfig(final ModConfigSpec.ConfigValue<T> value, final T override, final ThrowingRunnable action) throws Exception {
        final Field cachedValue = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cachedValue.setAccessible(true);
        final Object previous = cachedValue.get(value);
        cachedValue.set(value, override);
        try {
            action.run();
        } finally {
            cachedValue.set(value, previous);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private record TestArguments(Object... values) implements Arguments {
        @Override
        public int count() {
            return values.length;
        }

        @Override
        public Object checkAny(final int index) {
            return values[index];
        }

        @Override
        public boolean checkBoolean(final int index) {
            return (Boolean) values[index];
        }

        @Override
        public int checkInteger(final int index) {
            return ((Number) values[index]).intValue();
        }

        @Override
        public long checkLong(final int index) {
            return ((Number) values[index]).longValue();
        }

        @Override
        public double checkDouble(final int index) {
            return ((Number) values[index]).doubleValue();
        }

        @Override
        public String checkString(final int index) {
            return (String) values[index];
        }

        @Override
        public byte[] checkByteArray(final int index) {
            return (byte[]) values[index];
        }

        @Override
        public Map checkTable(final int index) {
            return (Map) values[index];
        }

        @Override
        public ItemStack checkItemStack(final int index) {
            return (ItemStack) values[index];
        }

        @Override
        public Object optAny(final int index, final Object def) {
            return index < values.length ? values[index] : def;
        }

        @Override
        public boolean optBoolean(final int index, final boolean def) {
            return index < values.length ? checkBoolean(index) : def;
        }

        @Override
        public int optInteger(final int index, final int def) {
            return index < values.length ? checkInteger(index) : def;
        }

        @Override
        public long optLong(final int index, final long def) {
            return index < values.length ? checkLong(index) : def;
        }

        @Override
        public double optDouble(final int index, final double def) {
            return index < values.length ? checkDouble(index) : def;
        }

        @Override
        public String optString(final int index, final String def) {
            return index < values.length ? checkString(index) : def;
        }

        @Override
        public byte[] optByteArray(final int index, final byte[] def) {
            return index < values.length ? checkByteArray(index) : def;
        }

        @Override
        public Map optTable(final int index, final Map def) {
            return index < values.length ? checkTable(index) : def;
        }

        @Override
        public ItemStack optItemStack(final int index, final ItemStack def) {
            return index < values.length ? checkItemStack(index) : def;
        }

        @Override
        public boolean isBoolean(final int index) {
            return values[index] instanceof Boolean;
        }

        @Override
        public boolean isInteger(final int index) {
            return values[index] instanceof Integer;
        }

        @Override
        public boolean isLong(final int index) {
            return values[index] instanceof Long;
        }

        @Override
        public boolean isDouble(final int index) {
            return values[index] instanceof Double;
        }

        @Override
        public boolean isString(final int index) {
            return values[index] instanceof String;
        }

        @Override
        public boolean isByteArray(final int index) {
            return values[index] instanceof byte[];
        }

        @Override
        public boolean isTable(final int index) {
            return values[index] instanceof Map;
        }

        @Override
        public boolean isItemStack(final int index) {
            return values[index] instanceof ItemStack;
        }

        @Override
        public Object[] toArray() {
            return values;
        }

        @Override
        public Iterator<Object> iterator() {
            return java.util.Arrays.asList(values).iterator();
        }
    }
}
