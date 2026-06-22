package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.prefab.AbstractProvider;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.damage.ModDamageSources;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.stream.IntStream;

public final class NanomachineHungryProvider extends AbstractProvider {
    private static final String PROVIDER_ID = "d697c24a-014c-4773-a288-23084a59e9e8";
    private static final int FILL_COUNT = 10;

    public NanomachineHungryProvider() {
        super(PROVIDER_ID);
    }

    @Override
    public Iterable<Behavior> createBehaviors(final Player player) {
        return IntStream.range(0, FILL_COUNT)
            .mapToObj(index -> new HungryBehavior(player))
            .map(Behavior.class::cast)
            .toList();
    }

    @Override
    protected void writeBehaviorToNBT(final Behavior behavior, final CompoundTag nbt) {
    }

    @Override
    protected Behavior readBehaviorFromNBT(final Player player, final CompoundTag nbt) {
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
            player.hurt(ModDamageSources.nanomachinesHungry(player), (float) ModSettings.nanomachinesHungryDamage());
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
