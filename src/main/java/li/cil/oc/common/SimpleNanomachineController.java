package li.cil.oc.common;

import li.cil.oc.api.Network;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.common.damage.ModDamageSources;
import li.cil.oc.common.item.NanomachineItemData;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

final class SimpleNanomachineController implements Controller, WirelessEndpoint {
    private static final String TAG_UUID = "uuid";
    private static final String TAG_PORT = "port";
    private static final String TAG_ENERGY = "energy";
    private static final String TAG_TRIGGERS = "triggers";
    private static final String TAG_IS_ACTIVE = "isActive";
    private static final String TAG_ACTIVE_INPUTS = "activeInputs";
    private static final String TAG_CONFIGURATION = "configuration";
    private static final String TAG_CONNECTORS = "connectors";
    private static final String TAG_BEHAVIORS = "behaviors";
    private static final String TAG_BEHAVIOR = "behavior";
    private static final String TAG_TRIGGER_INPUTS = "triggerInputs";
    private static final String TAG_CONNECTOR_INPUTS = "connectorInputs";

    private final Player player;
    private final NanomachinesRegistry registry;
    private final Random graphRandom;
    private List<ConnectorEntry> connectors = List.of();
    private List<BehaviorEntry> behaviorEntries = List.of();
    private List<Behavior> behaviors = List.of();
    private List<Behavior> activeBehaviors = List.of();
    private boolean[] inputs = new boolean[0];
    private boolean activeBehaviorsDirty;
    private boolean hadPower = true;
    private String uuid = UUID.randomUUID().toString();
    private int responsePort;
    private int commandDelay;
    private int updateTicks;
    private Runnable queuedCommand;
    private boolean configured;
    private double buffer;

    SimpleNanomachineController(final Player player, final NanomachinesRegistry registry) {
        this(player, registry, new Random(player == null ? 0L : UUID.randomUUID().getMostSignificantBits()));
    }

    SimpleNanomachineController(final Player player, final NanomachinesRegistry registry, final Random graphRandom) {
        this.player = player;
        this.registry = registry;
        this.graphRandom = graphRandom == null ? new Random(0L) : graphRandom;
        buffer = ModSettings.nanomachinesBuffer() * 0.25D;
        reconfigure();
        if (isServerController()) {
            Network.joinWirelessNetwork(this);
        }
    }

    @Override
    public Controller reconfigure() {
        final List<BehaviorEntry> created = createBehaviorEntries();
        disableActive(DisableReason.Default);
        configureGeneratedGraph(created);
        if (configured) {
            applyReconfigureEffects();
            changeBuffer(-ModSettings.nanomachinesReconfigureCost());
        } else {
            configured = true;
        }
        return this;
    }

