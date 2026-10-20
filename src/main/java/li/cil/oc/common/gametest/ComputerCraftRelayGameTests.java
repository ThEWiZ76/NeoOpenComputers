package li.cil.oc.common.gametest;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.blockentity.RelayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@GameTestHolder("neoopencomputers")
public final class ComputerCraftRelayGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("computercraft")) {
            if (Boolean.getBoolean("neoopencomputers.testComputerCraft")) throw new IllegalStateException("Requested CC is absent");
            return List.of();
        }
        return Loaded.tests();
    }

    private static final class Loaded {
        static Collection<TestFunction> tests() {
            return List.of(test("cc_relay_actual_lua_contract", Loaded::actualLua),
                test("cc_relay_actual_lua_messages", Loaded::actualMessages),
                test("cc_relay_attachment_filtering", Loaded::attachments),
                test("cc_relay_queue_and_remote_visibility", Loaded::queueAndVisibility),
                test("cc_relay_native53_roundtrip", Loaded::nativeRoundtrip));
        }

        static TestFunction test(String name, java.util.function.Consumer<GameTestHelper> body) {
            return new TestFunction("computerCraftRelay", name, "neoopencomputers:ae2_export", 600, 0, true, body);
        }

        @SuppressWarnings("unchecked")
        static dan200.computercraft.api.peripheral.IDynamicPeripheral peripheral(GameTestHelper helper, RelayBlockEntity relay, Direction side) {
            try {
                final var capability = (net.neoforged.neoforge.capabilities.BlockCapability<dan200.computercraft.api.peripheral.IPeripheral, Direction>)
                    Class.forName("dan200.computercraft.api.peripheral.PeripheralCapability").getMethod("get").invoke(null);
                final var peripheral = helper.getLevel().getCapability(capability, relay.getBlockPos(), side);
                helper.assertTrue(peripheral instanceof dan200.computercraft.api.peripheral.IDynamicPeripheral,
                    "OC relay is missing its ComputerCraft modem peripheral capability");
                return (dan200.computercraft.api.peripheral.IDynamicPeripheral) peripheral;
            } catch (ReflectiveOperationException exception) { throw new RuntimeException(exception); }
        }

        static RelayBlockEntity relay(GameTestHelper helper) {
            final var pos = helper.absolutePos(new BlockPos(2, 2, 2));
            helper.getLevel().setBlockAndUpdate(pos, ModBlocks.RELAY.get().defaultBlockState());
            final var relay = (RelayBlockEntity) helper.getLevel().getBlockEntity(pos);
            peripheral(helper, relay, Direction.UP);
            return relay;
        }

        static Object[] call(dan200.computercraft.api.peripheral.IDynamicPeripheral peripheral,
                             Access access, String method, Object... values) {
            try {
                final int index = Arrays.asList(peripheral.getMethodNames()).indexOf(method);
                if (index < 0) throw new AssertionError("Missing CC method " + method);
                final var context = new dan200.computercraft.api.lua.ILuaContext() {
                    public long issueMainThreadTask(dan200.computercraft.api.lua.LuaTask task) { throw new AssertionError("Unexpected asynchronous task"); }
                    public dan200.computercraft.api.lua.MethodResult executeMainThreadTask(dan200.computercraft.api.lua.LuaTask task) throws dan200.computercraft.api.lua.LuaException {
                        return dan200.computercraft.api.lua.MethodResult.of(task.execute());
                    }
                };
                return peripheral.callMethod(access, context, index, new dan200.computercraft.api.lua.ObjectArguments(values)).getResult();
            } catch (dan200.computercraft.api.lua.LuaException exception) { throw new IllegalArgumentException(exception.getMessage(), exception); }
        }

        static void rejects(GameTestHelper helper, Runnable action, String expected) {
            try { action.run(); helper.fail("Expected error: " + expected); }
            catch (IllegalArgumentException exception) { helper.assertTrue(exception.getMessage().contains(expected), "Wrong error: " + exception.getMessage()); }
        }

        static void actualLua(GameTestHelper helper) {
            final var relay = relay(helper);
            final var peerPos = relay.getBlockPos().east(2);
            helper.getLevel().setBlockAndUpdate(peerPos, ModBlocks.RELAY.get().defaultBlockState());
            final var peer = (RelayBlockEntity) helper.getLevel().getBlockEntity(peerPos);
            relay.sidedNode(Direction.EAST).connect(peer.sidedNode(Direction.WEST));
            final var probe = new Probe(); relay.sidedNode(Direction.SOUTH).connect(probe.node());
            final var ccPos = relay.getBlockPos().above();
            final Runnable cleanup;
            try {
                cleanup = RedstoneCardGameTests.bootComputerCraft(helper, ccPos, guarded("""
                    local m = assert(peripheral.wrap('bottom'), 'relay not discovered')
                    assert(peripheral.getType('bottom') == 'modem')
                    assert(not m.isWireless() and not m.isAccessPoint() and not m.isTunnel())
                    assert(m.maxPacketSize() == %d)
                    for channel = 0,127 do assert(m.open(channel)) end
                    assert(m.isOpen(0) and m.isOpen(127))
                    local ok,err = pcall(m.open,128)
                    assert(not ok and err:find('too many open channels',1,true))
                    assert(m.close(0) and not m.close(0) and not m.isOpen(0))
                    assert(m.open(65535))
                    m.closeAll()
                    assert(not m.isOpen(127) and not m.isOpen(65535))
                    assert(not pcall(m.open,-1) and not pcall(m.open,65536) and not pcall(m.open,'bad'))
                    local names = m.getNamesRemote()
                    local address,probe
                    for _,name in ipairs(names) do
                      if m.getTypeRemote(name) == 'relay' then address = name end
                      if m.getTypeRemote(name) == 'cc_relay_probe' then probe = name end
                    end
                    assert(address and m.isPresentRemote(address))
                    local methods = m.getMethodsRemote(address)
                    local found = false
                    for _,name in ipairs(methods) do if name == 'setRepeater' then found = true end end
                    assert(found and m.callRemote(address,'setRepeater',false) == false)
                    assert(m.callRemote(address,'isRepeater') == false)
                    local input = {['clé']='Euro €',binary=string.char(0,128,255)}
                    input.self = input
                    local text,nested,binary = m.callRemote(assert(probe),'echo','Euro €',input)
                    assert(text == 'Euro €' and nested.value == 'Euro €' and nested['clé'] == 'Euro €'
                      and nested.self == nested and binary == string.char(0,128,255),'remote encoding mismatch')
                    assert(not m.isPresentRemote('missing') and m.getTypeRemote('missing') == nil)
                    assert(m.getMethodsRemote('missing') == nil and m.callRemote('missing','anything') == nil)
                    redstone.setBundledOutput('top',65535)
                    """.formatted(ModSettings.maxNetworkPacketSize())), true);
            } catch (Exception exception) { throw new RuntimeException(exception); }
            helper.succeedWhen(() -> {
                helper.assertTrue(dan200.computercraft.api.ComputerCraftAPI.getBundledRedstoneOutput(helper.getLevel(), ccPos, Direction.UP) == 65535,
                    "Actual CC Lua program did not finish relay modem/channel/remote contract: " + error(helper, ccPos));
                helper.assertTrue(!peer.isRepeaterEnabled(), "Actual CC callRemote did not mutate OC component");
                cleanup.run(); probe.node().remove();
            });
        }

        static void actualMessages(GameTestHelper helper) {
            final var relay = relay(helper);
            final var probe = new Probe();
            relay.sidedNode(Direction.EAST).connect(probe.node());
            final var ccPos = relay.getBlockPos().above();
            final Runnable cleanup;
            try {
                cleanup = RedstoneCardGameTests.bootComputerCraft(helper, ccPos, guarded("""
                    local m = assert(peripheral.wrap('bottom'))
                    assert(m.open(41) and m.transmit(42,43,'cc payload','Euro €',string.char(0,128,255)))
                    redstone.setBundledOutput('top',1)
                    local e,side,channel,reply,text,value,binary = os.pullEvent('modem_message')
                    assert(side == 'bottom' and channel == 41 and reply == 44 and text == 'Euro €' and value == true and binary == string.char(0,128,255),
                      textutils.serialize({side,channel,reply,text,value,binary}))
                    redstone.setBundledOutput('top',65535)
                    """), true);
            } catch (Exception exception) { throw new RuntimeException(exception); }
            final var sent = new java.util.concurrent.atomic.AtomicBoolean();
            helper.succeedWhen(() -> {
                helper.assertTrue(!probe.packets.isEmpty(), "Actual CC transmit did not reach OC network");
                final var packet = probe.packets.getFirst();
                helper.assertTrue(packet.port() == 42 && packet.destination() == null && packet.source().startsWith("cc") && packet.source().endsWith("_bottom")
                    && Arrays.deepEquals(packet.data(), new Object[]{43D, "cc payload".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        "Euro €".getBytes(java.nio.charset.StandardCharsets.UTF_8), new byte[]{0, (byte) 128, (byte) 255}}), "CC transmit lost source/channel/reply/UTF-8/binary payload convention");
                final int output = dan200.computercraft.api.ComputerCraftAPI.getBundledRedstoneOutput(helper.getLevel(), ccPos, Direction.UP);
                if (output == 1 && !sent.getAndSet(true)) probe.node().sendToReachable("network.message", Network.newPacket("oc-source", null, 41,
                    new Object[]{44D, "Euro €".getBytes(java.nio.charset.StandardCharsets.UTF_8), true, new byte[]{0, (byte) 128, (byte) 255}}));
                helper.assertTrue(output == 65535,
                    "Actual CC Lua did not receive OC modem_message with decoded UTF-8 payload: " + error(helper, ccPos));
                cleanup.run(); probe.node().remove();
            });
        }

        static String guarded(String script) {
            return "local ok,err=pcall(function()\n" + script + "\nend)\nif not ok then local f=fs.open('oc-relay-error','w'); f.write(tostring(err)); f.close() end\n";
        }

        static String error(GameTestHelper helper, BlockPos pos) {
            try {
                final var cc = helper.getLevel().getBlockEntity(pos);
                final var computer = cc.getClass().getMethod("createServerComputer").invoke(cc);
                final var mount = (dan200.computercraft.api.filesystem.WritableMount) computer.getClass().getMethod("createRootMount").invoke(computer);
                if (!mount.exists("oc-relay-error")) return "waiting for program";
                try (var channel = mount.openFile("oc-relay-error", java.util.Set.of(java.nio.file.StandardOpenOption.READ))) {
                    final var buffer = java.nio.ByteBuffer.allocate(4096); channel.read(buffer); buffer.flip();
                    return java.nio.charset.StandardCharsets.UTF_8.decode(buffer).toString();
                }
            } catch (Exception exception) { return exception.toString(); }
        }

        static void attachments(GameTestHelper helper) {
            final var relay = relay(helper);
            final var peripheral = peripheral(helper, relay, Direction.UP);
            helper.assertTrue(peripheral == peripheral(helper, relay, Direction.DOWN), "Relay exposes separate attachment state per face");
            final var first = new Access(1, "left"); final var second = new Access(2, "right");
            peripheral.attach(first); peripheral.attach(second);
            call(peripheral, first, "open", 7D);
            helper.assertTrue(Boolean.FALSE.equals(call(peripheral, second, "isOpen", 7D)[0]), "Open channels leak between computers");
            call(peripheral, second, "open", 7D);
            final var probe = new Probe(); relay.sidedNode(Direction.NORTH).connect(probe.node());
            probe.node().sendToReachable("network.message", Network.newPacket("oc", "cc1_left", 7, new Object[]{9D, "direct"}));
            helper.assertTrue(first.events.size() == 1 && second.events.isEmpty(), "Directed packet was delivered to wrong attachment");
            call(peripheral, first, "transmit", 7D, 8D, "self-test");
            helper.assertTrue(first.events.size() == 1 && second.events.size() == 1, "Transmitter received itself or sibling missed modem_message");
            helper.assertTrue(Arrays.deepEquals(second.events.getFirst(), new Object[]{"right", 7, 8, "self-test".getBytes(java.nio.charset.StandardCharsets.UTF_8)}), "Wrong modem event layout");
            peripheral.detach(first); peripheral.attach(first);
            helper.assertTrue(Boolean.FALSE.equals(call(peripheral, first, "isOpen", 7D)[0]), "Detached/re-attached channels survived");
            rejects(helper, () -> call(peripheral, first, "transmit", -1D, 8D), "[1, 65535]");
            rejects(helper, () -> call(peripheral, first, "transmit", 7D, 65536D), "[1, 65535]");
            rejects(helper, () -> call(peripheral, first, "transmit", 7D, 8D, Map.of("unsupported", true)), "unsupported data type");
            relay.removeNodes();
            rejects(helper, () -> call(peripheral, second, "open", 7D), "not attached");
            probe.node().remove();
            helper.succeed();
        }

        static void queueAndVisibility(GameTestHelper helper) {
            final var relay = relay(helper);
            final var peripheral = peripheral(helper, relay, Direction.UP);
            final var access = new Access(3, "back"); peripheral.attach(access);
            final var visible = new Probe(); final var hidden = new Probe();
            final var east = relay.sidedNode(Direction.EAST);
            east.connect(visible.node()); east.connect(hidden.node());
            ((Component) hidden.node()).setVisibility(Visibility.None);
            helper.assertTrue(Boolean.TRUE.equals(call(peripheral, access, "isPresentRemote", visible.node().address())[0])
                && Boolean.FALSE.equals(call(peripheral, access, "isPresentRemote", hidden.node().address())[0]), "Remote lookup ignored component visibility");
            final var result = call(peripheral, access, "callRemote", visible.node().address(), "context", "value");
            helper.assertTrue(Arrays.deepEquals(result, new Object[]{east.address().getBytes(java.nio.charset.StandardCharsets.UTF_8), true, false, "value".getBytes(java.nio.charset.StandardCharsets.UTF_8)}) && access.events.size() == 1,
                "Remote OC invocation lost network-specific context or event forwarding");
            for (int index = 0; index < relay.maxQueueSize(); index++) helper.assertTrue(Boolean.TRUE.equals(call(peripheral, access, "transmit", 10D, 11D, "data")[0]), "Queue filled too early");
            helper.assertTrue(Boolean.FALSE.equals(call(peripheral, access, "transmit", 10D, 11D, "overflow")[0]) && relay.queuedPackets() == relay.maxQueueSize(), "CC bypassed relay queue capacity");
            rejects(helper, () -> call(peripheral, access, "transmit", 1D, 2D, "x".repeat(ModSettings.maxNetworkPacketSize() + 1)), "packet too big");
            relay.setItem(RelayBlockEntity.CARD_SLOT, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER1.get()));
            helper.assertTrue(Boolean.TRUE.equals(call(peripheral, access, "isAccessPoint")[0]) && Boolean.FALSE.equals(call(peripheral, access, "isWireless")[0]), "Access-point flag changed wired remote-access compatibility");
            relay.setItem(RelayBlockEntity.CARD_SLOT, new ItemStack(ModItems.LINKED_CARD.get()));
            helper.assertTrue(Boolean.TRUE.equals(call(peripheral, access, "isTunnel")[0]), "Tunnel flag missing");
            visible.node().remove(); hidden.node().remove(); peripheral.detach(access);
            helper.succeed();
        }

        static void nativeRoundtrip(GameTestHelper helper) {
            final var relay = relay(helper);
            final var pos = relay.getBlockPos().east();
            helper.getLevel().setBlockAndUpdate(pos, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
            final var computer = (li.cil.oc.common.blockentity.ComputerCaseBlockEntity) helper.getLevel().getBlockEntity(pos);
            final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
            ((li.cil.oc.api.driver.item.MutableProcessor) li.cil.oc.api.Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLua53Architecture.class);
            computer.setItem(0, new ItemStack(ModItems.NETWORK_CARD.get()));
            computer.setItem(2, new ItemStack(ModItems.MEMORY_TIER1.get()));
            computer.setItem(4, cpu);
            computer.setItem(6, RobotMovementPersistenceGameTests.eeprom("""
                local modem = component.proxy(component.list('modem')())
                modem.open(82)
                modem.broadcast(81,83,'oc ping')
                repeat
                  local name,source,port,distance,reply,payload = computer.pullSignal()
                  if name == 'modem_message' and port == 82 then
                    assert(reply == 84 and payload == 'cc pong', 'CC reply/payload mismatch')
                    break
                  end
                until false
                component.proxy(component.list('eeprom')()).setData('cc-native-verified')
                while true do computer.pullSignal() end
                """));
            computer.onLoad(); Network.joinOrCreateNetwork(computer);
            ((li.cil.oc.api.network.Connector) computer.node()).changeBuffer(10000);
            final var ccPos = relay.getBlockPos().above();
            final Runnable cleanup;
            try {
                cleanup = RedstoneCardGameTests.bootComputerCraft(helper, ccPos, guarded("""
                    local m = assert(peripheral.wrap('bottom'))
                    assert(m.open(81))
                    redstone.setBundledOutput('top',1)
                    local name,side,port,reply,payload = os.pullEvent('modem_message')
                    assert(side == 'bottom' and port == 81 and reply == 83 and payload == 'oc ping')
                    assert(m.transmit(82,84,'cc pong'))
                    redstone.setBundledOutput('top',65535)
                    """), true);
            } catch (Exception exception) { throw new RuntimeException(exception); }
            helper.runAtTickTime(590, () -> { cleanup.run(); computer.machine().stop(); });
            final var started = new java.util.concurrent.atomic.AtomicBoolean();
            helper.succeedWhen(() -> {
                final int output = dan200.computercraft.api.ComputerCraftAPI.getBundledRedstoneOutput(helper.getLevel(), ccPos, Direction.UP);
                if (output == 1 && !started.getAndSet(true)) helper.assertTrue(computer.machine().start(), "Native computer failed to start");
                final var bytes = li.cil.oc.common.item.ItemDriverData.dataTag(computer.getItem(6)).getByteArray(li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG);
                helper.assertTrue(output == 65535 && new String(bytes, java.nio.charset.StandardCharsets.UTF_8).equals("cc-native-verified"),
                    "Real CC/native53 exchange did not finish: native=" + computer.machine().lastError() + ", cc=" + error(helper, ccPos));
                computer.machine().stop(); cleanup.run();
            });
        }

        static final class Access implements dan200.computercraft.api.peripheral.IComputerAccess {
            final int id; final String name; final List<Object[]> events = new ArrayList<>();
            Access(int id, String name) { this.id = id; this.name = name; }
            public String mount(String path, dan200.computercraft.api.filesystem.Mount mount, String driveName) { throw new UnsupportedOperationException(); }
            public String mountWritable(String path, dan200.computercraft.api.filesystem.WritableMount mount, String driveName) { throw new UnsupportedOperationException(); }
            public void unmount(String path) { }
            public int getID() { return id; }
            public String getAttachmentName() { return name; }
            public void queueEvent(String event, Object... args) { events.add(args); }
            public Map<String, dan200.computercraft.api.peripheral.IPeripheral> getAvailablePeripherals() { return Map.of(); }
            public dan200.computercraft.api.peripheral.IPeripheral getAvailablePeripheral(String name) { return null; }
            public dan200.computercraft.api.peripheral.WorkMonitor getMainThreadMonitor() { return null; }
        }
    }

    public static final class Probe implements Environment {
        private final Node node = Network.newNode(this, Visibility.Network).withComponent("cc_relay_probe", Visibility.Network).create();
        final List<Packet> packets = new ArrayList<>();
        public Node node() { return node; }
        public void onConnect(Node node) { }
        public void onDisconnect(Node node) { }
        public void onMessage(Message message) { if (message.name().equals("network.message") && message.data()[0] instanceof Packet packet) packets.add(packet); }
        @Callback
        public Object[] context(Context context, Arguments args) {
            context.signal("oc_probe", "event");
            return new Object[]{context.node().address(), context.isRunning(), context.isPaused(), args.checkString(0)};
        }
        @Callback
        public Object[] echo(Context context, Arguments args) {
            final var value = args.checkString(0);
            if (!value.equals("Euro €")) throw new IllegalArgumentException("OC callback did not receive UTF-8 text");
            final var input = args.checkTable(1);
            if (!(input.get("clé") instanceof byte[] bytes) || !Arrays.equals(bytes, value.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                || input.get("self") != input || !(input.get("binary") instanceof byte[] binary) || !Arrays.equals(binary, new byte[]{0, (byte) 128, (byte) 255})) {
                throw new IllegalArgumentException("OC callback received invalid table keys, bytes or cycles");
            }
            final Map<Object, Object> nested = new java.util.LinkedHashMap<>();
            nested.put("value", value); nested.put("clé", value); nested.put("self", nested);
            return new Object[]{value, nested, new byte[]{0, (byte) 128, (byte) 255}};
        }
    }
}
