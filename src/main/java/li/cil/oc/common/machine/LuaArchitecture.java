package li.cil.oc.common.machine;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.machine.Architecture;
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
        installComputerLibrary();
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
}
