package li.cil.oc.api.machine;

import li.cil.oc.api.network.Node;

public final class TestContext implements Context {
    @Override
    public Node node() {
        return TestNodes.node("context");
    }

    @Override
    public boolean canInteract(final String player) {
        return true;
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public boolean isPaused() {
        return false;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean pause(final double seconds) {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void consumeCallBudget(final double callCost) {
    }

    @Override
    public boolean signal(final String name, final Object... args) {
        return true;
    }
}
