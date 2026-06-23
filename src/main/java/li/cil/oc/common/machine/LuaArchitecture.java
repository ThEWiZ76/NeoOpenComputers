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
import li.cil.oc.common.util.FontWidths;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
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
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
    private static final String SYNCHRONIZED_CALLBACK_MARKER = "\u0000oc.synchronizedCallback";
    private static final double PRIMARY_REPLACEMENT_DELAY_SECONDS = 0.1D;
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
    private PendingBudgetCall pendingSynchronizedCall;
    private Varargs pendingSynchronizedResults;
    private double memoryBytes;
    private boolean waitingForSignal;
    private boolean waitingForSleep;
    private double signalDeadlineSeconds;
    private final LongSupplier wallTimeMillis;
    private final Map<String, String> primaryComponents = new HashMap<>();
    private final Map<String, PendingPrimaryComponent> pendingPrimaryComponents = new HashMap<>();
    private final Map<String, LuaTable> componentProxyCache = new HashMap<>();
    private final Map<Value, LuaValue> valueProxyCache = new IdentityHashMap<>();
    private final Map<LuaTable, Value> valueProxyValues = new IdentityHashMap<>();
    private final LuaValue synchronizedErrorMarker = LuaValue.userdataOf(new Object());

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
        primaryComponents.clear();
        pendingPrimaryComponents.clear();
        globals = sandboxGlobals();
        pendingResult = null;
        pendingBudgetCall = null;
        pendingSynchronizedCall = null;
        pendingSynchronizedResults = null;
        valueProxyCache.clear();
        valueProxyValues.clear();
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
        pendingSynchronizedCall = null;
        pendingSynchronizedResults = null;
        waitingForSignal = false;
        waitingForSleep = false;
        signalDeadlineSeconds = 0D;
        primaryComponents.clear();
        pendingPrimaryComponents.clear();
        componentProxyCache.clear();
    }

    @Override
    public void runSynchronized() {
        if (pendingSynchronizedCall == null) {
            return;
        }
        try {
            pendingSynchronizedResults = pendingSynchronizedCall.invoke();
        } catch (LimitReachedException e) {
            pendingSynchronizedResults = LuaValue.NONE;
        } catch (LuaError e) {
            pendingSynchronizedResults = LuaValue.varargsOf(
                synchronizedErrorMarker,
                LuaValue.valueOf(e.getMessage() == null ? "unknown error" : e.getMessage()));
        } finally {
            pendingSynchronizedCall = null;
        }
    }

    @Override
    public ExecutionResult runThreaded(final boolean isSynchronizedReturn) {
        if (!initialized) {
            return new ExecutionResult.Error("Lua architecture is not initialized");
        }
        processPendingPrimaryComponents();
        if (pendingSynchronizedResults != null) {
            final Varargs results = pendingSynchronizedResults;
            pendingSynchronizedResults = null;
            return resumeBoot(results);
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
                if (waitingForSleep) {
                    if (machineUpTime() < signalDeadlineSeconds) {
                        return sleepUntilSignalDeadline();
                    }
                    waitingForSignal = false;
                    waitingForSleep = false;
                    return resumeBoot(LuaValue.NONE);
                }
                waitingForSignal = false;
                waitingForSleep = false;
                return resumeBoot(signalToLuaValues(signal));
            }
            if (machineUpTime() >= signalDeadlineSeconds) {
                waitingForSignal = false;
                waitingForSleep = false;
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
        globals.load(new DebugLib());
        globals.set("package", LuaValue.NIL);
        globals.set("dofile", LuaValue.NIL);
        globals.set("loadfile", LuaValue.NIL);
        globals.set("print", LuaValue.NIL);
        globals.set("collectgarbage", LuaValue.NIL);
        globals.set("_VERSION", LuaValue.valueOf("Luaj"));
        installCheckArg(globals);
        installDebugLibrary(globals);
        installCoroutineCompatibility(globals);
        installGetMetatableCompatibility(globals);
        installLoadCompatibility(globals);
        installMathCompatibility(globals);
        installPairsCompatibility(globals);
        installStringCompatibility(globals);
        installXpcallCompatibility(globals);
        LoadState.install(globals);
        LuaC.install(globals);
        return globals;
    }

    private static void installGetMetatableCompatibility(final Globals globals) {
        final LuaValue originalGetMetatable = globals.get("getmetatable");
        globals.set("getmetatable", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (args.arg(1).isstring()) {
                    return LuaValue.NIL;
                }
                return originalGetMetatable.invoke(args);
            }
        });
    }

    private static void installCoroutineCompatibility(final Globals globals) {
        final LuaValue coroutine = globals.get("coroutine");
        final LuaValue originalCreate = coroutine.get("create");
        final LuaValue originalResume = coroutine.get("resume");
        final LuaValue originalYield = coroutine.get("yield");
        coroutine.set("resume", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final LuaValue thread = args.arg(1);
                if (thread.type() != LuaValue.TTHREAD) {
                    throw new LuaError("bad argument #1 (thread expected, got " + luaTypeName(thread) + ")");
                }
                final LuaThread coroutineThread = thread.checkthread();
                Varargs resumeArgs = args;
                while (true) {
                    final Varargs result = originalResume.invoke(resumeArgs);
                    if (!result.arg1().toboolean() || coroutineThread.state.status == LuaThread.STATUS_DEAD) {
                        return result;
                    }
                    if (result.arg(2).isnil()) {
                        return LuaValue.varargsOf(LuaValue.TRUE, result.subargs(3));
                    }
                    resumeArgs = LuaValue.varargsOf(thread, globals.yield(result.arg(2)));
                }
            }
        });
        coroutine.set("yield", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return originalYield.invoke(LuaValue.varargsOf(LuaValue.NIL, args));
            }
        });
        coroutine.set("wrap", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final LuaValue thread = originalCreate.call(args.checkfunction(1));
                return new VarArgFunction() {
                    @Override
                    public Varargs invoke(final Varargs args) {
                        final Varargs result = coroutine.get("resume").invoke(LuaValue.varargsOf(thread, args));
                        if (result.arg1().toboolean()) {
                            return result.subargs(2);
                        }
                        throw new LuaError(result.arg(2).tojstring());
                    }
                };
            }
        });
    }

    private static void installMathCompatibility(final Globals globals) {
        final LuaValue math = globals.get("math");
        final LuaValue originalRandomseed = math.get("randomseed");
        math.set("randomseed", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                originalRandomseed.call(LuaValue.valueOf(Math.floor(args.checkdouble(1))));
                return LuaValue.NONE;
            }
        });
    }

    private static void installLoadCompatibility(final Globals globals) {
        final LuaValue originalLoad = globals.get("load");
        globals.set("load", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final LuaValue chunk = args.arg(1);
                if (!ModSettings.allowBytecode()) {
                    if (chunk instanceof LuaString string && isLuaBytecode(string)) {
                        return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("attempt to load a binary chunk"));
                    }
                    if (chunk.isfunction()) {
                        final Varargs readerResult = textFromLoadReader(chunk);
                        if (readerResult.narg() > 1) {
                            return readerResult;
                        }
                        return originalLoad.invoke(textModeLoadArgs(args, readerResult.arg1(), globals));
                    }
                    return originalLoad.invoke(textModeLoadArgs(args, globals));
                }
                return originalLoad.invoke(loadArgsWithDefaultEnv(args, globals));
            }
        });
    }

    private static Varargs textModeLoadArgs(final Varargs args, final Globals globals) {
        return textModeLoadArgs(args, args.arg(1), globals);
    }

    private static Varargs textModeLoadArgs(final Varargs args, final LuaValue chunk, final Globals globals) {
        final LuaValue[] values = loadArgsWithDefaultEnvValues(args, globals);
        values[0] = chunk;
        values[2] = LuaValue.valueOf("t");
        return LuaValue.varargsOf(values);
    }

    private static Varargs loadArgsWithDefaultEnv(final Varargs args, final Globals globals) {
        return LuaValue.varargsOf(loadArgsWithDefaultEnvValues(args, globals));
    }

    private static LuaValue[] loadArgsWithDefaultEnvValues(final Varargs args, final Globals globals) {
        final int count = Math.max(4, args.narg());
        final LuaValue[] values = new LuaValue[count];
        for (int index = 0; index < count; index++) {
            values[index] = args.arg(index + 1);
        }
        if (args.narg() < 4 || args.arg(4).isnil()) {
            values[3] = globals;
        }
        return values;
    }

    private static Varargs textFromLoadReader(final LuaValue reader) {
        final StringBuilder text = new StringBuilder();
        while (true) {
            final LuaValue chunk = reader.invoke().arg1();
            if (chunk.isnil()) {
                return LuaValue.valueOf(text.toString());
            }
            if (chunk instanceof LuaString string) {
                if (text.isEmpty() && isLuaBytecode(string)) {
                    return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("attempt to load a binary chunk"));
                }
                text.append(string.tojstring());
            } else {
                return chunk;
            }
        }
    }

    private static void installXpcallCompatibility(final Globals globals) {
        final LuaValue originalXpcall = globals.get("xpcall");
        globals.set("xpcall", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final LuaValue messageHandler = args.arg(2);
                if (messageHandler.type() != LuaValue.TFUNCTION) {
                    throw new LuaError("bad argument #2 (function expected, got " + luaTypeName(messageHandler) + ")");
                }
                return originalXpcall.invoke(args);
            }
        });
    }

    private static boolean isLuaBytecode(final LuaString value) {
        return value.m_length > 0 && value.m_bytes[value.m_offset] == 0x1B;
    }

    private static void installPairsCompatibility(final Globals globals) {
        final LuaValue originalPairs = globals.get("pairs");
        globals.set("pairs", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final LuaTable table = args.checktable(1);
                final LuaValue metatable = table.getmetatable();
                if (metatable != null && !metatable.isnil()) {
                    final LuaValue pairs = metatable.get("__pairs");
                    if (!pairs.isnil()) {
                        return pairs.invoke(table);
                    }
                }
                return originalPairs.invoke(args);
            }
        });
    }

    private static void installDebugLibrary(final Globals globals) {
        final LuaValue fullDebug = globals.get("debug");
        final LuaTable debug = new LuaTable();
        final LuaValue fullGetInfo = fullDebug.get("getinfo");
        debug.set("getinfo", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final LuaValue info = fullGetInfo.invoke(args).arg1();
                if (info.isnil()) {
                    return LuaValue.NIL;
                }
                final LuaTable safe = new LuaTable();
                copyDebugInfoField(info, safe, "source");
                copyDebugInfoField(info, safe, "short_src");
                copyDebugInfoField(info, safe, "linedefined");
                copyDebugInfoField(info, safe, "lastlinedefined");
                copyDebugInfoField(info, safe, "what");
                copyDebugInfoField(info, safe, "currentline");
                copyDebugInfoField(info, safe, "nups");
                copyDebugInfoField(info, safe, "nparams");
                copyDebugInfoField(info, safe, "isvararg");
                copyDebugInfoField(info, safe, "name");
                copyDebugInfoField(info, safe, "namewhat");
                copyDebugInfoField(info, safe, "istailcall");
                return safe;
            }
        });
        debug.set("traceback", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return fullDebug.get("traceback").invoke(args);
            }
        });
        final LuaValue fullGetLocal = fullDebug.get("getlocal");
        debug.set("getlocal", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return debugNameOnly(fullGetLocal.invoke(args));
            }
        });
        final LuaValue fullGetUpvalue = fullDebug.get("getupvalue");
        debug.set("getupvalue", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return debugNameOnly(fullGetUpvalue.invoke(args));
            }
        });
        globals.set("debug", debug);
    }

    private static Varargs debugNameOnly(final Varargs result) {
        final LuaValue name = result.arg1();
        return name.isnil() ? LuaValue.NIL : name;
    }

    private static void copyDebugInfoField(final LuaValue source, final LuaTable target, final String key) {
        final LuaValue value = source.get(key);
        if (value.isstring() || value.isnumber() || value.isboolean()) {
            target.set(key, value);
        }
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

    private static String checkStringArgument(final Varargs args, final int index) {
        final LuaValue value = args.arg(index);
        if (value.type() == LuaValue.TSTRING) {
            return value.tojstring();
        }
        throw new LuaError("bad argument #" + index + " (string expected, got " + luaTypeName(value) + ")");
    }

    private static String checkOptionalStringArgument(final Varargs args, final int index) {
        final LuaValue value = args.arg(index);
        if (value.isnil()) {
            return null;
        }
        if (value.type() == LuaValue.TSTRING) {
            return value.tojstring();
        }
        throw new LuaError("bad argument #" + index + " (string or nil expected, got " + luaTypeName(value) + ")");
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
                if (ModSettings.ignorePower()) {
                    return LuaValue.valueOf(Double.POSITIVE_INFINITY);
                }
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
                bootAddress = checkOptionalStringArgument(args, 1);
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
                return globals.yield(LuaValue.NIL);
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
                return LuaValue.NONE;
            }
        });
        computer.set("users", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null) {
                    return LuaValue.NONE;
                }
                final String[] names = machine.users();
                final LuaValue[] users = new LuaValue[names.length];
                for (int index = 0; index < names.length; index++) {
                    users[index] = LuaValue.valueOf(names[index]);
                }
                return LuaValue.varargsOf(users);
            }
        });
        computer.set("addUser", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String name = checkStringArgument(args, 1);
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
                final String name = checkStringArgument(args, 1);
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
                waitingForSleep = false;
                signalDeadlineSeconds = Double.isInfinite(timeout) ? Double.POSITIVE_INFINITY : machineUpTime() + timeout;
                return globals.yield(LuaValue.valueOf(PULL_SIGNAL_MARKER));
            }
        });
        computer.set("pushSignal", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String signalName = checkStringArgument(args, 1);
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
                final String requestedName = checkStringArgument(args, 1);
                final Processor processor = processor();
                if (!(processor instanceof MutableProcessor mutableProcessor)) {
                    return LuaValue.NIL;
                }
                final ItemStack stack = processorStack();
                for (Class<? extends Architecture> architecture : mutableProcessor.allArchitectures()) {
                    if (requestedName.equals(li.cil.oc.api.Machine.getArchitectureName(architecture))) {
                        if (architecture != mutableProcessor.architecture(stack)) {
                            mutableProcessor.setArchitecture(stack, architecture);
                            pendingResult = new ExecutionResult.Shutdown(true);
                            return globals.yield(LuaValue.NIL);
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
                final boolean exact = args.arg(2).isboolean() && args.arg(2).toboolean();
                return createComponentList(filter, exact);
            }
        });
        component.set("get", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String prefix = checkStringArgument(args, 1);
                final String type = checkOptionalStringArgument(args, 2);
                if (machine == null) {
                    return LuaValue.NIL;
                }
                final String address = componentAddressByPrefix(prefix, type);
                if (address != null) {
                    return LuaValue.valueOf(address);
                }
                return noSuchComponent();
            }
        });
        component.set("type", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String address = checkStringArgument(args, 1);
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
                final String type = checkStringArgument(args, 1);
                if (machine == null) {
                    return LuaValue.FALSE;
                }
                return LuaValue.valueOf(ensurePrimaryAvailable(type));
            }
        });
        component.set("isPrimary", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String address = checkStringArgument(args, 1);
                if (machine == null) {
                    return LuaValue.FALSE;
                }
                final String type = machine.components().get(address);
                return LuaValue.valueOf(type != null && ensurePrimaryAvailable(type) && address.equals(primaryComponents.get(type)));
            }
        });
        component.set("slot", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String address = checkStringArgument(args, 1);
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
                final String type = checkStringArgument(args, 1);
                if (machine == null) {
                    return LuaValue.NIL;
                }
                if (!ensurePrimaryAvailable(type)) {
                    throw new LuaError("no primary '" + type + "' available");
                }
                final String address = primaryComponents.get(type);
                return createComponentProxy(address);
            }
        });
        component.set("setPrimary", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String type = checkStringArgument(args, 1);
                if (machine == null) {
                    return LuaValue.FALSE;
                }
                final String requestedAddress = checkOptionalStringArgument(args, 2);
                if (requestedAddress == null) {
                    clearPrimaryComponent(type);
                    return LuaValue.NIL;
                }
                final String address = componentAddressByPrefix(requestedAddress, type);
                if (address == null) {
                    throw new LuaError("no such component");
                }
                setPrimaryComponent(type, address);
                return LuaValue.NIL;
            }
        });
        component.set("methods", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String address = checkStringArgument(args, 1);
                final LuaTable methods = new LuaTable();
                if (machine != null) {
                    if (!hasComponent(address)) {
                        return noSuchComponent();
                    }
                    for (Map.Entry<String, Callback> entry : machine.methods(address).entrySet()) {
                        final Callback callback = entry.getValue();
                        if (callback != null && !callback.getter() && !callback.setter()) {
                            methods.set(entry.getKey(), LuaValue.valueOf(callback.direct()));
                        }
                    }
                }
                return methods;
            }
        });
        component.set("fields", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String address = checkStringArgument(args, 1);
                final LuaTable fields = new LuaTable();
                if (machine != null) {
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
                final String address = checkStringArgument(args, 1);
                if (machine == null) {
                    return LuaValue.NIL;
                }
                if (!hasComponent(address)) {
                    return noSuchComponent();
                }
                final String method = checkStringArgument(args, 2);
                final Callback callback = machine.methods(address).get(method);
                if (callback == null) {
                    return LuaValue.NIL;
                }
                if (callback.doc().isEmpty()) {
                    return LuaValue.NIL;
                }
                return LuaValue.valueOf(callback.doc());
            }
        });
        component.set("invoke", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String address = checkStringArgument(args, 1);
                final String method = checkStringArgument(args, 2);
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
                final String address = checkStringArgument(args, 1);
                if (machine == null) {
                    return LuaValue.NIL;
                }
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
                final String type = checkStringArgument(args, 2);
                if (!ensurePrimaryAvailable(type)) {
                    throw new LuaError("no primary '" + type + "' available");
                }
                return createComponentProxy(primaryComponents.get(type));
            }
        });
        metatable.set("__pairs", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return new VarArgFunction() {
                    private LuaValue componentKey = LuaValue.NIL;
                    private List<String> primaryTypes;
                    private int primaryIndex;

                    @Override
                    public Varargs invoke(final Varargs iteratorArgs) {
                        while (primaryTypes == null) {
                            final Varargs next = component.next(componentKey);
                            componentKey = next.arg1();
                            if (componentKey.isnil()) {
                                processPendingPrimaryComponents();
                                primaryTypes = new ArrayList<>(primaryComponents.keySet());
                                break;
                            }
                            return next;
                        }
                        while (primaryIndex < primaryTypes.size()) {
                            final String type = primaryTypes.get(primaryIndex++);
                            final String address = primaryComponents.get(type);
                            if (address != null && machine != null && type.equals(machine.components().get(address))) {
                                return LuaValue.varargsOf(LuaValue.valueOf(type), createComponentProxy(address));
                            }
                        }
                        return LuaValue.NIL;
                    }
                };
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
                final LuaValue value = args.arg(1);
                if (value.isnil()) {
                    return LuaValue.valueOf(worldTimestamp());
                }
                if (!value.istable()) {
                    throw new LuaError("bad argument #1 (table or nil expected, got " + luaTypeName(value) + ")");
                }
                final LuaTable time = value.checktable();
                final int second = intField(time, "sec", 0);
                final int minute = intField(time, "min", 0);
                final int hour = intField(time, "hour", 12);
                final int day = intField(time, "day", -1);
                final int month = intField(time, "month", -1);
                final int year = intField(time, "year", -1);
                final Long timestamp = GameTimeFormatter.mktime(year, month, day, hour, minute, second);
                return timestamp == null ? LuaValue.NIL : LuaValue.valueOf(timestamp);
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
        os.set("difftime", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return LuaValue.valueOf(args.checkdouble(1) - args.checkdouble(2));
            }
        });
        os.set("sleep", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null) {
                    return LuaValue.NIL;
                }
                if (args.narg() >= 1 && !args.arg(1).isnil() && !args.arg(1).isnumber()) {
                    throw new LuaError("bad argument #1 (number or nil expected, got " + luaTypeName(args.arg(1)) + ")");
                }
                final double timeout = args.narg() >= 1 && args.arg(1).isnumber() ? Math.max(0D, args.arg(1).todouble()) : 0D;
                waitingForSignal = true;
                waitingForSleep = true;
                signalDeadlineSeconds = machineUpTime() + timeout;
                return globals.yield(LuaValue.valueOf(PULL_SIGNAL_MARKER));
            }
        });
        globals.set("os", os);
    }

    private void installUserdataLibrary() {
        final LuaTable userdata = new LuaTable();
        userdata.set("save", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                try {
                    final Value value = checkValue(args, 1);
                    final CompoundTag data = new CompoundTag();
                    value.save(data);
                    return LuaValue.varargsOf(new LuaValue[]{
                        LuaValue.valueOf(value.getClass().getName()),
                        LuaString.valueOf(writeCompressed(data))
                    });
                } catch (IOException e) {
                    throw new LuaError(e.toString());
                }
            }
        });
        userdata.set("load", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                try {
                    final Value value = instantiateValue(args.checkjstring(1));
                    value.load(readCompressed(args.checkstring(2)));
                    return toLuaValue(value);
                } catch (ReflectiveOperationException | IOException | ClassCastException e) {
                    throw new LuaError(e.toString());
                }
            }
        });
        userdata.set("apply", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final Value value = checkValue(args, 1);
                return applyValueSynchronized(value, toJavaArgs(args, 2));
            }
        });
        userdata.set("unapply", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final Value value = checkValue(args, 1);
                return unapplyValueSynchronized(value, toJavaArgs(args, 2));
            }
        });
        userdata.set("call", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final Value value = checkValue(args, 1);
                return callValueSynchronized(value, toJavaArgs(args, 2));
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
                return LuaValue.NONE;
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
                return invokeValueSynchronized(value, method, toJavaArgs(args, 3));
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
                if (callback == null) {
                    return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("key not found: " + method));
                }
                return callback.doc().isEmpty() ? LuaValue.NIL : LuaValue.valueOf(callback.doc());
            }
        });
        globals.set("userdata", userdata);
    }

    private static byte[] writeCompressed(final CompoundTag data) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        NbtIo.writeCompressed(data, output);
        return output.toByteArray();
    }

    private static CompoundTag readCompressed(final LuaString string) throws IOException {
        final byte[] bytes = Arrays.copyOfRange(string.m_bytes, string.m_offset, string.m_offset + string.m_length);
        return NbtIo.readCompressed(new ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap());
    }

    private static Value instantiateValue(final String className) throws ReflectiveOperationException {
        final Class<?> type = Class.forName(className);
        if (!Value.class.isAssignableFrom(type)) {
            throw new ClassCastException(className + " is not a Value");
        }
        final Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return (Value) constructor.newInstance();
    }

    private void installUnicodeLibrary() {
        final LuaTable unicode = new LuaTable();
        unicode.set("lower", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return LuaValue.valueOf(args.checkjstring(1).toLowerCase());
            }
        });
        unicode.set("upper", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return LuaValue.valueOf(args.checkjstring(1).toUpperCase());
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
        if (machine != null) {
            for (Map.Entry<String, String> entry : machine.components().entrySet()) {
                if (matchesComponentFilter(entry.getValue(), filter, exact)) {
                    components.set(entry.getKey(), entry.getValue());
                }
            }
        }
        final LuaTable metatable = new LuaTable();
        metatable.set("__call", new VarArgFunction() {
            private LuaValue key = LuaValue.NIL;

            @Override
            public Varargs invoke(final Varargs args) {
                final Varargs next = components.next(key);
                key = next.arg1();
                if (key.isnil()) {
                    return LuaValue.NIL;
                }
                return next;
            }
        });
        components.setmetatable(metatable);
        return components;
    }

    private String firstComponentAddress(final String type) {
        if (machine == null) {
            return null;
        }
        processPendingPrimaryComponents();
        if (pendingPrimaryComponents.containsKey(type)) {
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
        return firstAvailableComponentAddress(type);
    }

    private boolean ensurePrimaryAvailable(final String type) {
        if (machine == null) {
            return false;
        }
        processPendingPrimaryComponents();
        if (!primaryComponents.containsKey(type) && !pendingPrimaryComponents.containsKey(type)) {
            final String address = firstAvailableComponentAddress(type);
            if (address != null) {
                setPrimaryComponent(type, address);
            }
        }
        return primaryComponents.containsKey(type);
    }

    private String firstAvailableComponentAddress(final String type) {
        if (machine == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : machine.components().entrySet()) {
            if (entry.getValue().equals(type)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private long componentCount(final String type) {
        if (machine == null) {
            return 0;
        }
        return machine.components().values().stream().filter(type::equals).count();
    }

    private String currentOrPendingPrimaryAddress(final String type) {
        final String primary = primaryComponents.get(type);
        if (primary != null) {
            return primary;
        }
        final PendingPrimaryComponent pending = pendingPrimaryComponents.get(type);
        return pending == null ? null : pending.address();
    }

    private List<String> componentKeyboardAddresses(final String address) {
        if (machine == null || address == null) {
            return List.of();
        }
        try {
            final Object[] results = machine.invoke(address, "getKeyboards", new Object[0]);
            final List<String> keyboards = new ArrayList<>();
            if (results != null) {
                for (Object result : results) {
                    if (result instanceof String keyboard) {
                        keyboards.add(keyboard);
                    }
                }
            }
            return keyboards;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String componentAddressByPrefix(final String prefix, final String type) {
        if (machine == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : machine.components().entrySet()) {
            if (entry.getKey().startsWith(prefix) && (type == null || type.equals(entry.getValue()))) {
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
        final String previous = primaryComponents.get(type);
        if (address.equals(previous)) {
            return false;
        }
        final PendingPrimaryComponent pending = pendingPrimaryComponents.get(type);
        if (pending != null && address.equals(pending.address())) {
            return false;
        }
        if (previous != null) {
            machine.signal("component_unavailable", type);
        }
        primaryComponents.remove(type);
        pendingPrimaryComponents.remove(type);
        if (previous != null || pending != null) {
            pendingPrimaryComponents.put(type, new PendingPrimaryComponent(address, machineUpTime() + PRIMARY_REPLACEMENT_DELAY_SECONDS));
        } else {
            primaryComponents.put(type, address);
            machine.signal("component_available", type);
        }
        return true;
    }

    private void clearPrimaryComponent(final String type) {
        if (machine == null) {
            return;
        }
        pendingPrimaryComponents.remove(type);
        if (primaryComponents.remove(type) != null) {
            machine.signal("component_unavailable", type);
        }
    }

    private static boolean matchesComponentFilter(final String type, final String filter, final boolean exact) {
        if (filter == null) {
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
        final LuaTable fields = componentFields(address);
        proxy.set("fields", fields);
        if (machine != null) {
            final Map<String, Callback> methods = machine.methods(address);
            if (methods != null) {
                for (Map.Entry<String, Callback> entry : methods.entrySet()) {
                    final Callback callback = entry.getValue();
                    if (callback != null && !callback.getter() && !callback.setter()) {
                        proxy.set(entry.getKey(), componentProxyFunction(address, entry.getKey(), proxy));
                    }
                }
            }
        }
        final LuaTable metatable = new LuaTable();
        metatable.set("__index", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String method = args.arg(2).tojstring();
                final LuaValue field = fields.get(method);
                final Callback callback = componentCallback(address, method);
                if (!field.isnil() && field.get("getter").toboolean()) {
                    if (callback == null) {
                        throw new LuaError("no such method");
                    }
                    return invokeComponent(address, method, new Object[0]);
                }
                if (!field.isnil() && field.get("setter").toboolean()) {
                    return LuaValue.NIL;
                }
                if (callback == null) {
                    return LuaValue.NIL;
                }
                return componentProxyFunction(address, method, proxy);
            }
        });
        metatable.set("__newindex", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final LuaValue key = args.arg(2);
                final String method = key.tojstring();
                final LuaValue field = fields.get(method);
                final Callback callback = componentCallback(address, method);
                if (!field.isnil() && field.get("setter").toboolean()) {
                    if (callback == null) {
                        throw new LuaError("no such method");
                    }
                    return invokeComponent(address, method, new Object[]{toJavaValue(args.arg(3))});
                }
                if (!field.isnil() && field.get("getter").toboolean()) {
                    throw new LuaError("field is read-only");
                }
                proxy.rawset(key, args.arg(3));
                return LuaValue.NIL;
            }
        });
        metatable.set("__pairs", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return new VarArgFunction() {
                    private LuaValue proxyKey = LuaValue.NIL;
                    private LuaValue fieldKey = LuaValue.NIL;
                    private boolean fieldsStarted;

                    @Override
                    public Varargs invoke(final Varargs iteratorArgs) {
                        while (!fieldsStarted) {
                            final Varargs next = proxy.next(proxyKey);
                            proxyKey = next.arg1();
                            if (proxyKey.isnil()) {
                                fieldsStarted = true;
                                break;
                            }
                            if (!"fields".equals(proxyKey.tojstring())) {
                                return next;
                            }
                        }
                        final Varargs next = fields.next(fieldKey);
                        fieldKey = next.arg1();
                        return next;
                    }
                };
            }
        });
        proxy.setmetatable(metatable);
        componentProxyCache.put(address, proxy);
        return proxy;
    }

    private LuaTable componentProxyFunction(final String address, final String method, final LuaTable proxy) {
        final LuaTable callback = new LuaTable();
        callback.set("address", address);
        callback.set("name", method);
        final LuaTable metatable = new LuaTable();
        metatable.set("__call", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs callbackArgs) {
                if (componentCallback(address, method) == null) {
                    throw new LuaError("no such method");
                }
                int offset = callbackArgs.narg() > 0 && callbackArgs.arg(1).eq_b(callback) ? 1 : 0;
                if (callbackArgs.narg() > offset && callbackArgs.arg(offset + 1).eq_b(proxy)) {
                    offset++;
                }
                final Object[] javaArgs = new Object[Math.max(0, callbackArgs.narg() - offset)];
                for (int index = 0; index < javaArgs.length; index++) {
                    javaArgs[index] = toJavaValue(callbackArgs.arg(index + offset + 1));
                }
                return invokeComponent(address, method, javaArgs);
            }
        });
        metatable.set("__tostring", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                final Callback callback = componentCallback(address, method);
                if (callback == null) {
                    throw new LuaError("no such method");
                }
                final String doc = callback.doc();
                return LuaValue.valueOf(doc == null || doc.isEmpty() ? "function" : doc);
            }
        });
        callback.setmetatable(metatable);
        return callback;
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
        if (SYNCHRONIZED_CALLBACK_MARKER.equals(result.arg(2).tojstring())) {
            return new ExecutionResult.Sleep(1);
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

    private void processPendingPrimaryComponents() {
        if (machine == null || pendingPrimaryComponents.isEmpty()) {
            return;
        }
        final double now = machineUpTime();
        final List<String> readyTypes = new ArrayList<>();
        for (Map.Entry<String, PendingPrimaryComponent> entry : pendingPrimaryComponents.entrySet()) {
            if (now + 1.0e-9D >= entry.getValue().readyAtSeconds()) {
                readyTypes.add(entry.getKey());
            }
        }
        for (String type : readyTypes) {
            final PendingPrimaryComponent pending = pendingPrimaryComponents.remove(type);
            if (pending != null && type.equals(machine.components().get(pending.address()))) {
                primaryComponents.put(type, pending.address());
                machine.signal("component_available", type);
            }
        }
    }

    private Varargs signalToLuaValues(final Signal signal) {
        processComponentSignal(signal);
        final Object[] signalArgs = signal.args();
        final LuaValue[] values = new LuaValue[signalArgs.length + 1];
        values[0] = LuaValue.valueOf(signal.name());
        for (int index = 0; index < signalArgs.length; index++) {
            values[index + 1] = toLuaValue(signalArgs[index]);
        }
        return LuaValue.varargsOf(values);
    }

    private void processComponentSignal(final Signal signal) {
        if (machine == null || signal == null) {
            return;
        }
        final Object[] args = signal.args();
        if (args.length < 2 || !(args[0] instanceof String address) || !(args[1] instanceof String type)) {
            return;
        }
        if ("component_added".equals(signal.name())) {
            if (!type.equals(machine.components().get(address))) {
                return;
            }
            boolean shouldSelect = !primaryComponents.containsKey(type) && !pendingPrimaryComponents.containsKey(type) && componentCount(type) == 1;
            final String previous = currentOrPendingPrimaryAddress(type);
            if (previous != null && "screen".equals(type)) {
                final List<String> previousKeyboards = componentKeyboardAddresses(previous);
                final List<String> addedKeyboards = componentKeyboardAddresses(address);
                if (previousKeyboards.isEmpty() && !addedKeyboards.isEmpty()) {
                    setPrimaryComponent("keyboard", addedKeyboards.getFirst());
                    shouldSelect = true;
                }
            } else if (previous != null && "keyboard".equals(type) && !address.equals(previous)) {
                final String currentScreen = currentOrPendingPrimaryAddress("screen");
                final List<String> screenKeyboards = componentKeyboardAddresses(currentScreen);
                shouldSelect = !screenKeyboards.isEmpty() && address.equals(screenKeyboards.getFirst());
            }
            if (shouldSelect) {
                setPrimaryComponent(type, address);
            }
        } else if ("component_removed".equals(signal.name())) {
            final PendingPrimaryComponent pending = pendingPrimaryComponents.get(type);
            if (address.equals(primaryComponents.get(type)) || (pending != null && address.equals(pending.address()))) {
                final String next = firstAvailableComponentAddress(type);
                if (next != null) {
                    setPrimaryComponent(type, next);
                    if ("screen".equals(type)) {
                        final List<String> nextKeyboards = componentKeyboardAddresses(next);
                        final String oldKeyboard = currentOrPendingPrimaryAddress("keyboard");
                        if (!nextKeyboards.isEmpty() && !nextKeyboards.getFirst().equals(oldKeyboard)) {
                            setPrimaryComponent("keyboard", nextKeyboards.getFirst());
                        }
                    }
                } else {
                    clearPrimaryComponent(type);
                }
            }
        }
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

    private Varargs invokeValueSynchronized(final Value value, final String method, final Object[] javaArgs) {
        return invokeSynchronized(() -> invokeValueOnce(value, method, javaArgs));
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

    private Varargs callValue(final Value value, final Object[] javaArgs) {
        try {
            return toLuaValues(value.call(machine, new LuaArguments(javaArgs)));
        } catch (IllegalArgumentException e) {
            throw new LuaError(e.getMessage() == null ? "bad argument" : e.getMessage());
        } catch (RuntimeException e) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(e.getMessage() == null ? "unknown error" : e.getMessage()));
        }
    }

    private Varargs callValueSynchronized(final Value value, final Object[] javaArgs) {
        return invokeSynchronized(() -> callValue(value, javaArgs));
    }

    private Varargs applyValue(final Value value, final Object[] javaArgs) {
        try {
            return toLuaValue(value.apply(machine, new LuaArguments(javaArgs)));
        } catch (IllegalArgumentException e) {
            throw new LuaError(e.getMessage() == null ? "bad argument" : e.getMessage());
        } catch (RuntimeException e) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(e.getMessage() == null ? "unknown error" : e.getMessage()));
        }
    }

    private Varargs applyValueSynchronized(final Value value, final Object[] javaArgs) {
        return invokeSynchronized(() -> applyValue(value, javaArgs));
    }

    private Varargs unapplyValue(final Value value, final Object[] javaArgs) {
        try {
            value.unapply(machine, new LuaArguments(javaArgs));
            return LuaValue.NONE;
        } catch (IllegalArgumentException e) {
            throw new LuaError(e.getMessage() == null ? "bad argument" : e.getMessage());
        } catch (RuntimeException e) {
            return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(e.getMessage() == null ? "unknown error" : e.getMessage()));
        }
    }

    private Varargs unapplyValueSynchronized(final Value value, final Object[] javaArgs) {
        return invokeSynchronized(() -> unapplyValue(value, javaArgs));
    }

    private Varargs invokeSynchronized(final PendingBudgetCall call) {
        pendingSynchronizedCall = call;
        final Varargs results = globals.yield(LuaValue.valueOf(SYNCHRONIZED_CALLBACK_MARKER));
        if (results.arg(1).eq_b(synchronizedErrorMarker)) {
            throw new LuaError(results.arg(2).tojstring());
        }
        return results;
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
        return toLuaValue(value, new IdentityHashMap<>());
    }

    private LuaValue toLuaValue(final Object value, final IdentityHashMap<Object, LuaValue> processed) {
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
        if (value instanceof Character characterValue) {
            return LuaValue.valueOf(String.valueOf(characterValue));
        }
        if (value instanceof Map<?, ?> mapValue) {
            final LuaValue cached = processed.get(value);
            if (cached != null) {
                return cached;
            }
            final LuaTable table = new LuaTable();
            processed.put(value, table);
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                final LuaValue key = toLuaValue(entry.getKey(), processed);
                if (!key.isnil()) {
                    table.set(key, toLuaValue(entry.getValue(), processed));
                }
            }
            return table;
        }
        if (value instanceof Iterable<?> iterableValue) {
            final LuaValue cached = processed.get(value);
            if (cached != null) {
                return cached;
            }
            final LuaTable table = new LuaTable();
            processed.put(value, table);
            int index = 1;
            for (Object entry : iterableValue) {
                table.set(index++, toLuaValue(entry, processed));
            }
            return table;
        }
        if (value.getClass().isArray()) {
            final LuaValue cached = processed.get(value);
            if (cached != null) {
                return cached;
            }
            final LuaTable table = new LuaTable();
            processed.put(value, table);
            final int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                table.set(index + 1, toLuaValue(Array.get(value, index), processed));
            }
            return table;
        }
        if (value instanceof String text) {
            return LuaValue.valueOf(text);
        }
        return LuaValue.NIL;
    }

    private Varargs toLuaValues(final Object[] values) {
        if (values == null || values.length == 0) {
            return LuaValue.NONE;
        }
        final IdentityHashMap<Object, LuaValue> processed = new IdentityHashMap<>();
        final LuaValue[] luaValues = new LuaValue[values.length];
        for (int index = 0; index < values.length; index++) {
            luaValues[index] = toLuaValue(values[index], processed);
        }
        return LuaValue.varargsOf(luaValues);
    }

    private Object[] toJavaArgs(final Varargs args, final int offset) {
        final Object[] javaArgs = new Object[Math.max(0, args.narg() - offset + 1)];
        for (int index = 0; index < javaArgs.length; index++) {
            javaArgs[index] = toJavaValue(args.arg(index + offset));
        }
        return javaArgs;
    }

    private Value checkValue(final Varargs args, final int index) {
        final LuaValue arg = args.arg(index);
        if (arg instanceof LuaTable table) {
            final Value value = valueProxyValues.get(table);
            if (value != null) {
                return value;
            }
            throw new LuaError("bad argument #" + index + " (userdata expected)");
        }
        final Object userdata = args.checkuserdata(index, Value.class);
        return (Value) userdata;
    }

    private LuaValue valueProxy(final Value value) {
        final LuaValue cached = valueProxyCache.get(value);
        if (cached != null) {
            return cached;
        }
        final LuaTable proxy = new LuaTable();
        proxy.set("type", "userdata");
        valueProxyCache.put(value, proxy);
        valueProxyValues.put(proxy, value);
        final Map<String, Callback> methods = machine == null ? Map.of() : machine.methods(value);
        for (String methodName : methods.keySet()) {
            proxy.set(methodName, valueCallbackFunction(value, methodName, proxy));
        }
        final LuaTable metatable = new LuaTable();
        metatable.set("__call", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return callValueSynchronized(value, toJavaArgs(args, 2));
            }
        });
        metatable.set("__index", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final LuaValue key = args.arg(2);
                return applyValueSynchronized(value, new Object[]{toJavaValue(key)});
            }
        });
        metatable.set("__newindex", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return unapplyValueSynchronized(value, new Object[]{toJavaValue(args.arg(2)), toJavaValue(args.arg(3))});
            }
        });
        metatable.set("__pairs", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                return new VarArgFunction() {
                    private LuaValue key = LuaValue.NIL;

                    @Override
                    public Varargs invoke(final Varargs iteratorArgs) {
                        final Varargs next = proxy.next(key);
                        key = next.arg1();
                        return next;
                    }
                };
            }
        });
        metatable.set("__metatable", "userdata");
        metatable.set("__tostring", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                try {
                    return LuaValue.valueOf(String.valueOf(value));
                } catch (Exception e) {
                    return LuaValue.valueOf(e.toString());
                }
            }
        });
        proxy.setmetatable(metatable);
        return proxy;
    }

    private LuaTable valueCallbackFunction(final Value value, final String methodName, final LuaValue proxy) {
        final LuaTable callback = new LuaTable();
        callback.set("name", methodName);
        callback.set("proxy", proxy);
        final LuaTable metatable = new LuaTable();
        metatable.set("__call", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final Callback methodCallback = machine == null ? null : machine.methods(value).get(methodName);
                if (methodCallback == null) {
                    throw new LuaError("no such method");
                }
                int offset = args.narg() > 0 && args.arg(1).eq_b(callback) ? 1 : 0;
                if (args.narg() > offset && args.arg(offset + 1).eq_b(proxy)) {
                    offset++;
                }
                final Object[] javaArgs = new Object[Math.max(0, args.narg() - offset)];
                for (int index = 0; index < javaArgs.length; index++) {
                    javaArgs[index] = toJavaValue(args.arg(index + offset + 1));
                }
                return methodCallback.direct()
                    ? invokeValue(value, methodName, javaArgs)
                    : invokeValueSynchronized(value, methodName, javaArgs);
            }
        });
        metatable.set("__tostring", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                final Callback callback = machine == null ? null : machine.methods(value).get(methodName);
                final String doc = callback == null ? "" : callback.doc();
                return LuaValue.valueOf(doc == null || doc.isEmpty() ? "function" : doc);
            }
        });
        callback.setmetatable(metatable);
        return callback;
    }

    private Object toJavaValue(final LuaValue value) {
        return toJavaValue(value, new IdentityHashMap<>());
    }

    private Object toJavaValue(final LuaValue value, final IdentityHashMap<LuaTable, Map<Object, Object>> processed) {
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
            return Arrays.copyOfRange(string.m_bytes, string.m_offset, string.m_offset + string.m_length);
        }
        if (value instanceof LuaTable table) {
            final Value machineValue = valueProxyValues.get(table);
            if (machineValue != null) {
                return machineValue;
            }
            final Map<Object, Object> cached = processed.get(table);
            if (cached != null) {
                return cached;
            }
            final Map<Object, Object> values = new LinkedHashMap<>();
            processed.put(table, values);
            LuaValue key = LuaValue.NIL;
            while (true) {
                final Varargs next = table.next(key);
                key = next.arg1();
                if (key.isnil()) {
                    break;
                }
                values.put(toJavaValue(key, processed), toJavaValue(next.arg(2), processed));
            }
            return values;
        }
        if (value.isuserdata()) {
            return value.touserdata();
        }
        return null;
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
        while (width < count) {
            previous = end;
            final int codePoint = value.codePointAt(end);
            width += Math.max(1, wcwidth(codePoint));
            end += Character.charCount(codePoint);
        }
        return previous > 0 ? value.substring(0, previous) : "";
    }

    private static int charWidth(final String value) {
        if (value.isEmpty()) {
            throw new LuaError("empty string");
        }
        return wcwidth(value.codePointAt(0));
    }

    private static int wcwidth(final int codePoint) {
        return FontWidths.wcwidth(codePoint);
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
            final Object value = checkAny(index, "boolean");
            if (!(value instanceof Boolean)) {
                throw new IllegalArgumentException("bad argument #" + (index + 1) + " (boolean expected, got " + argumentTypeName(value) + ")");
            }
            return (Boolean) value;
        }

        @Override
        public int checkInteger(final int index) {
            final Object value = checkAny(index, "integer");
            if (value instanceof Double doubleValue) {
                if (Double.isNaN(doubleValue)) {
                    throw new IllegalArgumentException("bad argument #" + (index + 1) + " (number has no integer representation)");
                }
                if (doubleValue > Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
                if (doubleValue < Integer.MIN_VALUE) {
                    return Integer.MIN_VALUE;
                }
                return doubleValue.intValue();
            }
            if (value instanceof Float floatValue) {
                if (Float.isNaN(floatValue)) {
                    throw new IllegalArgumentException("bad argument #" + (index + 1) + " (number has no integer representation)");
                }
                if (floatValue > Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
                if (floatValue < Integer.MIN_VALUE) {
                    return Integer.MIN_VALUE;
                }
                return floatValue.intValue();
            }
            if (!(value instanceof Number)) {
                throw new IllegalArgumentException("bad argument #" + (index + 1) + " (integer expected, got " + argumentTypeName(value) + ")");
            }
            return ((Number) value).intValue();
        }

        @Override
        public long checkLong(final int index) {
            final Object value = checkAny(index, "integer");
            if (value instanceof Double doubleValue) {
                if (Double.isNaN(doubleValue)) {
                    throw new IllegalArgumentException("bad argument #" + (index + 1) + " (number has no integer representation)");
                }
                if (doubleValue > Long.MAX_VALUE) {
                    return Long.MAX_VALUE;
                }
                if (doubleValue < Long.MIN_VALUE) {
                    return Long.MIN_VALUE;
                }
                return doubleValue.longValue();
            }
            if (value instanceof Float floatValue) {
                if (Float.isNaN(floatValue)) {
                    throw new IllegalArgumentException("bad argument #" + (index + 1) + " (number has no integer representation)");
                }
                if (floatValue > Long.MAX_VALUE) {
                    return Long.MAX_VALUE;
                }
                if (floatValue < Long.MIN_VALUE) {
                    return Long.MIN_VALUE;
                }
                return floatValue.longValue();
            }
            if (!(value instanceof Number)) {
                throw new IllegalArgumentException("bad argument #" + (index + 1) + " (integer expected, got " + argumentTypeName(value) + ")");
            }
            return ((Number) value).longValue();
        }

        @Override
        public double checkDouble(final int index) {
            final Object value = checkAny(index, "number");
            if (!(value instanceof Number)) {
                throw new IllegalArgumentException("bad argument #" + (index + 1) + " (number expected, got " + argumentTypeName(value) + ")");
            }
            return ((Number) value).doubleValue();
        }

        @Override
        public String checkString(final int index) {
            final Object value = checkAny(index, "string");
            if (value instanceof String string) {
                return string;
            }
            if (value instanceof byte[] bytes) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (string expected, got " + argumentTypeName(value) + ")");
        }

        @Override
        public byte[] checkByteArray(final int index) {
            final Object value = checkAny(index, "string");
            if (value instanceof byte[] bytes) {
                return bytes;
            }
            if (value instanceof String string) {
                return string.getBytes(StandardCharsets.UTF_8);
            }
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (string expected, got " + argumentTypeName(value) + ")");
        }

        @Override
        public Map checkTable(final int index) {
            final Object value = checkAny(index, "table");
            if (!(value instanceof Map)) {
                throw new IllegalArgumentException("bad argument #" + (index + 1) + " (table expected, got " + argumentTypeName(value) + ")");
            }
            return (Map) value;
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
            return index >= 0 && index < values.length && values[index] instanceof Number number && !Double.isNaN(number.doubleValue());
        }

        @Override
        public boolean isLong(final int index) {
            return index >= 0 && index < values.length && values[index] instanceof Number number && !Double.isNaN(number.doubleValue());
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
            return index >= 0 && index < values.length && (values[index] instanceof String || values[index] instanceof byte[]);
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
            final Object[] result = Arrays.copyOf(values, values.length);
            for (int index = 0; index < result.length; index++) {
                if (result[index] instanceof byte[] bytes) {
                    result[index] = new String(bytes, StandardCharsets.UTF_8);
                }
            }
            return result;
        }

        @Override
        public java.util.Iterator<Object> iterator() {
            return Arrays.asList(values).iterator();
        }

        private static String argumentTypeName(final Object value) {
            if (value == null) {
                return "nil";
            }
            if (value instanceof Boolean) {
                return "boolean";
            }
            if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
                return "integer";
            }
            if (value instanceof Number) {
                return "number";
            }
            if (value instanceof String || value instanceof byte[]) {
                return "string";
            }
            if (value instanceof Map) {
                return "table";
            }
            return "userdata";
        }

        private Object checkAny(final int index, final String type) {
            if (index < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (index >= values.length) {
                throw new IllegalArgumentException("bad arguments #" + (index + 1) + " (" + type + " expected, got no value)");
            }
            return values[index];
        }
    }

    @FunctionalInterface
    private interface PendingBudgetCall {
        Varargs invoke() throws LimitReachedException;
    }

    private record PendingPrimaryComponent(String address, double readyAtSeconds) {
    }
}
