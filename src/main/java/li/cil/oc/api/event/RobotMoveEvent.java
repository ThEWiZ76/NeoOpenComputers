package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class RobotMoveEvent extends RobotEvent {
    public final Direction direction;

    protected RobotMoveEvent(final Agent agent, final Direction direction) {
        super(agent);
        this.direction = direction;
    }

    public static class Pre extends RobotMoveEvent implements ICancellableEvent {
        public Pre(final Agent agent, final Direction direction) {
            super(agent, direction);
        }
    }

    public static class Post extends RobotMoveEvent {
        public Post(final Agent agent, final Direction direction) {
            super(agent, direction);
        }
    }
}
