package li.cil.oc.api.internal;

import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.world.phys.Vec3;

public interface Drone extends Agent, EnvironmentHost, Rotatable, Tiered {
    Vec3 getTarget();

    void setTarget(Vec3 value);

    Vec3 getVelocity();
}
