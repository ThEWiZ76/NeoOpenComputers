package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.DisableReason;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class NanomachineDisintegrationProvider implements BehaviorProvider {
    @Override
    public Iterable<Behavior> createBehaviors(final Player player) {
        return java.util.List.of(new DisintegrationBehavior(player));
    }

    @Override
    public CompoundTag writeToNBT(final Behavior behavior) {
        return new CompoundTag();
    }

    @Override
    public Behavior readFromNBT(final Player player, final CompoundTag nbt) {
        return new DisintegrationBehavior(player);
    }

    private record DisintegrationBehavior(Player player) implements Behavior {
        @Override
        public String getNameHint() {
            return "";
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable(final DisableReason reason) {
        }

        @Override
        public void update() {
            if (player == null || player.level().isClientSide()) {
                return;
            }
        }
    }
}
