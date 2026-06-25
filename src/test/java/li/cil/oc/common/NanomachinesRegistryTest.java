package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.Network;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.common.item.NanomachineItemData;
import li.cil.oc.common.nanomachines.provider.NanomachineDisintegrationProvider;
import li.cil.oc.common.nanomachines.provider.NanomachineHungryProvider;
import li.cil.oc.common.nanomachines.provider.NanomachineMagnetProvider;
import li.cil.oc.common.nanomachines.provider.NanomachineParticleProvider;
import li.cil.oc.common.nanomachines.provider.NanomachinePotionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertTrue(hasProvider((NanomachinesRegistry) API.nanomachines, NanomachineParticleProvider.class));
        assertTrue(hasProvider((NanomachinesRegistry) API.nanomachines, NanomachinePotionProvider.class));
        assertTrue(hasProvider((NanomachinesRegistry) API.nanomachines, NanomachineHungryProvider.class));
        assertTrue(hasProvider((NanomachinesRegistry) API.nanomachines, NanomachineMagnetProvider.class));
        assertTrue(hasProvider((NanomachinesRegistry) API.nanomachines, NanomachineDisintegrationProvider.class));
    }

    @Test
    void bootstrapRegistersDefaultNanomachineProvidersInUpstreamOrder() {
        OpenComputersApi.initialize();

        assertIterableEquals(List.of(
            NanomachineDisintegrationProvider.class,
            NanomachineHungryProvider.class,
            NanomachineParticleProvider.class,
            NanomachinePotionProvider.class,
            NanomachineMagnetProvider.class
        ), providerTypes((NanomachinesRegistry) API.nanomachines));
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
    void powerSyncMatchesUpstreamPeriodicAndChangedStateTiming() {
        assertTrue(NanomachinesRegistry.shouldSendPowerUpdate(20, false, false, 10));
        assertFalse(NanomachinesRegistry.shouldSendPowerUpdate(21, false, false, 10));
        assertTrue(NanomachinesRegistry.shouldSendPowerUpdate(21, true, false, 10));
        assertTrue(NanomachinesRegistry.shouldSendPowerUpdate(21, false, true, 10));
        assertTrue(NanomachinesRegistry.shouldSendPowerUpdate(21, false, false, 0));
    }

    @Test
    void controllerRespawnDrainsPowerLikeUpstream() {
        SimpleNanomachineController controller = new SimpleNanomachineController(null, new NanomachinesRegistry());

        NanomachinesRegistry.drainPowerOnRespawn(controller);

        assertEquals(0D, controller.getLocalBuffer(), 0.000_001D);
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
    void emptyNanomachineGraphHasNoTriggerInputsLikeUpstream() {
        NanomachinesRegistry registry = new NanomachinesRegistry();
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();

        controller.save(tag);

        assertEquals(0, controller.getTotalInputCount());
        assertEquals(0, tag.getList("triggers", CompoundTag.TAG_COMPOUND).size());
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
        ListTag behaviors = new ListTag();
        behaviors.add(behaviorTag("first", new int[]{0}, new int[0]));
        behaviors.add(behaviorTag("second", new int[]{1}, new int[0]));
        behaviors.add(behaviorTag("third", new int[]{0}, new int[0]));
        tag.put("behaviors", behaviors);
        tag.putIntArray("activeInputs", new int[]{0});

        controller.load(tag);

        assertIterableEquals(List.of(first, third), controller.getActiveBehaviors());
        assertEquals(1, controller.getInputCount(first));
        assertEquals(0, controller.getInputCount(second));
        assertEquals(1, controller.getInputCount(third));
    }

    @Test
    void controllerLoadsUpstreamTriggerListStates() {
        TestBehavior behavior = new TestBehavior("triggered");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new NamedBehaviorProvider(behavior));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();
        ListTag triggers = new ListTag();
        triggers.add(triggerTag(true));
        tag.put("triggers", triggers);
        ListTag behaviors = new ListTag();
        behaviors.add(behaviorTag("triggered", new int[]{0}, new int[0]));
        tag.put("behaviors", behaviors);

        controller.load(tag);

        assertTrue(controller.getInput(0));
        assertIterableEquals(List.of(behavior), controller.getActiveBehaviors());
    }

    @Test
    void controllerSavesUpstreamTriggerListStates() {
        TestBehavior behavior = new TestBehavior("triggered");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new NamedBehaviorProvider(behavior));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();
        ListTag behaviors = new ListTag();
        behaviors.add(behaviorTag("triggered", new int[]{0}, new int[0]));
        tag.put("behaviors", behaviors);
        tag.putIntArray("activeInputs", new int[]{0});
        controller.load(tag);

        CompoundTag saved = new CompoundTag();
        controller.save(saved);

        ListTag triggers = saved.getList("triggers", CompoundTag.TAG_COMPOUND);
        assertEquals(1, triggers.size());
        assertTrue(triggers.getCompound(0).getBoolean("isActive"));
    }

    @Test
    void controllerSavesUpstreamNestedRuntimeConfiguration() {
        TestBehavior behavior = new TestBehavior("nested");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new NamedBehaviorProvider(behavior));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();
        ListTag behaviors = new ListTag();
        behaviors.add(behaviorTag("nested", new int[]{0}, new int[0]));
        tag.put("behaviors", behaviors);
        tag.putIntArray("activeInputs", new int[]{0});
        controller.load(tag);

        CompoundTag saved = new CompoundTag();
        controller.save(saved);

        assertTrue(saved.contains("configuration", CompoundTag.TAG_COMPOUND));
        CompoundTag configuration = saved.getCompound("configuration");
        assertEquals(1, configuration.getList("triggers", CompoundTag.TAG_COMPOUND).size());
        assertEquals(1, configuration.getList("behaviors", CompoundTag.TAG_COMPOUND).size());
        assertTrue(configuration.getList("triggers", CompoundTag.TAG_COMPOUND).getCompound(0).getBoolean("isActive"));
    }

    @Test
    void controllerLoadsUpstreamNestedRuntimeConfiguration() {
        TestBehavior behavior = new TestBehavior("nested");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new NamedBehaviorProvider(behavior));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();
        CompoundTag configuration = new CompoundTag();
        ListTag triggers = new ListTag();
        triggers.add(triggerTag(true));
        configuration.put("triggers", triggers);
        ListTag behaviors = new ListTag();
        behaviors.add(behaviorTag("nested", new int[]{0}, new int[0]));
        configuration.put("behaviors", behaviors);
        tag.put("configuration", configuration);

        controller.load(tag);

        assertTrue(controller.getInput(0));
        assertIterableEquals(List.of(behavior), controller.getActiveBehaviors());
    }

    @Test
    void controllerRejectsSavedConnectorInputOutsideTriggerListLikeUpstream() {
        SimpleNanomachineController controller = new SimpleNanomachineController(null, new NanomachinesRegistry());
        CompoundTag tag = new CompoundTag();
        ListTag triggers = new ListTag();
        triggers.add(triggerTag(false));
        tag.put("triggers", triggers);
        ListTag connectors = new ListTag();
        CompoundTag connector = new CompoundTag();
        connector.putIntArray("triggerInputs", new int[]{1});
        connectors.add(connector);
        tag.put("connectors", connectors);

        assertThrows(IndexOutOfBoundsException.class, () -> controller.load(tag));
    }

    @Test
    void controllerRejectsSavedConnectorInputWhenTriggerListMissingLikeUpstream() {
        SimpleNanomachineController controller = new SimpleNanomachineController(null, new NanomachinesRegistry());
        CompoundTag tag = new CompoundTag();
        ListTag connectors = new ListTag();
        CompoundTag connector = new CompoundTag();
        connector.putIntArray("triggerInputs", new int[]{0});
        connectors.add(connector);
        tag.put("connectors", connectors);

        assertThrows(IndexOutOfBoundsException.class, () -> controller.load(tag));
    }

    @Test
    void controllerRejectsSavedBehaviorInputOutsideConnectorListLikeUpstream() {
        TestBehavior behavior = new TestBehavior("linked");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new NamedBehaviorProvider(behavior));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();
        ListTag triggers = new ListTag();
        triggers.add(triggerTag(false));
        tag.put("triggers", triggers);
        ListTag behaviors = new ListTag();
        behaviors.add(behaviorTag("linked", new int[0], new int[]{0}));
        tag.put("behaviors", behaviors);

        assertThrows(IndexOutOfBoundsException.class, () -> controller.load(tag));
    }

    @Test
    void controllerSetInputTrueFailsWhenAlreadyAtMaxActiveLikeUpstream() {
        SimpleNanomachineController controller = new SimpleNanomachineController(null, new NanomachinesRegistry());
        CompoundTag tag = new CompoundTag();
        ListTag triggers = new ListTag();
        final int maxActive = ModSettings.nanomachinesMaxInputsActive();
        assertTrue(maxActive > 0);
        for (int i = 0; i < maxActive; i++) {
            triggers.add(triggerTag(true));
        }
        tag.put("triggers", triggers);
        controller.load(tag);

        assertFalse(controller.setInput(0, true));
        assertTrue(controller.getInput(0));
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
    void controllerLoadsInputlessBehaviorAsActiveLikeUpstreamForAll() {
        TestBehavior behavior = new TestBehavior("inputless");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new NamedBehaviorProvider(behavior));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();
        ListTag behaviors = new ListTag();
        behaviors.add(behaviorTag("inputless", new int[0], new int[0]));
        tag.put("behaviors", behaviors);

        controller.load(tag);

        assertIterableEquals(List.of(behavior), controller.getActiveBehaviors());
        assertEquals(0, controller.getInputCount(behavior));
    }

    @Test
    void controllerLoadsBehaviorWithoutInputTagsAsInputlessLikeUpstream() {
        TestBehavior behavior = new TestBehavior("legacy-inputless");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new NamedBehaviorProvider(behavior));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();
        ListTag behaviors = new ListTag();
        CompoundTag behaviorTag = new CompoundTag();
        CompoundTag behaviorData = new CompoundTag();
        behaviorData.putString("name", "legacy-inputless");
        behaviorTag.put("behavior", behaviorData);
        behaviors.add(behaviorTag);
        tag.put("behaviors", behaviors);

        controller.load(tag);

        assertEquals(0, controller.getTotalInputCount());
        assertIterableEquals(List.of(behavior), controller.getActiveBehaviors());
        assertEquals(0, controller.getInputCount(behavior));
    }

    @Test
    void controllerLoadsEmptyConnectorsAsActiveLikeUpstream() {
        TestBehavior linked = new TestBehavior("linked");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new NamedBehaviorProvider(linked));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();
        tag.put("triggers", new ListTag());
        ListTag connectors = new ListTag();
        connectors.add(new CompoundTag());
        tag.put("connectors", connectors);
        ListTag behaviors = new ListTag();
        behaviors.add(behaviorTag("linked", new int[0], new int[]{0}));
        tag.put("behaviors", behaviors);

        controller.load(tag);

        assertEquals(0, controller.getTotalInputCount());
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
        ListTag behaviors = tag.getList("behaviors", CompoundTag.TAG_COMPOUND);
        assertTrue(hasConnectorBackedBehavior(behaviors));
        assertFalse(hasBehaviorWithoutInputs(behaviors));
        assertTrue(maxTriggerFanOut(connectors, behaviors) <= ModSettings.nanomachineMaxOutputs());
    }

    @Test
    void debugConfigurationMapsOneTriggerToEachBehaviorLikeUpstream() {
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new ListBehaviorProvider(List.of(
            new TestBehavior("first"),
            new TestBehavior("second"),
            new TestBehavior("third"))));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();

        controller.debugConfiguration();
        controller.save(tag);

        assertEquals(3, tag.getList("triggers", CompoundTag.TAG_COMPOUND).size());
        assertEquals(0, tag.getList("connectors", CompoundTag.TAG_COMPOUND).size());
        ListTag behaviors = tag.getList("behaviors", CompoundTag.TAG_COMPOUND);
        assertEquals(3, behaviors.size());
        for (int i = 0; i < behaviors.size(); i++) {
            assertArrayEquals(new int[]{i}, behaviors.getCompound(i).getIntArray("triggerInputs"));
            assertEquals(0, behaviors.getCompound(i).getIntArray("connectorInputs").length);
        }
    }

    @Test
    void configurationLinesShowBehaviorInputMappingLikeUpstreamPrint() {
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new ListBehaviorProvider(List.of(
            new TestBehavior("first"),
            new TestBehavior("second"))));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);

        controller.debugConfiguration();

        assertIterableEquals(List.of(
            "first <- (1)",
            "second <- (2)"), controller.configurationLines());
    }

    @Test
    void controllerIgnoresProvidersReturningNullBehaviorLists() {
        TestBehavior valid = new TestBehavior("valid");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new ListBehaviorProvider(null));
        registry.addProvider(new ListBehaviorProvider(List.of(valid)));

        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();
        controller.save(tag);

        assertEquals(1, tag.getList("behaviors", CompoundTag.TAG_COMPOUND).size());
    }

    @Test
    void controllerRandomizesGeneratedGraphOnReconfigure() {
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new ListBehaviorProvider(java.util.stream.IntStream.range(0, 20)
            .mapToObj(index -> (Behavior) new TestBehavior("behavior" + index))
            .toList()));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry, new Random(4L));
        CompoundTag first = new CompoundTag();
        CompoundTag second = new CompoundTag();

        controller.save(first);
        controller.reconfigure();
        controller.save(second);

        assertNotEquals(graphSignature(first), graphSignature(second));
        assertFalse(hasBehaviorWithoutInputs(second.getList("behaviors", CompoundTag.TAG_COMPOUND)));
        assertTrue(maxTriggerFanOut(
            second.getList("connectors", CompoundTag.TAG_COMPOUND),
            second.getList("behaviors", CompoundTag.TAG_COMPOUND)) <= ModSettings.nanomachineMaxOutputs());
    }

    @Test
    void generatedGraphCleansDeadConnectorsAfterBehaviorAssignmentLikeUpstream() throws Exception {
        withCachedConfig(ModSettings.NANOMACHINES_TRIGGER_QUOTA, 0.16D, () ->
            withCachedConfig(ModSettings.NANOMACHINES_CONNECTOR_QUOTA, 4D / 6D, () ->
                withCachedConfig(ModSettings.NANOMACHINE_MAX_INPUTS, 1, () ->
                    withCachedConfig(ModSettings.NANOMACHINE_MAX_OUTPUTS, 2, () -> {
                        NanomachinesRegistry registry = new NanomachinesRegistry();
                        registry.addProvider(new ListBehaviorProvider(java.util.stream.IntStream.range(0, 6)
                            .mapToObj(index -> (Behavior) new TestBehavior("behavior" + index))
                            .toList()));
                        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry, new Random(2L));
                        CompoundTag tag = new CompoundTag();

                        controller.save(tag);

                        assertEquals(1, tag.getList("triggers", CompoundTag.TAG_COMPOUND).size());
                        assertEquals(2, tag.getList("connectors", CompoundTag.TAG_COMPOUND).size());
                        assertEquals(2, tag.getList("behaviors", CompoundTag.TAG_COMPOUND).size());
                    }))));
    }

    @Test
    void generatedGraphCapsConnectorFanOutLikeUpstream() throws Exception {
        withCachedConfig(ModSettings.NANOMACHINES_TRIGGER_QUOTA, 0.25D, () ->
            withCachedConfig(ModSettings.NANOMACHINES_CONNECTOR_QUOTA, 0.2D, () ->
                withCachedConfig(ModSettings.NANOMACHINE_MAX_INPUTS, 1, () ->
                    withCachedConfig(ModSettings.NANOMACHINE_MAX_OUTPUTS, 2, () -> {
                        NanomachinesRegistry registry = new NanomachinesRegistry();
                        registry.addProvider(new ListBehaviorProvider(java.util.stream.IntStream.range(0, 20)
                            .mapToObj(index -> (Behavior) new TestBehavior("behavior" + index))
                            .toList()));
                        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry, new Random(1L));
                        CompoundTag tag = new CompoundTag();

                        controller.save(tag);

                        assertTrue(maxConnectorFanOut(tag.getList("behaviors", CompoundTag.TAG_COMPOUND)) <= ModSettings.nanomachineMaxOutputs());
                    }))));
    }

    @Test
    void generatedGraphMaintainsSourcePoolFanOutAcrossSeedsLikeUpstream() throws Exception {
        withCachedConfig(ModSettings.NANOMACHINES_TRIGGER_QUOTA, 0.4D, () ->
            withCachedConfig(ModSettings.NANOMACHINES_CONNECTOR_QUOTA, 0.2D, () ->
                withCachedConfig(ModSettings.NANOMACHINE_MAX_INPUTS, 2, () ->
                    withCachedConfig(ModSettings.NANOMACHINE_MAX_OUTPUTS, 2, () -> {
                        final List<Behavior> behaviors = java.util.stream.IntStream.range(0, 40)
                            .mapToObj(index -> (Behavior) new TestBehavior("behavior" + index))
                            .toList();
                        for (int seed = 0; seed < 64; seed++) {
                            NanomachinesRegistry registry = new NanomachinesRegistry();
                            registry.addProvider(new ListBehaviorProvider(behaviors));
                            SimpleNanomachineController controller = new SimpleNanomachineController(null, registry, new Random(seed));
                            CompoundTag tag = new CompoundTag();

                            controller.save(tag);

                            ListTag connectors = tag.getList("connectors", CompoundTag.TAG_COMPOUND);
                            ListTag savedBehaviors = tag.getList("behaviors", CompoundTag.TAG_COMPOUND);
                            assertFalse(hasConnectorWithoutInputs(connectors), "seed " + seed + " left dead connector");
                            assertFalse(hasBehaviorWithoutInputs(savedBehaviors), "seed " + seed + " left dead behavior");
                            assertTrue(maxTriggerFanOut(connectors, savedBehaviors, controller.getTotalInputCount()) <= ModSettings.nanomachineMaxOutputs(),
                                "seed " + seed + " exceeded trigger fan-out");
                            assertTrue(maxConnectorFanOut(savedBehaviors, connectors.size()) <= ModSettings.nanomachineMaxOutputs(),
                                "seed " + seed + " exceeded connector fan-out");
                        }
                    }))));
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
        runNanomachineCommandDelay(controller);

        assertTrue(sender.lastPacket != null);
        assertSame(endpoint, sender.lastSender);
        assertEquals(123, sender.lastPacket.port());
        assertArrayEquals(new Object[]{"nanomachines", "port", 123}, sender.lastPacket.data());
    }

    @Test
    void controllerResponsesConsumeWirelessEnergyLikeUpstream() {
        API.network = new NetworkRegistry();
        SimpleNanomachineController controller = new SimpleNanomachineController(null, new NanomachinesRegistry());
        SimpleNanomachineController baseline = new SimpleNanomachineController(null, new NanomachinesRegistry());
        RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint();
        Network.joinWirelessNetwork(sender);
        WirelessEndpoint endpoint = (WirelessEndpoint) (Object) controller;
        final double range = ModSettings.nanomachinesCommandRange();
        final double expectedCost = ModSettings.wirelessCostPerRange(1) * range * range;

        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 124}), sender);
        runNanomachineCommandDelay(controller);
        runNanomachineCommandDelay(baseline);

        assertTrue(sender.lastPacket != null);
        assertEquals(baseline.getLocalBuffer() - expectedCost, controller.getLocalBuffer(), 0.000_001D);
    }

    @Test
    void controllerResponsesUseUpstreamSquaredCommandRangeStrength() {
        API.network = new NetworkRegistry();
        SimpleNanomachineController controller = new SimpleNanomachineController(null, new NanomachinesRegistry());
        final double range = ModSettings.nanomachinesCommandRange();
        final int distancePastPlainRange = (int) Math.floor(range) + 1;
        assertTrue(range * range >= distancePastPlainRange);
        RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint(distancePastPlainRange, 0, 0);
        Network.joinWirelessNetwork(sender);
        WirelessEndpoint endpoint = (WirelessEndpoint) (Object) controller;

        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 125}), sender);
        runNanomachineCommandDelay(controller);

        assertTrue(sender.lastPacket != null);
        assertEquals(125, sender.lastPacket.port());
    }

    @Test
    void controllerIgnoresWirelessCommandsWithExtraArgumentsLikeUpstream() {
        API.network = new NetworkRegistry();
        SimpleNanomachineController controller = new SimpleNanomachineController(null, new NanomachinesRegistry());
        RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint();
        Network.joinWirelessNetwork(sender);
        WirelessEndpoint endpoint = (WirelessEndpoint) (Object) controller;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 126}), sender);
        runNanomachineCommandDelay(controller);
        sender.lastPacket = null;

        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "getPowerState", "extra"}), sender);
        runNanomachineCommandDelay(controller);

        assertNull(sender.lastPacket);
    }

    @Test
    void controllerRuntimeSaveRestoresUuidAndResponsePortLikeUpstream() {
        API.network = new NetworkRegistry();
        SimpleNanomachineController controller = new SimpleNanomachineController(null, new NanomachinesRegistry());
        RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint();
        Network.joinWirelessNetwork(sender);
        WirelessEndpoint endpoint = (WirelessEndpoint) (Object) controller;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 777}), sender);
        runNanomachineCommandDelay(controller);
        CompoundTag tag = new CompoundTag();

        controller.save(tag);

        SimpleNanomachineController loaded = new SimpleNanomachineController(null, new NanomachinesRegistry());
        loaded.load(tag);
        sender.lastPacket = null;
        ((WirelessEndpoint) (Object) loaded).receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "getPowerState"}), sender);
        runNanomachineCommandDelay(loaded);

        assertEquals(controller.uuid(), loaded.uuid());
        assertTrue(sender.lastPacket != null);
        assertEquals(777, sender.lastPacket.port());
    }

    @Test
    void controllerDelaysWirelessCommandResponsesAndIgnoresCommandsWhileWaiting() {
        API.network = new NetworkRegistry();
        SimpleNanomachineController controller = new SimpleNanomachineController(null, new NanomachinesRegistry());
        RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint();
        Network.joinWirelessNetwork(sender);
        WirelessEndpoint endpoint = (WirelessEndpoint) (Object) controller;

        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 654}), sender);

        assertNull(sender.lastPacket);

        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "getPowerState"}), sender);
        runNanomachineCommandDelay(controller);

        assertTrue(sender.lastPacket != null);
        assertEquals(654, sender.lastPacket.port());
        assertArrayEquals(new Object[]{"nanomachines", "port", 654}, sender.lastPacket.data());
    }

    @Test
    void controllerRespondsToGetPowerStateWirelessCommand() {
        API.network = new NetworkRegistry();
        SimpleNanomachineController controller = new SimpleNanomachineController(null, new NanomachinesRegistry());
        RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint();
        Network.joinWirelessNetwork(sender);
        WirelessEndpoint endpoint = (WirelessEndpoint) (Object) controller;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 321}), sender);
        runNanomachineCommandDelay(controller);
        sender.lastPacket = null;

        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "getPowerState"}), sender);
        final double expectedBuffer = controller.getLocalBuffer();
        runNanomachineCommandDelay(controller);

        assertTrue(sender.lastPacket != null);
        assertEquals(321, sender.lastPacket.port());
        assertArrayEquals(new Object[]{"nanomachines", "power", expectedBuffer, controller.getLocalBufferSize()}, sender.lastPacket.data());
    }

    @Test
    void controllerAcceptsByteArrayHeaderAndCommandForStatusCommandsLikeUpstream() {
        API.network = new NetworkRegistry();
        SimpleNanomachineController controller = new SimpleNanomachineController(null, new NanomachinesRegistry());
        RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint();
        Network.joinWirelessNetwork(sender);
        WirelessEndpoint endpoint = (WirelessEndpoint) (Object) controller;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{
            "nanomachines".getBytes(java.nio.charset.StandardCharsets.UTF_8),
            "setResponsePort".getBytes(java.nio.charset.StandardCharsets.UTF_8),
            322
        }), sender);
        runNanomachineCommandDelay(controller);
        sender.lastPacket = null;

        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{
            "nanomachines".getBytes(java.nio.charset.StandardCharsets.UTF_8),
            "getPowerState".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        }), sender);
        final double expectedBuffer = controller.getLocalBuffer();
        runNanomachineCommandDelay(controller);

        assertTrue(sender.lastPacket != null);
        assertEquals(322, sender.lastPacket.port());
        assertArrayEquals(new Object[]{"nanomachines", "power", expectedBuffer, controller.getLocalBufferSize()}, sender.lastPacket.data());
    }

    @Test
    void controllerRespondsToInputCountWirelessCommands() {
        API.network = new NetworkRegistry();
        SimpleNanomachineController controller = new SimpleNanomachineController(null, new NanomachinesRegistry());
        RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint();
        Network.joinWirelessNetwork(sender);
        WirelessEndpoint endpoint = (WirelessEndpoint) (Object) controller;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 444}), sender);
        runNanomachineCommandDelay(controller);

        sender.lastPacket = null;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "getTotalInputCount"}), sender);
        runNanomachineCommandDelay(controller);
        assertTrue(sender.lastPacket != null);
        assertEquals(444, sender.lastPacket.port());
        assertArrayEquals(new Object[]{"nanomachines", "totalInputCount", controller.getTotalInputCount()}, sender.lastPacket.data());

        sender.lastPacket = null;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "getSafeActiveInputs"}), sender);
        runNanomachineCommandDelay(controller);
        assertTrue(sender.lastPacket != null);
        assertEquals(444, sender.lastPacket.port());
        assertArrayEquals(new Object[]{"nanomachines", "safeActiveInputs", controller.getSafeActiveInputs()}, sender.lastPacket.data());

        sender.lastPacket = null;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "getMaxActiveInputs"}), sender);
        runNanomachineCommandDelay(controller);
        assertTrue(sender.lastPacket != null);
        assertEquals(444, sender.lastPacket.port());
        assertArrayEquals(new Object[]{"nanomachines", "maxActiveInputs", controller.getMaxActiveInputs()}, sender.lastPacket.data());
    }

    @Test
    void controllerRespondsToGetAndSetInputWirelessCommands() {
        API.network = new NetworkRegistry();
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new ListBehaviorProvider(List.of(new TestBehavior("input"))));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint();
        Network.joinWirelessNetwork(sender);
        WirelessEndpoint endpoint = (WirelessEndpoint) (Object) controller;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 555}), sender);
        runNanomachineCommandDelay(controller);

        sender.lastPacket = null;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "getInput", 1}), sender);
        runNanomachineCommandDelay(controller);
        assertTrue(sender.lastPacket != null);
        assertEquals(555, sender.lastPacket.port());
        assertArrayEquals(new Object[]{"nanomachines", "input", 1, false}, sender.lastPacket.data());

        sender.lastPacket = null;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setInput", 1, true}), sender);
        runNanomachineCommandDelay(controller);
        assertTrue(sender.lastPacket != null);
        assertEquals(555, sender.lastPacket.port());
        assertArrayEquals(new Object[]{"nanomachines", "input", 1, true}, sender.lastPacket.data());

        sender.lastPacket = null;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "getInput", 1}), sender);
        runNanomachineCommandDelay(controller);
        assertTrue(sender.lastPacket != null);
        assertEquals(555, sender.lastPacket.port());
        assertArrayEquals(new Object[]{"nanomachines", "input", 1, true}, sender.lastPacket.data());
    }

    @Test
    void controllerReportsInputErrorForInvalidWirelessIndex() {
        API.network = new NetworkRegistry();
        SimpleNanomachineController controller = new SimpleNanomachineController(null, new NanomachinesRegistry());
        RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint();
        Network.joinWirelessNetwork(sender);
        WirelessEndpoint endpoint = (WirelessEndpoint) (Object) controller;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 556}), sender);
        runNanomachineCommandDelay(controller);

        sender.lastPacket = null;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "getInput", 2}), sender);
        runNanomachineCommandDelay(controller);
        assertTrue(sender.lastPacket != null);
        assertEquals(556, sender.lastPacket.port());
        assertArrayEquals(new Object[]{"nanomachines", "input", "error"}, sender.lastPacket.data());

        sender.lastPacket = null;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setInput", 2, true}), sender);
        runNanomachineCommandDelay(controller);
        assertTrue(sender.lastPacket != null);
        assertEquals(556, sender.lastPacket.port());
        assertArrayEquals(new Object[]{"nanomachines", "input", "error"}, sender.lastPacket.data());
    }

    @Test
    void controllerRespondsToGetActiveEffectsWirelessCommand() {
        API.network = new NetworkRegistry();
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new ListBehaviorProvider(List.of(
            new TestBehavior("speed,boost\"now"),
            new TestBehavior(""))));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        controller.setInput(0, true);
        RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint();
        Network.joinWirelessNetwork(sender);
        WirelessEndpoint endpoint = (WirelessEndpoint) (Object) controller;
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 557}), sender);
        runNanomachineCommandDelay(controller);
        sender.lastPacket = null;

        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "getActiveEffects"}), sender);
        runNanomachineCommandDelay(controller);

        assertTrue(sender.lastPacket != null);
        assertEquals(557, sender.lastPacket.port());
        assertArrayEquals(new Object[]{"nanomachines", "effects", "{speed_boost_now}"}, sender.lastPacket.data());
    }

    @Test
    void controllerTicksActiveBehaviorsWhilePowered() {
        CountingBehavior behavior = new CountingBehavior("active");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new ListBehaviorProvider(List.of(behavior)));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        controller.setInput(0, true);

        controller.update();

        assertEquals(1, behavior.updateCount);

        controller.changeBuffer(-controller.getLocalBuffer());
        controller.update();

        assertEquals(1, behavior.updateCount);
    }

    @Test
    void controllerReportsActiveParticleEffectsForClientSync() {
        TestBehavior flame = new TestBehavior("particles.flame");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new ListBehaviorProvider(List.of(
            flame,
            new TestBehavior("speed"))));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();
        ListTag behaviors = new ListTag();
        behaviors.add(behaviorTag("particles.flame", new int[]{0}, new int[0]));
        tag.put("behaviors", behaviors);
        tag.putIntArray("activeInputs", new int[]{0});

        controller.load(tag);
        controller.setInput(0, true);

        assertEquals(List.of("flame"), controller.activeParticleEffects());
    }

    @Test
    void controllerRepeatsParticleEffectsByActiveInputCountForClientSync() {
        TestBehavior flame = new TestBehavior("particles.flame");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new NamedBehaviorProvider(flame));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();
        ListTag behaviors = new ListTag();
        behaviors.add(behaviorTag("particles.flame", new int[]{0, 1}, new int[0]));
        tag.put("behaviors", behaviors);
        tag.putIntArray("activeInputs", new int[]{0, 1});

        controller.load(tag);

        assertEquals(List.of("flame", "flame"), controller.activeParticleEffects());
    }

    @Test
    void controllerDrainsEnergyForActiveInputsOnPowerTicks() {
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new ListBehaviorProvider(List.of(new TestBehavior("active"))));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        controller.setInput(0, true);
        final double before = controller.getLocalBuffer();
        final double expectedCost = ModSettings.nanomachinesInputCost() * ModSettings.mfuTickFrequency() * 1.5D;

        for (int i = 1; i < ModSettings.mfuTickFrequency(); i++) {
            controller.update();
        }
        assertEquals(before, controller.getLocalBuffer());

        controller.update();

        assertEquals(before - expectedCost, controller.getLocalBuffer(), 0.000_001D);
    }

    @Test
    void controllerDisablesAndReenablesActiveBehaviorsOnPowerStateChangesLikeUpstream() {
        CountingBehavior behavior = new CountingBehavior("active");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new ListBehaviorProvider(List.of(behavior)));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        controller.setInput(0, true);

        controller.update();

        assertEquals(1, behavior.enableCount);
        assertEquals(1, behavior.updateCount);

        controller.changeBuffer(-controller.getLocalBuffer());
        controller.update();

        assertEquals(1, behavior.disableCount);
        assertEquals(DisableReason.OutOfEnergy, behavior.lastDisableReason);
        assertEquals(1, behavior.updateCount);

        controller.changeBuffer(1D);
        controller.update();

        assertEquals(2, behavior.enableCount);
        assertEquals(2, behavior.updateCount);
    }

    @Test
    void controllerDisposeClearsActiveInputsLikeUpstreamReset() {
        CountingBehavior behavior = new CountingBehavior("active");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new CountingBehaviorProvider(behavior));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        CompoundTag tag = new CompoundTag();
        ListTag triggers = new ListTag();
        triggers.add(triggerTag(true));
        tag.put("triggers", triggers);
        ListTag behaviors = new ListTag();
        behaviors.add(behaviorTag("active", new int[]{0}, new int[0]));
        tag.put("behaviors", behaviors);

        controller.load(tag);
        assertIterableEquals(List.of(behavior), controller.getActiveBehaviors());

        controller.dispose();

        assertFalse(controller.getInput(0));
        assertIterableEquals(List.of(), controller.getActiveBehaviors());
        assertEquals(1, behavior.disableCount);
        assertEquals(DisableReason.Default, behavior.lastDisableReason);
    }

    @Test
    void controllerDrainsEnergyWhenReconfigured() {
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new ListBehaviorProvider(List.of(new TestBehavior("active"))));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        final double before = controller.getLocalBuffer();

        controller.reconfigure();

        assertEquals(before - ModSettings.nanomachinesReconfigureCost(), controller.getLocalBuffer(), 0.000_001D);
    }

    @Test
    void controllerSavesConfigurationForNanomachinesItemWithoutRuntimeState() {
        TestBehavior behavior = new TestBehavior("saved");
        NanomachinesRegistry registry = new NanomachinesRegistry();
        registry.addProvider(new NamedBehaviorProvider(behavior));
        SimpleNanomachineController controller = new SimpleNanomachineController(null, registry);
        controller.setInput(0, true);
        controller.changeBuffer(-123D);
        final CompoundTag itemData = new CompoundTag();

        controller.saveItemConfiguration(itemData);

        assertEquals(controller.uuid(), NanomachineItemData.uuid(itemData));
        assertTrue(NanomachineItemData.hasConfiguration(itemData));
        final CompoundTag configuration = NanomachineItemData.configuration(itemData);
        assertFalse(configuration.contains("energy"));
        assertFalse(configuration.contains("activeInputs"));
        assertTrue(configuration.contains("behaviors", CompoundTag.TAG_LIST));

        final SimpleNanomachineController loaded = new SimpleNanomachineController(null, registry);
        loaded.loadItemConfiguration(itemData);

        assertEquals(controller.getTotalInputCount(), loaded.getTotalInputCount());
        assertFalse(loaded.getInput(0));
        assertEquals(ModSettings.nanomachinesBuffer() * 0.25D, loaded.getLocalBuffer(), 0.000_001D);
        assertIterableEquals(List.of(), loaded.getActiveBehaviors());
    }

    @Test
    void potionProviderCreatesBehaviorsInRegistryOrderLikeUpstream() throws Exception {
        withCachedConfig(ModSettings.NANOMACHINES_POTION_WHITELIST, List.of("haste", "speed"), () -> {
            NanomachinePotionProvider provider = new NanomachinePotionProvider();
            List<String> names = new java.util.ArrayList<>();

            for (final Behavior behavior : provider.createBehaviors(null)) {
                names.add(behavior.getNameHint());
            }

            assertEquals(List.of("speed", "haste"), names);
        });
    }

    private static boolean hasConnectorBackedBehavior(final ListTag behaviors) {
        for (int i = 0; i < behaviors.size(); i++) {
            if (behaviors.getCompound(i).getIntArray("connectorInputs").length > 0) {
                return true;
            }
        }
        return false;
    }

    private static CompoundTag behaviorTag(final String name, final int[] triggerInputs, final int[] connectorInputs) {
        final CompoundTag behavior = new CompoundTag();
        final CompoundTag behaviorData = new CompoundTag();
        behaviorData.putString("name", name);
        behavior.put("behavior", behaviorData);
        behavior.putIntArray("triggerInputs", triggerInputs);
        behavior.putIntArray("connectorInputs", connectorInputs);
        return behavior;
    }

    private static CompoundTag triggerTag(final boolean isActive) {
        final CompoundTag trigger = new CompoundTag();
        trigger.putBoolean("isActive", isActive);
        return trigger;
    }

    private static int maxTriggerFanOut(final ListTag connectors, final ListTag behaviors) {
        return maxTriggerFanOut(connectors, behaviors, 16);
    }

    private static int maxTriggerFanOut(final ListTag connectors, final ListTag behaviors, final int triggerCount) {
        final int[] counts = new int[Math.max(0, triggerCount)];
        for (int i = 0; i < connectors.size(); i++) {
            for (final int input : connectors.getCompound(i).getIntArray("triggerInputs")) {
                counts[input]++;
            }
        }
        for (int i = 0; i < behaviors.size(); i++) {
            for (final int input : behaviors.getCompound(i).getIntArray("triggerInputs")) {
                counts[input]++;
            }
        }
        int max = 0;
        for (final int count : counts) {
            max = Math.max(max, count);
        }
        return max;
    }

    private static int maxConnectorFanOut(final ListTag behaviors) {
        return maxConnectorFanOut(behaviors, 16);
    }

    private static int maxConnectorFanOut(final ListTag behaviors, final int connectorCount) {
        final int[] counts = new int[Math.max(0, connectorCount)];
        for (int i = 0; i < behaviors.size(); i++) {
            for (final int connector : behaviors.getCompound(i).getIntArray("connectorInputs")) {
                counts[connector]++;
            }
        }
        int max = 0;
        for (final int count : counts) {
            max = Math.max(max, count);
        }
        return max;
    }

    private static boolean hasConnectorWithoutInputs(final ListTag connectors) {
        for (int i = 0; i < connectors.size(); i++) {
            if (connectors.getCompound(i).getIntArray("triggerInputs").length == 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBehaviorWithoutInputs(final ListTag behaviors) {
        for (int i = 0; i < behaviors.size(); i++) {
            final CompoundTag behavior = behaviors.getCompound(i);
            if (behavior.getIntArray("triggerInputs").length == 0 && behavior.getIntArray("connectorInputs").length == 0) {
                return true;
            }
        }
        return false;
    }

    private static String graphSignature(final CompoundTag tag) {
        return tag.getList("connectors", CompoundTag.TAG_COMPOUND).toString() +
            tag.getList("behaviors", CompoundTag.TAG_COMPOUND);
    }

    private static boolean hasProvider(final NanomachinesRegistry registry, final Class<?> providerType) {
        for (final BehaviorProvider provider : registry.getProviders()) {
            if (providerType.isInstance(provider)) {
                return true;
            }
        }
        return false;
    }

    private static List<Class<?>> providerTypes(final NanomachinesRegistry registry) {
        final java.util.ArrayList<Class<?>> types = new java.util.ArrayList<>();
        for (final BehaviorProvider provider : registry.getProviders()) {
            types.add(provider.getClass());
        }
        return types;
    }

    private static <T> void withCachedConfig(
        final ModConfigSpec.ConfigValue<T> value,
        final T override,
        final ThrowingRunnable action
    ) throws Exception {
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

    private static void runNanomachineCommandDelay(final SimpleNanomachineController controller) {
        final int ticks = Math.max(1, (int) (ModSettings.nanomachinesCommandDelay() * 20D));
        for (int i = 0; i < ticks; i++) {
            controller.update();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class RecordingWirelessEndpoint implements WirelessEndpoint {
        private Packet lastPacket;
        private WirelessEndpoint lastSender;
        private final int x;
        private final int y;
        private final int z;

        private RecordingWirelessEndpoint() {
            this(0, 0, 0);
        }

        private RecordingWirelessEndpoint(final int x, final int y, final int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public int x() {
            return x;
        }

        @Override
        public int y() {
            return y;
        }

        @Override
        public int z() {
            return z;
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
            for (final Behavior behavior : behaviors) {
                if (behavior instanceof TestBehavior testBehavior && testBehavior.name().equals(nbt.getString("name"))) {
                    return behavior;
                }
            }
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

    private record CountingBehaviorProvider(CountingBehavior behavior) implements BehaviorProvider {
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
            return behavior.name.equals(nbt.getString("name")) ? behavior : null;
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

    private static final class CountingBehavior implements Behavior {
        private final String name;
        private int enableCount;
        private int disableCount;
        private DisableReason lastDisableReason;
        private int updateCount;

        private CountingBehavior(final String name) {
            this.name = name;
        }

        @Override
        public String getNameHint() {
            return name;
        }

        @Override
        public void onEnable() {
            enableCount++;
        }

        @Override
        public void onDisable(final DisableReason reason) {
            disableCount++;
            lastDisableReason = reason;
        }

        @Override
        public void update() {
            updateCount++;
        }
    }
}
