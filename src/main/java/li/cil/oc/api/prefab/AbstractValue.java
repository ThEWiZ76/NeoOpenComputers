package li.cil.oc.api.prefab;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Value;
import net.minecraft.nbt.CompoundTag;

public class AbstractValue implements Value {
    @Override
    public Object apply(final Context context, final Arguments arguments) {
        return null;
    }

    @Override
    public void unapply(final Context context, final Arguments arguments) {
    }

    @Override
    public Object[] call(final Context context, final Arguments arguments) {
        throw new RuntimeException("trying to call a non-callable value");
    }

    @Override
    public void dispose(final Context context) {
    }

    @Override
    public void load(final CompoundTag nbt) {
    }

    @Override
    public void save(final CompoundTag nbt) {
    }
}
