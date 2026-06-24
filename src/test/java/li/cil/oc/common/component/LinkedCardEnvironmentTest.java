package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.machine.Signal;
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
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LinkedCardEnvironmentTest {
    @Test
    void exposesTunnelCallbacks() throws NoSuchMethodException {
        assertCallback("send");
        assertCallback("getChannel");
        assertCallback("maxPacketSize");
        assertCallback("getWakeMessage");
        assertCallback("setWakeMessage");
    }

    @Test
    void reportsDeviceInfo() {
        OpenComputersApi.initialize();
        LinkedCardEnvironment card = new LinkedCardEnvironment(new TestMachineHost(), "pair");

        DeviceInfo info = assertInstanceOf(DeviceInfo.class, card);
        Map<String, String> metadata = info.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Network, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Quantumnet controller", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("HyperLink IV: Ender Edition", metadata.get(DeviceInfo.DeviceAttribute.Product));
    }

    @Test
    void returnsConfiguredChannelAndMaxPacketSize() {
        OpenComputersApi.initialize();
        LinkedCardEnvironment card = new LinkedCardEnvironment(new TestMachineHost(), "pair");

        assertArrayEquals(new Object[]{"pair"}, card.getChannel(null, new TestArguments()));
        assertArrayEquals(new Object[]{8192}, card.maxPacketSize(null, new TestArguments()));
    }

    @Test
    void reportsConfiguredMaxPacketSize() throws Exception {
        OpenComputersApi.initialize();

        withCachedConfig(ModSettings.MAX_NETWORK_PACKET_SIZE, 32, () -> {
            LinkedCardEnvironment card = new LinkedCardEnvironment(new TestMachineHost(), "pair");

            assertArrayEquals(new Object[]{32}, card.maxPacketSize(null, new TestArguments()));
            assertEquals("32", card.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Capacity));
        });
    }

    @Test
    void reportsConfiguredPacketPartCount() throws Exception {
        OpenComputersApi.initialize();

        withCachedConfig(ModSettings.MAX_NETWORK_PACKET_PARTS, 4, () -> {
            LinkedCardEnvironment card = new LinkedCardEnvironment(new TestMachineHost(), "pair");

            assertEquals("4", card.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Width));
        });
    }

    @Test
    void sendsPayloadToOtherCardOnSameChannel() {
        OpenComputersApi.initialize();
        TestMachineHost leftHost = new TestMachineHost();
        TestMachineHost rightHost = new TestMachineHost();
        LinkedCardEnvironment left = new LinkedCardEnvironment(leftHost, "pair");
        LinkedCardEnvironment right = new LinkedCardEnvironment(rightHost, "pair");
        Network.joinNewNetwork(left.node());
        Network.joinNewNetwork(right.node());
        charge(left, 101D);

        assertArrayEquals(new Object[]{true}, left.send(null, new TestArguments("payload", 7)));

        assertEquals(List.of(Arrays.asList("modem_message", left.node().address(), 0, 0D, "payload", 7)), rightHost.signals);
        assertEquals(List.of(), leftHost.signals);
    }

    @Test
    void receivePacketSignalsReachableNodeWhenHostIsNotMachineHostLikeUpstream() {
        OpenComputersApi.initialize();
        LinkedCardEnvironment sender = new LinkedCardEnvironment(new TestMachineHost(), "pair");
        LinkedCardEnvironment receiver = new LinkedCardEnvironment(new TestHost(), "pair");
        RecordingEnvironment computer = new RecordingEnvironment();
        Network.joinNewNetwork(sender.node());
        Network.joinNewNetwork(receiver.node());
        receiver.node().connect(computer.node());
        charge(sender, 101D);

        assertArrayEquals(new Object[]{true}, sender.send(null, new TestArguments("payload")));

        assertEquals(1, computer.messages.size());
        Message message = computer.messages.get(0);
        assertEquals("computer.signal", message.name());
        assertArrayEquals(new Object[]{"modem_message", sender.node().address(), 0, 0D, "payload"}, message.data());
    }

    @Test
    void sendRequiresEnergyAndConsumesBuffer() {
        OpenComputersApi.initialize();
        TestMachineHost rightHost = new TestMachineHost();
        LinkedCardEnvironment left = new LinkedCardEnvironment(new TestMachineHost(), "pair");
        LinkedCardEnvironment right = new LinkedCardEnvironment(rightHost, "pair");
        ComponentConnector connector = assertInstanceOf(ComponentConnector.class, left.node());
        RecordingContext context = new RecordingContext(left.node());
        Network.joinNewNetwork(left.node());
        Network.joinNewNetwork(right.node());

        assertArrayEquals(new Object[]{null, "not enough energy"}, left.send(context, new TestArguments("payload")));
        assertEquals(List.of(), rightHost.signals);

        connector.setLocalBufferSize(101D);
        connector.changeBuffer(101D);

        assertArrayEquals(new Object[]{true}, left.send(context, new TestArguments("payload")));

        assertEquals(0.71875D, connector.localBuffer(), 0.000_001D);
        assertEquals(List.of(Arrays.asList("modem_message", left.node().address(), 0, 0D, "payload")), rightHost.signals);
    }

    @Test
    void sendConsumesOwnConnectorEnergyLikeUpstream() {
        OpenComputersApi.initialize();
        TestMachineHost rightHost = new TestMachineHost();
        LinkedCardEnvironment left = new LinkedCardEnvironment(new TestMachineHost(), "pair");
        LinkedCardEnvironment right = new LinkedCardEnvironment(rightHost, "pair");
        ComponentConnector leftConnector = assertInstanceOf(ComponentConnector.class, left.node());
        ComponentConnector contextConnector = assertInstanceOf(ComponentConnector.class, right.node());
        RecordingContext context = new RecordingContext(right.node());
        Network.joinNewNetwork(left.node());
        Network.joinNewNetwork(right.node());
        leftConnector.setLocalBufferSize(101D);
        leftConnector.changeBuffer(101D);

        assertArrayEquals(new Object[]{true}, left.send(context, new TestArguments("payload")));

        assertEquals(0.71875D, leftConnector.localBuffer(), 0.000_001D);
        assertEquals(0D, contextConnector.localBuffer(), 0.000_001D);
        assertEquals(List.of(Arrays.asList("modem_message", left.node().address(), 0, 0D, "payload")), rightHost.signals);
    }

    @Test
    void ignoresCardsOnOtherChannels() {
        OpenComputersApi.initialize();
        TestMachineHost receiverHost = new TestMachineHost();
        LinkedCardEnvironment sender = new LinkedCardEnvironment(new TestMachineHost(), "pair");
        LinkedCardEnvironment receiver = new LinkedCardEnvironment(receiverHost, "other");
        Network.joinNewNetwork(sender.node());
        Network.joinNewNetwork(receiver.node());
        charge(sender, 101D);

        assertArrayEquals(new Object[]{true}, sender.send(null, new TestArguments("payload")));

        assertEquals(List.of(), receiverHost.signals);
    }

    @Test
    void persistsChannelAndWakeMessage() {
        OpenComputersApi.initialize();
        LinkedCardEnvironment saved = new LinkedCardEnvironment(new TestMachineHost(), "pair");
        saved.setWakeMessage(null, new TestArguments("boot", true));
        CompoundTag tag = new CompoundTag();

        saved.save(tag);
        LinkedCardEnvironment loaded = new LinkedCardEnvironment(new TestMachineHost(), "fallback");
        loaded.load(tag);

        assertArrayEquals(new Object[]{"pair"}, loaded.getChannel(null, new TestArguments()));
        assertArrayEquals(new Object[]{"boot", true}, loaded.getWakeMessage(null, new TestArguments()));
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = LinkedCardEnvironment.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
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

    private static void charge(final LinkedCardEnvironment card, final double energy) {
        ComponentConnector connector = assertInstanceOf(ComponentConnector.class, card.node());
        connector.setLocalBufferSize(energy);
        connector.changeBuffer(energy);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private record RecordingContext(Node node) implements Context {
        @Override public boolean canInteract(final String player) { return true; }
        @Override public boolean isRunning() { return true; }
        @Override public boolean isPaused() { return false; }
        @Override public boolean start() { return true; }
        @Override public boolean pause(final double seconds) { return true; }
        @Override public boolean stop() { return true; }
        @Override public void consumeCallBudget(final double callCost) {}
        @Override public boolean signal(final String name, final Object... args) { return true; }
    }

    private static class TestHost implements EnvironmentHost {
        @Override public Level world() { return null; }
        @Override public double xPosition() { return 0; }
        @Override public double yPosition() { return 0; }
        @Override public double zPosition() { return 0; }
        @Override public void markChanged() {}
    }

    private static final class TestMachineHost extends TestHost implements MachineHost {
        private final List<List<Object>> signals = new ArrayList<>();
        private final TestMachine machine = new TestMachine(signals);

        @Override public Machine machine() { return machine; }
        @Override public Iterable<ItemStack> internalComponents() { return List.of(); }
        @Override public int componentSlot(final String address) { return -1; }
        @Override public void onMachineConnect(final Node node) {}
        @Override public void onMachineDisconnect(final Node node) {}
    }

    private static final class TestMachine implements Machine {
        private final List<List<Object>> signals;
        private final Node node;

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
        @Override public boolean start() { return true; }
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

    private static final class RecordingEnvironment implements li.cil.oc.api.network.Environment {
        private final List<Message> messages = new ArrayList<>();
        private final Node node = Network.newNode(this, Visibility.Network).create();

        @Override public Node node() { return node; }
        @Override public void onConnect(final Node node) {}
        @Override public void onDisconnect(final Node node) {}
        @Override public void onMessage(final Message message) { messages.add(message); }
    }

    private record TestArguments(Object... values) implements Arguments {
        @Override public int count() { return values.length; }
        @Override public Object checkAny(final int index) { return values[index]; }
        @Override public boolean checkBoolean(final int index) { return (Boolean) values[index]; }
        @Override public int checkInteger(final int index) { return ((Number) values[index]).intValue(); }
        @Override public long checkLong(final int index) { return ((Number) values[index]).longValue(); }
        @Override public double checkDouble(final int index) { return ((Number) values[index]).doubleValue(); }
        @Override public String checkString(final int index) { return (String) values[index]; }
        @Override public byte[] checkByteArray(final int index) { return (byte[]) values[index]; }
        @Override public Map checkTable(final int index) { return (Map) values[index]; }
        @Override public ItemStack checkItemStack(final int index) { return (ItemStack) values[index]; }
        @Override public Object optAny(final int index, final Object def) { return index < values.length ? values[index] : def; }
        @Override public boolean optBoolean(final int index, final boolean def) { return index < values.length ? checkBoolean(index) : def; }
        @Override public int optInteger(final int index, final int def) { return index < values.length ? checkInteger(index) : def; }
        @Override public long optLong(final int index, final long def) { return index < values.length ? checkLong(index) : def; }
        @Override public double optDouble(final int index, final double def) { return index < values.length ? checkDouble(index) : def; }
        @Override public String optString(final int index, final String def) { return index < values.length ? checkString(index) : def; }
        @Override public byte[] optByteArray(final int index, final byte[] def) { return index < values.length ? checkByteArray(index) : def; }
        @Override public Map optTable(final int index, final Map def) { return index < values.length ? checkTable(index) : def; }
        @Override public ItemStack optItemStack(final int index, final ItemStack def) { return index < values.length ? checkItemStack(index) : def; }
        @Override public boolean isBoolean(final int index) { return values[index] instanceof Boolean; }
        @Override public boolean isInteger(final int index) { return values[index] instanceof Integer; }
        @Override public boolean isLong(final int index) { return values[index] instanceof Long; }
        @Override public boolean isDouble(final int index) { return values[index] instanceof Double; }
        @Override public boolean isString(final int index) { return values[index] instanceof String; }
        @Override public boolean isByteArray(final int index) { return values[index] instanceof byte[]; }
        @Override public boolean isTable(final int index) { return values[index] instanceof Map; }
        @Override public boolean isItemStack(final int index) { return values[index] instanceof ItemStack; }
        @Override public Object[] toArray() { return values; }
        @Override public Iterator<Object> iterator() { return Arrays.asList(values).iterator(); }
    }
}
