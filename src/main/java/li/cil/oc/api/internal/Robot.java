package li.cil.oc.api.internal;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.world.WorldlyContainer;

public interface Robot extends Agent, Environment, EnvironmentHost, Tiered, WorldlyContainer {
    int componentCount();

    Environment getComponentInSlot(int index);

    void synchronizeSlot(int slot);

    boolean shouldAnimate();
}
