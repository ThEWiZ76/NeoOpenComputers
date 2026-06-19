package li.cil.oc.api.network;

import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import java.util.Collection;

/**
 * Addressable node that exposes callbacks to machines.
 */
public interface Component extends Node {
    String name();

    Visibility visibility();

    void setVisibility(Visibility value);

    boolean canBeSeenFrom(Node other);

    Collection<String> methods();

    Callback annotation(String method);

    Object[] invoke(String method, Context context, Object... arguments) throws Exception;
}
