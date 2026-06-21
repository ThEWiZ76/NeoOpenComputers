package li.cil.oc.common;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.nanomachines.DisableReason;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

final class SimpleNanomachineController implements Controller {
    private static final double BUFFER_SIZE = 100_000D;
    private static final int SAFE_ACTIVE_INPUTS = 2;
    private static final int MAX_ACTIVE_INPUTS = 4;

    private final Player player;
    private final NanomachinesRegistry registry;
    private List<Behavior> behaviors = List.of();
    private boolean[] inputs = new boolean[0];
    private double buffer = BUFFER_SIZE * 0.25D;

    SimpleNanomachineController(final Player player, final NanomachinesRegistry registry) {
        this.player = player;
        this.registry = registry;
        reconfigure();
    }

    @Override
    public Controller reconfigure() {
        final List<Behavior> created = new ArrayList<>();
        for (final var provider : registry.getProviders()) {
            for (final Behavior behavior : provider.createBehaviors(player)) {
                if (behavior != null) {
                    created.add(behavior);
                }
            }
        }
        disableAll(DisableReason.Default);
        behaviors = List.copyOf(created);
        inputs = new boolean[Math.max(1, (int) Math.ceil(behaviors.size() * 0.4D))];
        return this;
    }

    @Override
    public int getTotalInputCount() {
        return inputs.length;
    }

    @Override
    public int getSafeActiveInputs() {
        return SAFE_ACTIVE_INPUTS;
    }

    @Override
    public int getMaxActiveInputs() {
        return MAX_ACTIVE_INPUTS;
    }

    @Override
    public boolean getInput(final int index) {
        return inputs[index];
    }

    @Override
    public boolean setInput(final int index, final boolean value) {
        if (value && !inputs[index] && activeInputCount() >= MAX_ACTIVE_INPUTS) {
            return false;
        }
        inputs[index] = value;
        return true;
    }

    @Override
    public Iterable<Behavior> getActiveBehaviors() {
        return inputs.length > 0 && activeInputCount() > 0 ? behaviors : List.of();
    }

    @Override
    public int getInputCount(final Behavior behavior) {
        return activeInputCount();
    }

    @Override
    public double getLocalBuffer() {
        return buffer;
    }

    @Override
    public double getLocalBufferSize() {
        return BUFFER_SIZE;
    }

    @Override
    public double changeBuffer(final double delta) {
        final double requested = buffer + delta;
        buffer = Math.clamp(requested, 0D, BUFFER_SIZE);
        return requested - buffer;
    }

    void dispose() {
        disableAll(DisableReason.Default);
    }

    private int activeInputCount() {
        int active = 0;
        for (final boolean input : inputs) {
            if (input) {
                active++;
            }
        }
        return active;
    }

    private void disableAll(final DisableReason reason) {
        for (final Behavior behavior : behaviors) {
            behavior.onDisable(reason);
        }
    }
}
