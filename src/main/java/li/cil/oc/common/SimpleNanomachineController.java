package li.cil.oc.common;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.nanomachines.DisableReason;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

final class SimpleNanomachineController implements Controller {
    private static final String TAG_ENERGY = "energy";
    private static final String TAG_ACTIVE_INPUTS = "activeInputs";

    private final Player player;
    private final NanomachinesRegistry registry;
    private List<Behavior> behaviors = List.of();
    private List<Behavior> activeBehaviors = List.of();
    private boolean[] inputs = new boolean[0];
    private boolean activeBehaviorsDirty;
    private double buffer;

    SimpleNanomachineController(final Player player, final NanomachinesRegistry registry) {
        this.player = player;
        this.registry = registry;
        buffer = ModSettings.nanomachinesBuffer() * 0.25D;
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
        disableActive(DisableReason.Default);
        behaviors = List.copyOf(created);
        activeBehaviors = List.of();
        inputs = new boolean[Math.max(1, (int) Math.ceil(behaviors.size() * 0.4D))];
        activeBehaviorsDirty = true;
        return this;
    }

    @Override
    public int getTotalInputCount() {
        return inputs.length;
    }

    @Override
    public int getSafeActiveInputs() {
        return ModSettings.nanomachinesSafeInputsActive();
    }

    @Override
    public int getMaxActiveInputs() {
        return ModSettings.nanomachinesMaxInputsActive();
    }

    @Override
    public boolean getInput(final int index) {
        return inputs[index];
    }

    @Override
    public boolean setInput(final int index, final boolean value) {
        if (value && !inputs[index] && activeInputCount() >= getMaxActiveInputs()) {
            return false;
        }
        if (inputs[index] != value) {
            activeBehaviorsDirty = true;
        }
        inputs[index] = value;
        saveState();
        return true;
    }

    @Override
    public Iterable<Behavior> getActiveBehaviors() {
        cleanActiveBehaviors(DisableReason.InputChanged);
        return activeBehaviors;
    }

    @Override
    public int getInputCount(final Behavior behavior) {
        return behaviors.contains(behavior) ? activeInputCount() : 0;
    }

    @Override
    public double getLocalBuffer() {
        return buffer;
    }

    @Override
    public double getLocalBufferSize() {
        return ModSettings.nanomachinesBuffer();
    }

    @Override
    public double changeBuffer(final double delta) {
        final double requested = buffer + delta;
        buffer = Math.clamp(requested, 0D, getLocalBufferSize());
        saveState();
        return requested - buffer;
    }

    void dispose() {
        disableActive(DisableReason.Default);
    }

    void save(final CompoundTag tag) {
        tag.putDouble(TAG_ENERGY, buffer);
        tag.putIntArray(TAG_ACTIVE_INPUTS, activeInputs());
    }

    void load(final CompoundTag tag) {
        if (tag.contains(TAG_ENERGY)) {
            buffer = Math.clamp(tag.getDouble(TAG_ENERGY), 0D, getLocalBufferSize());
        }
        final int[] activeInputs = tag.getIntArray(TAG_ACTIVE_INPUTS);
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = false;
        }
        for (final int activeInput : activeInputs) {
            if (activeInput >= 0 && activeInput < inputs.length) {
                inputs[activeInput] = true;
            }
        }
        activeBehaviorsDirty = true;
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

    private int[] activeInputs() {
        final int[] activeInputs = new int[activeInputCount()];
        int activeIndex = 0;
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i]) {
                activeInputs[activeIndex++] = i;
            }
        }
        return activeInputs;
    }

    private void saveState() {
        final CompoundTag tag = new CompoundTag();
        save(tag);
        player.getPersistentData().put(NanomachinesRegistry.TAG_CONTROLLER, tag);
    }

    private void cleanActiveBehaviors(final DisableReason reason) {
        if (!activeBehaviorsDirty) {
            return;
        }

        final List<Behavior> newBehaviors = inputs.length > 0 && activeInputCount() > 0 ? behaviors : List.of();
        final List<Behavior> addedBehaviors = new ArrayList<>();
        final List<Behavior> removedBehaviors = new ArrayList<>();
        for (final Behavior behavior : newBehaviors) {
            if (!activeBehaviors.contains(behavior)) {
                addedBehaviors.add(behavior);
            }
        }
        for (final Behavior behavior : activeBehaviors) {
            if (!newBehaviors.contains(behavior)) {
                removedBehaviors.add(behavior);
            }
        }
        activeBehaviors = List.copyOf(newBehaviors);
        activeBehaviorsDirty = false;

        for (final Behavior behavior : addedBehaviors) {
            behavior.onEnable();
        }
        for (final Behavior behavior : removedBehaviors) {
            behavior.onDisable(reason);
        }
    }

    private void disableActive(final DisableReason reason) {
        for (final Behavior behavior : activeBehaviors) {
            behavior.onDisable(reason);
        }
        activeBehaviors = List.of();
        activeBehaviorsDirty = false;
    }
}
