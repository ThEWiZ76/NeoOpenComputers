package li.cil.oc.common.machine;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
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

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Architecture.Name("Lua")
public final class LuaArchitecture implements Architecture, MachineBoundArchitecture {
    private static final String EEPROM_SLOT = "eeprom";
    private static final String INITIALIZED_TAG = "initialized";
    private static final String BOOTED_TAG = "booted";
    private static final String BOOT_SOURCE_TAG = "bootSource";
    private static final String MEMORY_TAG = "memory";

    private boolean initialized;
    private boolean booted;
    private String bootSource;
    private Machine machine;
    private Globals globals;
    private LuaValue bootChunk;
    private ExecutionResult pendingResult;
    private double memoryBytes;
    private final Map<String, String> primaryComponents = new HashMap<>();

    public LuaArchitecture() {
        this("");
    }

    LuaArchitecture(final String bootSource) {
        this.bootSource = bootSource == null ? "" : bootSource;
    }

    @Override
    public void bind(final Machine machine) {
        this.machine = machine;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
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
        return true;
    }

    @Override
    public boolean initialize() {
        globals = sandboxGlobals();
        pendingResult = null;
        installComputerLibrary();
        installComponentLibrary();
        try {
            bootChunk = globals.load(bootSource, "boot");
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
        pendingResult = null;
    }

    @Override
    public void runSynchronized() {
    }

    @Override
    public ExecutionResult runThreaded(final boolean isSynchronizedReturn) {
        if (!initialized) {
            return new ExecutionResult.Error("Lua architecture is not initialized");
        }
        if (!booted) {
            booted = true;
            try {
                bootChunk.call();
            } catch (LuaError e) {
                return new ExecutionResult.Error(e.getMessage());
            }
            if (pendingResult != null) {
                return pendingResult;
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
        LoadState.install(globals);
        LuaC.install(globals);
        return globals;
    }

    private void installComputerLibrary() {
        final LuaTable computer = new LuaTable();
        computer.set("uptime", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(machine == null ? 0D : machine.upTime());
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
        computer.set("tmpAddress", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return machineAddress();
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
                } else if (args.narg() >= 2) {
                    machine.beep((short) args.arg(1).toint(), (short) args.arg(2).toint());
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
                if (machine == null || args.narg() < 1) {
                    return LuaValue.FALSE;
                }
                try {
                    machine.addUser(args.arg1().tojstring());
                    return LuaValue.TRUE;
                } catch (Exception e) {
                    throw new LuaError(e.getMessage() == null ? e.toString() : e.getMessage());
                }
            }
        });
        computer.set("removeUser", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null || args.narg() < 1) {
                    return LuaValue.FALSE;
                }
                return LuaValue.valueOf(machine.removeUser(args.arg1().tojstring()));
            }
        });
        computer.set("pullSignal", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null) {
                    return LuaValue.NIL;
                }
                final Signal signal = machine.popSignal();
                if (signal == null) {
                    return LuaValue.NIL;
                }
                final Object[] signalArgs = signal.args();
                final LuaValue[] values = new LuaValue[signalArgs.length + 1];
                values[0] = LuaValue.valueOf(signal.name());
                for (int index = 0; index < signalArgs.length; index++) {
                    values[index + 1] = toLuaValue(signalArgs[index]);
                }
                return LuaValue.varargsOf(values);
            }
        });
        computer.set("pushSignal", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null || args.narg() < 1) {
                    return LuaValue.FALSE;
                }
                final Object[] signalArgs = new Object[Math.max(0, args.narg() - 1)];
                for (int index = 0; index < signalArgs.length; index++) {
                    signalArgs[index] = toJavaValue(args.arg(index + 2));
                }
                return LuaValue.valueOf(machine.signal(args.arg1().tojstring(), signalArgs));
            }
        });
        globals.set("computer", computer);
    }

    private void installComponentLibrary() {
        final LuaTable component = new LuaTable();
        component.set("list", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String filter = args.narg() >= 1 && !args.arg1().isnil() ? args.arg1().tojstring() : null;
                final boolean exact = args.narg() < 2 || args.arg(2).toboolean();
                return createComponentList(filter, exact);
            }
        });
        component.set("type", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null || args.narg() < 1) {
                    return LuaValue.NIL;
                }
                final String type = machine.components().get(args.arg(1).tojstring());
                return type == null ? LuaValue.NIL : LuaValue.valueOf(type);
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
        component.set("slot", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null || args.narg() < 1) {
                    return LuaValue.NIL;
                }
                final MachineHost host = machine.host();
                if (host == null) {
                    return LuaValue.NIL;
                }
                final int slot = host.componentSlot(args.arg1().tojstring());
                return slot < 0 ? LuaValue.NIL : LuaValue.valueOf(slot);
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
                return LuaValue.valueOf(setPrimaryComponent(args.arg(1).tojstring(), args.arg(2).tojstring()));
            }
        });
        component.set("methods", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final LuaTable methods = new LuaTable();
                if (machine != null && args.narg() >= 1) {
                    for (Map.Entry<String, Callback> entry : machine.methods(args.arg(1).tojstring()).entrySet()) {
                        final Callback callback = entry.getValue();
                        methods.set(entry.getKey(), LuaValue.valueOf(callback != null && callback.direct()));
                    }
                }
                return methods;
            }
        });
        component.set("doc", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null || args.narg() < 2) {
                    return LuaValue.NIL;
                }
                final Callback callback = machine.methods(args.arg(1).tojstring()).get(args.arg(2).tojstring());
                if (callback == null || callback.doc().isEmpty()) {
                    return LuaValue.NIL;
                }
                return LuaValue.valueOf(callback.doc());
            }
        });
        component.set("invoke", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null || args.narg() < 2) {
                    return LuaValue.NIL;
                }
                final String address = args.arg(1).tojstring();
                final String method = args.arg(2).tojstring();
                final Object[] javaArgs = new Object[Math.max(0, args.narg() - 2)];
                for (int index = 0; index < javaArgs.length; index++) {
                    javaArgs[index] = toJavaValue(args.arg(index + 3));
                }
                try {
                    return toLuaValues(machine.invoke(address, method, javaArgs));
                } catch (Exception e) {
                    throw new LuaError(e.getMessage() == null ? e.toString() : e.getMessage());
                }
            }
        });
        component.set("proxy", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                if (machine == null || args.narg() < 1) {
                    return LuaValue.NIL;
                }
                return createComponentProxy(args.arg(1).tojstring());
            }
        });
        globals.set("component", component);
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
        return exact ? type.equals(filter) : type.startsWith(filter);
    }

    private LuaTable createComponentProxy(final String address) {
        final LuaTable proxy = new LuaTable();
        final LuaTable metatable = new LuaTable();
        metatable.set("__index", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final String method = args.arg(2).tojstring();
                return new VarArgFunction() {
                    @Override
                    public Varargs invoke(final Varargs callbackArgs) {
                        final int offset = callbackArgs.narg() > 0 && callbackArgs.arg(1).eq_b(proxy) ? 1 : 0;
                        final Object[] javaArgs = new Object[Math.max(0, callbackArgs.narg() - offset)];
                        for (int index = 0; index < javaArgs.length; index++) {
                            javaArgs[index] = toJavaValue(callbackArgs.arg(index + offset + 1));
                        }
                        try {
                            return toLuaValues(machine.invoke(address, method, javaArgs));
                        } catch (Exception e) {
                            throw new LuaError(e.getMessage() == null ? e.toString() : e.getMessage());
                        }
                    }
                };
            }
        });
        proxy.setmetatable(metatable);
        return proxy;
    }

    private LuaValue machineAddress() {
        if (machine == null || machine.tmpAddress() == null) {
            return LuaValue.NIL;
        }
        return LuaValue.valueOf(machine.tmpAddress());
    }

    private Connector machineConnector() {
        if (machine == null || !(machine.node() instanceof Connector connector)) {
            return null;
        }
        return connector;
    }

    private static LuaValue toLuaValue(final Object value) {
        if (value == null) {
            return LuaValue.NIL;
        }
        if (value instanceof Boolean booleanValue) {
            return LuaValue.valueOf(booleanValue);
        }
        if (value instanceof Number numberValue) {
            return LuaValue.valueOf(numberValue.doubleValue());
        }
        if (value instanceof byte[] bytes) {
            return LuaValue.valueOf(new String(bytes, StandardCharsets.UTF_8));
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

    private static Varargs toLuaValues(final Object[] values) {
        if (values == null || values.length == 0) {
            return LuaValue.NIL;
        }
        final LuaValue[] luaValues = new LuaValue[values.length];
        for (int index = 0; index < values.length; index++) {
            luaValues[index] = toLuaValue(values[index]);
        }
        return LuaValue.varargsOf(luaValues);
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
        if (value instanceof LuaTable table) {
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
}
