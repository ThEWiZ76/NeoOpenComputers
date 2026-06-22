package li.cil.oc.common.machine;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaThread;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.BaseLib;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.CoroutineLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

@Architecture.Name("Lua")
public final class LuaArchitecture implements Architecture, MachineBoundArchitecture {
    private static final String EEPROM_SLOT = "eeprom";
    private static final String INITIALIZED_TAG = "initialized";
    private static final String BOOTED_TAG = "booted";
    private static final String BOOT_SOURCE_TAG = "bootSource";
    private static final String BOOT_ADDRESS_TAG = "bootAddress";
    private static final String MEMORY_TAG = "memory";
    private static final String PULL_SIGNAL_MARKER = "\u0000oc.pullSignal";
    private static final String BUDGET_RETRY_MARKER = "\u0000oc.budgetRetry";
    private static final String VALUE_MARKER = "\u0000oc.value";
    private boolean initialized;
    private boolean booted;
    private String bootSource;
    private String bootAddress;
    private Machine machine;
    private Globals globals;
    private LuaValue bootChunk;
    private LuaThread bootThread;
    private ExecutionResult pendingResult;
    private PendingBudgetCall pendingBudgetCall;
    private double memoryBytes;
    private boolean waitingForSignal;
    private double signalDeadlineSeconds;
    private final LongSupplier wallTimeMillis;
    private final Map<String, String> primaryComponents = new HashMap<>();
    private final Map<String, LuaTable> componentProxyCache = new HashMap<>();

    public LuaArchitecture() {
        this("");
    }

    LuaArchitecture(final String bootSource) {
        this(bootSource, System::currentTimeMillis);
    }

    LuaArchitecture(final String bootSource, final LongSupplier wallTimeMillis) {
        this.bootSource = bootSource == null ? "" : bootSource;
        this.wallTimeMillis = wallTimeMillis;
    }

    @Override
    public void bind(final Machine machine) {
        this.machine = machine;
    }

    @Override
    public boolean isInitialized() {
        return initialized && booted;
    }

    @Override
    public boolean recomputeMemory(final Iterable<ItemStack> components) {
        double totalMemory = 0D;
        for (ItemStack stack : components) {
            final DriverItem driver = Driver.driverFor(stack);
            if (driver != null && EEPROM_SLOT.equals(driver.slot(stack))) {
                configureBootSource(driver.dataTag(stack));
            }
            if (driver instanceof Memory memory) {
                totalMemory += Math.max(0D, memory.amount(stack)) * 1024D;
            }
        }
        memoryBytes = Math.max(0D, totalMemory);
        return memoryBytes > 0D;
    }

    @Override
    public boolean initialize() {
        componentProxyCache.clear();
        globals = sandboxGlobals();
        pendingResult = null;
        pendingBudgetCall = null;
        installComputerLibrary();
        installComponentLibrary();
        installUserdataLibrary();
        installOsLibrary();
        installUnicodeLibrary();
        installSystemLibrary();
        try {
            bootChunk = globals.load(bootSource, "boot");
            bootThread = new LuaThread(globals, bootChunk);
        } catch (LuaError e) {
            close();
            return false;
        }
        initialized = true;
        return true;
    }

    @Override
    public void close() {
        initialized = false;
        booted = false;
        globals = null;
        bootChunk = null;
        bootThread = null;
        pendingResult = null;
        pendingBudgetCall = null;
        waitingForSignal = false;
        signalDeadlineSeconds = 0D;
        componentProxyCache.clear();
    }

    @Override
    public void runSynchronized() {
    }

    @Override
    public ExecutionResult runThreaded(final boolean isSynchronizedReturn) {
        if (!initialized) {
            return new ExecutionResult.Error("Lua architecture is not initialized");
        }
        if (pendingBudgetCall != null) {
            try {
                final Varargs results = pendingBudgetCall.invoke();
                pendingBudgetCall = null;
                return resumeBoot(results);
            } catch (LimitReachedException e) {
                return new ExecutionResult.Sleep(1);
            }
        }
        if (waitingForSignal) {
            final Signal signal = machine == null ? null : machine.popSignal();
            if (signal != null) {
                waitingForSignal = false;
                return resumeBoot(signalToLuaValues(signal));
            }
            if (machineUpTime() >= signalDeadlineSeconds) {
                waitingForSignal = false;
                return resumeBoot(LuaValue.NONE);
            }
            return sleepUntilSignalDeadline();
        }
        if (!booted) {
            try {
                return resumeBoot(LuaValue.NONE);
            } finally {
                booted = true;
            }
        }
        return new ExecutionResult.Sleep(1);
    }

    @Override
    public void onSignal() {
    }

    @Override
    public void onConnect() {
    }

    @Override
    public void load(final CompoundTag nbt) {
        bootSource = nbt.contains(BOOT_SOURCE_TAG) ? nbt.getString(BOOT_SOURCE_TAG) : "";
        bootAddress = nbt.contains(BOOT_ADDRESS_TAG) ? nbt.getString(BOOT_ADDRESS_TAG) : null;
        booted = nbt.getBoolean(BOOTED_TAG);
        if (nbt.getBoolean(INITIALIZED_TAG)) {
            initialize();
        } else {
            initialized = false;
        }
        memoryBytes = Math.max(0D, nbt.getDouble(MEMORY_TAG));
    }

    @Override
    public void save(final CompoundTag nbt) {
        nbt.putBoolean(INITIALIZED_TAG, initialized);
        nbt.putBoolean(BOOTED_TAG, booted);
        nbt.putString(BOOT_SOURCE_TAG, bootSource);
        if (bootAddress != null) {
            nbt.putString(BOOT_ADDRESS_TAG, bootAddress);
        }
        nbt.putDouble(MEMORY_TAG, memoryBytes);
    }

    int globalInteger(final String name) {
        if (globals == null) {
            return 0;
        }
        return globals.get(name).toint();
    }

    double globalDouble(final String name) {
        if (globals == null) {
            return 0D;
        }
        return globals.get(name).todouble();
    }

    String globalString(final String name) {
        if (globals == null) {
            return "";
        }
        return globals.get(name).tojstring();
    }

    boolean globalBoolean(final String name) {
        return globals != null && globals.get(name).toboolean();
    }

    void configureBootSource(final CompoundTag eepromData) {
        bootSource = bootSourceFrom(eepromData);
    }

    private static String bootSourceFrom(final CompoundTag eepromData) {
        if (eepromData == null || !eepromData.contains(ItemRegistry.EEPROM_CODE_TAG)) {
            return "";
        }
        return new String(eepromData.getByteArray(ItemRegistry.EEPROM_CODE_TAG), StandardCharsets.UTF_8);
    }

