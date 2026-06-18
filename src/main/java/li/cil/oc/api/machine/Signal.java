package li.cil.oc.api.machine;

/**
 * A queued signal delivered to a machine.
 */
public interface Signal {
    String name();

    Object[] args();
}
