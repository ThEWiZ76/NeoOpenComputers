package li.cil.oc.api.util;

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