    private static Globals sandboxGlobals() {
        final Globals globals = new Globals();
        globals.load(new BaseLib());
        final LuaTable packageTable = new LuaTable();
        packageTable.set("loaded", new LuaTable());
        globals.set("package", packageTable);
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new CoroutineLib());
        globals.load(new Bit32Lib());
        globals.load(new JseMathLib());
        globals.set("package", LuaValue.NIL);
        installCheckArg(globals);
        installDebugLibrary(globals);
        installStringCompatibility(globals);
        LoadState.install(globals);
        LuaC.install(globals);
        return globals;
    }

    private static void installDebugLibrary(final Globals globals) {
        final LuaTable debug = new LuaTable();
        debug.set("traceback", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final LuaValue message = args.arg(1);
                final String prefix = message.isnil() ? "" : message.tojstring() + "\n";
                return LuaValue.valueOf(prefix + "stack traceback unavailable");
            }
        });
        globals.set("debug", debug);
    }

    private static void installCheckArg(final Globals globals) {
        globals.set("checkArg", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final int index = args.checkint(1);
                final LuaValue value = args.arg(2);
                if (matchesAnyType(value, args, 3)) {
                    return LuaValue.NONE;
                }
                throw new LuaError("bad argument #" + index + " (" + expectedTypes(args, 3) + " expected, got " + luaTypeName(value) + ")");
            }
        });
    }

    private static boolean matchesAnyType(final LuaValue value, final Varargs args, final int firstTypeIndex) {
        for (int index = firstTypeIndex; index <= args.narg(); index++) {
            if (matchesType(value, args.checkjstring(index))) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesType(final LuaValue value, final String expectedType) {
        return switch (expectedType) {
            case "nil" -> value.type() == LuaValue.TNIL;
            case "boolean" -> value.type() == LuaValue.TBOOLEAN;
            case "number" -> value.type() == LuaValue.TNUMBER;
            case "string" -> value.type() == LuaValue.TSTRING;
            case "table" -> value.type() == LuaValue.TTABLE;
            case "function" -> value.type() == LuaValue.TFUNCTION;
            case "thread" -> value.type() == LuaValue.TTHREAD;
            case "userdata" -> value.type() == LuaValue.TUSERDATA;
            default -> expectedType.equals(luaTypeName(value));
        };
    }

    private static String expectedTypes(final Varargs args, final int firstTypeIndex) {
        final int count = Math.max(0, args.narg() - firstTypeIndex + 1);
        if (count == 0) {
            return "";
        }
        if (count == 1) {
            return args.checkjstring(firstTypeIndex);
        }
        final StringBuilder builder = new StringBuilder();
        for (int index = firstTypeIndex; index <= args.narg(); index++) {
            if (index > firstTypeIndex) {
                builder.append(index == args.narg() ? " or " : ", ");
            }
            builder.append(args.checkjstring(index));
        }
        return builder.toString();
    }

    private static String luaTypeName(final LuaValue value) {
        return value.isnil() ? "nil" : value.typename();
    }

    private static void installStringCompatibility(final Globals globals) {
        final LuaValue string = globals.get("string");
        final LuaValue originalFormat = string.get("format");
        string.set("format", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (args.narg() < 2) {
                    return originalFormat.invoke(args);
                }
                final String format = args.checkjstring(1);
                final LuaValue[] converted = new LuaValue[args.narg()];
                converted[0] = args.arg(1);
                for (int index = 2; index <= args.narg(); index++) {
                    converted[index - 1] = args.arg(index);
                }
                int argumentIndex = 2;
                for (int index = 0; index < format.length() && argumentIndex <= args.narg(); index++) {
                    if (format.charAt(index) != '%') {
                        continue;
                    }
                    if (index + 1 < format.length() && format.charAt(index + 1) == '%') {
                        index++;
                        continue;
                    }
                    while (index + 1 < format.length() && !Character.isLetter(format.charAt(index + 1))) {
                        index++;
                    }
                    if (index + 1 < format.length()) {
                        final char conversion = format.charAt(++index);
                        if (conversion == 's') {
                            converted[argumentIndex - 1] = LuaValue.valueOf(args.arg(argumentIndex).tojstring());
                        }
                        argumentIndex++;
                    }
                }
                return originalFormat.invoke(LuaValue.varargsOf(converted));
            }
        });
    }

    private void installComputerLibrary() {
        final LuaTable computer = new LuaTable();
        computer.set("uptime", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(machine == null ? 0D : machine.upTime());
            }
        });
        computer.set("realTime", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(wallTimeMillis.getAsLong() / 1000D);
            }
        });
        computer.set("address", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return machineAddress();
            }
        });
        computer.set("freeMemory", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(memoryBytes / 2D);
            }
        });
        computer.set("totalMemory", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(memoryBytes);
            }
        });
        computer.set("energy", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                final Connector connector = machineConnector();
                return LuaValue.valueOf(connector == null ? 0D : connector.globalBuffer());
            }
        });
        computer.set("maxEnergy", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                final Connector connector = machineConnector();
                return LuaValue.valueOf(connector == null ? 0D : connector.globalBufferSize());
            }
        });
        computer.set("getBootAddress", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return bootAddress == null ? LuaValue.NIL : LuaValue.valueOf(bootAddress);
            }
        });
        computer.set("setBootAddress", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                bootAddress = args.isnoneornil(1) ? null : args.checkjstring(1);
                return LuaValue.TRUE;
            }
        });
        computer.set("isRobot", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(machine != null && machine.host() instanceof Robot);
            }
        });
        computer.set("getDeviceInfo", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return deviceInfo();
            }
        });
        computer.set("getProgramLocations", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return programLocations();
            }
        });
        computer.set("tmpAddress", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return machineTmpAddress();
            }
        });
        computer.set("shutdown", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                pendingResult = new ExecutionResult.Shutdown(args.narg() > 0 && args.arg1().toboolean());
                return LuaValue.NIL;
            }
        });
        computer.set("beep", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null) {
                    return LuaValue.FALSE;
                }
                if (args.narg() == 1 && args.arg1().isstring()) {
                    machine.beep(args.arg1().tojstring());
                } else {
                    final int frequency = args.isnoneornil(1) ? 440 : args.checkint(1);
                    if (frequency < 20 || frequency > 2000) {
                        throw new IllegalArgumentException("invalid frequency, must be in [20, 2000]");
                    }
                    final double duration = args.isnoneornil(2) ? 0.1D : args.checkdouble(2);
                    final int durationInMilliseconds = Math.max(50, Math.min(5000, (int) (duration * 1000D)));
                    machine.beep((short) frequency, (short) durationInMilliseconds);
                }
                return LuaValue.TRUE;
            }
        });
        computer.set("users", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                final LuaTable users = new LuaTable();
                if (machine != null) {
                    final String[] names = machine.users();
                    for (int index = 0; index < names.length; index++) {
                        users.set(index + 1, names[index]);
                    }
                }
                return users;
            }
        });
        computer.set("addUser", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String name = args.checkjstring(1);
                if (machine == null) {
                    return LuaValue.FALSE;
                }
                try {
                    machine.addUser(name);
                    return LuaValue.TRUE;
                } catch (Exception e) {
                    throw new LuaError(e.getMessage() == null ? e.toString() : e.getMessage());
                }
            }
        });
        computer.set("removeUser", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String name = args.checkjstring(1);
                if (machine == null) {
                    return LuaValue.FALSE;
                }
                return LuaValue.valueOf(machine.removeUser(name));
            }
        });
        computer.set("pullSignal", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null) {
                    return LuaValue.NIL;
                }
                final Signal signal = machine.popSignal();
                if (signal != null) {
                    return signalToLuaValues(signal);
                }
                final double timeout = args.narg() >= 1 && args.arg(1).isnumber()
                    ? Math.max(0D, args.arg(1).todouble())
                    : Double.POSITIVE_INFINITY;
                waitingForSignal = true;
                signalDeadlineSeconds = Double.isInfinite(timeout) ? Double.POSITIVE_INFINITY : machineUpTime() + timeout;
                return globals.yield(LuaValue.valueOf(PULL_SIGNAL_MARKER));
            }
        });
        computer.set("pushSignal", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String signalName = args.checkjstring(1);
                if (machine == null) {
                    return LuaValue.FALSE;
                }
                final Object[] signalArgs = new Object[Math.max(0, args.narg() - 1)];
                for (int index = 0; index < signalArgs.length; index++) {
                    signalArgs[index] = toJavaValue(args.arg(index + 2));
                }
                return LuaValue.valueOf(machine.signal(signalName, signalArgs));
            }
        });
        computer.set("getArchitectures", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final LuaTable architectures = new LuaTable();
                final Processor processor = processor();
                if (processor == null) {
                    return architectures;
                }
                int index = 1;
                if (processor instanceof MutableProcessor mutableProcessor) {
                    for (Class<? extends Architecture> architecture : mutableProcessor.allArchitectures()) {
                        final String name = li.cil.oc.api.Machine.getArchitectureName(architecture);
                        if (name != null) {
                            architectures.set(index++, name);
                        }
                    }
                } else {
                    final String name = li.cil.oc.api.Machine.getArchitectureName(processor.architecture(processorStack()));
                    if (name != null) {
                        architectures.set(index, name);
                    }
                }
                return architectures;
            }
        });
        computer.set("getArchitecture", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final Processor processor = processor();
                if (processor == null) {
                    return LuaValue.NIL;
                }
                final String name = li.cil.oc.api.Machine.getArchitectureName(processor.architecture(processorStack()));
                return name == null ? LuaValue.NIL : LuaValue.valueOf(name);
            }
        });
        computer.set("setArchitecture", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String requestedName = args.checkjstring(1);
                final Processor processor = processor();
                if (!(processor instanceof MutableProcessor mutableProcessor)) {
                    return LuaValue.NIL;
                }
                final ItemStack stack = processorStack();
                for (Class<? extends Architecture> architecture : mutableProcessor.allArchitectures()) {
                    if (requestedName.equals(li.cil.oc.api.Machine.getArchitectureName(architecture))) {
                        if (architecture != mutableProcessor.architecture(stack)) {
                            mutableProcessor.setArchitecture(stack, architecture);
                            return LuaValue.TRUE;
                        }
                        return LuaValue.FALSE;
                    }
                }
                return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("unknown architecture"));
            }
        });
        globals.set("computer", computer);
    }

    private void installComponentLibrary() {
        final LuaTable component = new LuaTable();
        component.set("list", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String filter = args.arg(1).isstring() ? args.arg(1).tojstring() : null;
                final boolean exact = args.narg() >= 2 && args.arg(2).toboolean();
                return createComponentList(filter, exact);
            }
        });
        component.set("get", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null || args.narg() < 1) {
                    return LuaValue.NIL;
                }
                final String prefix = args.arg(1).tojstring();
                final String type = args.narg() >= 2 && !args.arg(2).isnil() ? args.arg(2).tojstring() : null;
                for (Map.Entry<String, String> entry : machine.components().entrySet()) {
                    if (entry.getKey().startsWith(prefix) && (type == null || type.equals(entry.getValue()))) {
                        return LuaValue.valueOf(entry.getKey());
                    }
                }
                return noSuchComponent();
            }
        });
        component.set("type", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String address = args.checkjstring(1);
                if (machine == null) {
                    return LuaValue.NIL;
                }
                final String type = machine.components().get(address);
                return type == null ? noSuchComponent() : LuaValue.valueOf(type);
            }
        });
        component.set("isAvailable", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null || args.narg() < 1) {
                    return LuaValue.FALSE;
                }
                final String type = args.arg1().tojstring();
                return LuaValue.valueOf(machine.components().containsValue(type));
            }
        });
        component.set("isPrimary", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null || args.narg() < 1) {
                    return LuaValue.FALSE;
                }
                final String address = args.arg(1).tojstring();
                final String type = machine.components().get(address);
                return LuaValue.valueOf(type != null && address.equals(firstComponentAddress(type)));
            }
        });
        component.set("slot", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String address = args.checkjstring(1);
                if (machine == null) {
                    return LuaValue.NIL;
                }
                final MachineHost host = machine.host();
                if (host == null) {
                    return LuaValue.NIL;
                }
                if (!hasComponent(address)) {
                    return noSuchComponent();
                }
                return LuaValue.valueOf(host.componentSlot(address));
            }
        });
        component.set("getPrimary", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null || args.narg() < 1) {
                    return LuaValue.NIL;
                }
                final String address = firstComponentAddress(args.arg1().tojstring());
                return address == null ? LuaValue.NIL : createComponentProxy(address);
            }
        });
        component.set("setPrimary", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null || args.narg() < 2) {
                    return LuaValue.FALSE;
                }
                final String address = args.arg(2).tojstring();
                if (!hasComponent(address)) {
                    return noSuchComponent();
                }
                return LuaValue.valueOf(setPrimaryComponent(args.arg(1).tojstring(), address));
            }
        });
        component.set("methods", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String address = args.checkjstring(1);
                final LuaTable methods = new LuaTable();
                if (machine != null) {
                    if (!hasComponent(address)) {
                        return noSuchComponent();
                    }
                    for (Map.Entry<String, Callback> entry : machine.methods(address).entrySet()) {
                        final Callback callback = entry.getValue();
                        final LuaTable metadata = new LuaTable();
                        metadata.set("direct", LuaValue.valueOf(callback != null && callback.direct()));
                        metadata.set("getter", LuaValue.valueOf(callback != null && callback.getter()));
                        metadata.set("setter", LuaValue.valueOf(callback != null && callback.setter()));
                        methods.set(entry.getKey(), metadata);
                    }
                }
                return methods;
            }
        });
        component.set("fields", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final LuaTable fields = new LuaTable();
                if (machine != null && args.narg() >= 1) {
                    final String address = args.arg(1).tojstring();
                    if (!hasComponent(address)) {
                        return noSuchComponent();
                    }
                    for (Map.Entry<String, Callback> entry : machine.methods(address).entrySet()) {
                        final Callback callback = entry.getValue();
                        if (callback != null && (callback.getter() || callback.setter())) {
                            final LuaTable metadata = new LuaTable();
                            metadata.set("direct", LuaValue.valueOf(callback.direct()));
                            metadata.set("getter", LuaValue.valueOf(callback.getter()));
                            metadata.set("setter", LuaValue.valueOf(callback.setter()));
                            fields.set(entry.getKey(), metadata);
                        }
                    }
                }
                return fields;
            }
        });
        component.set("doc", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String address = args.checkjstring(1);
                if (machine == null) {
                    return LuaValue.NIL;
                }
                if (!hasComponent(address)) {
                    return noSuchComponent();
                }
                final String method = args.checkjstring(2);
                final Callback callback = machine.methods(address).get(method);
                if (callback == null || callback.doc().isEmpty()) {
                    return LuaValue.NIL;
                }
                return LuaValue.valueOf(callback.doc());
            }
        });
        component.set("invoke", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String address = args.checkjstring(1);
                final String method = args.checkjstring(2);
                if (machine == null) {
                    return LuaValue.NIL;
                }
                if (!hasComponent(address)) {
                    return noSuchComponent();
                }
                final Object[] javaArgs = new Object[Math.max(0, args.narg() - 2)];
                for (int index = 0; index < javaArgs.length; index++) {
                    javaArgs[index] = toJavaValue(args.arg(index + 3));
                }
                return invokeComponent(address, method, javaArgs);
            }
        });
        component.set("proxy", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null || args.narg() < 1) {
                    return LuaValue.NIL;
                }
                final String address = args.arg(1).tojstring();
                return hasComponent(address) ? createComponentProxy(address) : noSuchComponent();
            }
        });
        final LuaTable metatable = new LuaTable();
        metatable.set("__index", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null) {
                    return LuaValue.NIL;
                }
                final String address = firstComponentAddress(args.arg(2).tojstring());
                return address == null ? LuaValue.NIL : createComponentProxy(address);
            }
        });
        component.setmetatable(metatable);
        globals.set("component", component);
    }

    private void installOsLibrary() {
        final LuaTable os = new LuaTable();
        os.set("clock", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(machine == null ? 0D : machine.cpuTime());
            }
        });
        os.set("time", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (args.isnoneornil(1)) {
                    return LuaValue.valueOf(worldTimestamp());
                }
                final LuaTable time = args.checktable(1);
                final int second = intField(time, "sec", 0);
                final int minute = intField(time, "min", 0);
                final int hour = intField(time, "hour", 12);
                final int day = intField(time, "day", -1);
                final int month = intField(time, "month", -1);
                final int year = intField(time, "year", -1);
                final Long timestamp = GameTimeFormatter.mktime(year, month, day, hour, minute, second);
                return timestamp == null ? LuaValue.NIL : LuaValue.valueOf(timestamp.doubleValue());
            }
        });
        os.set("date", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                String format = args.narg() > 0 && args.arg(1).isstring() ? args.arg(1).tojstring() : "%d/%m/%y %H:%M:%S";
                final double time = args.narg() > 1 && args.arg(2).isnumber() ? args.arg(2).todouble() : worldTimestamp();
                if (format.startsWith("!")) {
                    format = format.substring(1);
                }
                final GameTimeFormatter.DateTime dateTime = GameTimeFormatter.parse(time);
                if ("*t".equals(format)) {
                    final LuaTable table = new LuaTable();
                    table.set("year", dateTime.year());
                    table.set("month", dateTime.month());
                    table.set("day", dateTime.day());
                    table.set("hour", dateTime.hour());
                    table.set("min", dateTime.minute());
                    table.set("sec", dateTime.second());
                    table.set("wday", dateTime.weekDay());
                    table.set("yday", dateTime.yearDay());
                    return table;
                }
                return LuaValue.valueOf(GameTimeFormatter.format(format, dateTime));
            }
        });
        globals.set("os", os);
    }

    private void installUserdataLibrary() {
        final LuaTable userdata = new LuaTable();
        userdata.set("apply", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final Value value = checkValue(args, 1);
                return toLuaValue(value.apply(machine, new LuaArguments(toJavaArgs(args, 2))));
            }
        });
        userdata.set("unapply", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final Value value = checkValue(args, 1);
                value.unapply(machine, new LuaArguments(toJavaArgs(args, 2)));
                return LuaValue.NIL;
            }
        });
        userdata.set("call", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final Value value = checkValue(args, 1);
                return toLuaValues(value.call(machine, new LuaArguments(toJavaArgs(args, 2))));
            }
        });
        userdata.set("dispose", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final Value value = checkValue(args, 1);
                try {
                    value.dispose(machine);
                } catch (Exception ignored) {
                    // Upstream logs and suppresses userdata dispose failures.
                }
                return LuaValue.NIL;
            }
        });
        userdata.set("methods", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final Value value = checkValue(args, 1);
                final LuaTable methods = new LuaTable();
                if (machine != null) {
                    for (Map.Entry<String, Callback> entry : machine.methods(value).entrySet()) {
                        final Callback callback = entry.getValue();
                        methods.set(entry.getKey(), LuaValue.valueOf(callback != null && callback.direct()));
                    }
                }
                return methods;
            }
        });
        userdata.set("invoke", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final Value value = checkValue(args, 1);
                final String method = args.checkjstring(2);
                return invokeValue(value, method, toJavaArgs(args, 3));
            }
        });
        userdata.set("doc", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final Value value = checkValue(args, 1);
                final String method = args.checkjstring(2);
                if (machine == null) {
                    return LuaValue.NIL;
                }
                final Callback callback = machine.methods(value).get(method);
                return callback == null || callback.doc().isEmpty() ? LuaValue.NIL : LuaValue.valueOf(callback.doc());
            }
        });
        globals.set("userdata", userdata);
    }

    private void installUnicodeLibrary() {
        final LuaTable unicode = new LuaTable();
        unicode.set("lower", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return LuaValue.valueOf(args.checkjstring(1).toLowerCase(Locale.ROOT));
            }
        });
        unicode.set("upper", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return LuaValue.valueOf(args.checkjstring(1).toUpperCase(Locale.ROOT));
            }
        });
        unicode.set("char", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final StringBuilder builder = new StringBuilder();
                for (int index = 1; index <= args.narg(); index++) {
                    builder.appendCodePoint(args.checkint(index));
                }
                return LuaValue.valueOf(builder.toString());
            }
        });
        unicode.set("len", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String value = args.checkjstring(1);
                return LuaValue.valueOf(value.codePointCount(0, value.length()));
            }
        });
        unicode.set("reverse", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return LuaValue.valueOf(reverseUnicode(args.checkjstring(1)));
            }
        });
        unicode.set("sub", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String value = args.checkjstring(1);
                final int start = args.checkint(2);
                final int end = args.narg() > 2 ? args.checkint(3) : Integer.MAX_VALUE;
                return LuaValue.valueOf(subUnicode(value, start, end));
            }
        });
        unicode.set("isWide", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return LuaValue.valueOf(charWidth(args.checkjstring(1)) > 1);
            }
        });
        unicode.set("charWidth", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return LuaValue.valueOf(charWidth(args.checkjstring(1)));
            }
        });
        unicode.set("wlen", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return LuaValue.valueOf(displayWidth(args.checkjstring(1)));
            }
        });
        unicode.set("wtrunc", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return LuaValue.valueOf(truncateDisplayWidth(args.checkjstring(1), args.checkint(2)));
            }
        });
        globals.set("unicode", unicode);
    }

    private void installSystemLibrary() {
        final LuaTable system = new LuaTable();
        system.set("allowBytecode", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(ModSettings.allowBytecode());
            }
        });
        system.set("allowGC", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(ModSettings.allowGc());
            }
        });
        system.set("timeout", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(ModSettings.computerTimeout());
            }
        });
        globals.set("system", system);
    }

    private LuaTable createComponentList(final String filter, final boolean exact) {
        final LuaTable components = new LuaTable();
        final List<String> addresses = new ArrayList<>();
        if (machine != null) {
            for (Map.Entry<String, String> entry : machine.components().entrySet()) {
                if (matchesComponentFilter(entry.getValue(), filter, exact)) {
                    components.set(entry.getKey(), entry.getValue());
                    addresses.add(entry.getKey());
                }
            }
        }
        final LuaTable metatable = new LuaTable();
        metatable.set("__call", new VarArgFunction() {
            private int index;

            @Override
            public Varargs invoke(final Varargs args) {
                if (index >= addresses.size()) {
                    return LuaValue.NIL;
                }
                final String address = addresses.get(index++);
                return LuaValue.varargsOf(LuaValue.valueOf(address), components.get(address));
            }
        });
        components.setmetatable(metatable);
        return components;
    }

    private String firstComponentAddress(final String type) {
        if (machine == null) {
            return null;
        }
        final Map<String, String> components = machine.components();
        final String primary = primaryComponents.get(type);
        if (primary != null && type.equals(components.get(primary))) {
            return primary;
        }
        if (primary != null) {
            primaryComponents.remove(type);
        }
        for (Map.Entry<String, String> entry : components.entrySet()) {
            if (entry.getValue().equals(type)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean setPrimaryComponent(final String type, final String address) {
        if (machine == null) {
            return false;
        }
        if (!type.equals(machine.components().get(address))) {
            return false;
        }
        primaryComponents.put(type, address);
        return true;
    }

    private static boolean matchesComponentFilter(final String type, final String filter, final boolean exact) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        if (type == null) {
            return false;
        }
        return exact ? type.equals(filter) : type.contains(filter);
    }

    private LuaTable createComponentProxy(final String address) {
        final LuaTable cached = componentProxyCache.get(address);
        if (cached != null) {
            return cached;
        }
        final LuaTable proxy = new LuaTable();
        proxy.set("address", address);
        proxy.set("type", machine.components().get(address));
        final MachineHost host = machine.host();
        if (host != null) {
            proxy.set("slot", host.componentSlot(address));
        }
        proxy.set("fields", componentFields(address));
        final LuaTable metatable = new LuaTable();
        metatable.set("__index", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String method = args.arg(2).tojstring();
                final Callback callback = componentCallback(address, method);
                if (callback != null && callback.getter()) {
                    return invokeComponent(address, method, new Object[0]);
                }
                if (callback != null && callback.setter()) {
                    return LuaValue.NIL;
                }
                return new VarArgFunction() {
                    @Override
                    public Varargs invoke(final Varargs callbackArgs) {
                        final int offset = callbackArgs.narg() > 0 && callbackArgs.arg(1).eq_b(proxy) ? 1 : 0;
                        final Object[] javaArgs = new Object[Math.max(0, callbackArgs.narg() - offset)];
                        for (int index = 0; index < javaArgs.length; index++) {
                            javaArgs[index] = toJavaValue(callbackArgs.arg(index + offset + 1));
                        }
                        return invokeComponent(address, method, javaArgs);
                    }
                };
            }
        });
        metatable.set("__newindex", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final LuaValue key = args.arg(2);
                final Callback callback = componentCallback(address, key.tojstring());
                if (callback != null && callback.setter()) {
                    return invokeComponent(address, key.tojstring(), new Object[]{toJavaValue(args.arg(3))});
                }
                if (callback != null && callback.getter()) {
                    throw new LuaError("field is read-only");
                }
                proxy.rawset(key, args.arg(3));
                return LuaValue.NIL;
            }
        });
        proxy.setmetatable(metatable);
        componentProxyCache.put(address, proxy);
        return proxy;
    }

    private LuaTable componentFields(final String address) {
        final LuaTable fields = new LuaTable();
        if (machine == null) {
            return fields;
        }
        final Map<String, Callback> methods = machine.methods(address);
        if (methods == null) {
            return fields;
        }
        for (Map.Entry<String, Callback> entry : methods.entrySet()) {
            final Callback callback = entry.getValue();
            if (callback != null && (callback.getter() || callback.setter())) {
                fields.set(entry.getKey(), callbackMetadata(callback));
            }
        }
        return fields;
    }

    private static LuaTable callbackMetadata(final Callback callback) {
        final LuaTable metadata = new LuaTable();
        metadata.set("direct", LuaValue.valueOf(callback != null && callback.direct()));
        metadata.set("getter", LuaValue.valueOf(callback != null && callback.getter()));
        metadata.set("setter", LuaValue.valueOf(callback != null && callback.setter()));
        return metadata;
    }

    private ExecutionResult resumeBoot(final Varargs args) {
        final Varargs result = bootThread.resume(args);
        if (!result.arg1().toboolean()) {
            return new ExecutionResult.Error(result.arg(2).tojstring());
        }
        if (pendingResult != null) {
            return pendingResult;
        }
        if (waitingForSignal && PULL_SIGNAL_MARKER.equals(result.arg(2).tojstring())) {
            return sleepUntilSignalDeadline();
        }
        return new ExecutionResult.Sleep(1);
    }

    private ExecutionResult.Sleep sleepUntilSignalDeadline() {
        if (Double.isInfinite(signalDeadlineSeconds)) {
            return new ExecutionResult.Sleep(1);
        }
        final double remainingSeconds = Math.max(0D, signalDeadlineSeconds - machineUpTime());
        return new ExecutionResult.Sleep((int) Math.ceil(remainingSeconds * 20D));
    }

    private double machineUpTime() {
        return machine == null ? 0D : machine.upTime();
    }

    private Varargs signalToLuaValues(final Signal signal) {
        final Object[] signalArgs = signal.args();
        final LuaValue[] values = new LuaValue[signalArgs.length + 1];
        values[0] = LuaValue.valueOf(signal.name());
        for (int index = 0; index < signalArgs.length; index++) {
            values[index + 1] = toLuaValue(signalArgs[index]);
        }
        return LuaValue.varargsOf(values);
    }

    private LuaValue machineAddress() {
        if (machine == null || machine.node() == null || machine.node().address() == null) {
            return LuaValue.NIL;
        }
        return LuaValue.valueOf(machine.node().address());
    }

    private LuaValue machineTmpAddress() {
        if (machine == null || machine.tmpAddress() == null) {
            return LuaValue.NIL;
        }
        return LuaValue.valueOf(machine.tmpAddress());
    }

    private boolean hasComponent(final String address) {
        return machine != null && machine.components().containsKey(address);
    }

    private Callback componentCallback(final String address, final String method) {
        if (machine == null) {
            return null;
        }
        final Map<String, Callback> methods = machine.methods(address);
        return methods == null ? null : methods.get(method);
    }

    private static Varargs noSuchComponent() {
        return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such component"));
    }

    private Varargs invokeComponent(final String address, final String method, final Object[] javaArgs) {
        try {
            return invokeComponentOnce(address, method, javaArgs);
        } catch (LimitReachedException e) {
            pendingBudgetCall = () -> invokeComponentOnce(address, method, javaArgs);
            return globals.yield(LuaValue.valueOf(BUDGET_RETRY_MARKER));
        }
    }

    private Varargs invokeComponentOnce(final String address, final String method, final Object[] javaArgs) throws LimitReachedException {
        try {
            return toLuaValues(machine.invoke(address, method, javaArgs));
        } catch (LimitReachedException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(e.getMessage() == null ? "bad argument" : e.getMessage()));
        } catch (IndexOutOfBoundsException e) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("index out of bounds"));
        } catch (NoSuchMethodException e) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such method"));
        } catch (FileNotFoundException e) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("file not found"));
        } catch (SecurityException e) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("access denied"));
        } catch (IOException e) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("i/o error"));
        } catch (Exception e) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(e.getMessage() == null ? "unknown error" : e.getMessage()));
        }
    }

    private Varargs invokeValue(final Value value, final String method, final Object[] javaArgs) {
        try {
            return invokeValueOnce(value, method, javaArgs);
        } catch (LimitReachedException e) {
            pendingBudgetCall = () -> invokeValueOnce(value, method, javaArgs);
            return globals.yield(LuaValue.valueOf(BUDGET_RETRY_MARKER));
        }
    }

    private Varargs invokeValueOnce(final Value value, final String method, final Object[] javaArgs) throws LimitReachedException {
        try {
            return toLuaValues(machine.invoke(value, method, javaArgs));
        } catch (LimitReachedException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(e.getMessage() == null ? "bad argument" : e.getMessage()));
        } catch (IndexOutOfBoundsException e) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("index out of bounds"));
        } catch (NoSuchMethodException e) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such method"));
        } catch (SecurityException e) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("access denied"));
        } catch (Exception e) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(e.getMessage() == null ? "unknown error" : e.getMessage()));
        }
    }

    private Connector machineConnector() {
        if (machine == null || !(machine.node() instanceof Connector connector)) {
            return null;
        }
        return connector;
    }

    private LuaTable deviceInfo() {
        final LuaTable devices = new LuaTable();
        if (machine == null) {
            return devices;
        }
        if (machine.host() instanceof DeviceInfo hostInfo && machine.tmpAddress() != null) {
            addDeviceInfo(devices, machine.tmpAddress(), hostInfo.getDeviceInfo());
        }
        final Node machineNode = machine.node();
        if (machineNode == null || machineNode.network() == null) {
            return devices;
        }
        for (Node node : machineNode.reachableNodes()) {
            if (!(node.host() instanceof DeviceInfo nodeInfo)) {
                continue;
            }
            if (node instanceof Component component) {
                if (node != machineNode && !component.canBeSeenFrom(machineNode)) {
                    continue;
                }
            } else if (!node.canBeReachedFrom(machineNode)) {
                continue;
            }
            addDeviceInfo(devices, node.address(), nodeInfo.getDeviceInfo());
        }
        return devices;
    }

    private void addDeviceInfo(final LuaTable devices, final String address, final Map<String, String> info) {
        if (address != null && info != null) {
            devices.set(address, toLuaValue(info));
        }
    }

    private LuaTable programLocations() {
        final LuaTable locations = new LuaTable();
        int index = 1;
        for (ProgramLocations.Mapping mapping : ProgramLocations.mappings(currentArchitectureName())) {
            final LuaTable entry = new LuaTable();
            entry.set(1, mapping.program());
            entry.set(2, mapping.label());
            locations.set(index++, entry);
        }
        return locations;
    }

    private String currentArchitectureName() {
        final String registeredName = li.cil.oc.api.Machine.getArchitectureName(getClass());
        if (registeredName != null) {
            return registeredName;
        }
        final Architecture.Name name = getClass().getAnnotation(Architecture.Name.class);
        return name == null ? null : name.value();
    }

    private Processor processor() {
        final ProcessorCandidate candidate = processorCandidate();
        return candidate == null ? null : candidate.processor();
    }

    private ItemStack processorStack() {
        final ProcessorCandidate candidate = processorCandidate();
        return candidate == null ? null : candidate.stack();
    }

    private ProcessorCandidate processorCandidate() {
        if (machine == null || machine.host() == null) {
            return null;
        }
        for (ItemStack stack : machine.host().internalComponents()) {
            final DriverItem driver = Driver.driverFor(stack);
            if (driver instanceof Processor processor) {
                return new ProcessorCandidate(stack, processor);
            }
        }
        return null;
    }

    private double worldTimestamp() {
        return ((machine == null ? 0L : machine.worldTime()) + 6000L) * 60D * 60D / 1000D;
    }

    private static int intField(final LuaTable table, final String key, final int defaultValue) {
        final LuaValue value = table.get(key);
        if (value.isint()) {
            return value.toint();
        }
        if (defaultValue < 0) {
            throw new LuaError("field '" + key + "' missing in date table");
        }
        return defaultValue;
    }

    private LuaValue toLuaValue(final Object value) {
        if (value == null) {
            return LuaValue.NIL;
        }
        if (value instanceof Value machineValue) {
            return valueProxy(machineValue);
        }
        if (value instanceof Boolean booleanValue) {
            return LuaValue.valueOf(booleanValue);
        }
        if (value instanceof Number numberValue) {
            return LuaValue.valueOf(numberValue.doubleValue());
        }
        if (value instanceof byte[] bytes) {
            return LuaString.valueOf(bytes);
        }
        if (value instanceof Map<?, ?> mapValue) {
            final LuaTable table = new LuaTable();
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                final LuaValue key = toLuaValue(entry.getKey());
                if (!key.isnil()) {
                    table.set(key, toLuaValue(entry.getValue()));
                }
            }
            return table;
        }
        if (value instanceof Iterable<?> iterableValue) {
            final LuaTable table = new LuaTable();
            int index = 1;
            for (Object entry : iterableValue) {
                table.set(index++, toLuaValue(entry));
            }
            return table;
        }
        if (value.getClass().isArray()) {
            final LuaTable table = new LuaTable();
            final int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                table.set(index + 1, toLuaValue(Array.get(value, index)));
            }
            return table;
        }
        if (value instanceof CharSequence text) {
            return LuaValue.valueOf(text.toString());
        }
        return LuaValue.userdataOf(value);
    }

    private Varargs toLuaValues(final Object[] values) {
        if (values == null || values.length == 0) {
            return LuaValue.NIL;
        }
        final LuaValue[] luaValues = new LuaValue[values.length];
        for (int index = 0; index < values.length; index++) {
            luaValues[index] = toLuaValue(values[index]);
        }
        return LuaValue.varargsOf(luaValues);
    }

    private static Object[] toJavaArgs(final Varargs args, final int offset) {
        final Object[] javaArgs = new Object[Math.max(0, args.narg() - offset + 1)];
        for (int index = 0; index < javaArgs.length; index++) {
            javaArgs[index] = toJavaValue(args.arg(index + offset));
        }
        return javaArgs;
    }

    private static Value checkValue(final Varargs args, final int index) {
        final LuaValue arg = args.arg(index);
        if (arg instanceof LuaTable table) {
            final LuaValue marker = table.get(VALUE_MARKER);
            if (marker.isuserdata() && marker.touserdata() instanceof Value value) {
                return value;
            }
            throw new LuaError("bad argument #" + index + " (userdata expected)");
        }
        final Object userdata = args.checkuserdata(index, Value.class);
        return (Value) userdata;
    }

    private LuaTable valueProxy(final Value value) {
        final LuaTable table = new LuaTable();
        table.set(VALUE_MARKER, LuaValue.userdataOf(value));
        final Map<String, Callback> methods = machine == null ? Map.of() : machine.methods(value);
        for (String methodName : methods.keySet()) {
            table.set(methodName, new VarArgFunction() {
                @Override
                public Varargs invoke(final Varargs args) {
                    final int offset = args.narg() > 0 && args.arg(1) == table ? 2 : 1;
                    final Object[] javaArgs = new Object[Math.max(0, args.narg() - offset + 1)];
                    for (int index = 0; index < javaArgs.length; index++) {
                        javaArgs[index] = toJavaValue(args.arg(index + offset));
                    }
                    return invokeValue(value, methodName, javaArgs);
                }
            });
        }
        return table;
    }

    private static Object toJavaValue(final LuaValue value) {
        if (value.isnil()) {
            return null;
        }
        if (value.isboolean()) {
            return value.toboolean();
        }
        if (value.isnumber()) {
            return value.todouble();
        }
        if (value instanceof LuaString string) {
            if (string.isValidUtf8()) {
                return string.tojstring();
            }
            return Arrays.copyOfRange(string.m_bytes, string.m_offset, string.m_offset + string.m_length);
        }
        if (value instanceof LuaTable table) {
            final LuaValue rawValue = table.get(VALUE_MARKER);
            if (rawValue.isuserdata()) {
                return rawValue.touserdata();
            }
            final Map<Object, Object> values = new LinkedHashMap<>();
            LuaValue key = LuaValue.NIL;
            while (true) {
                final Varargs next = table.next(key);
                key = next.arg1();
                if (key.isnil()) {
                    break;
                }
                values.put(toJavaValue(key), toJavaValue(next.arg(2)));
            }
            return values;
        }
        if (value.isuserdata()) {
            return value.touserdata();
        }
        return value.tojstring();
    }

    private static String reverseUnicode(final String value) {
        final StringBuilder builder = new StringBuilder(value.length());
        for (int offset = value.length(); offset > 0; ) {
            final int codePoint = value.codePointBefore(offset);
            offset -= Character.charCount(codePoint);
            builder.appendCodePoint(codePoint);
        }
        return builder.toString();
    }

    private static String subUnicode(final String value, final int startIndex, final int endIndex) {
        final int codePointLength = value.codePointCount(0, value.length());
        final int start = startIndex < 0
            ? value.offsetByCodePoints(value.length(), Math.max(startIndex, -codePointLength))
            : startIndex == 0 ? 0 : value.offsetByCodePoints(0, Math.min(startIndex - 1, codePointLength));
        final int end = endIndex == Integer.MAX_VALUE
            ? value.length()
            : endIndex < 0
                ? value.offsetByCodePoints(value.length(), Math.max(endIndex + 1, -codePointLength))
                : value.offsetByCodePoints(0, Math.min(endIndex, codePointLength));
        return end <= start ? "" : value.substring(start, end);
    }

    private static int displayWidth(final String value) {
        int width = 0;
        for (int offset = 0; offset < value.length(); ) {
            final int codePoint = value.codePointAt(offset);
            width += Math.max(1, wcwidth(codePoint));
            offset += Character.charCount(codePoint);
        }
        return width;
    }

    private static String truncateDisplayWidth(final String value, final int count) {
        int width = 0;
        int previous = 0;
        int end = 0;
        while (width < count && end < value.length()) {
            previous = end;
            final int codePoint = value.codePointAt(end);
            width += Math.max(1, wcwidth(codePoint));
            end += Character.charCount(codePoint);
        }
        return previous > 0 ? value.substring(0, previous) : "";
    }

    private static int charWidth(final String value) {
        if (value.isEmpty()) {
            return 0;
        }
        return wcwidth(value.codePointAt(0));
    }

    private static int wcwidth(final int codePoint) {
        return isWideCodePoint(codePoint) ? 2 : 1;
    }

    private static boolean isWideCodePoint(final int codePoint) {
        return (codePoint >= 0x1100 && codePoint <= 0x115F)
            || (codePoint >= 0x2329 && codePoint <= 0x232A)
            || (codePoint >= 0x2E80 && codePoint <= 0xA4CF)
            || (codePoint >= 0xAC00 && codePoint <= 0xD7A3)
            || (codePoint >= 0xF900 && codePoint <= 0xFAFF)
            || (codePoint >= 0xFE10 && codePoint <= 0xFE19)
            || (codePoint >= 0xFE30 && codePoint <= 0xFE6F)
            || (codePoint >= 0xFF00 && codePoint <= 0xFF60)
            || (codePoint >= 0xFFE0 && codePoint <= 0xFFE6)
            || (codePoint >= 0x1F300 && codePoint <= 0x1FAFF)
            || (codePoint >= 0x20000 && codePoint <= 0x3FFFD);
    }

    private record ProcessorCandidate(ItemStack stack, Processor processor) {
    }

    private record LuaArguments(Object[] values) implements Arguments {
        @Override
        public int count() {
            return values.length;
        }

        @Override
        public Object checkAny(final int index) {
            if (index < 0 || index >= values.length) {
                throw new IllegalArgumentException("missing argument #" + (index + 1));
            }
            return values[index];
        }

        @Override
        public boolean checkBoolean(final int index) {
            return (Boolean) checkAny(index);
        }

        @Override
        public int checkInteger(final int index) {
            return ((Number) checkAny(index)).intValue();
        }

        @Override
        public long checkLong(final int index) {
            return ((Number) checkAny(index)).longValue();
        }

        @Override
        public double checkDouble(final int index) {
            return ((Number) checkAny(index)).doubleValue();
        }

        @Override
        public String checkString(final int index) {
            final Object value = checkAny(index);
            if (value instanceof String string) {
                return string;
            }
            if (value instanceof byte[] bytes) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (string expected)");
        }

        @Override
        public byte[] checkByteArray(final int index) {
            final Object value = checkAny(index);
            if (value instanceof byte[] bytes) {
                return bytes;
            }
            if (value instanceof String string) {
                return string.getBytes(StandardCharsets.UTF_8);
            }
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (byte array expected)");
        }

        @Override
        public Map checkTable(final int index) {
            return (Map) checkAny(index);
        }

        @Override
        public ItemStack checkItemStack(final int index) {
            return (ItemStack) checkAny(index);
        }

        @Override
        public Object optAny(final int index, final Object def) {
            return index >= 0 && index < values.length ? values[index] : def;
        }

        @Override
        public boolean optBoolean(final int index, final boolean def) {
            return index >= 0 && index < values.length ? checkBoolean(index) : def;
        }

        @Override
        public int optInteger(final int index, final int def) {
            return index >= 0 && index < values.length ? checkInteger(index) : def;
        }

        @Override
        public long optLong(final int index, final long def) {
            return index >= 0 && index < values.length ? checkLong(index) : def;
        }

        @Override
        public double optDouble(final int index, final double def) {
            return index >= 0 && index < values.length ? checkDouble(index) : def;
        }

        @Override
        public String optString(final int index, final String def) {
            return index >= 0 && index < values.length ? checkString(index) : def;
        }

        @Override
        public byte[] optByteArray(final int index, final byte[] def) {
            return index >= 0 && index < values.length ? checkByteArray(index) : def;
        }

        @Override
        public Map optTable(final int index, final Map def) {
            return index >= 0 && index < values.length ? checkTable(index) : def;
        }

        @Override
        public ItemStack optItemStack(final int index, final ItemStack def) {
            return index >= 0 && index < values.length ? checkItemStack(index) : def;
        }

        @Override
        public boolean isBoolean(final int index) {
            return index >= 0 && index < values.length && values[index] instanceof Boolean;
        }

        @Override
        public boolean isInteger(final int index) {
            return index >= 0 && index < values.length && values[index] instanceof Integer;
        }

        @Override
        public boolean isLong(final int index) {
            return index >= 0 && index < values.length && values[index] instanceof Long;
        }

        @Override
        public boolean isDouble(final int index) {
            return index >= 0 && index < values.length && values[index] instanceof Double;
        }

        @Override
        public boolean isString(final int index) {
            return index >= 0 && index < values.length && (values[index] instanceof String || values[index] instanceof byte[]);
        }

        @Override
        public boolean isByteArray(final int index) {
            return index >= 0 && index < values.length && values[index] instanceof byte[];
        }

        @Override
        public boolean isTable(final int index) {
            return index >= 0 && index < values.length && values[index] instanceof Map;
        }

        @Override
        public boolean isItemStack(final int index) {
            return index >= 0 && index < values.length && values[index] instanceof ItemStack;
        }

        @Override
        public Object[] toArray() {
            return Arrays.copyOf(values, values.length);
        }

        @Override
        public java.util.Iterator<Object> iterator() {
            return Arrays.asList(values).iterator();
        }
    }

    @FunctionalInterface
    private interface PendingBudgetCall {
        Varargs invoke() throws LimitReachedException;
    }
}