    void debugConfiguration() {
        final List<BehaviorEntry> created = createBehaviorEntries();
        final List<BehaviorEntry> debugEntries = new ArrayList<>(created.size());
        for (int i = 0; i < created.size(); i++) {
            final BehaviorEntry entry = created.get(i);
            debugEntries.add(new BehaviorEntry(entry.provider(), entry.behavior(), new int[]{i}, new int[0]));
        }
        disableActive(DisableReason.Default);
        connectors = List.of();
        setBehaviorEntries(debugEntries);
        saveState();
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
        if (value && activeInputCount() >= getMaxActiveInputs()) {
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
                return entry.activeInputCount(inputs, connectors);
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
        if (delta < 0D && (ModSettings.ignorePower() || isCreativePlayer())) {
            return 0D;
        }
        final double requested = buffer + delta;
        buffer = Math.clamp(requested, 0D, getLocalBufferSize());
        saveState();
        return requested - buffer;
    }

    @Override
    public int x() {
        return player == null ? 0 : player.blockPosition().getX();
    }

    @Override
    public int y() {
        return player == null ? 0 : player.blockPosition().getY();
    }

    @Override
    public int z() {
        return player == null ? 0 : player.blockPosition().getZ();
    }

    @Override
    public Level world() {
        return player == null ? null : player.level();
    }

    @Override
    public void receivePacket(final Packet packet, final WirelessEndpoint sender) {
        if (packet == null || sender == null || isDeadPlayer() || getLocalBuffer() <= 0D || commandDelay > 0 || !isSenderInCommandRange(sender)) {
            return;
        }
        final Object[] data = packet.data();
        if (data.length < 2 || !isNanomachinesHeader(data[0])) {
            return;
        }
        final Object command = commandValue(data[1]);
        if ("setResponsePort".equals(command) && data.length == 3 && commandValue(data[2]) instanceof Number port) {
            responsePort = clampPort(port.intValue());
            respond(sender, "port", responsePort);
        } else if ("getPowerState".equals(command) && data.length == 2) {
            respond(sender, "power", getLocalBuffer(), getLocalBufferSize());
        } else if ("saveConfiguration".equals(command) && data.length == 2) {
            respond(sender, saveConfigurationResponse());
        } else if ("getTotalInputCount".equals(command) && data.length == 2) {
            respond(sender, "totalInputCount", getTotalInputCount());
        } else if ("getSafeActiveInputs".equals(command) && data.length == 2) {
            respond(sender, "safeActiveInputs", getSafeActiveInputs());
        } else if ("getMaxActiveInputs".equals(command) && data.length == 2) {
            respond(sender, "maxActiveInputs", getMaxActiveInputs());
        } else if ("getInput".equals(command) && data.length == 3 && commandValue(data[2]) instanceof Number index) {
            try {
                respond(sender, "input", index.intValue(), getInput(index.intValue() - 1));
            } catch (final RuntimeException e) {
                respond(sender, "input", "error");
            }
        } else if ("setInput".equals(command) && data.length == 4 && commandValue(data[2]) instanceof Number index && commandValue(data[3]) instanceof Boolean value) {
            try {
                if (setInput(index.intValue() - 1, value)) {
                    respond(sender, "input", index.intValue(), getInput(index.intValue() - 1));
                } else {
                    respond(sender, "input", "too many active inputs");
                }
            } catch (final RuntimeException e) {
                respond(sender, "input", "error");
            }
        } else if ("getActiveEffects".equals(command) && data.length == 2) {
            respond(sender, "effects", activeEffects());
        } else if ("getHealth".equals(command) && data.length == 2 && player != null) {
            respond(sender, "health", player.getHealth(), player.getMaxHealth());
        } else if ("getHunger".equals(command) && data.length == 2 && player != null) {
            respond(sender, "hunger", player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel());
        } else if ("getAge".equals(command) && data.length == 2 && player != null) {
            respond(sender, "age", idleSeconds());
        } else if ("getName".equals(command) && data.length == 2 && player != null) {
            respond(sender, "name", player.getDisplayName().getString());
        } else if ("getExperience".equals(command) && data.length == 2 && player != null) {
            respond(sender, "experience", player.experienceLevel);
        }
    }

    void dispose() {
        resetInputs(DisableReason.Default);
        if (isServerController()) {
            Network.leaveWirelessNetwork(this);
        }
    }

    void update() {
        if (player != null && !player.isAlive()) {
            return;
        }
        if (isServerController()) {
            Network.updateWirelessNetwork(this);
        }
        updateTicks++;
        if (commandDelay > 0) {
            commandDelay--;
            if (commandDelay == 0) {
                runQueuedCommand();
            }
        }
        final boolean hasPower = updatePowerState();
        if (hasPower) {
            updateActiveBehaviors();
            drainActiveInputEnergy();
            damageOverloadedPlayer();
        }
        hadPower = hasPower;
    }

    List<String> activeParticleEffects() {
        final List<String> effects = new ArrayList<>();
        for (final Behavior behavior : getActiveBehaviors()) {
            final String name = behavior.getNameHint();
            if (name != null && name.startsWith("particles.") && name.length() > "particles.".length()) {
                final String effect = name.substring("particles.".length());
                if (!effect.isBlank()) {
                    final int count = Math.max(1, getInputCount(behavior));
                    for (int i = 0; i < count; i++) {
                        effects.add(effect);
                    }
                }
            }
        }
        return List.copyOf(effects);
    }

    List<String> configurationLines() {
        final List<String> lines = new ArrayList<>(behaviorEntries.size());
        for (final BehaviorEntry entry : behaviorEntries) {
            final StringBuilder line = new StringBuilder(behaviorName(entry.behavior())).append(" <- (");
            boolean first = true;
            for (final int input : entry.triggerInputs()) {
                if (first) {
                    first = false;
                } else {
                    line.append(", ");
                }
                line.append(input + 1);
            }
            for (final int connector : entry.connectorInputs()) {
                if (connector < 0 || connector >= connectors.size()) {
                    continue;
                }
                if (first) {
                    first = false;
                } else {
                    line.append(", ");
                }
                line.append('(');
                final int[] triggerInputs = connectors.get(connector).triggerInputs();
                for (int i = 0; i < triggerInputs.length; i++) {
                    if (i > 0) {
                        line.append(", ");
                    }
                    line.append(triggerInputs[i] + 1);
                }
                line.append(')');
            }
            lines.add(line.append(')').toString());
        }
        return List.copyOf(lines);
    }

    private static String behaviorName(final Behavior behavior) {
        final String name = behavior.getNameHint();
        return name == null ? behavior.getClass().getSimpleName() : name;
    }

    void save(final CompoundTag tag) {
        tag.putString(TAG_UUID, uuid);
        tag.putInt(TAG_PORT, responsePort);
        tag.putDouble(TAG_ENERGY, buffer);
        final CompoundTag configuration = new CompoundTag();
        saveGraph(configuration, false);
        copyGraphTags(configuration, tag);
        tag.put(TAG_CONFIGURATION, configuration);
        tag.putIntArray(TAG_ACTIVE_INPUTS, activeInputs());
    }

    void load(final CompoundTag tag) {
        if (tag.contains(TAG_UUID)) {
            uuid = tag.getString(TAG_UUID);
        }
        if (tag.contains(TAG_PORT)) {
            responsePort = clampPort(tag.getInt(TAG_PORT));
        }
        if (tag.contains(TAG_ENERGY)) {
            buffer = Math.clamp(tag.getDouble(TAG_ENERGY), 0D, getLocalBufferSize());
        }
        final CompoundTag graphTag = tag.contains(TAG_CONFIGURATION, CompoundTag.TAG_COMPOUND)
            ? tag.getCompound(TAG_CONFIGURATION)
            : tag;
        final int savedTriggerCount = graphTag.contains(TAG_TRIGGERS, CompoundTag.TAG_LIST)
            ? graphTag.getList(TAG_TRIGGERS, CompoundTag.TAG_COMPOUND).size()
            : -1;
        connectors = graphTag.contains(TAG_CONNECTORS, CompoundTag.TAG_LIST)
            ? loadConnectorEntries(graphTag.getList(TAG_CONNECTORS, CompoundTag.TAG_COMPOUND), savedTriggerCount)
            : List.of();
        if (graphTag.contains(TAG_BEHAVIORS, CompoundTag.TAG_LIST)) {
            disableActive(DisableReason.Default);
            setBehaviorEntries(loadBehaviorEntries(graphTag.getList(TAG_BEHAVIORS, CompoundTag.TAG_COMPOUND), savedTriggerCount, connectors.size()));
        }
        loadTriggerStates(graphTag);
        activeBehaviorsDirty = true;
    }

    String uuid() {
        return uuid;
    }

    void saveItemConfiguration(final CompoundTag itemData) {
        final CompoundTag configuration = new CompoundTag();
        saveGraph(configuration, true);
        NanomachineItemData.save(itemData, uuid, configuration);
    }

    void loadItemConfiguration(final CompoundTag itemData) {
        if (!NanomachineItemData.hasConfiguration(itemData)) {
            return;
        }
        final String savedUuid = NanomachineItemData.uuid(itemData);
        if (!savedUuid.isEmpty()) {
            uuid = savedUuid;
        }
        disableActive(DisableReason.Default);
        load(NanomachineItemData.configuration(itemData));
        saveState();
    }

    private void setBehaviorEntries(final List<BehaviorEntry> entries) {
        behaviorEntries = List.copyOf(entries);
        final List<Behavior> created = new ArrayList<>(behaviorEntries.size());
        for (final BehaviorEntry entry : behaviorEntries) {
            created.add(entry.behavior());
        }
        behaviors = List.copyOf(created);
        activeBehaviors = List.of();
        inputs = new boolean[computeInputCount(connectors, behaviorEntries)];
        activeBehaviorsDirty = true;
    }

    private List<BehaviorEntry> createBehaviorEntries() {
        final List<BehaviorEntry> created = new ArrayList<>();
        for (final var provider : registry.getProviders()) {
            final Iterable<Behavior> providerBehaviors = provider.createBehaviors(player);
            if (providerBehaviors == null) {
                continue;
            }
            for (final Behavior behavior : providerBehaviors) {
                if (behavior != null) {
                    created.add(new BehaviorEntry(provider, behavior, new int[0], new int[0]));
                }
            }
        }
        return created;
    }

    private void configureGeneratedGraph(final List<BehaviorEntry> entries) {
        final int inputCount = generatedTriggerCount(entries.size());
        final int connectorCount = (int) Math.ceil(entries.size() * ModSettings.nanomachineConnectorQuota());
        final List<Integer> triggerSourcePool = triggerSourcePool(inputCount);
        connectors = createConnectorEntries(triggerSourcePool, connectorCount);
        setBehaviorEntries(cleanGeneratedGraph(assignGeneratedInputs(entries, triggerSourcePool, connectors.size())));
    }

    private int generatedTriggerCount(final int behaviorCount) {
        return Math.max(0, (int) Math.ceil(behaviorCount * ModSettings.nanomachineTriggerQuota()));
    }

    private List<Integer> triggerSourcePool(final int inputCount) {
        final int maxOutputs = Math.max(1, ModSettings.nanomachineMaxOutputs());
        final List<Integer> sources = new ArrayList<>(inputCount * maxOutputs);
        for (int output = 0; output < maxOutputs; output++) {
            for (int input = 0; input < inputCount; input++) {
                sources.add(input);
            }
        }
        return sources;
    }

    private List<ConnectorEntry> createConnectorEntries(final List<Integer> triggerSourcePool, final int connectorCount) {
        final int maxInputs = Math.max(1, ModSettings.nanomachineMaxInputs());
        final List<int[]> inputs = new ArrayList<>(Collections.nCopies(connectorCount, new int[0]));
        final List<Integer> sinkPool = shuffledIndices(connectorCount);
        for (final int sink : sinkPool) {
            final int[] triggerInputs = takeRandomInputs(triggerSourcePool, maxInputs);
            if (triggerInputs.length > 0) {
                inputs.set(sink, triggerInputs);
            }
        }
        return inputs.stream()
            .map(ConnectorEntry::new)
            .toList();
    }

    private List<BehaviorEntry> assignGeneratedInputs(final List<BehaviorEntry> entries, final List<Integer> triggerSourcePool, final int connectorCount) {
        final int maxInputs = Math.max(1, ModSettings.nanomachineMaxInputs());
        final int maxOutputs = Math.max(1, ModSettings.nanomachineMaxOutputs());
        final List<SourceRef> sourcePool = behaviorSourcePool(triggerSourcePool, connectorCount, maxOutputs);
        final int[][] triggerInputs = new int[entries.size()][];
        final int[][] connectorInputs = new int[entries.size()][];
        for (int i = 0; i < entries.size(); i++) {
            triggerInputs[i] = new int[0];
            connectorInputs[i] = new int[0];
        }
        final List<Integer> sinkPool = shuffledIndices(entries.size());
        for (final int sink : sinkPool) {
            final List<SourceRef> sources = takeRandomSources(sourcePool, maxInputs);
            triggerInputs[sink] = sources.stream()
                .filter(source -> !source.connector())
                .mapToInt(SourceRef::index)
                .toArray();
            connectorInputs[sink] = sources.stream()
                .filter(SourceRef::connector)
                .mapToInt(SourceRef::index)
                .toArray();
        }

        final List<BehaviorEntry> assigned = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            final BehaviorEntry entry = entries.get(i);
            if (triggerInputs[i].length > 0 || connectorInputs[i].length > 0) {
                assigned.add(new BehaviorEntry(entry.provider(), entry.behavior(), triggerInputs[i], connectorInputs[i]));
            }
        }
        return assigned;
    }

