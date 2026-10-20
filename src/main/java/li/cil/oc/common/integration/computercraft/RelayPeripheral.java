package li.cil.oc.common.integration.computercraft;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.NotAttachedException;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.blockentity.RelayBlockEntity;
import li.cil.oc.common.integration.ComputerCraftRelayIntegration;
import net.minecraft.core.Direction;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/** Original OC relay/modem contract, scheduled on CC's main-thread task API. */
public final class RelayPeripheral implements IDynamicPeripheral, ComputerCraftRelayIntegration.Receiver {
    private static final String[] METHODS = {"callRemote", "close", "closeAll", "getMethodsRemote", "getNamesRemote", "getTypeRemote",
        "isAccessPoint", "isOpen", "isPresentRemote", "isTunnel", "isWireless", "maxPacketSize", "open", "transmit"};
    private final RelayBlockEntity relay;
    private final Map<IComputerAccess, Set<Integer>> channels = new ConcurrentHashMap<>();

    public RelayPeripheral(final RelayBlockEntity relay) { this.relay = relay; }
    @Override public String getType() { return "modem"; }
    @Override public Object getTarget() { return relay; }
    @Override public String[] getMethodNames() { return METHODS.clone(); }
    @Override public boolean equals(final IPeripheral other) { return other instanceof RelayPeripheral peripheral && peripheral.relay == relay; }
    @Override public void attach(final IComputerAccess computer) { channels.putIfAbsent(computer, ConcurrentHashMap.newKeySet()); }
    @Override public void detach(final IComputerAccess computer) { channels.remove(computer); }
    @Override public void clear() { channels.clear(); }

    @Override
    public MethodResult callMethod(final IComputerAccess computer, final ILuaContext context, final int method, final IArguments arguments) throws LuaException {
        if (method < 0 || method >= METHODS.length) throw new LuaException("no such method");
        final Object[] values = arguments.getAll();
        if (METHODS[method].equals("transmit") || METHODS[method].equals("callRemote")) {
            final var memo = new IdentityHashMap<Object, Object>();
            for (int index = 2; index < values.length; index++) {
                if (values[index] instanceof String) {
                    final var bytes = arguments.getBytes(index);
                    final byte[] copy = new byte[bytes.remaining()]; bytes.get(copy);
                    values[index] = copy;
                } else values[index] = transform(values[index], true, false, memo);
            }
        }
        return context.executeMainThreadTask(() -> {
            if (relay.isRemoved() || !channels.containsKey(computer)) throw new LuaException("peripheral is not attached");
            try { return (Object[]) transform(invoke(METHODS[method], computer, values), false, false, new IdentityHashMap<>()); }
            catch (LuaException exception) { throw exception; }
            catch (Exception exception) { throw new LuaException(exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()); }
        });
    }

    private Object[] invoke(final String method, final IComputerAccess computer, final Object[] args) throws Exception {
        final Set<Integer> ports = channels.get(computer);
        if (ports == null) throw new LuaException("peripheral is not attached");
        return switch (method) {
            case "open" -> {
                final int port = port(args, 0);
                if (ports.size() >= 128) throw new LuaException("too many open channels");
                yield new Object[]{ports.add(port)};
            }
            case "isOpen" -> new Object[]{ports.contains(port(args, 0))};
            case "close" -> new Object[]{ports.remove(port(args, 0))};
            case "closeAll" -> { ports.clear(); yield new Object[0]; }
            case "transmit" -> {
                final int port = port(args, 0), reply = port(args, 1);
                final Object[] data = new Object[Math.max(1, args.length - 1)];
                // Match the receiver's leading-number convention; upstream incorrectly appended an Integer.
                data[0] = (double) reply;
                if (args.length > 2) System.arraycopy(args, 2, data, 1, args.length - 2);
                yield new Object[]{relay.tryEnqueuePacket(null, Network.newPacket(address(computer), null, port, data))};
            }
            case "isWireless" -> new Object[]{false};
            case "isAccessPoint" -> new Object[]{relay.isWirelessEnabled()};
            case "isTunnel" -> new Object[]{relay.isLinkedEnabled()};
            case "maxPacketSize" -> new Object[]{ModSettings.maxNetworkPacketSize()};
            case "getNamesRemote" -> new Object[]{list(visible().keySet())};
            case "getTypeRemote", "getMethodsRemote", "isPresentRemote", "callRemote" -> {
                final Visible found = visible().get(string(args, 0));
                if (method.equals("isPresentRemote")) yield new Object[]{found != null};
                if (found == null) yield new Object[0];
                yield switch (method) {
                    case "getTypeRemote" -> new Object[]{found.component.name()};
                    case "getMethodsRemote" -> new Object[]{list(found.component.methods())};
                    default -> li.cil.oc.common.OpenComputersApi.convert(found.component.invoke(string(args, 1), new CallingContext(computer, found.source), Arrays.copyOfRange(args, 2, args.length)));
                };
            }
            default -> throw new LuaException("no such method");
        };
    }

