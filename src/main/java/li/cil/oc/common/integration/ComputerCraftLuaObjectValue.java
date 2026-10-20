package li.cil.oc.common.integration;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.prefab.AbstractValue;
import net.minecraft.nbt.CompoundTag;

/** The saved shell must also load when ComputerCraft is absent. */
public final class ComputerCraftLuaObjectValue extends AbstractValue implements ManagedPeripheral {
    private volatile ManagedPeripheral delegate;

    public ComputerCraftLuaObjectValue() { }
    public ComputerCraftLuaObjectValue(final ManagedPeripheral delegate) { this.delegate = delegate; }

    @Override public String[] methods() {
        final var current = delegate;
        return current == null ? new String[0] : current.methods();
    }

    @Override public Object[] invoke(String method, Context context, Arguments args) throws Exception {
        final var current = delegate;
        return current == null ? new Object[]{null, "ComputerCraft userdata cannot be persisted"}
            : current.invoke(method, context, args);
    }

    @Override public void load(CompoundTag data) { dispose(null); }

    @Override public void dispose(Context context) {
        final var previous = delegate;
        delegate = null;
        if (previous instanceof AutoCloseable closeable) {
            try { closeable.close(); }
            catch (Exception failure) { throw new IllegalStateException("Cannot dispose ComputerCraft value", failure); }
        }
    }
}
