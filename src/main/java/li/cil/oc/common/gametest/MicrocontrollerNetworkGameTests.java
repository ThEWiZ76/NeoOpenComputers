package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.network.Packet;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class MicrocontrollerNetworkGameTests {
    @GameTest(template = "empty", batch = "microcontrollerPower")
    public static void microcontrollerConsumesConfiguredPowerAndHandlesExhaustion(final GameTestHelper helper) throws Exception {
        NeoOpenComputersGameTests.withCachedConfig(ModSettings.MICROCONTROLLER_BUFFER, 64D, () ->
            NeoOpenComputersGameTests.withCachedConfig(ModSettings.COMPUTER_COST, 9D, () ->
                NeoOpenComputersGameTests.withCachedConfig(ModSettings.MFU_TICK_FREQUENCY, 4, () -> {
                    for (final boolean ignorePower : List.of(false, true)) {
                        for (final double cost : List.of(0.25D, 0D)) {
                            NeoOpenComputersGameTests.withCachedConfig(ModSettings.IGNORE_POWER, ignorePower, () ->
                                NeoOpenComputersGameTests.withCachedConfig(ModSettings.MICROCONTROLLER_COST, cost,
                                    () -> configuredPower(helper, cost, ignorePower)));
                        }
                    }
                })));
        helper.succeed();
    }

    private static void configuredPower(final GameTestHelper helper, final double cost, final boolean ignorePower) {
        // An unregistered host receives exactly the explicit updates below; no
        // natural world tick or other test can advance this machine's power counter.
        final var host = new MicrocontrollerBlockEntity(helper.absolutePos(new BlockPos(1, 1, 1)),
            ModBlocks.MICROCONTROLLER_TIER1.get().defaultBlockState());
        host.setLevel(helper.getLevel());
        try {
            host.onLoad();
            MicrocontrollerFixture.setItem(host, 0, nativeCpu());
            MicrocontrollerFixture.setItem(host, 1, new ItemStack(ModItems.MEMORY_TIER1.get()));
            MicrocontrollerFixture.setItem(host, 2, RobotMovementPersistenceGameTests.eeprom("while true do computer.pullSignal() end"));
            final var machine = host.machine();
            final var connector = (Connector) machine.node();
            helper.assertTrue(connector.localBufferSize() == 0 && connector.globalBufferSize() == 64 && connector.globalBuffer() == 0,
                "Custom private capacity ignored or new host received free power");
            helper.assertTrue(machine.getCostPerTick() == cost, "Microcontroller used generic computer cost instead of its own setting");
            if (!ignorePower) connector.changeBuffer(64);
            final double initial = connector.globalBuffer();
            helper.assertTrue(machine.start(), "Configured native machine did not start");
            final int frequency = ModSettings.mfuTickFrequency();
            for (int tick = 0; tick < frequency * 3; tick++) machine.update();
            final double charged = ignorePower ? 0 : cost * frequency * 3;
            helper.assertTrue(machine.isRunning() && Math.abs(connector.globalBuffer() - (initial - charged)) < 0.000001,
                "Wrong running consumption: cost=" + cost + ", ignore=" + ignorePower + ", buffer=" + connector.globalBuffer()
                    + ", error=" + machine.lastError());
            helper.assertTrue(machine.pause(1), "Could not pause running machine");
            for (int tick = 0; tick < frequency; tick++) machine.update();
            final double afterPause = initial - (ignorePower ? 0 : cost * frequency * 4);
            helper.assertTrue(machine.isPaused() && Math.abs(connector.globalBuffer() - afterPause) < 0.000001,
                "Paused machine did not retain running power policy");
            machine.stop();
            for (int tick = 0; tick < frequency * 2; tick++) machine.update();
            helper.assertTrue(Math.abs(connector.globalBuffer() - afterPause) < 0.000001, "Stopped machine still consumed running energy");
            if (!ignorePower && cost > 0) {
                connector.changeBuffer(-connector.globalBuffer());
                final double insufficient = cost * frequency - 0.125D;
                connector.changeBuffer(insufficient);
                helper.assertTrue(machine.start(), "Native machine did not restart for exhaustion probe");
                for (int tick = 0; tick < frequency; tick++) machine.update();
                helper.assertTrue(!machine.isRunning() && "gui.Error.NoEnergy".equals(machine.lastError())
                    && Math.abs(connector.globalBuffer() - insufficient) < 0.000001,
                    "Exhaustion did not stop atomically with NoEnergy");
            }
            if (!ignorePower) {
                final var legacy = host.saveWithFullMetadata(helper.getLevel().registryAccess());
                legacy.remove("oc:snooper");
                legacy.getCompound("oc:machine").getCompound("node").putDouble("buffer", 317);
                host.loadWithComponents(legacy, helper.getLevel().registryAccess());
                helper.assertTrue(connector.globalBuffer() == 64 && connector.localBuffer() == 0,
                    "Legacy energy did not honor custom private capacity");
            }
        } finally {
            host.setRemoved();
        }
    }

    @GameTest(template = "empty")
    public static void microcontrollerRotationRewiresPhysicalCablesAndClosesNewFront(final GameTestHelper helper) {
        final var controller = place(helper);
        final var relative = new BlockPos(1, 1, 1);
        final var cables = new java.util.EnumMap<Direction, li.cil.oc.common.blockentity.CableBlockEntity>(Direction.class);
        for (final Direction side : Direction.values()) {
            helper.setBlock(relative.relative(side), ModBlocks.CABLE.get());
            final li.cil.oc.common.blockentity.CableBlockEntity cable = helper.getBlockEntity(relative.relative(side));
            cable.onLoad();
            Network.joinOrCreateNetwork(cable);
            cables.put(side, cable);
        }
        ((Connector) controller.machine().node()).changeBuffer(731);
        final String privateAddress = controller.machine().node().address();
        for (final Direction front : List.of(Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH)) {
            final Node closingPlug = controller.sidedNode(front);
            final Node closingControl = controller.onAnalyze(null, front, 0, 0, 0)[0];
            helper.getLevel().setBlockAndUpdate(controller.getBlockPos(), controller.getBlockState()
                .setValue(li.cil.oc.common.block.MicrocontrollerBlock.FACING, front));
            helper.assertTrue(helper.getLevel().getBlockEntity(controller.getBlockPos()) == controller, "Rotation replaced machine host");
            helper.assertTrue(closingPlug.network() == null && closingControl.network() == null, "Rotation retained network on new front");
            for (final Direction side : Direction.values()) {
                final var cable = cables.get(side);
                if (side == front) helper.assertTrue(controls(cable.node()) == 0, "Front cable still sees control component");
                else helper.assertTrue(cable.node().isNeighborOf(controller.sidedNode(side)) && controls(cable.node()) == 1,
                    "Rotated face did not reconnect cable on " + side);
                final var connection = helper.getLevel().getBlockState(cable.getBlockPos())
                    .getValue(li.cil.oc.common.block.CableBlock.CONNECTIONS.get(side.getOpposite()));
                helper.assertTrue(connection == (side == front ? li.cil.oc.common.block.CableBlock.Connection.NONE
                    : li.cil.oc.common.block.CableBlock.Connection.DEVICE), "Cable connection shape disagrees with rotated port on " + side
                        + ": " + connection + ", front=" + front);
                helper.assertTrue((helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, controller.getBlockPos(), side) == null)
                    == (side == front), "Rotated FE input is exposed on the wrong face");
            }
            helper.assertTrue(privateAddress.equals(controller.machine().node().address())
                && ((Connector) controller.machine().node()).globalBuffer() == 731, "Rotation changed private identity or energy");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void microcontrollerRoutesPacketsWithoutPassiveExternalRelay(final GameTestHelper helper) throws Exception {
        final var controller = place(helper);
        final var west = new Receiver();
        final var east = new Receiver();
        final var internal = new Receiver();
        west.node.connect(controller.sidedNode(Direction.WEST));
        east.node.connect(controller.sidedNode(Direction.EAST));
        internal.node.connect(controller.machine().node());
        final Component control = control(helper, controller, Direction.WEST);
        control.invoke("setSideOpen", null, Direction.WEST.get3DDataValue(), false);
        final Packet incoming = Network.newPacket("external-probe", null, 731, new byte[]{0, (byte) 255, 7});
        west.node.sendToReachable("network.message", incoming);
        helper.assertTrue(internal.messages.size() == 1 && internal.messages.getFirst().data()[0] == incoming,
            "Closed output must still deliver the unchanged incoming packet internally");
        helper.assertTrue(east.messages.isEmpty(), "Microcontroller passively relayed an external packet");
        west.node.connect(east.node);
        internal.messages.clear();
        west.node.sendToReachable("network.message", incoming);
        helper.assertTrue(internal.messages.size() == 1, "Two plugs on one external network duplicated incoming delivery");
        west.node.disconnect(east.node);
        west.messages.clear();
        east.messages.clear();
        final Packet outgoing = Network.newPacket("internal-probe", null, 732, "reply");
        internal.node.sendToReachable("network.message", outgoing);
        helper.assertTrue(west.messages.isEmpty(), "Closed output emitted a packet");
        helper.assertTrue(east.messages.size() == 1 && east.messages.getFirst().data()[0] == outgoing
            && ((Packet) east.messages.getFirst().data()[0]).ttl() == outgoing.ttl(), "Open output altered or dropped packet");
        west.node.remove();
        east.node.remove();
        internal.node.remove();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void microcontrollerPumpsExternalPowerAndAcceptsSidedForgeEnergy(final GameTestHelper helper) {
        final var controller = place(helper);
        final Connector machine = (Connector) controller.machine().node();
        helper.assertTrue(machine.localBufferSize() == 0 && machine.globalBufferSize() == ModSettings.microcontrollerBuffer()
            && machine.globalBuffer() == 0, "Fresh microcontroller must use its own initially empty private buffer");
        final var world = helper.getLevel();
        helper.assertTrue(world.getCapability(Capabilities.EnergyStorage.BLOCK, controller.getBlockPos(), controller.facing()) == null,
            "Front face exposes FE input");
        final var energy = world.getCapability(Capabilities.EnergyStorage.BLOCK, controller.getBlockPos(), Direction.UP);
        helper.assertTrue(energy != null && energy.canReceive() && !energy.canExtract(), "Missing receive-only FE capability");
        final int accepted = energy.receiveEnergy(Integer.MAX_VALUE, true);
        helper.assertTrue(accepted == ModSettings.toForgeEnergy(ModSettings.caseRate(0)) && machine.globalBuffer() == 0,
            "FE simulation mutated storage or ignored tier-one throughput");
        helper.assertTrue(energy.receiveEnergy(Integer.MAX_VALUE, false) == accepted
            && machine.globalBuffer() == ModSettings.caseRate(0), "FE did not charge private buffer");
        machine.changeBuffer(-machine.globalBuffer());
        final var source = new Receiver();
        ((Connector) source.node).changeBuffer(731);
        source.node.connect(controller.sidedNode(Direction.WEST));
        source.node.connect(controller.sidedNode(Direction.EAST));
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(machine.globalBuffer() == 731,
            "Shared external network did not fill private buffer exactly once"))
            .thenExecute(() -> {
                helper.assertTrue(((Connector) source.node).globalBuffer() == 0, "Energy was duplicated across side networks");
                source.node.remove();
            }).thenSucceed();
    }

    @GameTest(template = "empty")
    public static void microcontrollerRestoresExternalIdentityEnergyAndOutputFlags(final GameTestHelper helper) throws Exception {
        MicrocontrollerBlockEntity controller = place(helper);
        ((Connector) controller.machine().node()).changeBuffer(731);
        control(helper, controller, Direction.WEST).invoke("setSideOpen", null, Direction.WEST.get3DDataValue(), false);
        final List<String> addresses = addresses(controller);
        for (final boolean sameInstance : List.of(false, true)) {
            controller.onChunkUnloaded();
            final var saved = controller.saveWithFullMetadata(helper.getLevel().registryAccess());
            controller.setRemoved();
            if (sameInstance) controller.clearRemoved();
            else controller = (MicrocontrollerBlockEntity) BlockEntity.loadStatic(controller.getBlockPos(), controller.getBlockState(), saved,
                helper.getLevel().registryAccess());
            helper.getLevel().setBlockEntity(controller);
            controller.onLoad();
            helper.assertTrue(addresses.equals(addresses(controller)), "Reload changed private, plug or control identity");
            helper.assertTrue(((Connector) controller.machine().node()).globalBuffer() == 731, "Reload changed private energy");
            helper.assertTrue(Boolean.FALSE.equals(control(helper, controller, Direction.WEST).invoke("isSideOpen", null, 4)[0]),
                "Reload reopened a closed output");
        }
        final var legacy = controller.saveWithFullMetadata(helper.getLevel().registryAccess());
        legacy.remove("oc:snooper");
        legacy.getCompound("oc:machine").getCompound("node").putDouble("buffer", 317);
        controller.loadWithComponents(legacy, helper.getLevel().registryAccess());
        helper.assertTrue(((Connector) controller.machine().node()).globalBuffer() == 317, "Legacy computer-node energy was discarded");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeComputerControlsAndExchangesPacketsWithIsolatedMicrocontroller(final GameTestHelper helper) {
        final var controller = place(helper);
        MicrocontrollerFixture.setItem(controller, 0, nativeCpu());
        MicrocontrollerFixture.setItem(controller, 1, new ItemStack(ModItems.MEMORY_TIER1.get()));
        MicrocontrollerFixture.setItem(controller, 3, new ItemStack(ModItems.NETWORK_CARD.get()));
        MicrocontrollerFixture.setItem(controller, 2, RobotMovementPersistenceGameTests.eeprom("""
            local modem = component.proxy(component.list('modem')())
            local mcu = component.proxy(component.list('microcontroller')())
            assert(not mcu.isSideOpen(4), 'external configuration missing')
            modem.open(731)
            modem.broadcast(731, 'ready')
            repeat
              local name, source, port, distance, data = computer.pullSignal()
              if name == 'modem_message' and port == 731 and data == 'ping' then break end
            until false
            modem.broadcast(731, 'pong')
            while true do computer.pullSignal() end
            """));
        final BlockPos casePos = new BlockPos(2, 1, 1);
        helper.setBlock(casePos, ModBlocks.COMPUTER_CASE_TIER1.get());
        final ComputerCaseBlockEntity computer = helper.getBlockEntity(casePos);
        computer.setItem(0, new ItemStack(ModItems.NETWORK_CARD.get()));
        computer.setItem(2, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(4, nativeCpu());
        computer.setItem(6, RobotMovementPersistenceGameTests.eeprom("""
            local modem = component.proxy(component.list('modem')())
            local mcu = component.proxy(component.list('microcontroller')())
            local count = 0
            for address in component.list('eeprom') do count = count + 1 end
            assert(count == 1, 'microcontroller leaked its EEPROM')
            modem.open(731)
            assert(not mcu.isRunning() and mcu.setSideOpen(4, false))
            assert(mcu.start())
            while true do
              local name, source, port, distance, data = computer.pullSignal()
              if name == 'modem_message' and port == 731 then
                if data == 'ready' then modem.broadcast(731, 'ping')
                elseif data == 'pong' then break end
              end
            end
            assert(mcu.stop() and not mcu.isRunning())
            component.proxy(component.list('eeprom')()).setData('verified')
            while true do computer.pullSignal() end
            """));
        computer.onLoad();
        Network.joinOrCreateNetwork(controller);
        final var power = new Receiver();
        ((Connector) power.node).changeBuffer(1000);
        power.node.connect(computer.node());
        final var west = new Receiver();
        west.node.connect(controller.sidedNode(Direction.WEST));
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(((Connector) controller.machine().node()).globalBuffer() >= 1000,
            "Microcontroller did not draw external test power"))
            .thenExecute(() -> helper.assertTrue(computer.machine().start(), "External native computer did not start"))
            .thenWaitUntil(() -> helper.assertTrue(marker(computer.getItem(6)).equals("verified"),
                "Native control exchange did not finish: external=" + computer.machine().lastError() + ", internal=" + controller.machine().lastError()))
            .thenExecute(() -> {
                helper.assertTrue(west.messages.isEmpty(), "Closed output forwarded native packets");
                computer.machine().stop();
                power.node.remove();
                west.node.remove();
            }).thenSucceed();
    }

    private static List<String> addresses(final MicrocontrollerBlockEntity controller) {
        final var result = new ArrayList<String>();
        result.add(controller.machine().node().address());
        for (final Direction side : Direction.values()) {
            if (side == controller.facing()) continue;
            result.add(controller.sidedNode(side).address());
            result.add(controller.onAnalyze(null, side, 0, 0, 0)[0].address());
        }
        for (final var entry : controller.machine().components().entrySet()) {
            if (entry.getValue().equals("microcontroller")) result.add(entry.getKey());
        }
        return result;
    }

    private static ItemStack nativeCpu() {
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        return cpu;
    }

    private static String marker(final ItemStack eeprom) {
        return new String(ItemDriverData.dataTag(eeprom).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }

    @GameTest(template = "empty")
    public static void microcontrollerSeparatesExternalSidesAndInternalHardware(final GameTestHelper helper) {
        final var controller = place(helper);
        helper.assertTrue(controller.node() == null, "Microcontroller exposes its internal machine as the external node");
        final SidedEnvironment sided = sides(helper, controller);
        helper.assertTrue(!sided.canConnect(controller.facing()) && sided.sidedNode(controller.facing()) == null,
            "Front face must not have a network connector");
        final List<Node> plugs = new ArrayList<>();
        for (final Direction side : Direction.values()) {
            if (side == controller.facing()) continue;
            final Node plug = sided.sidedNode(side);
            helper.assertTrue(sided.canConnect(side) && plug instanceof Connector, "Missing powered connector on " + side);
            helper.assertTrue(plug.network() != controller.machine().node().network(), "External network exposes internal hardware");
            for (final Node other : plugs) helper.assertTrue(other.network() != plug.network(), "External sides share a network");
            helper.assertTrue(controls(plug) == 1, "Expected one external control component on " + side);
            plugs.add(plug);
        }
        final Node first = plugs.get(0), second = plugs.get(1);
        first.connect(second);
        helper.assertTrue(controls(first) == 1, "Merged external network has duplicate control components");
        first.disconnect(second);
        helper.assertTrue(controls(first) == 1 && controls(second) == 1, "Split network lost a control component");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void microcontrollerExposesExternalControlAndGlobalOutputSides(final GameTestHelper helper) throws Exception {
        final var controller = place(helper);
        MicrocontrollerFixture.setItem(controller, 0, new ItemStack(ModItems.CPU_TIER1.get()));
        MicrocontrollerFixture.setItem(controller, 1, new ItemStack(ModItems.MEMORY_TIER1.get()));
        MicrocontrollerFixture.setItem(controller, 2, RobotMovementPersistenceGameTests.eeprom("while true do computer.pullSignal() end"));
        ((Connector) controller.machine().node()).changeBuffer(500);
        final Component control = control(helper, controller, Direction.WEST);
        for (final String name : List.of("start", "stop", "isRunning", "lastError", "isSideOpen", "setSideOpen")) {
            helper.assertTrue(control.methods().contains(name), "Missing microcontroller callback " + name);
        }
        helper.assertTrue(Boolean.FALSE.equals(control.invoke("isRunning", null)[0]), "Unstarted controller is running");
        helper.assertTrue(Boolean.TRUE.equals(control.invoke("start", null)[0]), "External start did not start machine");
        helper.assertTrue(Boolean.TRUE.equals(control.invoke("isRunning", null)[0]), "External state does not see running machine");
        controller.machine().pause(1);
        helper.assertTrue(Boolean.FALSE.equals(control.invoke("start", null)[0]), "External start resumed paused machine");
        helper.assertTrue(Boolean.TRUE.equals(control.invoke("stop", null)[0]), "External stop did not stop machine");
        controller.machine().crash("micro-test-error");
        helper.assertTrue("micro-test-error".equals(control.invoke("lastError", null)[0]), "External error lost crash reason");
        helper.assertTrue(control.annotation("isRunning").direct() && !control.annotation("start").direct(), "Wrong callback scheduling");
        final int west = Direction.WEST.get3DDataValue();
        helper.assertTrue(Boolean.TRUE.equals(control.invoke("setSideOpen", null, west, false)[0]), "setSideOpen must return old state");
        helper.assertTrue(Boolean.FALSE.equals(control.invoke("isSideOpen", null, west)[0]), "Output side did not close");
        helper.assertTrue(sides(helper, controller).canConnect(Direction.WEST), "Closing output disconnected physical control");
        assertFailure(helper, control, 6, "invalid side");
        assertFailure(helper, control, controller.facing().get3DDataValue(), "unsupported side");
        helper.succeed();
    }

    private static void assertFailure(final GameTestHelper helper, final Component component, final int side, final String expected) throws Exception {
        try {
            component.invoke("isSideOpen", null, side);
            helper.fail("Accepted invalid side " + side);
        } catch (final IllegalArgumentException exception) {
            helper.assertTrue(expected.equals(exception.getMessage()), "Wrong side error: " + exception.getMessage());
        }
    }

    private static MicrocontrollerBlockEntity place(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.MICROCONTROLLER_TIER1.get());
        final MicrocontrollerBlockEntity controller = helper.getBlockEntity(new BlockPos(1, 1, 1));
        controller.onLoad();
        return controller;
    }

    private static SidedEnvironment sides(final GameTestHelper helper, final MicrocontrollerBlockEntity controller) {
        helper.assertTrue(controller instanceof SidedEnvironment, "Microcontroller has no separate side networks");
        return (SidedEnvironment) controller;
    }

    private static Component control(final GameTestHelper helper, final MicrocontrollerBlockEntity controller, final Direction side) {
        final Node[] analyzed = controller.onAnalyze(null, side, 0.5F, 0.5F, 0.5F);
        helper.assertTrue(analyzed.length == 1 && analyzed[0] instanceof Component, "Missing analyzed component");
        final Component component = (Component) analyzed[0];
        helper.assertTrue("microcontroller".equals(component.name()), "External analyzer exposes " + component.name());
        return component;
    }

    private static int controls(final Node source) {
        int result = 0;
        for (final Node node : source.reachableNodes()) {
            if (node instanceof Component component && component.name().equals("microcontroller")) result++;
        }
        return result;
    }

    private static final class Receiver implements Environment {
        private final Node node = Network.newNode(this, Visibility.Network).withConnector(1000).create();
        private final List<Message> messages = new ArrayList<>();

        private Receiver() { Network.joinNewNetwork(node); }
        @Override public Node node() { return node; }
        @Override public void onConnect(final Node connected) { }
        @Override public void onDisconnect(final Node disconnected) { }
        @Override public void onMessage(final Message message) { if (message.name().equals("network.message")) messages.add(message); }
    }
}
