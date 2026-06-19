package li.cil.oc.common.machine;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.ExecutionResult;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.common.ItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Architecture.Name("Lua")
public final class LuaArchitecture implements Architecture, MachineBoundArchitecture {
    private static final String EEPROM_SLOT = "eeprom";
    private static final String INITIALIZED_TAG = "initialized";
    private static final String BOOTED_TAG = "booted";
    private static final String BOOT_SOURCE_TAG = "bootSource";

    private boolean initialized;
    private boolean booted;
    private String bootSource;
    private Machine machine;
    private Globals globals;
    private LuaValue bootChunk;
    private ExecutionResult pendingResult;

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
        for (ItemStack stack : components) {
            final DriverItem driver = Driver.driverFor(stack);
            if (driver != null && EEPROM_SLOT.equals(driver.slot(stack))) {
                configureBootSource(driver.dataTag(stack));
                break;
            }
        }
        return true;
    }

    @Override
    public boolean initialize() {
        globals = JsePlatform.standardGlobals();
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
    }

    @Override
    public void save(final CompoundTag nbt) {
        nbt.putBoolean(INITIALIZED_TAG, initialized);
        nbt.putBoolean(BOOTED_TAG, booted);
        nbt.putString(BOOT_SOURCE_TAG, bootSource);
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
        globals.set("computer", computer);
    }

    private void installComponentLibrary() {
        final LuaTable component = new LuaTable();
        component.set("list", new VarArgFunction() {
            @Override
            public Varargs invoke(final Varargs args) {
                final LuaTable components = new LuaTable();
                if (machine != null) {
                    for (Map.Entry<String, String> entry : machine.components().entrySet()) {
                        components.set(entry.getKey(), entry.getValue());
                    }
                }
                return components;
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
        return LuaValue.valueOf(String.valueOf(value));
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
        return value.tojstring();
    }
}
