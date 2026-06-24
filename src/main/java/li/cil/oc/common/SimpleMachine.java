package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.MethodWhitelist;
import li.cil.oc.api.driver.item.CallBudget;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.FilteredEnvironment;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.machine.MachineBoundArchitecture;
import li.cil.oc.common.machine.ProgramLocations;
import li.cil.oc.common.machine.SynchronizedCallAware;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.concurrent.TimeUnit;

final class SimpleMachine extends AbstractManagedEnvironment implements Machine, DeviceInfo {
    private static final double NANOS_PER_SECOND = 1_000_000_000D;
    private static final long NANOS_PER_TICK = 50_000_000L;
    private static final int MAX_SIGNAL_QUEUE_SIZE = 256;
    private static final int MAX_SYNCHRONIZED_CALLS_PER_UPDATE = 512;
    private static final String RUNNING_TAG = "running";
    private static final String LAST_ERROR_TAG = "lastError";
    private static final String USERS_TAG = "users";
    private static final String SIGNALS_TAG = "signals";
    private static final String SIGNAL_NAME_TAG = "name";
    private static final String SIGNAL_ARGS_TAG = "args";
    private static final String SIGNAL_ARG_TYPE_TAG = "type";
    private static final String SIGNAL_ARG_VALUE_TAG = "value";
    private static final String SIGNAL_ARG_KEY_TAG = "key";
    private static final String ARCHITECTURE_TAG = "architecture";
    private static final String CPU_TIME_NANOS_TAG = "cpuTimeNanos";
    private static final String CHECKED_SIGNAL_MESSAGE = "computer.checked_signal";
    private static final String COMPUTER_SIGNAL_MESSAGE = "computer.signal";
    private static final String COMPUTER_START_MESSAGE = "computer.start";
    private static final String COMPUTER_STOP_MESSAGE = "computer.stop";
    private static final String COMPUTER_STARTED_MESSAGE = "computer.started";
    private static final String COMPUTER_STOPPED_MESSAGE = "computer.stopped";

    private final MachineHost host;
    private final LongSupplier nanoTime;
    private final ArrayDeque<Signal> signals = new ArrayDeque<>();
    private final Set<String> users = new LinkedHashSet<>();
    private final Set<ManagedEnvironment> componentEnvironments = new LinkedHashSet<>();
    private ManagedEnvironment temporaryFileSystemEnvironment;
    private Architecture architecture;
    private boolean running;
    private boolean paused;
    private String lastError;
    private double costPerTick;
    private int maxComponents;
    private double maxCallBudget = 1D;
    private double callBudget;
    private boolean inSynchronizedCall;
    private long startedAtNanos = -1L;
    private long sleepUntilNanos = -1L;
    private long sleepUntilWorldTime = -1L;
    private long pauseUntilNanos = -1L;
    private long pauseUntilWorldTime = -1L;
    private long cpuTimeNanos;
    private short lastBeepFrequency;
    private short lastBeepDuration;
    private String lastBeepPattern;

    SimpleMachine(final MachineHost host) {
        this(host, System::nanoTime);
    }

    SimpleMachine(final MachineHost host, final LongSupplier nanoTime) {
        this.host = host;
        this.nanoTime = nanoTime;
        if (API.network == null) {
            API.network = new NetworkRegistry();
        }
        final double computerBuffer = ModSettings.computerBuffer();
        final Connector connector = Network.newNode(this, Visibility.Network)
            .withComponent("computer", Visibility.Neighbors)
            .withConnector(computerBuffer)
            .create();
        connector.changeBuffer(computerBuffer);
        setNode(connector);
        if (API.fileSystem == null) {
            API.fileSystem = new FileSystemRegistry();
        }
        temporaryFileSystemEnvironment = createTemporaryFileSystemEnvironment();
    }

    @Override
    public MachineHost host() {
        return host;
    }

