package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.neoforged.bus.api.Event;

public abstract class RobotEvent extends Event {
    public final Agent agent;

    protected RobotEvent(final Agent agent) {
        this.agent = agent;
    }
}
