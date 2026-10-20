package li.cil.oc.common.inventory;

import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.util.Lifecycle;

/** Instance lifetime is independent of temporary network membership. */
public final class ComponentLifecycle {
    private ComponentLifecycle() { }

    public static void setState(ManagedEnvironment environment, Lifecycle.LifecycleState state) {
        if (environment instanceof Lifecycle listener) listener.onLifecycleStateChange(state);
    }
}
