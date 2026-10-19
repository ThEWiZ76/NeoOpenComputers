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
        controller.setItem(0, nativeCpu());
        controller.setItem(1, new ItemStack(ModItems.MEMORY_TIER1.get()));
        controller.setItem(3, new ItemStack(ModItems.NETWORK_CARD.get()));
        controller.setItem(2, RobotMovementPersistenceGameTests.eeprom("""
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
        controller.setItem(0, new ItemStack(ModItems.CPU_TIER1.get()));
        controller.setItem(1, new ItemStack(ModItems.MEMORY_TIER1.get()));
        controller.setItem(2, RobotMovementPersistenceGameTests.eeprom("while true do computer.pullSignal() end"));
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
