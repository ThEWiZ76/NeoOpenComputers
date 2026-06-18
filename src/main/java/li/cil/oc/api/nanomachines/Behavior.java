package li.cil.oc.api.nanomachines;

/**
 * Single nanomachine behavior lifecycle.
 */
public interface Behavior {
    String getNameHint();

    void onEnable();

    void onDisable(DisableReason reason);

    void update();
}