    private Map<String, Visible> visible() {
        final Map<String, Visible> result = new TreeMap<>();
        for (final Direction side : Direction.values()) {
            final Node source = relay.sidedNode(side);
            for (final Node node : source.reachableNodes()) {
                if (node instanceof Component component && component.canBeSeenFrom(source) && component.address() != null) {
                    result.putIfAbsent(component.address(), new Visible(component, source));
                }
            }
        }
        return result;
    }

    private static Map<Integer, String> list(final Collection<String> values) {
        final Map<Integer, String> result = new LinkedHashMap<>();
        for (final String value : values) result.put(result.size() + 1, value);
        return result;
    }

    @Override
    public void receive(final Packet packet) {
        final Object[] data = packet.data();
        final boolean hasReply = data.length > 0 && data[0] instanceof Number;
        final int reply = hasReply ? ((Number) data[0]).intValue() : -1;
        for (final var entry : channels.entrySet()) {
            final IComputerAccess computer = entry.getKey();
            if (!entry.getValue().contains(packet.port())) continue;
            try {
                final String address = address(computer);
                if (address.equals(packet.source()) || packet.destination() != null && !address.equals(packet.destination())) continue;
                final int start = hasReply ? 1 : 0;
                final Object[] event = new Object[3 + data.length - start];
                event[0] = computer.getAttachmentName(); event[1] = packet.port(); event[2] = reply;
                for (int index = start; index < data.length; index++) {
                    // CC Java strings use an eight-bit encoding; preserve Lua packet bytes instead.
                    event[3 + index - start] = data[index] instanceof byte[] bytes ? bytes.clone()
                        : data[index] instanceof String text ? text.getBytes(StandardCharsets.UTF_8) : data[index];
                }
                computer.queueEvent("modem_message", event);
            } catch (NotAttachedException detached) { channels.remove(computer); }
        }
    }

    private static String address(final IComputerAccess computer) { return "cc" + computer.getID() + "_" + computer.getAttachmentName(); }

    private static int port(final Object[] args, final int index) throws LuaException {
        if (index >= args.length || !(args[index] instanceof Number number)) throw new LuaException("bad argument #" + (index + 1) + " (number expected)");
        final double value = number.doubleValue();
        final int port = (int) value;
        if (!Double.isFinite(value) || port < 0 || port > 65535) throw new LuaException("bad argument #" + (index + 1) + " (number in [1, 65535] expected)");
        return port;
    }

    private static String string(final Object[] args, final int index) throws LuaException {
        if (index >= args.length || !(args[index] instanceof String value)) throw new LuaException("bad argument #" + (index + 1) + " (string expected)");
        return value;
    }

    private record Visible(Component component, Node source) { }

    // Native OC arguments use byte strings and UTF-8 table keys. CC's Java strings represent eight-bit Lua bytes.
    private static Object transform(final Object value, final boolean fromComputerCraft, final boolean key, final IdentityHashMap<Object, Object> memo) {
        if (value instanceof String text) {
            final byte[] bytes = text.getBytes(fromComputerCraft ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_8);
            return fromComputerCraft && key ? new String(bytes, StandardCharsets.UTF_8) : bytes;
        }
        if (value instanceof byte[] bytes) return bytes.clone();
        if (memo.containsKey(value)) return memo.get(value);
        if (value instanceof Map<?, ?> map) {
            final Map<Object, Object> result = new LinkedHashMap<>(); memo.put(value, result);
            for (final var entry : map.entrySet()) result.put(transform(entry.getKey(), fromComputerCraft, true, memo), transform(entry.getValue(), fromComputerCraft, false, memo));
            return result;
        }
        if (value instanceof Object[] array) {
            final Object[] result = new Object[array.length]; memo.put(value, result);
            for (int index = 0; index < array.length; index++) result[index] = transform(array[index], fromComputerCraft, false, memo);
            return result;
        }
        return value;
    }

    private record CallingContext(IComputerAccess computer, Node node) implements Context {
        public boolean canInteract(String player) { return true; }
        public boolean isRunning() { return true; }
        public boolean isPaused() { return false; }
        public boolean start() { return false; }
        public boolean pause(double seconds) { return false; }
        public boolean stop() { return false; }
        public void consumeCallBudget(double cost) { }
        public boolean signal(String name, Object... args) {
            computer.queueEvent(name, (Object[]) transform(args, false, false, new IdentityHashMap<>())); return true;
        }
    }
}
