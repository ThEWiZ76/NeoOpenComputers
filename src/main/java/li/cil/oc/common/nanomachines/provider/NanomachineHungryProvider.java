package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.common.ModSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.stream.IntStream;

public final class NanomachineHungryProvider implements BehaviorProvider {
    private static final int FILL_COUNT = 10;

    @Override
    public Iterable<Behavior> createBehaviors(final Player player) {
        return IntStream.range(0, FILL_COUNT)
            .mapToObj(index -> new HungryBehavior(player))
            .map(Behavior.class::cast)
            .toList();
    }

    @Override
    public CompoundTag writeToNBT(final Behavior behavior) {
        return new CompoundTag();
    }

    @Override
    public Behavior readFromNBT(final Player player, final CompoundTag nbt) {
        return new HungryBehavior(player);
    }

    private record HungryBehavior(Player player) implements Behavior {
        @Override
        public String getNameHint() {
            return "";
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable(final DisableReason reason) {
            if (reason != DisableReason.OutOfEnergy || player == null) {
                return;
            }
            player.hurt(player.damageSources().magic(), (float) ModSettings.nanomachinesHungryDamage());
            final Controller controller = Nanomachines.getController(player);
            if (controller != null) {
                controller.changeBuffer(ModSettings.nanomachinesHungryEnergyRestored());
            }
        }

        @Override
        public void update() {
        }
    }
}
