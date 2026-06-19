package li.cil.oc.api.nanomachines;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public interface BehaviorProvider {
    Iterable<Behavior> createBehaviors(Player player);

    CompoundTag writeToNBT(Behavior behavior);

    Behavior readFromNBT(Player player, CompoundTag nbt);
}
