package li.cil.oc.api.prefab;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import net.minecraft.world.entity.player.Player;

public abstract class AbstractBehavior implements Behavior {
    public final Player player;

    protected AbstractBehavior(final Player player) {
        this.player = player;
    }

    protected AbstractBehavior() {
        this(null);
    }

    @Override
    public String getNameHint() {
        return null;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable(final DisableReason reason) {
    }

    @Override
    public void update() {
    }
}
