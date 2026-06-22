package li.cil.oc.common;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.nanomachines.DisableReason;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

final class SimpleNanomachineController implements Controller {
    private static final String TAG_ENERGY = "energy";
    private static final String TAG_ACTIVE_INPUTS = "activeInputs";
    private static final String TAG_BEHAVIORS = "behaviors";
    private static final String TAG_BEHAVIOR = "behavior";
    private static final String TAG_TRIGGER_INPUTS = "triggerInputs";

    private final Player player;
    private final NanomachinesRegistry registry;
    private List<BehaviorEntry> behaviorEntries = List.of();
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
        final List<BehaviorEntry> created = new ArrayList<>();
        for (final var provider : registry.getProviders()) {
            for (final Behavior behavior : provider.createBehaviors(player)) {
                if (behavior != null) {
                    created.add(new BehaviorEntry(provider, behavior, new int[0]));
                }
            }
        }
        disableActive(DisableReason.Default);
        setBehaviorEntries(assignTriggerInputs(created));
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
        for (final BehaviorEntry entry : behaviorEntries) {
            if (entry.behavior().equals(behavior)) {
                return entry.activeInputCount(inputs);
            }
        }
        return 0;
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
        tag.put(TAG_BEHAVIORS, saveBehaviorEntries());
    }

    void load(final CompoundTag tag) {
        if (tag.contains(TAG_ENERGY)) {
            buffer = Math.clamp(tag.getDouble(TAG_ENERGY), 0D, getLocalBufferSize());
        }
        if (tag.contains(TAG_BEHAVIORS, CompoundTag.TAG_LIST)) {
            disableActive(DisableReason.Default);
            setBehaviorEntries(loadBehaviorEntries(tag.getList(TAG_BEHAVIORS, CompoundTag.TAG_COMPOUND)));
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

    private void setBehaviorEntries(final List<BehaviorEntry> entries) {
        behaviorEntries = List.copyOf(entries);
        final List<Behavior> created = new ArrayList<>(behaviorEntries.size());
        for (final BehaviorEntry entry : behaviorEntries) {
            created.add(entry.behavior());
        }
        behaviors = List.copyOf(created);
        activeBehaviors = List.of();
        inputs = new boolean[computeInputCount(behaviorEntries)];
        activeBehaviorsDirty = true;
    }

    private List<BehaviorEntry> assignTriggerInputs(final List<BehaviorEntry> entries) {
        final int inputCount = Math.max(1, (int) Math.ceil(entries.size() * ModSettings.nanomachineTriggerQuota()));
        final List<BehaviorEntry> assigned = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            final BehaviorEntry entry = entries.get(i);
            assigned.add(new BehaviorEntry(entry.provider(), entry.behavior(), new int[]{i % inputCount}));
        }
        return assigned;
    }

    private int computeInputCount(final List<BehaviorEntry> entries) {
        int inputCount = Math.max(1, (int) Math.ceil(entries.size() * ModSettings.nanomachineTriggerQuota()));
        for (final BehaviorEntry entry : entries) {
            for (final int input : entry.triggerInputs()) {
                inputCount = Math.max(inputCount, input + 1);
            }
        }
        return inputCount;
    }

    private ListTag saveBehaviorEntries() {
        final ListTag tags = new ListTag();
        for (final BehaviorEntry entry : behaviorEntries) {
            final CompoundTag tag = new CompoundTag();
            final CompoundTag data = entry.provider().writeToNBT(entry.behavior());
            if (data != null) {
                tag.put(TAG_BEHAVIOR, data);
            }
            tag.putIntArray(TAG_TRIGGER_INPUTS, entry.triggerInputs());
            tags.add(tag);
        }
        return tags;
    }

    private List<BehaviorEntry> loadBehaviorEntries(final ListTag tags) {
        final List<BehaviorEntry> entries = new ArrayList<>();
        final int fallbackInputCount = Math.max(1, (int) Math.ceil(tags.size() * ModSettings.nanomachineTriggerQuota()));
        for (int i = 0; i < tags.size(); i++) {
            final CompoundTag tag = tags.getCompound(i);
            final CompoundTag data = tag.getCompound(TAG_BEHAVIOR);
            final int[] triggerInputs = tag.contains(TAG_TRIGGER_INPUTS, CompoundTag.TAG_INT_ARRAY)
                ? tag.getIntArray(TAG_TRIGGER_INPUTS)
                : new int[]{i % fallbackInputCount};
            for (final BehaviorProvider provider : registry.getProviders()) {
                final Behavior behavior = provider.readFromNBT(player, data);
                if (behavior != null) {
                    entries.add(new BehaviorEntry(provider, behavior, triggerInputs));
                    break;
                }
            }
        }
        return entries;
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

        final List<Behavior> newBehaviors = new ArrayList<>();
        if (inputs.length > 0 && activeInputCount() > 0) {
            for (final BehaviorEntry entry : behaviorEntries) {
                if (entry.isActive(inputs)) {
                    newBehaviors.add(entry.behavior());
                }
            }
        }
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

    private record BehaviorEntry(BehaviorProvider provider, Behavior behavior, int[] triggerInputs) {
        private BehaviorEntry {
            triggerInputs = triggerInputs.clone();
        }

        private boolean isActive(final boolean[] inputs) {
            if (triggerInputs.length == 0) {
                return false;
            }
            for (final int input : triggerInputs) {
                if (input < 0 || input >= inputs.length || !inputs[input]) {
                    return false;
                }
            }
            return true;
        }

        private int activeInputCount(final boolean[] inputs) {
            int count = 0;
            for (final int input : triggerInputs) {
                if (input >= 0 && input < inputs.length && inputs[input]) {
                    count++;
                }
            }
            return count;
        }
    }
}
