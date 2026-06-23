package li.cil.oc.common.blockentity;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.component.LinkedNetwork;
import li.cil.oc.common.item.LinkedCardItem;
import li.cil.oc.common.menu.RelayMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Queue;

public class RelayBlockEntity extends BlockEntity implements SidedEnvironment, Container, MenuProvider, WirelessEndpoint, Analyzable, LinkedNetwork.Endpoint {
    public static final int CONTAINER_SIZE = 4;
    public static final int CPU_SLOT = 0;
    public static final int MEMORY_SLOT = 1;
    public static final int HDD_SLOT = 2;
    public static final int CARD_SLOT = 3;
    public static final int DEFAULT_MAX_QUEUE_SIZE = 20;
    public static final int DEFAULT_RELAY_DELAY = 5;
    public static final int DEFAULT_RELAY_AMOUNT = 1;

    private static final String NETWORK_MESSAGE = "network.message";
    private static final String TAG_PLUGS = "oc:plugs";
    private static final String TAG_QUEUE = "oc:queue";
    private static final String TAG_STRENGTH = "oc:strength";
    private static final String TAG_REPEATER = "oc:isRepeater";
    private static final String TAG_RELAY_COOLDOWN = "oc:relayCooldown";
    private static final String TAG_SIDE = "side";
    private static final String TAG_PACKET = "packet";
    private static final int MAX_TIER = 2;

    private final Plug[] plugs = new Plug[Direction.values().length];
    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final Queue<QueuedPacket> queue = new ArrayDeque<>();
    private int maxQueueSize = DEFAULT_MAX_QUEUE_SIZE;
    private int relayDelay = DEFAULT_RELAY_DELAY;
    private int relayAmount = DEFAULT_RELAY_AMOUNT;
    private int relayCooldown = -1;
    private boolean wirelessEnabled;
    private int wirelessTier = -1;
    private double wirelessMaxRange;
    private double wirelessStrength;
    private boolean linkedEnabled;
    private String linkedChannel = LinkedNetwork.DEFAULT_CHANNEL;
    private boolean isRepeater = true;