    private List<BehaviorEntry> cleanGeneratedGraph(final List<BehaviorEntry> entries) {
        final int[] connectorRemap = new int[connectors.size()];
        Arrays.fill(connectorRemap, -1);
        final List<ConnectorEntry> liveConnectors = new ArrayList<>(connectors.size());
        for (int i = 0; i < connectors.size(); i++) {
            final ConnectorEntry connector = connectors.get(i);
            if (connector.triggerInputs().length > 0) {
                connectorRemap[i] = liveConnectors.size();
                liveConnectors.add(connector);
            }
        }

        final List<BehaviorEntry> liveEntries = new ArrayList<>(entries.size());
        for (final BehaviorEntry entry : entries) {
            final int[] connectorInputs = Arrays.stream(entry.connectorInputs())
                .filter(input -> input >= 0 && input < connectorRemap.length && connectorRemap[input] >= 0)
                .map(input -> connectorRemap[input])
                .toArray();
            if (entry.triggerInputs().length > 0 || connectorInputs.length > 0) {
                liveEntries.add(new BehaviorEntry(entry.provider(), entry.behavior(), entry.triggerInputs(), connectorInputs));
            }
        }

        connectors = List.copyOf(liveConnectors);
        return liveEntries;
    }

    private List<SourceRef> behaviorSourcePool(final List<Integer> triggerSourcePool, final int connectorCount, final int maxOutputs) {
        final List<SourceRef> sources = new ArrayList<>(triggerSourcePool.size() + connectorCount * maxOutputs);
        for (final int trigger : triggerSourcePool) {
            sources.add(new SourceRef(false, trigger));
        }
        triggerSourcePool.clear();
        for (int output = 0; output < maxOutputs; output++) {
            for (int connector = 0; connector < connectorCount; connector++) {
                sources.add(new SourceRef(true, connector));
            }
        }
        return sources;
    }

