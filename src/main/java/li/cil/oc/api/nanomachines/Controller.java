package li.cil.oc.api.nanomachines;

/**
 * Nanomachine neural connection and energy controller.
 */
public interface Controller {
    Controller reconfigure();

    int getTotalInputCount();

    int getSafeActiveInputs();

    int getMaxActiveInputs();

    boolean getInput(int index);

    boolean setInput(int index, boolean value);

    Iterable<Behavior> getActiveBehaviors();

    int getInputCount(Behavior behavior);

    double getLocalBuffer();

    double getLocalBufferSize();

    double changeBuffer(double delta);
}
