package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.neoforged.bus.api.ICancellableEvent;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class RobotRenderEvent extends RobotEvent implements ICancellableEvent {
    public final MountPoint[] mountPoints;

    public RobotRenderEvent(final Agent agent, final MountPoint[] mountPoints) {
        super(agent);
        this.mountPoints = mountPoints;
    }

    public static class MountPoint {
        public final Vector3f offset = new Vector3f(0, 0, 0);
        public final Vector4f rotation = new Vector4f(0, 0, 0, 0);
        public final String name;

        public MountPoint() {
            name = null;
        }

        public MountPoint(final String name) {
            this.name = name;
        }
    }
}
