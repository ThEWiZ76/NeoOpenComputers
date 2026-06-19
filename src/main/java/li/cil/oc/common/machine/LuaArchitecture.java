package li.cil.oc.common.machine;

import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.ExecutionResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

@Architecture.Name("Lua")
public final class LuaArchitecture implements Architecture {
    private static final String INITIALIZED_TAG = "initialized";
    private static final String BOOTED_TAG = "booted";
    private static final String BOOT_SOURCE_TAG = "bootSource";

    private boolean initialized;
    private boolean booted;
    private String bootSource;
    private Globals globals;
    private LuaValue bootChunk;

    public LuaArchitecture() {
        this("");
    }

    LuaArchitecture(final String bootSource) {
        this.bootSource = bootSource == null ? "" : bootSource;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public boolean recomputeMemory(final Iterable<ItemStack> components) {
        return true;
    }

    @Override
    public boolean initialize() {
        globals = JsePlatform.standardGlobals();
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
}
