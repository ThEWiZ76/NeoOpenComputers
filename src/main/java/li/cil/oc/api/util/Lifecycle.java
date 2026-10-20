package li.cil.oc.api.util;

/**
 * Optional lifetime notifications for managed components owned by a host.
 * Constructing precedes loading saved state; Initializing precedes connecting
 * the component node; Initialized precedes regular updates. Disposing precedes
 * detaching the component and Disposed ends its lifetime. A temporary change in
 * network membership does not itself end the component's lifetime.
 * Environments without a node can also receive these notifications.
 */
public interface Lifecycle {
    enum LifecycleState {
        Constructing,
        Initializing,
        Initialized,
        Disposing,
        Disposed
    }

    void onLifecycleStateChange(LifecycleState state);
}