    public RelayBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.RELAY.get(), pos, blockState);
        OpenComputersApi.initialize();
        for (final Direction direction : Direction.values()) {
            plugs[direction.ordinal()] = new Plug(direction);
        }
        updateLimits();
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final RelayBlockEntity relay) {
        relay.relayQueuedPacket();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.neoopencomputers.relay");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory playerInventory, final Player player) {
        return new RelayMenu(containerId, playerInventory, this);
    }

    @Override
    public Node sidedNode(final Direction side) {
        return side == null ? null : plugs[side.ordinal()].node();
    }

    @Override
    public boolean canConnect(final Direction side) {
        return side != null;
    }

    @Override
    public Node[] onAnalyze(final Player player, final Direction side, final float hitX, final float hitY, final float hitZ) {
        if (!wirelessEnabled || side == null) {
            return null;
        }
        return new Node[]{sidedNode(side)};
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        final ListTag plugTags = tag.getList(TAG_PLUGS, CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < Math.min(plugTags.size(), plugs.length); index++) {
            plugs[index].node().load(plugTags.getCompound(index));
        }
        ContainerHelper.loadAllItems(tag, items, registries);
        updateLimits();
        if (tag.contains(TAG_STRENGTH)) {
            wirelessStrength = clampWirelessStrength(tag.getDouble(TAG_STRENGTH));
        }
        if (tag.contains(TAG_REPEATER)) {
            isRepeater = tag.getBoolean(TAG_REPEATER);
        }
        relayCooldown = tag.contains(TAG_RELAY_COOLDOWN) ? tag.getInt(TAG_RELAY_COOLDOWN) : -1;
        queue.clear();
        final ListTag queueTags = tag.getList(TAG_QUEUE, CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < queueTags.size() && queue.size() < maxQueueSize; index++) {
            final CompoundTag queued = queueTags.getCompound(index);
            if (queued.contains(TAG_PACKET)) {
                queue.add(new QueuedPacket(readQueuedSide(queued), Network.newPacket(queued.getCompound(TAG_PACKET))));
            }
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putDouble(TAG_STRENGTH, wirelessStrength);
        tag.putBoolean(TAG_REPEATER, isRepeater);
        if (relayCooldown > 0) {
            tag.putInt(TAG_RELAY_COOLDOWN, relayCooldown);
        }
        final ListTag plugTags = new ListTag();
        for (final Plug plug : plugs) {
            final CompoundTag plugTag = new CompoundTag();
            if (plug.node().address() == null) {
                Network.joinNewNetwork(plug.node());
            }
            plug.node().save(plugTag);
            plugTags.add(plugTag);
        }
        tag.put(TAG_PLUGS, plugTags);

        final ListTag queueTags = new ListTag();
        for (final QueuedPacket queued : queue) {
            final CompoundTag queuedTag = new CompoundTag();
            queuedTag.putInt(TAG_SIDE, queued.sourceSide() == null ? -1 : queued.sourceSide().get3DDataValue());
            final CompoundTag packetTag = new CompoundTag();
            queued.packet().save(packetTag);
            queuedTag.put(TAG_PACKET, packetTag);
            queueTags.add(queuedTag);
        }
        tag.put(TAG_QUEUE, queueTags);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        removeNodes();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeNodes();
    }

    public void removeNodes() {
        Network.leaveWirelessNetwork(this);
        LinkedNetwork.remove(this);
        for (final Plug plug : plugs) {
            plug.node().remove();
        }
    }

    public int maxQueueSize() {
        return maxQueueSize;
    }

    public int relayDelay() {
        return relayDelay;
    }

    public int relayAmount() {
        return relayAmount;
    }

    public int queuedPackets() {
        return queue.size();
    }

    public boolean isWirelessEnabled() {
        return wirelessEnabled;
    }

    public boolean isLinkedEnabled() {
        return linkedEnabled;
    }

    public int wirelessStrength() {
        return (int) Math.round(wirelessStrength);
    }

    public boolean isRepeaterEnabled() {
        return isRepeater;
    }

    @Override
    public int x() {
        return worldPosition.getX();
    }

    @Override
    public int y() {
        return worldPosition.getY();
    }

    @Override
    public int z() {
        return worldPosition.getZ();
    }

    @Override
    public Level world() {
        return level;
    }

    @Override
    public void receivePacket(final Packet packet, final WirelessEndpoint sender) {
        if (wirelessEnabled && packet != null) {
            enqueue(null, packet);
        }
    }

    @Override
    public String linkedChannel() {
        return linkedChannel;
    }

    @Override
    public void receiveLinkedPacket(final Packet packet) {
        if (linkedEnabled && packet != null) {
            enqueue(null, packet);
        }
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (final ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return isValidSlot(slot) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(final int slot, final int amount) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            updateLimits();
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) {
            updateLimits();
        }
        return removed;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (!isValidSlot(slot) || (!stack.isEmpty() && !canPlaceItem(slot, stack))) {
            return;
        }
        final ItemStack stored = stack.copy();
        if (!stored.isEmpty() && stored.getCount() > getMaxStackSize()) {
            stored.setCount(getMaxStackSize());
        }
        items.set(slot, stored);
        updateLimits();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        if (!isValidSlot(slot) || stack.isEmpty()) {
            return false;
        }
        final DriverItem driver = Driver.driverFor(stack);
        if (driver == null || driver.tier(stack) > MAX_TIER) {
            return false;
        }
        return switch (slot) {
            case CPU_SLOT -> Slot.CPU.equals(driver.slot(stack));
            case MEMORY_SLOT -> Slot.Memory.equals(driver.slot(stack));
            case HDD_SLOT -> Slot.HDD.equals(driver.slot(stack));
            case CARD_SLOT -> Slot.Card.equals(driver.slot(stack)) && isSupportedRelayCard(stack);
            default -> false;
        };
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(final Player player) {
        return !isRemoved();
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        updateLimits();
        setChanged();
    }

    private void enqueue(final Direction sourceSide, final Packet packet) {
        if (packet.ttl() <= 0 || queue.size() >= maxQueueSize) {
            return;
        }
        queue.add(new QueuedPacket(sourceSide, packet.hop()));
        if (relayCooldown < 0) {
            relayCooldown = relayDelay - 1;
        }
        setChanged();
    }

    private void relayQueuedPacket() {
        if (relayCooldown > 0) {
            relayCooldown--;
            setChanged();
            return;
        }
        relayCooldown = -1;
        final int packetsToRelay = Math.min(queue.size(), relayAmount);
        for (int packetIndex = 0; packetIndex < packetsToRelay; packetIndex++) {
            final QueuedPacket queued = queue.poll();
            if (queued == null) {
                return;
            }
            for (final Direction direction : Direction.values()) {
                if (direction != queued.sourceSide()) {
                    plugs[direction.ordinal()].node().sendToReachable(NETWORK_MESSAGE, queued.packet());
                }
            }
            relayWirelessPacket(queued);
            relayLinkedPacket(queued);
            setChanged();
        }
        if (!queue.isEmpty()) {
            relayCooldown = relayDelay - 1;
        }
    }

    private void relayWirelessPacket(final QueuedPacket queued) {
        if (!wirelessEnabled || wirelessStrength <= 0D || queued.sourceSide() == null && !isRepeater) {
            return;
        }
        final double cost = wirelessStrength * wirelessCostPerRange();
        if (cost <= 0D || trySpendWirelessEnergy(queued.sourceSide(), cost)) {
            Network.sendWirelessPacket(this, wirelessStrength, queued.packet());
        }
    }

    private boolean trySpendWirelessEnergy(final Direction sourceSide, final double cost) {
        if (sourceSide != null) {
            return plugs[sourceSide.ordinal()].node() instanceof Connector connector && connector.tryChangeBuffer(-cost);
        }
        for (final Plug plug : plugs) {
            if (plug.node() instanceof Connector connector && connector.tryChangeBuffer(-cost)) {
                return true;
            }
        }
        return false;
    }

    private void relayLinkedPacket(final QueuedPacket queued) {
        if (!linkedEnabled || queued.sourceSide() == null) {
            return;
        }
        final double cost = queued.packet().size() / 32.0D + linkedCardBaseCost();
        if (trySpendWirelessEnergy(queued.sourceSide(), cost)) {
            LinkedNetwork.send(linkedChannel, this, queued.packet());
        }
    }

    private boolean isRelayPlug(final Node node) {
        for (final Plug plug : plugs) {
            if (plug.node() == node) {
                return true;
            }
        }
        return false;
    }

    private boolean isPrimary(final Plug plug) {
        for (final Plug candidate : plugs) {
            if (candidate.node().network() == plug.node().network()) {
                return candidate == plug;
            }
        }
        return false;
    }

    private static Node createNode(final Environment environment) {
        return Network.newNode(environment, Visibility.Network)
            .withConnector(connectorBufferSize())
            .withComponent("relay", Visibility.Network)
            .create();
    }

    public static double connectorBufferSize() {
        return ModSettings.accessPointBuffer();
    }

    private void updateLimits() {
        final boolean wasWirelessEnabled = wirelessEnabled;
        final boolean wasLinkedEnabled = linkedEnabled;
        final String oldLinkedChannel = linkedChannel;
        final double oldWirelessStrength = wirelessStrength;
        relayDelay = ModSettings.defaultRelayDelay();
        relayAmount = ModSettings.defaultRelayAmount();
        maxQueueSize = ModSettings.defaultMaxQueueSize();
        wirelessEnabled = false;
        wirelessTier = -1;
        wirelessMaxRange = 0D;
        wirelessStrength = 0D;
        linkedEnabled = false;
        linkedChannel = LinkedNetwork.DEFAULT_CHANNEL;

        final DriverItem cpu = driverFor(CPU_SLOT);
        if (cpu != null) {
            relayDelay = Math.max(1, ModSettings.defaultRelayDelay() - (int) ((cpu.tier(items.get(CPU_SLOT)) + 1) * ModSettings.relayDelayUpgrade()));
        }
        final DriverItem memory = driverFor(MEMORY_SLOT);
        if (memory != null) {
            relayAmount = Math.max(1, ModSettings.defaultRelayAmount() + (memory.tier(items.get(MEMORY_SLOT)) + 1) * ModSettings.relayAmountUpgrade());
        }
        final DriverItem hdd = driverFor(HDD_SLOT);
        if (hdd != null) {
            maxQueueSize = Math.max(1, ModSettings.defaultMaxQueueSize() + (hdd.tier(items.get(HDD_SLOT)) + 1) * ModSettings.queueSizeUpgrade());
        }
        wirelessTier = wirelessTier(items.get(CARD_SLOT));
        wirelessMaxRange = wirelessTier >= 0 ? ModSettings.maxWirelessRange(wirelessTier) : 0D;
        wirelessEnabled = wirelessMaxRange > 0D;
        if (items.get(CARD_SLOT).is(ModItems.LINKED_CARD.get())) {
            linkedEnabled = true;
            linkedChannel = linkedChannel(items.get(CARD_SLOT));
        }
        if (wirelessEnabled) {
            wirelessStrength = wasWirelessEnabled ? Math.min(oldWirelessStrength, wirelessMaxRange) : wirelessMaxRange;
        }
        if (level != null && wirelessEnabled != wasWirelessEnabled) {
            if (wirelessEnabled) {
                Network.joinWirelessNetwork(this);
            } else {
                Network.leaveWirelessNetwork(this);
            }
        }
        if (linkedEnabled != wasLinkedEnabled || !linkedChannel.equals(oldLinkedChannel)) {
            LinkedNetwork.remove(this);
            if (linkedEnabled) {
                LinkedNetwork.add(this);
            }
        }
        trimQueue();
    }

    private DriverItem driverFor(final int slot) {
        if (!isValidSlot(slot) || items.get(slot).isEmpty()) {
            return null;
        }
        return Driver.driverFor(items.get(slot));
    }

    private void trimQueue() {
        while (queue.size() > maxQueueSize) {
            queue.poll();
        }
    }

    private static boolean isValidSlot(final int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }

    private static boolean isSupportedRelayCard(final ItemStack stack) {
        return stack.is(ModItems.WIRELESS_NETWORK_CARD_TIER1.get())
            || stack.is(ModItems.WIRELESS_NETWORK_CARD_TIER2.get())
            || stack.is(ModItems.LINKED_CARD.get());
    }

    private static int wirelessTier(final ItemStack stack) {
        if (stack.is(ModItems.WIRELESS_NETWORK_CARD_TIER1.get())) {
            return 0;
        }
        if (stack.is(ModItems.WIRELESS_NETWORK_CARD_TIER2.get())) {
            return 1;
        }
        return -1;
    }

    private double wirelessCostPerRange() {
        return wirelessTier >= 0 ? ModSettings.wirelessCostPerRange(wirelessTier) : 0D;
    }

    private static double linkedCardBaseCost() {
        return ModSettings.maxWirelessRange(1) * ModSettings.wirelessCostPerRange(1) * 5D;
    }

    private static String linkedChannel(final ItemStack stack) {
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag tag = customData == null ? new CompoundTag() : customData.copyTag();
        return LinkedNetwork.normalizeChannel(tag.getString(LinkedCardItem.TUNNEL_TAG));
    }

    private double setWirelessStrength(final double value) {
        wirelessStrength = clampWirelessStrength(value);
        setChanged();
        return wirelessStrength;
    }

    private double clampWirelessStrength(final double value) {
        return Math.max(0D, Math.min(value, wirelessMaxRange));
    }

    private boolean setRepeater(final boolean value) {
        isRepeater = value;
        setChanged();
        return isRepeater;
    }

    private static Direction readQueuedSide(final CompoundTag tag) {
        if (!tag.contains(TAG_SIDE)) {
            return Direction.from3DDataValue(0);
        }
        final int value = tag.getInt(TAG_SIDE);
        return value < 0 ? null : Direction.from3DDataValue(value);
    }

    private record QueuedPacket(Direction sourceSide, Packet packet) {
    }

    private final class Plug implements Environment {
        private final Direction side;
        private final Node node;

        private Plug(final Direction side) {
            this.side = side;
            node = createNode(this);
        }

        @Override
        public Node node() {
            return node;
        }

        @Override
        public void onConnect(final Node node) {
        }

        @Override
        public void onDisconnect(final Node node) {
        }

        @Override
        public void onMessage(final Message message) {
            if (!isPrimary(this) || !NETWORK_MESSAGE.equals(message.name()) || isRelayPlug(message.source())) {
                return;
            }
            if (message.data().length == 1 && message.data()[0] instanceof Packet packet) {
                enqueue(side, packet);
            }
        }

        @Callback(direct = true, doc = "function():number -- Get the signal strength (range) used when relaying messages.")
        public Object[] getStrength(final Context context, final Arguments args) {
            return new Object[]{wirelessStrength};
        }

        @Callback(doc = "function(strength:number):number -- Set the signal strength (range) used when relaying messages.")
        public Object[] setStrength(final Context context, final Arguments args) {
            return new Object[]{setWirelessStrength(args.checkDouble(0))};
        }

        @Callback(direct = true, doc = "function():boolean -- Get whether the relay repeats received wireless packets wirelessly.")
        public Object[] isRepeater(final Context context, final Arguments args) {
            return new Object[]{isRepeater};
        }

        @Callback(doc = "function(enabled:boolean):boolean -- Set whether the relay repeats received wireless packets wirelessly.")
        public Object[] setRepeater(final Context context, final Arguments args) {
            return new Object[]{RelayBlockEntity.this.setRepeater(args.checkBoolean(0))};
        }
    }
}
