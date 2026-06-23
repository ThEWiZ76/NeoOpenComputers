package li.cil.oc.common.machine;

public interface SynchronizedCallAware {
    boolean hasPendingSynchronizedCall();

    boolean hasSynchronizedReturn();
}
