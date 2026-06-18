package li.cil.oc.api.network;

import li.cil.oc.api.Persistable;

/**
 * Environment whose lifecycle is managed by a containing host.
 */
public interface ManagedEnvironment extends Environment, Persistable {
    boolean canUpdate();

    void update();
}
