package li.cil.oc.common.blockentity;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackBusConnectable;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.component.TerminalServerRackMountableEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import li.cil.oc.common.menu.RackMenu;

import java.util.ArrayList;
import java.util.List;

public class RackBlockEntity extends BlockEntity implements Rack, MenuProvider, Analyzable {
    public static final int CONTAINER_SIZE = 4;
    public static final String DATA_TAG = "oc:rack";
    private static final String NETWORK_MESSAGE = "network.message";

    private static final String TAG_MOUNTABLE_DATA = "oc:mountableData";
    private static final String TAG_SIDE_NODES = "oc:sideNodes";
    private static final String TAG_NODE_MAPPING = "oc:nodeMapping";
    private static final String STACK_MOUNTABLE_DATA_TAG = "oc:rackMountable";

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final CompoundTag[] mountableData = new CompoundTag[CONTAINER_SIZE];
    private final RackMountable[] mountables = new RackMountable[CONTAINER_SIZE];
    private Direction[][] nodeMapping;
    private SidePlug[] sidePlugs;
    private SecondaryPlug[][] secondaryPlugs;

    public RackBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.RACK.get(), pos, blockState);
        OpenComputersApi.initialize();
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            mountableData[slot] = new CompoundTag();
        }
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final RackBlockEntity rack) {
        rack.tickServer();
    }

    public static boolean acceptsDriverSlot(final String slot) {
        return Slot.RackMountable.equals(slot);
    }

    public List<ItemStack> stacksForDrop() {
        saveMountableData();
        final List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            final ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                stacks.add(stackWithMountableData(slot, stack.copy()));
            }
        }
        return stacks;
    }

    public void saveToStack(final ItemStack stack, final HolderLookup.Provider registries) {
        final CompoundTag data = new CompoundTag();
        saveRackData(data, registries);
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag root = customData == null ? new CompoundTag() : customData.copyTag();
        root.put(DATA_TAG, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public void loadFromStack(final ItemStack stack, final HolderLookup.Provider registries) {
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return;
        }
        final CompoundTag data = customData.copyTag().getCompound(DATA_TAG);
        if (data.isEmpty()) {
            return;
        }
        loadRackData(data, registries);
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.neoopencomputers.rack");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory playerInventory, final Player player) {
        return new RackMenu(containerId, playerInventory, this);
    }

    @Override
    public int indexOfMountable(final RackMountable mountable) {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            if (mountables[slot] == mountable) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public RackMountable getMountable(final int slot) {
        return isValidSlot(slot) ? mountables[slot] : null;
    }

    @Override
    public CompoundTag getMountableData(final int slot) {
        return isValidSlot(slot) ? mountableData[slot] : new CompoundTag();
    }

    @Override
    public void markChanged(final int slot) {
        if (isValidSlot(slot)) {
            saveMountableData(slot);
            setChanged();
        }
    }

    @Override
    public Node sidedNode(final Direction side) {
        if (!canConnect(side)) {
            return null;
        }
        return sidePlug(side).node();
    }

    @Override
    public boolean canConnect(final Direction side) {
        return side != null && side != facing();
    }

    public void connect(final int slot, final int connectableIndex, final Direction side) {
        if (!isValidSlot(slot) || connectableIndex < -1 || connectableIndex > 2) {
            return;
        }

        final Direction newSide = canConnect(side) ? side : null;
        final int mappingIndex = connectableIndex + 1;
        final Direction oldSide = nodeMapping()[slot][mappingIndex];
        if (oldSide == newSide) {
            return;
        }

        final RackMountable mountable = getMountable(slot);
        if (mountable != null && oldSide != null && mountable.node() != null) {
            if (connectableIndex == -1) {
                final Node plug = sidedNode(oldSide);
                if (plug != null) {
                    mountable.node().disconnect(plug);
                }
            } else {
                removeSecondaryPlug(slot, connectableIndex);
            }
        }

        nodeMapping()[slot][mappingIndex] = newSide;

        if (mountable == null || newSide == null) {
            return;
        }

        if (connectableIndex == -1 && mountable.node() != null) {
            final Node plug = sidedNode(newSide);
            if (plug != null) {
                Network.joinNewNetwork(plug);
                mountable.node().connect(plug);
            }
        } else if (connectableIndex >= 0 && connectableIndex < mountable.getConnectableCount()) {
            final RackBusConnectable connectable = mountable.getConnectableAt(connectableIndex);
            if (connectable != null && connectable.node() != null) {
                Network.joinNewNetwork(connectable.node());
                connectable.node().connect(secondaryPlug(slot, connectableIndex).node());
            }
        }
    }

    @Override
    public Node[] onAnalyze(final Player player, final Direction side, final float hitX, final float hitY, final float hitZ) {
        final Integer slot = slotAt(side, hitX, hitY, hitZ);
        if (slot != null) {
            final RackMountable mountable = getMountable(slot);
            return mountable instanceof Analyzable analyzable ? analyzable.onAnalyze(player, side, hitX, hitY, hitZ) : null;
        }
        return new Node[]{sidedNode(side)};
    }

    @Override
    public Level world() {
        return getLevel();
    }

    @Override
    public double xPosition() {
        return getBlockPos().getX() + 0.5D;
    }

    @Override
    public double yPosition() {
        return getBlockPos().getY() + 0.5D;
    }

    @Override
    public double zPosition() {
        return getBlockPos().getZ() + 0.5D;
    }

    @Override
    public void markChanged() {
        setChanged();
    }

    @Override
    public Direction facing() {
        if (getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)) {
            return getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public Direction toGlobal(final Direction value) {
        return ComputerCaseBlockEntity.toGlobal(facing(), value);
    }

    @Override
    public Direction toLocal(final Direction value) {
        return ComputerCaseBlockEntity.toLocal(facing(), value);
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
        saveMountableData(slot);
        final ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            writeMountableData(removed, mountableData[slot]);
            removeMountable(slot);
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        saveMountableData(slot);
        final ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) {
            writeMountableData(removed, mountableData[slot]);
            removeMountable(slot);
        }
        return removed;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (!isValidSlot(slot) || (!stack.isEmpty() && !canPlaceItem(slot, stack))) {
            return;
        }
        removeMountable(slot);
        final ItemStack stored = stack.copy();
        if (!stored.isEmpty() && stored.getCount() > getMaxStackSize()) {
            stored.setCount(getMaxStackSize());
        }
        items.set(slot, stored);
        mountableData[slot] = readMountableData(stored);
        refreshMountable(slot);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return isValidSlot(slot) && isRackMountableStack(stack);
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
            removeMountable(slot);
            items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadRackData(tag, registries);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveRackData(tag, registries);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        removeSideNodes();
        removeMountables();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeSideNodes();
        removeMountables();
    }

    private static boolean isValidSlot(final int slot) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }

    public Integer slotAt(final Direction side, final float hitX, final float hitY, final float hitZ) {
        if (side != facing()) {
            return null;
        }
        final int globalY = (int) (hitY * 16);
        final int slot = ((15 - globalY) - 2) * CONTAINER_SIZE / (14 - 2);
        return Math.max(0, Math.min(CONTAINER_SIZE - 1, slot));
    }

    private static boolean isRackMountableStack(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        final DriverItem driver = Driver.driverFor(stack);
        return driver != null && acceptsDriverSlot(driver.slot(stack));
    }

    private ItemStack stackWithMountableData(final int slot, final ItemStack stack) {
        if (isValidSlot(slot)) {
            writeMountableData(stack, mountableData[slot]);
        }
        return stack;
    }

    private static CompoundTag readMountableData(final ItemStack stack) {
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return new CompoundTag();
        }
        return customData.copyTag().getCompound(STACK_MOUNTABLE_DATA_TAG).copy();
    }

    private static void writeMountableData(final ItemStack stack, final CompoundTag data) {
        if (stack.isEmpty() || data == null || data.isEmpty()) {
            return;
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag root = customData == null ? new CompoundTag() : customData.copyTag();
        root.put(STACK_MOUNTABLE_DATA_TAG, data.copy());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private void loadRackData(final CompoundTag tag, final HolderLookup.Provider registries) {
        loadSideNodes(tag);
        loadNodeMappings(tag);
        removeMountables();
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            items.set(slot, ItemStack.EMPTY);
            mountableData[slot] = new CompoundTag();
        }
        ContainerHelper.loadAllItems(tag, items, registries);
        if (tag.contains(TAG_MOUNTABLE_DATA)) {
            final ListTag data = tag.getList(TAG_MOUNTABLE_DATA, CompoundTag.TAG_COMPOUND);
            for (int slot = 0; slot < Math.min(data.size(), CONTAINER_SIZE); slot++) {
                mountableData[slot] = data.getCompound(slot).copy();
            }
        }
        refreshMountables();
    }

    private void saveRackData(final CompoundTag tag, final HolderLookup.Provider registries) {
        saveSideNodes(tag);
        saveNodeMappings(tag);
        saveMountableData();
        ContainerHelper.saveAllItems(tag, items, registries);
        final ListTag data = new ListTag();
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            data.add(mountableData[slot] == null ? new CompoundTag() : mountableData[slot].copy());
        }
        tag.put(TAG_MOUNTABLE_DATA, data);
    }

    private void loadSideNodes(final CompoundTag tag) {
        if (!tag.contains(TAG_SIDE_NODES)) {
            return;
        }
        final ListTag sideNodes = tag.getList(TAG_SIDE_NODES, CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < Math.min(sideNodes.size(), Direction.values().length); index++) {
            final Direction side = Direction.values()[index];
            if (canConnect(side)) {
                sidePlug(side).node().load(sideNodes.getCompound(index));
            }
        }
    }

    private void saveSideNodes(final CompoundTag tag) {
        final ListTag sideNodes = new ListTag();
        for (final Direction side : Direction.values()) {
            final CompoundTag sideNode = new CompoundTag();
            if (canConnect(side)) {
                final Node node = sidePlug(side).node();
                if (node.address() == null) {
                    Network.joinNewNetwork(node);
                }
                node.save(sideNode);
            }
            sideNodes.add(sideNode);
        }
        tag.put(TAG_SIDE_NODES, sideNodes);
    }

    private void loadNodeMappings(final CompoundTag tag) {
        if (!tag.contains(TAG_NODE_MAPPING)) {
            return;
        }
        final Direction[][] mappings = nodeMapping();
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            for (int mappingIndex = 0; mappingIndex < 4; mappingIndex++) {
                mappings[slot][mappingIndex] = null;
            }
        }

        final ListTag nodeMappings = tag.getList(TAG_NODE_MAPPING, Tag.TAG_INT_ARRAY);
        final Direction[] directions = Direction.values();
        for (int slot = 0; slot < Math.min(nodeMappings.size(), CONTAINER_SIZE); slot++) {
            final int[] sideOrdinals = nodeMappings.getIntArray(slot);
            for (int mappingIndex = 0; mappingIndex < Math.min(sideOrdinals.length, 4); mappingIndex++) {
                final int sideOrdinal = sideOrdinals[mappingIndex];
                final Direction side = sideOrdinal >= 0 && sideOrdinal < directions.length ? directions[sideOrdinal] : null;
                mappings[slot][mappingIndex] = canConnect(side) ? side : null;
            }
        }
    }

    private void saveNodeMappings(final CompoundTag tag) {
        final ListTag nodeMappings = new ListTag();
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            final int[] sideOrdinals = new int[4];
            for (int mappingIndex = 0; mappingIndex < 4; mappingIndex++) {
                final Direction side = nodeMapping == null ? null : nodeMapping[slot][mappingIndex];
                sideOrdinals[mappingIndex] = side == null ? -1 : side.ordinal();
            }
            nodeMappings.add(new IntArrayTag(sideOrdinals));
        }
        tag.put(TAG_NODE_MAPPING, nodeMappings);
    }

    private SidePlug sidePlug(final Direction side) {
        if (sidePlugs == null) {
            sidePlugs = new SidePlug[Direction.values().length];
        }
        final int index = side.ordinal();
        if (sidePlugs[index] == null) {
            sidePlugs[index] = new SidePlug(side);
        }
        return sidePlugs[index];
    }

    private Direction[][] nodeMapping() {
        if (nodeMapping == null) {
            nodeMapping = new Direction[CONTAINER_SIZE][4];
        }
        return nodeMapping;
    }

    private void removeSideNodes() {
        if (sidePlugs == null) {
            return;
        }
        for (final SidePlug plug : sidePlugs) {
            if (plug != null) {
                plug.node().remove();
            }
        }
        removeSecondaryPlugs();
    }

    private void tickServer() {
        for (final RackMountable mountable : mountables) {
            if (mountable != null && mountable.canUpdate()) {
                mountable.update();
            }
        }
    }

    private void refreshMountables() {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            refreshMountable(slot);
        }
    }

    private void refreshMountable(final int slot) {
        removeMountable(slot, false);
        final ItemStack stack = items.get(slot);
        final DriverItem driver = Driver.driverFor(stack);
        if (driver == null || !acceptsDriverSlot(driver.slot(stack))) {
            return;
        }
        final ManagedEnvironment environment = driver.createEnvironment(stack, this);
        if (environment instanceof RackMountable rackMountable) {
            rackMountable.load(mountableData[slot]);
            mountables[slot] = rackMountable;
        }
    }

    private void removeMountables() {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            removeMountable(slot, false);
        }
    }

    private void removeMountable(final int slot) {
        removeMountable(slot, true);
    }

    private void removeMountable(final int slot, final boolean clearMappings) {
        if (!isValidSlot(slot)) {
            return;
        }
        saveMountableData(slot);
        final RackMountable mountable = mountables[slot];
        if (clearMappings) {
            clearNodeMappings(slot, mountable);
        }
        if (mountable instanceof TerminalServerRackMountableEnvironment terminalServer) {
            terminalServer.removeVirtualNodes();
        }
        if (mountable != null && mountable.node() != null) {
            mountable.node().remove();
        }
        mountables[slot] = null;
    }

    private void clearNodeMappings(final int slot, final RackMountable mountable) {
        if (nodeMapping == null) {
            return;
        }
        for (int mappingIndex = 0; mappingIndex < 4; mappingIndex++) {
            final Direction side = nodeMapping[slot][mappingIndex];
            if (side == null) {
                continue;
            }
            if (mappingIndex == 0 && mountable != null && mountable.node() != null) {
                final Node plug = sidedNode(side);
                if (plug != null) {
                    mountable.node().disconnect(plug);
                }
            } else if (mappingIndex > 0) {
                removeSecondaryPlug(slot, mappingIndex - 1);
            }
            nodeMapping[slot][mappingIndex] = null;
        }
    }

    private void saveMountableData() {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            saveMountableData(slot);
        }
    }

    private void saveMountableData(final int slot) {
        if (!isValidSlot(slot) || mountables[slot] == null) {
            return;
        }
        final CompoundTag data = mountables[slot].getData();
        mountables[slot].save(data);
        mountableData[slot] = data;
    }

    private final class SidePlug implements Environment {
        private final Direction side;
        private final Node node;

        private SidePlug(final Direction side) {
            this.side = side;
            node = Network.newNode(this, Visibility.Network)
                .withConnector(PowerDistributorBlockEntity.connectorBufferSize())
                .create();
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
            if (NETWORK_MESSAGE.equals(message.name()) && message.data().length == 1 && message.data()[0] instanceof Packet packet) {
                sendPacketToSecondaryConnectables(side, packet);
            }
        }
    }

    private SecondaryPlug secondaryPlug(final int slot, final int connectableIndex) {
        if (secondaryPlugs == null) {
            secondaryPlugs = new SecondaryPlug[CONTAINER_SIZE][3];
        }
        if (secondaryPlugs[slot][connectableIndex] == null) {
            secondaryPlugs[slot][connectableIndex] = new SecondaryPlug(slot, connectableIndex);
        }
        return secondaryPlugs[slot][connectableIndex];
    }

    private void removeSecondaryPlug(final int slot, final int connectableIndex) {
        if (secondaryPlugs == null || secondaryPlugs[slot][connectableIndex] == null) {
            return;
        }
        secondaryPlugs[slot][connectableIndex].node().remove();
        secondaryPlugs[slot][connectableIndex] = null;
    }

    private void removeSecondaryPlugs() {
        if (secondaryPlugs == null) {
            return;
        }
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            for (int connectableIndex = 0; connectableIndex < 3; connectableIndex++) {
                removeSecondaryPlug(slot, connectableIndex);
            }
        }
    }

    private void sendPacketToSecondaryConnectables(final Direction sourceSide, final Packet packet) {
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            final RackMountable mountable = getMountable(slot);
            if (mountable == null) {
                continue;
            }
            for (int connectableIndex = 0; connectableIndex < 3; connectableIndex++) {
                if (nodeMapping()[slot][connectableIndex + 1] == sourceSide && connectableIndex < mountable.getConnectableCount()) {
                    final RackBusConnectable connectable = mountable.getConnectableAt(connectableIndex);
                    if (connectable != null) {
                        connectable.receivePacket(packet);
                    }
                }
            }
        }
    }

    private void relayPacketFromSecondaryConnectable(final int sourceSlot, final int sourceConnectableIndex, final Packet packet, final Node sourceNode) {
        final Direction side = nodeMapping()[sourceSlot][sourceConnectableIndex + 1];
        if (side == null) {
            return;
        }
        final Node bus = sidedNode(side);
        if (bus != null) {
            bus.sendToReachable(NETWORK_MESSAGE, packet);
        }
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            final RackMountable mountable = getMountable(slot);
            if (mountable == null) {
                continue;
            }
            for (int connectableIndex = 0; connectableIndex < 3; connectableIndex++) {
                if (slot == sourceSlot && connectableIndex == sourceConnectableIndex) {
                    continue;
                }
                if (nodeMapping()[slot][connectableIndex + 1] == side && connectableIndex < mountable.getConnectableCount()) {
                    final RackBusConnectable connectable = mountable.getConnectableAt(connectableIndex);
                    if (connectable != null && connectable.node() != sourceNode) {
                        connectable.receivePacket(packet);
                    }
                }
            }
        }
    }

    private final class SecondaryPlug implements Environment {
        private final int slot;
        private final int connectableIndex;
        private final Node node;

        private SecondaryPlug(final int slot, final int connectableIndex) {
            this.slot = slot;
            this.connectableIndex = connectableIndex;
            node = Network.newNode(this, Visibility.Neighbors).create();
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
            if (NETWORK_MESSAGE.equals(message.name()) && message.data().length == 1 && message.data()[0] instanceof Packet packet) {
                relayPacketFromSecondaryConnectable(slot, connectableIndex, packet, message.source());
            }
        }
    }
}
