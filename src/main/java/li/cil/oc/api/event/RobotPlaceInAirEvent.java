package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;

public class RobotPlaceInAirEvent extends RobotEvent {
    private boolean allowed;

    public RobotPlaceInAirEvent(final Agent agent) {
        super(agent);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(final boolean value) {
        allowed = value;
    }
}
