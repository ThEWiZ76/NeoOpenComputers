package li.cil.oc.common;

import li.cil.oc.api.Network;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.common.item.NanomachineItemData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class SimpleNanomachineController implements Controller, WirelessEndpoint {
    private static final String TAG_ENERGY = "energy";
    private static final String TAG_ACTIVE_INPUTS = "activeInputs";
    private static final String TAG_CONNECTORS = "connectors";
    private static final String TAG_BEHAVIORS = "behaviors";
    private static final String TAG_BEHAVIOR = "behavior";
    private static final String TAG_TRIGGER_INPUTS = "triggerInputs";
    private static final String TAG_CONNECTOR_INPUTS = "connectorInputs";

    private final Player player;
    private final NanomachinesRegistry registry;
    private List<ConnectorEntry> connectors = List.of();
    private List<BehaviorEntry> behaviorEntries = List.of();
    private List<Behavior> behaviors = List.of();
    private List<Behavior> activeBehaviors = List.of();
    private boolean[] inputs = new boolean[0];
    private boolean activeBehaviorsDirty;
    private String uuid = UUID.randomUUID().toString();
    private int responsePort;
    private int commandDelay;
    private int updateTicks;
    private Runnable queuedCommand;
    private boolean configured;
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
                    created.add(new BehaviorEntry(provider, behavior, new int[0], new int[0]));
                }
            }
        }
        disableActive(DisableReason.Default);
        configureGeneratedGraph(created);
        if (configured) {
            changeBuffer(-ModSettings.nanomachinesReconfigureCost());
        } else {
            configured = true;
        }
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
        if (packet == null || sender == null || getLocalBuffer() <= 0D || commandDelay > 0 || !isSenderInCommandRange(sender)) {
            return;
        }
        final Object[] data = packet.data();
        if (data.length < 2 || !isNanomachinesHeader(data[0])) {
            return;
        }
        final Object command = commandValue(data[1]);
        if ("setResponsePort".equals(command) && data.length >= 3 && commandValue(data[2]) instanceof Number port) {
            responsePort = clampPort(port.intValue());
            respond(sender, "port", responsePort);
        } else if ("getPowerState".equals(command)) {
            respond(sender, "power", getLocalBuffer(), getLocalBufferSize());
        } else if ("saveConfiguration".equals(command)) {
            respond(sender, saveConfigurationResponse());
        } else if ("getTotalInputCount".equals(command)) {
            respond(sender, "totalInputCount", getTotalInputCount());
        } else if ("getSafeActiveInputs".equals(command)) {
            respond(sender, "safeActiveInputs", getSafeActiveInputs());
        } else if ("getMaxActiveInputs".equals(command)) {
            respond(sender, "maxActiveInputs", getMaxActiveInputs());
        } else if ("getInput".equals(command) && data.length >= 3 && commandValue(data[2]) instanceof Number index) {
            try {
                respond(sender, "input", index.intValue(), getInput(index.intValue() - 1));
            } catch (final RuntimeException e) {
                respond(sender, "input", "error");
            }
        } else if ("setInput".equals(command) && data.length >= 4 && commandValue(data[2]) instanceof Number index && commandValue(data[3]) instanceof Boolean value) {
            try {
                if (setInput(index.intValue() - 1, value)) {
                    respond(sender, "input", index.intValue(), getInput(index.intValue() - 1));
                } else {
                    respond(sender, "input", "too many active inputs");
                }
            } catch (final RuntimeException e) {
                respond(sender, "input", "error");
            }
        } else if ("getActiveEffects".equals(command)) {
            respond(sender, "effects", activeEffects());
        } else if ("getHealth".equals(command) && player != null) {
            respond(sender, "health", player.getHealth(), player.getMaxHealth());
        } else if ("getHunger".equals(command) && player != null) {
            respond(sender, "hunger", player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel());
        } else if ("getAge".equals(command) && player != null) {
            respond(sender, "age", Math.max(0, player.tickCount / 20));
        } else if ("getName".equals(command) && player != null) {
            respond(sender, "name", player.getDisplayName().getString());
        } else if ("getExperience".equals(command) && player != null) {
            respond(sender, "experience", player.experienceLevel);
        }
    }

    void dispose() {
        disableActive(DisableReason.Default);
    }

    void update() {
        if (player != null && !player.isAlive()) {
            return;
        }
        updateTicks++;
        if (commandDelay > 0) {
            commandDelay--;
            if (commandDelay == 0) {
                runQueuedCommand();
            }
        }
        updateActiveBehaviors();
        drainActiveInputEnergy();
        damageOverloadedPlayer();
    }

    void save(final CompoundTag tag) {
        tag.putDouble(TAG_ENERGY, buffer);
        tag.putIntArray(TAG_ACTIVE_INPUTS, activeInputs());
        tag.put(TAG_CONNECTORS, saveConnectorEntries());
        tag.put(TAG_BEHAVIORS, saveBehaviorEntries());
    }

    void load(final CompoundTag tag) {
        if (tag.contains(TAG_ENERGY)) {
            buffer = Math.clamp(tag.getDouble(TAG_ENERGY), 0D, getLocalBufferSize());
        }
        connectors = tag.contains(TAG_CONNECTORS, CompoundTag.TAG_LIST)
            ? loadConnectorEntries(tag.getList(TAG_CONNECTORS, CompoundTag.TAG_COMPOUND))
            : List.of();
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

    String uuid() {
        return uuid;
    }

    void saveItemConfiguration(final CompoundTag itemData) {
        final CompoundTag configuration = new CompoundTag();
        configuration.put(TAG_CONNECTORS, saveConnectorEntries());
        configuration.put(TAG_BEHAVIORS, saveBehaviorEntries());
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

    private void configureGeneratedGraph(final List<BehaviorEntry> entries) {
        final int inputCount = Math.max(1, (int) Math.ceil(entries.size() * ModSettings.nanomachineTriggerQuota()));
        final int connectorCount = (int) Math.ceil(entries.size() * ModSettings.nanomachineConnectorQuota());
        final List<Integer> triggerSourcePool = triggerSourcePool(inputCount);
        connectors = createConnectorEntries(triggerSourcePool, connectorCount);
        setBehaviorEntries(assignGeneratedInputs(entries, triggerSourcePool, connectors.size()));
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
        final List<ConnectorEntry> entries = new ArrayList<>(connectorCount);
        for (int i = 0; i < connectorCount; i++) {
            final int[] triggerInputs = takeTriggerInputs(triggerSourcePool, maxInputs);
            if (triggerInputs.length > 0) {
                entries.add(new ConnectorEntry(triggerInputs));
            }
        }
        return List.copyOf(entries);
    }

    private List<BehaviorEntry> assignGeneratedInputs(final List<BehaviorEntry> entries, final List<Integer> triggerSourcePool, final int connectorCount) {
        final int maxInputs = Math.max(1, ModSettings.nanomachineMaxInputs());
        final int maxOutputs = Math.max(1, ModSettings.nanomachineMaxOutputs());
        final List<Integer> connectorSourcePool = connectorSourcePool(connectorCount, maxOutputs);
        final int[] connectorUseCounts = new int[connectorCount];
        final List<BehaviorEntry> assigned = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            final BehaviorEntry entry = entries.get(i);
            int[] connectorInputs = new int[0];
            if (connectorCount > 0 && maxInputs > 1 && (i % maxInputs == 1 || triggerSourcePool.isEmpty()) && !connectorSourcePool.isEmpty()) {
                final int connector = connectorSourcePool.remove(0);
                if (connectorUseCounts[connector] < maxOutputs) {
                    connectorUseCounts[connector]++;
                    connectorInputs = new int[]{connector};
                }
            }
            final int triggerInputLimit = Math.max(1, maxInputs - connectorInputs.length);
            final int[] triggerInputs = takeTriggerInputs(triggerSourcePool, triggerInputLimit);
            if (triggerInputs.length > 0 || connectorInputs.length > 0) {
                assigned.add(new BehaviorEntry(entry.provider(), entry.behavior(), triggerInputs, connectorInputs));
            }
        }
        return assigned;
    }

    private List<Integer> connectorSourcePool(final int connectorCount, final int maxOutputs) {
        final List<Integer> sources = new ArrayList<>(connectorCount * maxOutputs);
        for (int output = 0; output < maxOutputs; output++) {
            for (int connector = 0; connector < connectorCount; connector++) {
                sources.add(connector);
            }
        }
        return sources;
    }

    private int[] takeTriggerInputs(final List<Integer> triggerSourcePool, final int limit) {
        final List<Integer> inputs = new ArrayList<>(limit);
        for (int i = 0; i < triggerSourcePool.size() && inputs.size() < limit; i++) {
            final int input = triggerSourcePool.get(i);
            if (!inputs.contains(input)) {
                inputs.add(input);
                triggerSourcePool.remove(i);
                i--;
            }
        }
        final int[] result = new int[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) {
            result[i] = inputs.get(i);
        }
        return result;
    }

    private int computeInputCount(final List<ConnectorEntry> connectors, final List<BehaviorEntry> entries) {
        int inputCount = Math.max(1, (int) Math.ceil(entries.size() * ModSettings.nanomachineTriggerQuota()));
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

    private ListTag saveConnectorEntries() {
        final ListTag tags = new ListTag();
        for (final ConnectorEntry entry : connectors) {
            final CompoundTag tag = new CompoundTag();
            tag.putIntArray(TAG_TRIGGER_INPUTS, entry.triggerInputs());
            tags.add(tag);
        }
        return tags;
    }

    private List<ConnectorEntry> loadConnectorEntries(final ListTag tags) {
        final List<ConnectorEntry> entries = new ArrayList<>(tags.size());
        for (int i = 0; i < tags.size(); i++) {
            entries.add(new ConnectorEntry(tags.getCompound(i).getIntArray(TAG_TRIGGER_INPUTS)));
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

    private List<BehaviorEntry> loadBehaviorEntries(final ListTag tags) {
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
        final Object[] response = new Object[data.length + 1];
        response[0] = "nanomachines";
        System.arraycopy(data, 0, response, 1, data.length);
        final Packet packet = Network.newPacket(uuid, null, responsePort, response);
        if (packet != null) {
            Network.sendWirelessPacket(this, ModSettings.nanomachinesCommandRange(), packet);
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
                if (stack.is(ModItems.NANOMACHINES.get()) && !NanomachineItemData.hasConfiguration(NanomachineItemData.dataTag(stack))) {
                    saveItemConfiguration(NanomachineItemData.dataTag(stack));
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
        if (player == null || player.getAbilities().instabuild || getLocalBuffer() <= 0D || updateTicks % 20 != 0) {
            return;
        }
        final int overload = activeInputCount() - getSafeActiveInputs();
        if (overload > 0) {
            player.hurt(player.damageSources().magic(), overload);
        }
    }

    private void updateActiveBehaviors() {
        if (getLocalBuffer() <= 0D) {
            return;
        }
        for (final Behavior behavior : getActiveBehaviors()) {
            behavior.update();
        }
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
        if (inputs.length > 0 && activeInputCount() > 0) {
            for (final BehaviorEntry entry : behaviorEntries) {
                if (entry.isActive(inputs, connectors)) {
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

    private record ConnectorEntry(int[] triggerInputs) {
        private ConnectorEntry {
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
    }

    private record BehaviorEntry(BehaviorProvider provider, Behavior behavior, int[] triggerInputs, int[] connectorInputs) {
        private BehaviorEntry {
            triggerInputs = triggerInputs.clone();
            connectorInputs = connectorInputs.clone();
        }

        private boolean isActive(final boolean[] inputs, final List<ConnectorEntry> connectors) {
            if (triggerInputs.length == 0 && connectorInputs.length == 0) {
                return false;
            }
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
