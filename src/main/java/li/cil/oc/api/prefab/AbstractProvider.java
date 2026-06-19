package li.cil.oc.api.prefab;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public abstract class AbstractProvider implements BehaviorProvider {
    protected final String id;

    protected AbstractProvider(final String id) {
        if (id == null) {
            throw new NullPointerException("id must not be null");
        }
        this.id = id;
    }

    protected void writeBehaviorToNBT(final Behavior behavior, final CompoundTag nbt) {
    }

    protected abstract Behavior readBehaviorFromNBT(Player player, CompoundTag nbt);

    @Override
    public CompoundTag writeToNBT(final Behavior behavior) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("provider", id);
        writeBehaviorToNBT(behavior, nbt);
        return nbt;
    }

    @Override
    public Behavior readFromNBT(final Player player, final CompoundTag nbt) {
        if (id.equals(nbt.getString("provider"))) {
            return readBehaviorFromNBT(player, nbt);
        }
        return null;
    }
}
