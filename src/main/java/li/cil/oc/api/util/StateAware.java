package li.cil.oc.api.util;

import java.util.EnumSet;

/**
 * Object that can report whether work can be or is being performed.
 */
@FunctionalInterface
public interface StateAware {
    EnumSet<State> getCurrentState();

    enum State {
        None,
        CanWork,
        IsWorking
    }
}
