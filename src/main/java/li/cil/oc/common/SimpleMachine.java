package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class SimpleMachine extends AbstractManagedEnvironment implements Machine {
    private static final String RUNNING_TAG = "running";
    private static final String LAST_ERROR_TAG = "lastError";
    private static final String ARCHITECTURE_TAG = "architecture";

    private final MachineHost host;
    private final ArrayDeque<Signal> signals = new ArrayDeque<>();
    private final Set<String> users = new LinkedHashSet<>();
    private final Set<ManagedEnvironment> componentEnvironments = new LinkedHashSet<>();
    private Architecture architecture;
    private boolean running;
    private boolean paused;
    private String lastError;
    private double costPerTick;

    SimpleMachine(final MachineHost host) {
        this.host = host;
        if (API.network == null) {
            API.network = new NetworkRegistry();
        }
        setNode(Network.newNode(this, Visibility.Network)
            .withComponent("computer", Visibility.Network)
            .create());
    }

    @Override
    public MachineHost host() {
        return host;
    }

    @Override
    public void onHostChanged() {
        for (ManagedEnvironment environment : componentEnvironments) {
            if (environment.node() != null) {
                host.onMachineDisconnect(environment.node());
                environment.node().remove();
            }
        }
        componentEnvironments.clear();
        if (architecture != null) {
            architecture.close();
            architecture = null;
        }
        running = false;
        paused = false;

        if (host == null || node() == null) {
            return;
        }

        if (node().network() == null) {
            Network.joinNewNetwork(node());
        }

        for (ItemStack stack : host.internalComponents()) {
            final DriverItem driver = Driver.driverFor(stack, host.getClass());
            if (driver == null) {
                continue;
            }

            if (driver instanceof Processor processor && architecture == null) {
                architecture = instantiate(processor.architecture(stack));
                if (architecture != null) {
                    architecture.recomputeMemory(host.internalComponents());
                }
            }

            final ManagedEnvironment environment = driver.createEnvironment(stack, host);
            if (environment == null || environment.node() == null) {
                continue;
            }

            node().connect(environment.node());
            componentEnvironments.add(environment);
            host.onMachineConnect(environment.node());
        }
    }

    @Override
    public Architecture architecture() {
        return architecture;
    }

    @Override
    public Map<String, String> components() {
        if (node() == null || node().network() == null) {
            return Map.of();
        }
        final Map<String, String> components = new LinkedHashMap<>();
        for (li.cil.oc.api.network.Node reachable : node().reachableNodes()) {
            if (reachable instanceof Component component && component.canBeSeenFrom(node())) {
                components.put(component.address(), component.name());
            }
        }
        return components;
    }

    @Override
    public int componentCount() {
        return components().size();
    }

    @Override
    public int maxComponents() {
        return 64;
    }

    @Override
    public double getCostPerTick() {
        return costPerTick;
    }

    @Override
    public void setCostPerTick(final double value) {
        costPerTick = Math.max(0, value);
    }

    @Override
    public String tmpAddress() {
        return node() == null ? null : node().address();
    }

    @Override
    public String lastError() {
        return lastError;
    }

    @Override
    public long worldTime() {
        return 0;
    }

    @Override
    public double upTime() {
        return 0;
    }

    @Override
    public double cpuTime() {
        return 0;
    }

    @Override
    public void beep(final short frequency, final short duration) {
    }

    @Override
    public void beep(final String pattern) {
    }

    @Override
    public boolean crash(final String message) {
        lastError = message;
        running = false;
        paused = false;
        if (architecture != null) {
            architecture.close();
        }
        return true;
    }

    @Override
    public Signal popSignal() {
        return signals.pollFirst();
    }

    @Override
    public Map<String, Callback> methods(final Object value) {
        return Map.of();
    }

    @Override
    public Object[] invoke(final String address, final String method, final Object[] args) throws Exception {
        if (node() == null || node().network() == null) {
            throw new IllegalStateException("machine is not in a network");
        }
        final li.cil.oc.api.network.Node target = node().network().node(address);
        if (target instanceof Component component) {
            return component.invoke(method, this, args == null ? new Object[0] : args);
        }
        throw new NoSuchMethodException(method);
    }

    @Override
    public Object[] invoke(final Value value, final String method, final Object[] args) throws Exception {
        return value.call(this, new MachineArguments(args == null ? new Object[0] : args));
    }

    @Override
    public String[] users() {
        return users.toArray(String[]::new);
    }

    @Override
    public void addUser(final String name) {
        users.add(name);
    }

    @Override
    public boolean removeUser(final String name) {
        return users.remove(name);
    }

    @Override
    public boolean canInteract(final String player) {
        return users.isEmpty() || users.contains(player);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        if (!running || paused || architecture == null) {
            return;
        }
        try {
            architecture.runSynchronized();
            final ExecutionResult result = architecture.runThreaded(false);
            if (result instanceof ExecutionResult.Shutdown shutdown) {
                stop();
                if (shutdown.reboot) {
                    start();
                }
            } else if (result instanceof ExecutionResult.Error error) {
                crash(error.message);
            }
        } catch (RuntimeException e) {
            crash(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    @Override
    public boolean start() {
        if (architecture != null && !architecture.isInitialized() && !architecture.initialize()) {
            return false;
        }
        running = true;
        paused = false;
        return true;
    }

    @Override
    public boolean pause(final double seconds) {
        if (!running) {
            return false;
        }
        paused = true;
        return true;
    }

    @Override
    public boolean stop() {
        final boolean wasRunning = running || paused;
        running = false;
        paused = false;
        if (architecture != null) {
            architecture.close();
        }
        return wasRunning;
    }

    @Override
    public void consumeCallBudget(final double callCost) {
    }

    @Override
    public boolean signal(final String name, final Object... args) {
        signals.addLast(new SimpleSignal(name, args == null ? new Object[0] : Arrays.copyOf(args, args.length)));
        if (architecture != null) {
            architecture.onSignal();
        }
        return true;
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        running = nbt.getBoolean(RUNNING_TAG);
        lastError = nbt.contains(LAST_ERROR_TAG) ? nbt.getString(LAST_ERROR_TAG) : null;
        if (architecture != null && nbt.contains(ARCHITECTURE_TAG)) {
            architecture.load(nbt.getCompound(ARCHITECTURE_TAG));
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        nbt.putBoolean(RUNNING_TAG, running);
        if (lastError != null) {
            nbt.putString(LAST_ERROR_TAG, lastError);
        }
        if (architecture != null) {
            final CompoundTag architectureTag = new CompoundTag();
            architecture.save(architectureTag);
            nbt.put(ARCHITECTURE_TAG, architectureTag);
        }
    }

    private record SimpleSignal(String name, Object[] args) implements Signal {
    }

    private static Architecture instantiate(final Class<? extends Architecture> type) {
        if (type == null) {
            return null;
        }
        try {
            final var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("failed to instantiate architecture " + type.getName(), e);
        }
    }

    private record MachineArguments(Object[] values) implements Arguments {
        @Override public int count() { return values.length; }
        @Override public Object checkAny(final int index) {
            if (index < 0 || index >= values.length) throw new IllegalArgumentException("missing argument #" + (index + 1));
            return values[index];
        }
        @Override public boolean checkBoolean(final int index) { return (Boolean) checkAny(index); }
        @Override public int checkInteger(final int index) { return ((Number) checkAny(index)).intValue(); }
        @Override public long checkLong(final int index) { return ((Number) checkAny(index)).longValue(); }
        @Override public double checkDouble(final int index) { return ((Number) checkAny(index)).doubleValue(); }
        @Override public String checkString(final int index) { return (String) checkAny(index); }
        @Override public byte[] checkByteArray(final int index) {
            final Object value = checkAny(index);
            if (value instanceof byte[] bytes) return bytes;
            if (value instanceof String string) return string.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (byte array expected)");
        }
        @Override public Map checkTable(final int index) { return (Map) checkAny(index); }
        @Override public ItemStack checkItemStack(final int index) { return (ItemStack) checkAny(index); }
        @Override public Object optAny(final int index, final Object def) { return index >= 0 && index < values.length ? values[index] : def; }
        @Override public boolean optBoolean(final int index, final boolean def) { return index >= 0 && index < values.length ? checkBoolean(index) : def; }
        @Override public int optInteger(final int index, final int def) { return index >= 0 && index < values.length ? checkInteger(index) : def; }
        @Override public long optLong(final int index, final long def) { return index >= 0 && index < values.length ? checkLong(index) : def; }
        @Override public double optDouble(final int index, final double def) { return index >= 0 && index < values.length ? checkDouble(index) : def; }
        @Override public String optString(final int index, final String def) { return index >= 0 && index < values.length ? checkString(index) : def; }
        @Override public byte[] optByteArray(final int index, final byte[] def) { return index >= 0 && index < values.length ? checkByteArray(index) : def; }
        @Override public Map optTable(final int index, final Map def) { return index >= 0 && index < values.length ? checkTable(index) : def; }
        @Override public ItemStack optItemStack(final int index, final ItemStack def) { return index >= 0 && index < values.length ? checkItemStack(index) : def; }
        @Override public boolean isBoolean(final int index) { return index >= 0 && index < values.length && values[index] instanceof Boolean; }
        @Override public boolean isInteger(final int index) { return index >= 0 && index < values.length && values[index] instanceof Integer; }
        @Override public boolean isLong(final int index) { return index >= 0 && index < values.length && values[index] instanceof Long; }
        @Override public boolean isDouble(final int index) { return index >= 0 && index < values.length && values[index] instanceof Double; }
        @Override public boolean isString(final int index) { return index >= 0 && index < values.length && values[index] instanceof String; }
        @Override public boolean isByteArray(final int index) { return index >= 0 && index < values.length && values[index] instanceof byte[]; }
        @Override public boolean isTable(final int index) { return index >= 0 && index < values.length && values[index] instanceof Map; }
        @Override public boolean isItemStack(final int index) { return index >= 0 && index < values.length && values[index] instanceof ItemStack; }
        @Override public Object[] toArray() { return Arrays.copyOf(values, values.length); }
        @Override public java.util.Iterator<Object> iterator() { return Arrays.asList(values).iterator(); }
    }
}
