package li.cil.oc.api.machine;

import li.cil.oc.api.network.Node;

/**
 * Calling context for callbacks invoked from a machine.
 */
public interface Context {
    Node node();

    boolean canInteract(String player);

    boolean isRunning();

    boolean isPaused();

    boolean start();

    boolean pause(double seconds);

    boolean stop();

    void consumeCallBudget(double callCost);

    boolean signal(String name, Object... args);
}
