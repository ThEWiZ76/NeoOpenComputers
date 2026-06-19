package li.cil.oc.common.machine;

import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.ExecutionResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

@Architecture.Name("Lua")
public final class LuaArchitecture implements Architecture {
    private static final String INITIALIZED_TAG = "initialized";

    private boolean initialized;

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
        initialized = true;
        return true;
    }

    @Override
    public void close() {
        initialized = false;
    }

    @Override
    public void runSynchronized() {
    }

    @Override
    public ExecutionResult runThreaded(final boolean isSynchronizedReturn) {
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
        initialized = nbt.getBoolean(INITIALIZED_TAG);
    }

    @Override
    public void save(final CompoundTag nbt) {
        nbt.putBoolean(INITIALIZED_TAG, initialized);
    }
}
