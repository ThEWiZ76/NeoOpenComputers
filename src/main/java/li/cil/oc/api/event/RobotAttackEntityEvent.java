package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.ICancellableEvent;

public class RobotAttackEntityEvent extends RobotEvent {
    public final Entity target;

    protected RobotAttackEntityEvent(final Agent agent, final Entity target) {
        super(agent);
        this.target = target;
    }

    public static class Pre extends RobotAttackEntityEvent implements ICancellableEvent {
        public Pre(final Agent agent, final Entity target) {
            super(agent, target);
        }
    }

    public static class Post extends RobotAttackEntityEvent {
        public Post(final Agent agent, final Entity target) {
            super(agent, target);
        }
    }
}