    @Override
    public void onHostChanged() {
        final boolean wasRunning = running || paused;
        if (wasRunning) {
            sendLifecycleMessage(COMPUTER_STOPPED_MESSAGE);
        }
        for (ManagedEnvironment environment : componentEnvironments) {
            if (environment.node() != null) {
                saveComponentEnvironment(environment);
                host.onMachineDisconnect(environment.node());
                environment.node().remove();
            }
        }
        componentEnvironments.clear();
        maxComponents = 0;
        double callBudgetSum = 0D;
        int callBudgetCount = 0;
        if (architecture != null) {
            architecture.close();
            architecture = null;
        }
        running = false;
        paused = false;
        startedAtNanos = -1L;
        sleepUntilNanos = -1L;
        sleepUntilWorldTime = -1L;
        pauseUntilNanos = -1L;
        pauseUntilWorldTime = -1L;

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

            if (driver instanceof CallBudget budgetDriver) {
                callBudgetSum += Math.max(0D, budgetDriver.getCallBudget(stack));
                callBudgetCount++;
            }

            if (driver instanceof Processor processor) {
                maxComponents += Math.max(0, processor.supportedComponents(stack));
                if (architecture == null) {
                    architecture = instantiate(processor.architecture(stack));
                    if (architecture != null) {
                        bindArchitecture(architecture);
                        if (!architecture.recomputeMemory(host.internalComponents())) {
                            architecture.close();
                            architecture = null;
                        }
                    }
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
        maxCallBudget = callBudgetCount == 0 ? 1D : callBudgetSum / callBudgetCount;
    }

    @Override
    public Architecture architecture() {
        return architecture;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return host instanceof DeviceInfo deviceInfo ? deviceInfo.getDeviceInfo() : null;
    }

    @Callback(doc = "function():boolean -- Starts the computer. Returns true if the state changed.")
    public Object[] start(final Context context, final Arguments arguments) {
        return new Object[]{!isPaused() && start()};
    }

    @Callback(doc = "function():boolean -- Stops the computer. Returns true if the state changed.")
    public Object[] stop(final Context context, final Arguments arguments) {
        return new Object[]{stop()};
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the computer is running.")
    public Object[] isRunning(final Context context, final Arguments arguments) {
        return new Object[]{isRunning()};
    }

    @Callback(direct = true, doc = "function():number -- Returns the amount of energy stored in the computer.")
    public Object[] energy(final Context context, final Arguments arguments) {
        if (ModSettings.ignorePower()) {
            return new Object[]{Double.POSITIVE_INFINITY};
        }
        return new Object[]{node() instanceof Connector connector ? connector.globalBuffer() : 0D};
    }

    @Callback(direct = true, doc = "function():number -- Returns the maximum amount of energy that can be stored in the computer.")
    public Object[] maxEnergy(final Context context, final Arguments arguments) {
        return new Object[]{node() instanceof Connector connector ? connector.globalBufferSize() : 0D};
    }

    @Callback(direct = true, doc = "function():string... -- Returns the list of users allowed to interact with the computer.")
    public Object[] users(final Context context, final Arguments arguments) {
        return new Object[]{users()};
    }

    @Callback(doc = "function(name:string):boolean -- Adds a user to the list of users allowed to interact with the computer.")
    public Object[] addUser(final Context context, final Arguments arguments) throws Exception {
        addUser(arguments.checkString(0));
        return new Object[]{true};
    }

    @Callback(doc = "function(name:string):boolean -- Removes a user from the list of users allowed to interact with the computer.")
    public Object[] removeUser(final Context context, final Arguments arguments) {
        return new Object[]{removeUser(arguments.checkString(0))};
    }

    @Callback(doc = "function([frequency:string or number[, duration:number]]) -- Plays a tone, useful to alert users via audible feedback.")
    public Object[] beep(final Context context, final Arguments arguments) {
        if (arguments != null && arguments.count() == 1 && arguments.isString(0)) {
            beep(arguments.checkString(0));
        } else {
            final int frequency = arguments == null ? 440 : arguments.optInteger(0, 440);
            if (frequency < 20 || frequency > 2000) {
                throw new IllegalArgumentException("invalid frequency, must be in [20, 2000]");
            }
            final double duration = arguments == null ? 0.1D : arguments.optDouble(1, 0.1D);
            final int durationInMilliseconds = Math.max(50, Math.min(5000, (int) (duration * 1000D)));
            if (context != null) {
                context.pause(durationInMilliseconds / 1000D);
            }
            beep((short) frequency, (short) durationInMilliseconds);
        }
        return new Object[0];
    }

    @Callback(doc = "function():table -- Collect information on all connected devices.")
    public Object[] getDeviceInfo(final Context context, final Arguments arguments) {
        if (context != null) {
            context.pause(1);
        }
        if (node() == null || node().network() == null) {
            return new Object[]{Map.of()};
        }

        final Map<String, Map<String, String>> devices = new LinkedHashMap<>();
        for (li.cil.oc.api.network.Node reachable : node().network().nodes()) {
            if (reachable.address() == null || !(reachable.host() instanceof DeviceInfo deviceInfo)) {
                continue;
            }
            if (!canReportDeviceInfo(reachable)) {
                continue;
            }
            final Map<String, String> info = deviceInfo.getDeviceInfo();
            if (info != null) {
                devices.put(reachable.address(), info);
            }
        }
        return new Object[]{devices};
    }

    @Callback(doc = "function():table -- Returns a map of program name to disk label for known programs.")
    public Object[] getProgramLocations(final Context context, final Arguments arguments) {
        return new Object[]{ProgramLocations.mappingsByProgram(currentArchitectureName())};
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
        double count = 0D;
        for (Map.Entry<String, String> component : components().entrySet()) {
            if (node() != null && component.getKey().equals(node().address())) {
                continue;
            }
            count += "filesystem".equals(component.getValue()) ? 0.25D : 1D;
        }
        return (int) count;
    }

    @Override
    public int maxComponents() {
        return maxComponents;
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
        connectTemporaryFileSystem();
        return temporaryFileSystemEnvironment == null || temporaryFileSystemEnvironment.node() == null
            ? null
            : temporaryFileSystemEnvironment.node().address();
    }

    @Override
    public String lastError() {
        return lastError;
    }

    @Override
    public long worldTime() {
        return host != null && host.world() != null ? host.world().getGameTime() : 0;
    }

    @Override
    public double upTime() {
        if ((!running && !paused) || startedAtNanos < 0) {
            return 0;
        }
        return Math.max(0, nanoTime.getAsLong() - startedAtNanos) / NANOS_PER_SECOND;
    }

    @Override
    public double cpuTime() {
        return Math.max(0, cpuTimeNanos) / NANOS_PER_SECOND;
    }

    @Override
    public void beep(final short frequency, final short duration) {
        lastBeepFrequency = frequency;
        lastBeepDuration = duration;
        lastBeepPattern = null;
    }

    @Override
    public void beep(final String pattern) {
        lastBeepPattern = pattern;
    }

    int lastBeepFrequency() {
        return lastBeepFrequency;
    }

    int lastBeepDuration() {
        return lastBeepDuration;
    }

    String lastBeepPattern() {
        return lastBeepPattern;
    }

    @Override
    public void onMessage(final Message message) {
        if (CHECKED_SIGNAL_MESSAGE.equals(message.name())) {
            queueCheckedSignal(message);
        } else if (COMPUTER_SIGNAL_MESSAGE.equals(message.name())) {
            queueNetworkSignal(message);
        } else if (COMPUTER_START_MESSAGE.equals(message.name())) {
            if (!isPaused()) {
                start();
            }
        } else if (COMPUTER_STOP_MESSAGE.equals(message.name())) {
            stop();
        }
    }

    @Override
    public void onConnect(final Node node) {
        queueComponentChangeSignal("component_added", node);
    }

    @Override
    public void onDisconnect(final Node node) {
        queueComponentChangeSignal("component_removed", node);
    }

    @Override
    public boolean crash(final String message) {
        final boolean wasRunning = running || paused;
        lastError = message;
        running = false;
        paused = false;
        if (architecture != null) {
            architecture.close();
        }
        signals.clear();
        startedAtNanos = -1L;
        sleepUntilNanos = -1L;
        sleepUntilWorldTime = -1L;
        pauseUntilNanos = -1L;
        pauseUntilWorldTime = -1L;
        if (wasRunning) {
            sendLifecycleMessage(COMPUTER_STOPPED_MESSAGE);
        }
        return true;
    }

    @Override
    public Signal popSignal() {
        return signals.pollFirst();
    }

    @Override
    public Map<String, Callback> methods(final Object value) {
        if (value instanceof Value) {
            final Map<String, Callback> methods = new LinkedHashMap<>();
            for (Map.Entry<String, Method> entry : discoverCallbacks(value).entrySet()) {
                methods.put(entry.getKey(), entry.getValue().getAnnotation(Callback.class));
            }
            return methods;
        }
        if (!(value instanceof String address) || node() == null || node().network() == null) {
            return Map.of();
        }
        final Node target = node().network().node(address);
        if (!(target instanceof Component component) || target != node() && !component.canBeSeenFrom(node())) {
            return Map.of();
        }
        final Map<String, Callback> methods = new LinkedHashMap<>();
        for (String method : component.methods()) {
            methods.put(method, component.annotation(method));
        }
        return methods;
    }

    private Component component(final String address) {
        if (node() == null || node().network() == null) {
            return null;
        }
        final Node target = node().network().node(address);
        if (target instanceof Component component && (target == node() || component.canBeSeenFrom(node()))) {
            return component;
        }
        return null;
    }

    @Override
    public Object[] invoke(final String address, final String method, final Object[] args) throws Exception {
        final Component component = component(address);
        if (component != null) {
            final Callback callback = component.annotation(method);
            if (callback.direct()) {
                consumeCallBudget(1D / callback.limit());
            }
            return component.invoke(method, this, args == null ? new Object[0] : args);
        }
        if (node() == null || node().network() == null) {
            throw new IllegalStateException("machine is not in a network");
        }
        throw new IllegalArgumentException("no such component");
    }

    @Override
    public Object[] invoke(final Value value, final String method, final Object[] args) throws Exception {
        final Method callback = discoverCallbacks(value).get(method);
        if (callback == null) {
            throw new NoSuchMethodException(method);
        }
        final Callback annotation = callback.getAnnotation(Callback.class);
        if (annotation.direct()) {
            consumeCallBudget(1D / annotation.limit());
        }
        try {
            final Object result = callback.invoke(value, this, new MachineArguments(args == null ? new Object[0] : args));
            if (result == null) {
                return null;
            }
            if (result instanceof Object[] values) {
                return values;
            }
            return new Object[]{result};
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        }
    }

    @Override
    public String[] users() {
        return users.toArray(String[]::new);
    }

    @Override
    public void addUser(final String name) throws Exception {
        if (users.size() >= ModSettings.maxUsers()) {
            throw new Exception("too many users");
        }
        if (users.contains(name)) {
            throw new Exception("user exists");
        }
        if (name != null && name.length() > ModSettings.maxUsernameLength()) {
            throw new Exception("username too long");
        }
        users.add(name);
    }

    @Override
    public boolean removeUser(final String name) {
        return users.remove(name);
    }

    @Override
    public boolean canInteract(final String player) {
        return !ModSettings.canComputersBeOwned() || users.isEmpty() || users.contains(player);
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
        if (!running || architecture == null) {
            return;
        }
        callBudget = maxCallBudget;
        final long updateStartedAt = nanoTime.getAsLong();
        if (paused) {
            if (pauseUntilWorldTime >= 0 && host != null && host.world() != null && host.world().getGameTime() < pauseUntilWorldTime) {
                return;
            }
            if (pauseUntilWorldTime < 0 && pauseUntilNanos > updateStartedAt) {
                return;
            }
            paused = false;
            pauseUntilNanos = -1L;
            pauseUntilWorldTime = -1L;
        }
        final SynchronizedCallAware synchronizedCallAware = architecture instanceof SynchronizedCallAware aware ? aware : null;
        final boolean hasPendingSynchronizedCall = hasPendingSynchronizedCall(synchronizedCallAware);
        if (sleepUntilWorldTime >= 0 && host != null && host.world() != null && host.world().getGameTime() < sleepUntilWorldTime && !hasPendingSynchronizedCall) {
            return;
        }
        if (sleepUntilWorldTime < 0 && sleepUntilNanos > updateStartedAt && !hasPendingSynchronizedCall) {
            return;
        }
        sleepUntilNanos = -1L;
        sleepUntilWorldTime = -1L;
        try {
            ExecutionResult result;
            int synchronizedCalls = 0;
            do {
                runArchitectureSynchronized();
                final boolean isSynchronizedReturn = synchronizedCallAware != null && synchronizedCallAware.hasSynchronizedReturn();
                result = architecture.runThreaded(isSynchronizedReturn);
                if (!(result instanceof ExecutionResult.Sleep) || !hasPendingSynchronizedCall(synchronizedCallAware)) {
                    break;
                }
                synchronizedCalls++;
            } while (synchronizedCalls < MAX_SYNCHRONIZED_CALLS_PER_UPDATE);
            if (result instanceof ExecutionResult.Shutdown shutdown) {
                stop();
                if (shutdown.reboot) {
                    if (ModSettings.eraseTmpOnReboot()) {
                        resetTemporaryFileSystem();
                    }
                    start();
                }
            } else if (result instanceof ExecutionResult.Error error) {
                crash(error.message);
            } else if (result instanceof ExecutionResult.Sleep sleep) {
                setSleepDelay(sleep);
            }
        } catch (RuntimeException e) {
            crash(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        } finally {
            cpuTimeNanos += Math.max(0, nanoTime.getAsLong() - updateStartedAt);
        }
    }

    private boolean hasPendingSynchronizedCall(final SynchronizedCallAware synchronizedCallAware) {
        return synchronizedCallAware != null && synchronizedCallAware.hasPendingSynchronizedCall();
    }

    private void runArchitectureSynchronized() {
        inSynchronizedCall = true;
        try {
            architecture.runSynchronized();
        } finally {
            inSynchronizedCall = false;
        }
    }

    private void setSleepDelay(final ExecutionResult.Sleep sleep) {
        if (sleep.ticks > 0 && host != null && host.world() != null) {
            sleepUntilWorldTime = host.world().getGameTime() + sleep.ticks;
            sleepUntilNanos = -1L;
        } else {
            sleepUntilNanos = sleep.ticks <= 0
                ? nanoTime.getAsLong() + TimeUnit.MILLISECONDS.toNanos(ModSettings.executionDelay())
                : nanoTime.getAsLong() + sleep.ticks * NANOS_PER_TICK;
            sleepUntilWorldTime = -1L;
        }
    }

    @Override
    public boolean start() {
        final boolean wasRunning = running;
        if (architecture == null && host != null) {
            return false;
        }
        if (architecture != null && !architecture.isInitialized() && !architecture.initialize()) {
            return false;
        }
        if (!wasRunning) {
            signals.clear();
        }
        running = true;
        paused = false;
        pauseUntilNanos = -1L;
        pauseUntilWorldTime = -1L;
        sleepUntilNanos = -1L;
        sleepUntilWorldTime = -1L;
        if (!wasRunning) {
            startedAtNanos = nanoTime.getAsLong();
            sendLifecycleMessage(COMPUTER_STARTED_MESSAGE);
        }
        return true;
    }

    @Override
    public boolean pause(final double seconds) {
        if (!running) {
            return false;
        }
        paused = true;
        final long ticks = Math.max(0L, (long) Math.ceil(seconds * 20D));
        if (host != null && host.world() != null) {
            pauseUntilWorldTime = host.world().getGameTime() + ticks;
            pauseUntilNanos = -1L;
        } else {
            pauseUntilNanos = nanoTime.getAsLong() + Math.max(0L, (long) Math.ceil(seconds * NANOS_PER_SECOND));
            pauseUntilWorldTime = -1L;
        }
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
        signals.clear();
        startedAtNanos = -1L;
        sleepUntilNanos = -1L;
        sleepUntilWorldTime = -1L;
        pauseUntilNanos = -1L;
        pauseUntilWorldTime = -1L;
        if (wasRunning) {
            sendLifecycleMessage(COMPUTER_STOPPED_MESSAGE);
        }
        return wasRunning;
    }

    @Override
    public void consumeCallBudget(final double callCost) throws LimitReachedException {
        if (architecture != null && architecture.isInitialized() && !inSynchronizedCall) {
            final double clampedCost = Math.max(0D, callCost);
            if (clampedCost > callBudget) {
                throw new LimitReachedException();
            }
            callBudget -= clampedCost;
        }
    }

    @Override
    public boolean signal(final String name, final Object... args) {
        if (!running || signals.size() >= MAX_SIGNAL_QUEUE_SIZE) {
            return false;
        }
        signals.addLast(new SimpleSignal(name, normalizeSignalArgs(args)));
        sleepUntilNanos = -1L;
        sleepUntilWorldTime = -1L;
        if (running && architecture != null) {
            architecture.onSignal();
        }
        return true;
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        running = nbt.getBoolean(RUNNING_TAG);
        startedAtNanos = running ? nanoTime.getAsLong() : -1L;
        cpuTimeNanos = nbt.getLong(CPU_TIME_NANOS_TAG);
        lastError = nbt.contains(LAST_ERROR_TAG) ? nbt.getString(LAST_ERROR_TAG) : null;
        users.clear();
        final ListTag userTags = nbt.getList(USERS_TAG, StringTag.TAG_STRING);
        for (int i = 0; i < userTags.size(); i++) {
            users.add(userTags.getString(i));
        }
        signals.clear();
        final ListTag signalTags = nbt.getList(SIGNALS_TAG, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < signalTags.size(); i++) {
            final CompoundTag signalTag = signalTags.getCompound(i);
            signals.addLast(loadSignal(signalTag));
        }
        if (architecture != null && nbt.contains(ARCHITECTURE_TAG)) {
            architecture.load(nbt.getCompound(ARCHITECTURE_TAG));
        }
        if (running) {
            pause(ModSettings.startupDelay());
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        for (ManagedEnvironment environment : componentEnvironments) {
            saveComponentEnvironment(environment);
        }
        nbt.putBoolean(RUNNING_TAG, running);
        nbt.putLong(CPU_TIME_NANOS_TAG, cpuTimeNanos);
        if (lastError != null) {
            nbt.putString(LAST_ERROR_TAG, lastError);
        }
        final ListTag userTags = new ListTag();
        for (String user : users) {
            userTags.add(StringTag.valueOf(user));
        }
        nbt.put(USERS_TAG, userTags);
        final ListTag signalTags = new ListTag();
        for (Signal signal : signals) {
            signalTags.add(saveSignal(signal));
        }
        nbt.put(SIGNALS_TAG, signalTags);
        if (architecture != null) {
            final CompoundTag architectureTag = new CompoundTag();
            architecture.save(architectureTag);
            nbt.put(ARCHITECTURE_TAG, architectureTag);
        }
    }

    private record SimpleSignal(String name, Object[] args) implements Signal {
    }

    private static Object[] normalizeSignalArgs(final Object[] args) {
        if (args == null) {
            return new Object[0];
        }
        final Object[] normalized = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            normalized[i] = normalizeSignalArg(args[i]);
        }
        return normalized;
    }

    private static Object normalizeSignalArg(final Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            final Map<Object, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                final Object key = normalizeScalarSignalArg(entry.getKey());
                final Object entryValue = normalizeScalarSignalArg(entry.getValue());
                if (key != null && entryValue != null) {
                    normalized.put(key, entryValue);
                }
            }
            return normalized;
        }
        return normalizeScalarSignalArg(value);
    }

    private static Object normalizeScalarSignalArg(final Object value) {
        return switch (value) {
            case null -> null;
            case Boolean typedValue -> typedValue;
            case Character typedValue -> Integer.valueOf(typedValue);
            case Byte typedValue -> typedValue;
            case Short typedValue -> typedValue;
            case Integer typedValue -> typedValue;
            case Long typedValue -> typedValue;
            case Number typedValue -> Double.valueOf(typedValue.doubleValue());
            case String typedValue -> typedValue;
            case byte[] typedValue -> typedValue;
            case CompoundTag typedValue -> typedValue;
            default -> null;
        };
    }

    private static CompoundTag saveSignal(final Signal signal) {
        final CompoundTag signalTag = new CompoundTag();
        signalTag.putString(SIGNAL_NAME_TAG, signal.name());
        final ListTag argsTag = new ListTag();
        for (Object arg : signal.args()) {
            argsTag.add(saveSignalArg(arg));
        }
        signalTag.put(SIGNAL_ARGS_TAG, argsTag);
        return signalTag;
    }

    private static Signal loadSignal(final CompoundTag signalTag) {
        final ListTag argTags = signalTag.getList(SIGNAL_ARGS_TAG, CompoundTag.TAG_COMPOUND);
        final Object[] args = new Object[argTags.size()];
        for (int i = 0; i < argTags.size(); i++) {
            args[i] = loadSignalArg(argTags.getCompound(i));
        }
        return new SimpleSignal(signalTag.getString(SIGNAL_NAME_TAG), args);
    }

    private static CompoundTag saveSignalArg(final Object value) {
        final CompoundTag tag = new CompoundTag();
        switch (value) {
            case null -> tag.putString(SIGNAL_ARG_TYPE_TAG, "null");
            case Boolean typedValue -> {
                tag.putString(SIGNAL_ARG_TYPE_TAG, "boolean");
                tag.putBoolean(SIGNAL_ARG_VALUE_TAG, typedValue);
            }
            case Byte typedValue -> {
                tag.putString(SIGNAL_ARG_TYPE_TAG, "byte");
                tag.putByte(SIGNAL_ARG_VALUE_TAG, typedValue);
            }
            case Short typedValue -> {
                tag.putString(SIGNAL_ARG_TYPE_TAG, "short");
                tag.putShort(SIGNAL_ARG_VALUE_TAG, typedValue);
            }
            case Integer typedValue -> {
                tag.putString(SIGNAL_ARG_TYPE_TAG, "int");
                tag.putInt(SIGNAL_ARG_VALUE_TAG, typedValue);
            }
            case Long typedValue -> {
                tag.putString(SIGNAL_ARG_TYPE_TAG, "long");
                tag.putLong(SIGNAL_ARG_VALUE_TAG, typedValue);
            }
            case Float typedValue -> {
                tag.putString(SIGNAL_ARG_TYPE_TAG, "float");
                tag.putFloat(SIGNAL_ARG_VALUE_TAG, typedValue);
            }
            case Double typedValue -> {
                tag.putString(SIGNAL_ARG_TYPE_TAG, "double");
                tag.putDouble(SIGNAL_ARG_VALUE_TAG, typedValue);
            }
            case String typedValue -> {
                tag.putString(SIGNAL_ARG_TYPE_TAG, "string");
                tag.putString(SIGNAL_ARG_VALUE_TAG, typedValue);
            }
            case Character typedValue -> {
                tag.putString(SIGNAL_ARG_TYPE_TAG, "string");
                tag.putString(SIGNAL_ARG_VALUE_TAG, typedValue.toString());
            }
            case byte[] typedValue -> {
                tag.putString(SIGNAL_ARG_TYPE_TAG, "bytes");
                tag.putByteArray(SIGNAL_ARG_VALUE_TAG, typedValue);
            }
            case Map<?, ?> typedValue -> {
                tag.putString(SIGNAL_ARG_TYPE_TAG, "map");
                final ListTag entries = new ListTag();
                for (Map.Entry<?, ?> entry : typedValue.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        final CompoundTag entryTag = new CompoundTag();
                        entryTag.putString(SIGNAL_ARG_KEY_TAG, entry.getKey().toString());
                        entryTag.putString(SIGNAL_ARG_VALUE_TAG, entry.getValue().toString());
                        entries.add(entryTag);
                    }
                }
                tag.put(SIGNAL_ARG_VALUE_TAG, entries);
            }
            default -> tag.putString(SIGNAL_ARG_TYPE_TAG, "null");
        }
        return tag;
    }

    private static Object loadSignalArg(final CompoundTag tag) {
        return switch (tag.getString(SIGNAL_ARG_TYPE_TAG)) {
            case "boolean" -> tag.getBoolean(SIGNAL_ARG_VALUE_TAG);
            case "byte" -> tag.getByte(SIGNAL_ARG_VALUE_TAG);
            case "short" -> tag.getShort(SIGNAL_ARG_VALUE_TAG);
            case "int" -> tag.getInt(SIGNAL_ARG_VALUE_TAG);
            case "long" -> tag.getLong(SIGNAL_ARG_VALUE_TAG);
            case "float" -> tag.getFloat(SIGNAL_ARG_VALUE_TAG);
            case "double" -> tag.getDouble(SIGNAL_ARG_VALUE_TAG);
            case "string" -> tag.getString(SIGNAL_ARG_VALUE_TAG);
            case "bytes" -> tag.getByteArray(SIGNAL_ARG_VALUE_TAG);
            case "map" -> {
                final ListTag entries = tag.getList(SIGNAL_ARG_VALUE_TAG, CompoundTag.TAG_COMPOUND);
                final Map<String, String> map = new LinkedHashMap<>();
                for (int i = 0; i < entries.size(); i++) {
                    final CompoundTag entryTag = entries.getCompound(i);
                    map.put(entryTag.getString(SIGNAL_ARG_KEY_TAG), entryTag.getString(SIGNAL_ARG_VALUE_TAG));
                }
                yield map;
            }
            default -> null;
        };
    }

    private static void saveComponentEnvironment(final ManagedEnvironment environment) {
        if (environment != null) {
            environment.save(new CompoundTag());
        }
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

    private void sendLifecycleMessage(final String name) {
        if (node() != null) {
            node().sendToReachable(name);
        }
    }

    private void connectTemporaryFileSystem() {
        if (node() == null || temporaryFileSystemEnvironment == null || temporaryFileSystemEnvironment.node() == null) {
            return;
        }
        if (node().network() == null) {
            Network.joinNewNetwork(node());
        }
        if (temporaryFileSystemEnvironment.node().network() != node().network()) {
            node().connect(temporaryFileSystemEnvironment.node());
        }
    }

    private ManagedEnvironment createTemporaryFileSystemEnvironment() {
        return API.fileSystem.asManagedEnvironment(
            API.fileSystem.fromMemory(ModSettings.tmpSize() * 1024L),
            "tmp",
            null,
            null,
            1);
    }

    private void resetTemporaryFileSystem() {
        if (temporaryFileSystemEnvironment != null && temporaryFileSystemEnvironment.node() != null) {
            temporaryFileSystemEnvironment.node().remove();
        }
        temporaryFileSystemEnvironment = createTemporaryFileSystemEnvironment();
    }

    private void queueComponentChangeSignal(final String name, final Node changedNode) {
        if (changedNode == node()) {
            return;
        }
        if (changedNode instanceof Component component && component.canBeSeenFrom(node())) {
            signal(name, component.address(), component.name());
        }
    }

    private void queueCheckedSignal(final Message message) {
        final Object[] data = message.data();
        if (data.length == 0) {
            return;
        }
        final int nameIndex = data[0] instanceof String ? 0 : 1;
        if (data.length <= nameIndex || !(data[nameIndex] instanceof String signalName)) {
            return;
        }
        if (data[0] instanceof Player player && !canInteract(player.getGameProfile().getName())) {
            return;
        }
        final Object[] rawArgs = Arrays.copyOfRange(data, nameIndex + 1, data.length);
        final Node source = message.source();
        if (source == null || source.address() == null) {
            signal(signalName, rawArgs);
            return;
        }
        final Object[] args = new Object[rawArgs.length + 1];
        args[0] = source.address();
        System.arraycopy(rawArgs, 0, args, 1, rawArgs.length);
        signal(signalName, args);
    }

    private void queueNetworkSignal(final Message message) {
        final Object[] data = message.data();
        if (data.length == 0 || !(data[0] instanceof String signalName)) {
            return;
        }
        final Object[] args = new Object[data.length];
        args[0] = message.source() == null ? null : message.source().address();
        System.arraycopy(data, 1, args, 1, data.length - 1);
        signal(signalName, args);
    }

    private void bindArchitecture(final Architecture architecture) {
        if (architecture instanceof MachineBoundArchitecture boundArchitecture) {
            boundArchitecture.bind(this);
        }
    }

    private boolean canReportDeviceInfo(final li.cil.oc.api.network.Node reachable) {
        if (reachable instanceof Component component) {
            return reachable == node() || component.canBeSeenFrom(node());
        }
        return reachable.canBeReachedFrom(node());
    }

    private String currentArchitectureName() {
        if (architecture == null) {
            return null;
        }
        final String registeredName = li.cil.oc.api.Machine.getArchitectureName(architecture.getClass());
        if (registeredName != null) {
            return registeredName;
        }
        final Architecture.Name name = architecture.getClass().getAnnotation(Architecture.Name.class);
        return name == null ? null : name.value();
    }

    private static Map<String, Method> discoverCallbacks(final Object value) {
        final Map<String, Method> discovered = new LinkedHashMap<>();
        final Set<String> whitelist = value instanceof MethodWhitelist methodWhitelist && methodWhitelist.whitelistedMethods() != null
            ? Set.copyOf(Arrays.asList(methodWhitelist.whitelistedMethods()))
            : Set.of();
        final FilteredEnvironment filter = value instanceof FilteredEnvironment filtered ? filtered : null;
        Class<?> type = value.getClass();
        while (type != null) {
            for (Method method : type.getDeclaredMethods()) {
                final Callback callback = method.getAnnotation(Callback.class);
                if (callback != null && isValidCallbackMethod(method)) {
                    method.setAccessible(true);
                    final String name = callback.value().trim().isEmpty() ? method.getName() : callback.value();
                    if ((whitelist.isEmpty() || whitelist.contains(name)) && (filter == null || filter.isCallbackEnabled(name))) {
                        discovered.putIfAbsent(name, method);
                    }
                }
            }
            type = type.getSuperclass();
        }
        return discovered;
    }

    private static boolean isValidCallbackMethod(final Method method) {
        final Class<?>[] parameterTypes = method.getParameterTypes();
        return method.getReturnType() == Object[].class &&
            parameterTypes.length == 2 &&
            parameterTypes[0] == Context.class &&
            parameterTypes[1] == Arguments.class &&
            Modifier.isPublic(method.getModifiers());
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
        @Override public String checkString(final int index) {
            final Object value = checkAny(index);
            if (value instanceof String string) return string;
            if (value instanceof byte[] bytes) return new String(bytes, StandardCharsets.UTF_8);
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (string expected)");
        }
        @Override public byte[] checkByteArray(final int index) {
            final Object value = checkAny(index);
            if (value instanceof byte[] bytes) return bytes;
            if (value instanceof String string) return string.getBytes(StandardCharsets.UTF_8);
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
        @Override public boolean isString(final int index) { return index >= 0 && index < values.length && (values[index] instanceof String || values[index] instanceof byte[]); }
        @Override public boolean isByteArray(final int index) { return index >= 0 && index < values.length && values[index] instanceof byte[]; }
        @Override public boolean isTable(final int index) { return index >= 0 && index < values.length && values[index] instanceof Map; }
        @Override public boolean isItemStack(final int index) { return index >= 0 && index < values.length && values[index] instanceof ItemStack; }
        @Override public Object[] toArray() {
            final Object[] result = Arrays.copyOf(values, values.length);
            for (int index = 0; index < result.length; index++) {
                if (result[index] instanceof byte[] bytes) {
                    result[index] = new String(bytes, StandardCharsets.UTF_8);
                }
            }
            return result;
        }
        @Override public java.util.Iterator<Object> iterator() { return Arrays.asList(values).iterator(); }
    }
}
