package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.Network;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.WirelessEndpoint;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NanomachinesRegistryTest {
    @AfterEach
    void resetApi() {
        API.nanomachines = null;
        API.network = null;
    }

    @Test
    void bootstrapInstallsNanomachinesApi() {
        OpenComputersApi.initialize();

        assertTrue(API.nanomachines instanceof NanomachinesRegistry);
    }

    @Test
    void registersProvidersInOrder() {
        NanomachinesRegistry registry = new NanomachinesRegistry();
        BehaviorProvider first = new TestBehaviorProvider();
        BehaviorProvider second = new TestBehaviorProvider();

        registry.addProvider(first);
        registry.addProvider(second);
        registry.addProvider(first);

        assertIterableEquals(List.of(first, second), registry.getProviders());
    }

    @Test
    void controllerRuntimeIsDeferred() {
        NanomachinesRegistry registry = new NanomachinesRegistry();

        assertFalse(registry.hasController(null));
        assertNull(registry.getController(null));
        assertNull(registry.installController(null));
        registry.uninstallController(null);
    }

    @Test
    void controllerPersistsBehaviorConfigurationThroughProviders() {
        NanomachinesRegistry registry = new NanomachinesRegistry();
        TrackingBehaviorProvider provider = new TrackingBehaviorProvider();
        registry.addProvider(provider);
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();

        controller.save(tag);

        assertEquals(1, provider.writeCount);

        SimpleNanomachineController loaded = new SimpleNanomachineController(null, registry);
        loaded.load(tag);

        assertEquals(1, provider.readCount);
    }

    @Test
    void controllerActivatesOnlyBehaviorsConnectedToActiveInputs() {
        TestBehavior first = new TestBehavior("first");
        TestBehavior second = new TestBehavior("second");
        TestBehavior third = new TestBehavior("third");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new ListBehaviorProvider(List.of(first, second, third)));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("activeInputs", new int[]{0});

        controller.load(tag);

        assertIterableEquals(List.of(first, third), controller.getActiveBehaviors());
        assertEquals(1, controller.getInputCount(first));
        assertEquals(0, controller.getInputCount(second));
        assertEquals(1, controller.getInputCount(third));
    }

    @Test
    void controllerActivatesConnectorBackedBehaviorsFromSavedConfiguration() {
        TestBehavior linked = new TestBehavior("linked");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new NamedBehaviorProvider(linked));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();
        ListTag connectors = new ListTag();
        CompoundTag connector = new CompoundTag();
        connector.putIntArray("triggerInputs", new int[]{0, 1});
        connectors.add(connector);
        tag.put("connectors", connectors);
        ListTag behaviors = new ListTag();
        CompoundTag behavior = new CompoundTag();
        CompoundTag behaviorData = new CompoundTag();
        behaviorData.putString("name", "linked");
        behavior.put("behavior", behaviorData);
        behavior.putIntArray("triggerInputs", new int[0]);
        behavior.putIntArray("connectorInputs", new int[]{0});
        behaviors.add(behavior);
        tag.put("behaviors", behaviors);
        tag.putIntArray("activeInputs", new int[]{0});

        controller.load(tag);

        assertIterableEquals(List.of(), controller.getActiveBehaviors());
        assertEquals(0, controller.getInputCount(linked));

        tag.putIntArray("activeInputs", new int[]{0, 1});
        controller.load(tag);

        assertIterableEquals(List.of(linked), controller.getActiveBehaviors());
        assertEquals(1, controller.getInputCount(linked));
    }

    @Test
    void controllerGeneratesConnectorGraphFromBehaviorCount() {
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new ListBehaviorProvider(java.util.stream.IntStream.range(0, 10)
            .mapToObj(index -> (Behavior) new TestBehavior("behavior" + index))
            .toList()));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();

        controller.save(tag);

        ListTag connectors = tag.getList("connectors", CompoundTag.TAG_COMPOUND);
        assertEquals(2, connectors.size());
        for (int i = 0; i < connectors.size(); i++) {
            assertTrue(connectors.getCompound(i).getIntArray("triggerInputs").length > 0);
        }
        assertTrue(hasConnectorBackedBehavior(tag.getList("behaviors", CompoundTag.TAG_COMPOUND)));
    }

    @Test
    void controllerRespondsToSetResponsePortWirelessCommand() {
        API.network = new NetworkRegistry();
        NanomachinesRegistry registry = new NanomachinesRegistry();
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint();
        Network.joinWirelessNetwork(sender);

        Object endpointCandidate = controller;
        assertTrue(endpointCandidate instanceof WirelessEndpoint);
        WirelessEndpoint endpoint = (WirelessEndpoint) endpointCandidate;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 123}), sender);

        assertTrue(sender.lastPacket != null);
        assertSame(endpoint, sender.lastSender);
        assertEquals(123, sender.lastPacket.port());
        assertArrayEquals(new Object[]{"nanomachines", "port", 123}, sender.lastPacket.data());
    }

    @Test
    void controllerRespondsToGetPowerStateWirelessCommand() {
        API.network = new NetworkRegistry();
        SimpleNanomachineController controller = new SimpleNanomachineController(null, new NanomachinesRegistry());
        RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint();
        Network.joinWirelessNetwork(sender);
        WirelessEndpoint endpoint = (WirelessEndpoint) (Object) controller;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 321}), sender);
        sender.lastPacket = null;

        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "getPowerState"}), sender);

        assertTrue(sender.lastPacket != null);
        assertEquals(321, sender.lastPacket.port());
        assertArrayEquals(new Object[]{"nanomachines", "power", controller.getLocalBuffer(), controller.getLocalBufferSize()}, sender.lastPacket.data());
    }

    private static boolean hasConnectorBackedBehavior(final ListTag behaviors) {
        for (int i = 0; i < behaviors.size(); i++) {
            if (behaviors.getCompound(i).getIntArray("connectorInputs").length > 0) {
                return true;
            }
        }
        return false;
    }

    private static final class RecordingWirelessEndpoint implements WirelessEndpoint {
        private Packet lastPacket;
        private WirelessEndpoint lastSender;

        @Override
        public int x() {
            return 0;
        }

        @Override
        public int y() {
            return 0;
        }

        @Override
        public int z() {
            return 0;
        }

        @Override
        public Level world() {
            return null;
        }

        @Override
        public void receivePacket(final Packet packet, final WirelessEndpoint sender) {
            lastPacket = packet;
            lastSender = sender;
        }
    }

    private static final class TestBehaviorProvider implements BehaviorProvider {
        @Override
        public Iterable<Behavior> createBehaviors(final Player player) {
            return List.of();
        }

        @Override
        public CompoundTag writeToNBT(final Behavior behavior) {
            return new CompoundTag();
        }

        @Override
        public Behavior readFromNBT(final Player player, final CompoundTag nbt) {
            return null;
        }
    }

    private record ListBehaviorProvider(List<Behavior> behaviors) implements BehaviorProvider {
        @Override
        public Iterable<Behavior> createBehaviors(final Player player) {
            return behaviors;
        }

        @Override
        public CompoundTag writeToNBT(final Behavior behavior) {
            return new CompoundTag();
        }

        @Override
        public Behavior readFromNBT(final Player player, final CompoundTag nbt) {
            return null;
        }
    }

    private record NamedBehaviorProvider(TestBehavior behavior) implements BehaviorProvider {
        @Override
        public Iterable<Behavior> createBehaviors(final Player player) {
            return List.of(behavior);
        }

        @Override
        public CompoundTag writeToNBT(final Behavior behavior) {
            final CompoundTag tag = new CompoundTag();
            tag.putString("name", behavior.getNameHint());
            return tag;
        }

        @Override
        public Behavior readFromNBT(final Player player, final CompoundTag nbt) {
            return behavior.name().equals(nbt.getString("name")) ? behavior : null;
        }
    }

    private static final class TrackingBehaviorProvider implements BehaviorProvider {
        private int writeCount;
        private int readCount;

        @Override
        public Iterable<Behavior> createBehaviors(final Player player) {
            return List.of(new TestBehavior("test"));
        }

        @Override
        public CompoundTag writeToNBT(final Behavior behavior) {
            writeCount++;
            final CompoundTag tag = new CompoundTag();
            tag.putString("name", behavior.getNameHint());
            return tag;
        }

        @Override
        public Behavior readFromNBT(final Player player, final CompoundTag nbt) {
            readCount++;
            return new TestBehavior("test");
        }
    }

    private record TestBehavior(String name) implements Behavior {
        @Override
        public String getNameHint() {
            return name;
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable(final li.cil.oc.api.nanomachines.DisableReason reason) {
        }

        @Override
        public void update() {
        }
    }
}
