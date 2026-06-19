package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.minecraft.world.item.ItemStack;

public class RobotUsedToolEvent extends RobotEvent {
    public final ItemStack toolBeforeUse;
    public final ItemStack toolAfterUse;
    protected double damageRate;

    protected RobotUsedToolEvent(final Agent agent, final ItemStack toolBeforeUse, final ItemStack toolAfterUse, final double damageRate) {
        super(agent);
        this.toolBeforeUse = toolBeforeUse;
        this.toolAfterUse = toolAfterUse;
        this.damageRate = damageRate;
    }

    public double getDamageRate() {
        return damageRate;
    }

    public static class ComputeDamageRate extends RobotUsedToolEvent {
        public ComputeDamageRate(final Agent agent, final ItemStack toolBeforeUse, final ItemStack toolAfterUse, final double damageRate) {
            super(agent, toolBeforeUse, toolAfterUse, damageRate);
        }

        public void setDamageRate(final double damageRate) {
            this.damageRate = Math.max(0, Math.min(1, damageRate));
        }
    }

    public static class ApplyDamageRate extends RobotUsedToolEvent {
        public ApplyDamageRate(final Agent agent, final ItemStack toolBeforeUse, final ItemStack toolAfterUse, final double damageRate) {
            super(agent, toolBeforeUse, toolAfterUse, damageRate);
        }
    }
}