    private List<Integer> shuffledIndices(final int size) {
        final List<Integer> indices = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, graphRandom);
        return indices;
    }

    private int[] takeRandomInputs(final List<Integer> sourcePool, final int maxInputs) {
        final List<SourceRef> refs = new ArrayList<>(sourcePool.size());
        for (final int input : sourcePool) {
            refs.add(new SourceRef(false, input));
        }
        final List<SourceRef> selected = takeRandomSources(refs, maxInputs);
        sourcePool.clear();
        for (final SourceRef source : refs) {
            sourcePool.add(source.index());
        }
        return selected.stream()
            .mapToInt(SourceRef::index)
            .toArray();
    }

    private List<SourceRef> takeRandomSources(final List<SourceRef> sourcePool, final int maxInputs) {
        final List<SourceRef> inputs = new ArrayList<>(maxInputs);
        final int count = graphRandom.nextInt(maxInputs) + 1;
        for (int i = 0; i < count && !sourcePool.isEmpty(); i++) {
            final int baseIndex = graphRandom.nextInt(sourcePool.size());
            final int sourceIndex = firstAvailableSource(sourcePool, inputs, baseIndex);
            if (sourceIndex >= 0) {
                inputs.add(sourcePool.remove(sourceIndex));
            }
        }
        return inputs;
    }

    private int firstAvailableSource(final List<SourceRef> sourcePool, final List<SourceRef> inputs, final int baseIndex) {
        for (int offset = 0; offset < sourcePool.size(); offset++) {
            final int index = (baseIndex + offset) % sourcePool.size();
            if (!inputs.contains(sourcePool.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private int computeInputCount(final List<ConnectorEntry> connectors, final List<BehaviorEntry> entries) {
        int inputCount = generatedTriggerCount(entries.size());
        for (final ConnectorEntry connector : connectors) {
            for (final int input : connector.triggerInputs()) {
                inputCount = Math.max(inputCount, input + 1);
            }
        }
        for (final BehaviorEntry entry : entries) {
            for (final int input : entry.triggerInputs()) {
                inputCount = Math.max(inputCount, input + 1);
            }
        }
        return inputCount;
    }

    private void saveGraph(final CompoundTag tag, final boolean forItem) {
        tag.put(TAG_TRIGGERS, saveTriggerEntries(forItem));
        tag.put(TAG_CONNECTORS, saveConnectorEntries());
        tag.put(TAG_BEHAVIORS, saveBehaviorEntries());
    }

    private void copyGraphTags(final CompoundTag source, final CompoundTag target) {
        for (final String key : source.getAllKeys()) {
            final Tag value = source.get(key);
            if (value != null) {
                target.put(key, value.copy());
            }
        }
    }

    private ListTag saveTriggerEntries(final boolean forItem) {
        final ListTag tags = new ListTag();
        for (final boolean input : inputs) {
            final CompoundTag tag = new CompoundTag();
            tag.putBoolean(TAG_IS_ACTIVE, input && !forItem);
            tags.add(tag);
        }
        return tags;
    }

    private void loadTriggerStates(final CompoundTag tag) {
        if (tag.contains(TAG_TRIGGERS, CompoundTag.TAG_LIST)) {
            final ListTag triggers = tag.getList(TAG_TRIGGERS, CompoundTag.TAG_COMPOUND);
            inputs = new boolean[triggers.size()];
            for (int i = 0; i < inputs.length; i++) {
                inputs[i] = triggers.getCompound(i).getBoolean(TAG_IS_ACTIVE);
            }
            return;
        }

        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = false;
        }
        for (final int activeInput : tag.getIntArray(TAG_ACTIVE_INPUTS)) {
            if (activeInput >= 0 && activeInput < inputs.length) {
                inputs[activeInput] = true;
            }
        }
    }

    private ListTag saveConnectorEntries() {
        final ListTag tags = new ListTag();
        for (final ConnectorEntry entry : connectors) {
            final CompoundTag tag = new CompoundTag();
            tag.putIntArray(TAG_TRIGGER_INPUTS, entry.triggerInputs());
            tags.add(tag);
        }
        return tags;
    }

    private List<ConnectorEntry> loadConnectorEntries(final ListTag tags, final int triggerCount) {
        final List<ConnectorEntry> entries = new ArrayList<>(tags.size());
        for (int i = 0; i < tags.size(); i++) {
            final int[] triggerInputs = tags.getCompound(i).getIntArray(TAG_TRIGGER_INPUTS);
            validateIndices(triggerInputs, triggerCount);
            entries.add(new ConnectorEntry(triggerInputs));
        }
        return List.copyOf(entries);
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
            tag.putIntArray(TAG_CONNECTOR_INPUTS, entry.connectorInputs());
            tags.add(tag);
        }
        return tags;
    }

    private List<BehaviorEntry> loadBehaviorEntries(final ListTag tags, final int triggerCount, final int connectorCount) {
        final List<BehaviorEntry> entries = new ArrayList<>();
        final int fallbackInputCount = Math.max(1, (int) Math.ceil(tags.size() * ModSettings.nanomachineTriggerQuota()));
        for (int i = 0; i < tags.size(); i++) {
            final CompoundTag tag = tags.getCompound(i);
            final CompoundTag data = tag.getCompound(TAG_BEHAVIOR);
            final int[] triggerInputs = tag.contains(TAG_TRIGGER_INPUTS, CompoundTag.TAG_INT_ARRAY)
                ? tag.getIntArray(TAG_TRIGGER_INPUTS)
                : new int[]{i % fallbackInputCount};
            final int[] connectorInputs = tag.contains(TAG_CONNECTOR_INPUTS, CompoundTag.TAG_INT_ARRAY)
                ? tag.getIntArray(TAG_CONNECTOR_INPUTS)
                : new int[0];
            validateIndices(triggerInputs, triggerCount);
            validateIndices(connectorInputs, connectorCount);
            for (final BehaviorProvider provider : registry.getProviders()) {
                final Behavior behavior = provider.readFromNBT(player, data);
                if (behavior != null) {
                    entries.add(new BehaviorEntry(provider, behavior, triggerInputs, connectorInputs));
                    break;
                }
            }
        }
        return entries;
    }

    private static void validateIndices(final int[] indices, final int size) {
        if (size < 0) {
            return;
        }
        for (final int index : indices) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("Saved nanomachine graph index " + index + " outside 0.." + (size - 1));
            }
        }
    }

    private void respond(final WirelessEndpoint endpoint, final Object... data) {
        queuedCommand = () -> sendResponse(endpoint, data);
        commandDelay = (int) (ModSettings.nanomachinesCommandDelay() * 20D);
        if (commandDelay <= 0) {
            runQueuedCommand();
        }
    }

    private void runQueuedCommand() {
        final Runnable command = queuedCommand;
        queuedCommand = null;
        if (command != null) {
            command.run();
        }
    }

    private void sendResponse(final WirelessEndpoint endpoint, final Object... data) {
        if (responsePort <= 0) {
            return;
        }
        final double cost = ModSettings.wirelessCostPerRange(1) * ModSettings.nanomachinesCommandRange() * ModSettings.nanomachinesCommandRange();
        if (changeBuffer(-cost) <= -0.1D) {
            return;
        }
        final Object[] response = new Object[data.length + 1];
        response[0] = "nanomachines";
        System.arraycopy(data, 0, response, 1, data.length);
        final Packet packet = Network.newPacket(uuid, null, responsePort, response);
        if (packet != null) {
            final double range = ModSettings.nanomachinesCommandRange();
            Network.sendWirelessPacket(this, range * range, packet);
        }
    }

    private static boolean isNanomachinesHeader(final Object value) {
        return "nanomachines".equals(commandValue(value));
    }

    private static Object commandValue(final Object value) {
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return value;
    }

    private static int clampPort(final int port) {
        return Math.max(0, Math.min(0xFFFF, port));
    }

    private boolean isSenderInCommandRange(final WirelessEndpoint sender) {
        if (player == null) {
            return true;
        }
        if (sender.world() != player.level()) {
            return false;
        }
        final double dx = sender.x() + 0.5D - player.getX();
        final double dy = sender.y() + 0.5D - player.getY();
        final double dz = sender.z() + 0.5D - player.getZ();
        final double range = ModSettings.nanomachinesCommandRange();
        final double effectiveRange = range * range;
        return Math.sqrt(dx * dx + dy * dy + dz * dz) <= effectiveRange;
    }

    private int idleSeconds() {
        if (player instanceof ServerPlayer serverPlayer) {
            final long seconds = Math.max(0L, (Util.getMillis() - serverPlayer.getLastActionTime()) / 1000L);
            return (int) Math.min(Integer.MAX_VALUE, seconds);
        }
        return 0;
    }

    private String activeEffects() {
        final StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (final Behavior behavior : getActiveBehaviors()) {
            final String name = behavior.getNameHint();
            if (name != null && !name.isEmpty()) {
                if (!first) {
                    builder.append(',');
                }
                builder.append(name.replace(',', '_').replace('"', '_'));
                first = false;
            }
        }
        return builder.append('}').toString();
    }

    private Object[] saveConfigurationResponse() {
        if (player == null) {
            return new Object[]{"saved", false, "no nanomachines"};
        }
        try {
            for (final ItemStack stack : player.getInventory().items) {
                if (stack.is(ModItems.NANOMACHINES.get()) && !NanomachineItemData.hasConfiguration(stack)) {
                    final ItemStack configured = stack.split(1);
                    saveItemConfiguration(NanomachineItemData.dataTag(configured));
                    if (!player.getInventory().add(configured)) {
                        player.drop(configured, false);
                    }
                    return new Object[]{"saved", true};
                }
            }
            return new Object[]{"saved", false, "no nanomachines"};
        } catch (final RuntimeException e) {
            return new Object[]{"saved", false, "error"};
        }
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

    private void damageOverloadedPlayer() {
        if (player == null || isCreativePlayer() || getLocalBuffer() <= 0D || updateTicks % 20 != 0) {
            return;
        }
        final int overload = activeInputCount() - getSafeActiveInputs();
        if (overload > 0) {
            player.hurt(ModDamageSources.nanomachinesOverload(player), overload);
        }
    }

    private void updateActiveBehaviors() {
        for (final Behavior behavior : getActiveBehaviors()) {
            behavior.update();
        }
    }

    private boolean updatePowerState() {
        boolean hasPower = getLocalBuffer() > 0D || ModSettings.ignorePower();
        if (hasPower != hadPower) {
            final List<Behavior> active = new ArrayList<>();
            for (final Behavior behavior : getActiveBehaviors()) {
                active.add(behavior);
            }
            if (!hasPower) {
                for (final Behavior behavior : active) {
                    behavior.onDisable(DisableReason.OutOfEnergy);
                }
                hasPower = getLocalBuffer() > 0D || ModSettings.ignorePower();
            } else {
                for (final Behavior behavior : active) {
                    behavior.onEnable();
                }
            }
        }
        return hasPower;
    }

    private void drainActiveInputEnergy() {
        final int tickFrequency = Math.max(1, ModSettings.mfuTickFrequency());
        if (getLocalBuffer() <= 0D || updateTicks % tickFrequency != 0) {
            return;
        }
        changeBuffer(-ModSettings.nanomachinesInputCost() * tickFrequency * (activeInputCount() + 0.5D));
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
        if (player == null) {
            return;
        }
        final CompoundTag tag = new CompoundTag();
        save(tag);
        player.getPersistentData().put(NanomachinesRegistry.TAG_CONTROLLER, tag);
    }

    private void cleanActiveBehaviors(final DisableReason reason) {
        if (!activeBehaviorsDirty) {
            return;
        }

        final List<Behavior> newBehaviors = new ArrayList<>();
        for (final BehaviorEntry entry : behaviorEntries) {
            if (entry.isActive(inputs, connectors)) {
                newBehaviors.add(entry.behavior());
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

    private void resetInputs(final DisableReason reason) {
        Arrays.fill(inputs, false);
        activeBehaviorsDirty = true;
        cleanActiveBehaviors(reason);
    }

    private boolean isCreativePlayer() {
        return player != null && (player.isCreative() || player.getAbilities().instabuild);
    }

    private boolean isDeadPlayer() {
        return player != null && !player.isAlive();
    }

    private boolean isServerController() {
        return player != null && !player.level().isClientSide();
    }

    private void applyReconfigureEffects() {
        if (player == null) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100));
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 150));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200));
    }

    private record ConnectorEntry(int[] triggerInputs) {
        private ConnectorEntry {
            triggerInputs = triggerInputs.clone();
        }

        private boolean isActive(final boolean[] inputs) {
            for (final int input : triggerInputs) {
                if (input < 0 || input >= inputs.length || !inputs[input]) {
                    return false;
                }
            }
            return true;
        }
    }

    private record SourceRef(boolean connector, int index) {
    }

    private record BehaviorEntry(BehaviorProvider provider, Behavior behavior, int[] triggerInputs, int[] connectorInputs) {
        private BehaviorEntry {
            triggerInputs = triggerInputs.clone();
            connectorInputs = connectorInputs.clone();
        }

        private boolean isActive(final boolean[] inputs, final List<ConnectorEntry> connectors) {
            for (final int input : triggerInputs) {
                if (input < 0 || input >= inputs.length || !inputs[input]) {
                    return false;
                }
            }
            for (final int input : connectorInputs) {
                if (input < 0 || input >= connectors.size() || !connectors.get(input).isActive(inputs)) {
                    return false;
                }
            }
            return true;
        }

        private int activeInputCount(final boolean[] inputs, final List<ConnectorEntry> connectors) {
            int count = 0;
            for (final int input : triggerInputs) {
                if (input >= 0 && input < inputs.length && inputs[input]) {
                    count++;
                }
            }
            for (final int input : connectorInputs) {
                if (input >= 0 && input < connectors.size() && connectors.get(input).isActive(inputs)) {
                    count++;
                }
            }
            return count;
        }
    }
}
