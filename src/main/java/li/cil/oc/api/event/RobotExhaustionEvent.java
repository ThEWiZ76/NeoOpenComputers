package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;

public class RobotExhaustionEvent extends RobotEvent {
    public final double exhaustion;

    public RobotExhaustionEvent(final Agent agent, final double exhaustion) {
        super(agent);
        this.exhaustion = exhaustion;
    }
}
