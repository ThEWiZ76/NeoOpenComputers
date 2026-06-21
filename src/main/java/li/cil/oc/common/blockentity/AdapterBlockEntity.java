package li.cil.oc.common.blockentity;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;

public class AdapterBlockEntity extends BlockEntity implements Adapter, EnvironmentHost, Analyzable {
    private static final String TAG_NODE = "node";
    private static final String TAG_BLOCKS = "oc:adapter.blocks";
    private static final String TAG_ITEMS = "oc:items";
    private static final String TAG_ITEM_COMPONENT = "oc:itemComponent";
    private static final String TAG_NAME = "name";
    private static final String TAG_DATA = "data";
    private static final int SIDE_COUNT = 6;
    private static final int UPGRADE_SLOT = 0;
    private static final int CONTAINER_SIZE = 1;

    private Node node;
    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private final ManagedEnvironment[] blockEnvironments = new ManagedEnvironment[SIDE_COUNT];
    private final DriverBlock[] blockDrivers = new DriverBlock[SIDE_COUNT];
    private final String[] blockEnvironmentNames = new String[SIDE_COUNT];
    private final CompoundTag[] blockEnvironmentData = new CompoundTag[SIDE_COUNT];
    private ManagedEnvironment itemEnvironment;
    private DriverItem itemDriver;
    private CompoundTag itemEnvironmentData;

    public AdapterBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.ADAPTER.get(), pos, blockState);
        OpenComputersApi.initialize();
        node = createNode(this);
    }

    @Override
    public Node node() {
        if (node == null) {
            node = createNode(this);
        }
        return node;
    }

    @Override
    public void onConnect(final Node node) {
        if (node == this.node) {
            refreshItemEnvironment();
            refreshNeighbors();
        }
    }

    @Override
    public void onDisconnect(final Node node) {
    }

    @Override
    public void onMessage(final Message message) {
    }

    @Override
    public Node[] onAnalyze(final Player player, final Direction side, final float hitX, final float hitY, final float hitZ) {
        final ArrayList<Node> nodes = new ArrayList<>();
        for (ManagedEnvironment environment : blockEnvironments) {
            if (environment != null && environment.node() != null) {
                nodes.add(environment.node());
            }
        }
        if (itemEnvironment != null && itemEnvironment.node() != null) {
            nodes.add(itemEnvironment.node());
        }
        return nodes.toArray(Node[]::new);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.get(UPGRADE_SLOT).isEmpty();
    }

    @Override
    public ItemStack getItem(final int slot) {
        return slot == UPGRADE_SLOT ? items.get(UPGRADE_SLOT) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(final int slot, final int amount) {
        if (slot != UPGRADE_SLOT) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            refreshItemEnvironment();
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        if (slot != UPGRADE_SLOT) {
            return ItemStack.EMPTY;
        }
        final ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) {
            refreshItemEnvironment();
        }
        return removed;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        if (slot != UPGRADE_SLOT) {
            return;
        }
        final ItemStack stored = stack.copy();
        if (!stored.isEmpty() && stored.getCount() > getMaxStackSize()) {
            stored.setCount(getMaxStackSize());
        }
        items.set(slot, stored);
        refreshItemEnvironment();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack, getClass());
        return slot == UPGRADE_SLOT && driver != null && Slot.Upgrade.equals(driver.slot(stack));
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
        items.set(UPGRADE_SLOT, ItemStack.EMPTY);
        refreshItemEnvironment();
        setChanged();
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
    protected void loadAdditional(final CompoundTag nbt, final HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        if (nbt.contains(TAG_NODE)) {
            node().load(nbt.getCompound(TAG_NODE));
        }
        if (nbt.contains(TAG_ITEMS)) {
            ContainerHelper.loadAllItems(nbt.getCompound(TAG_ITEMS), items, registries);
        }
        if (nbt.contains(TAG_ITEM_COMPONENT)) {
            itemEnvironmentData = nbt.getCompound(TAG_ITEM_COMPONENT);
        }
        if (nbt.contains(TAG_BLOCKS)) {
            final ListTag blocks = nbt.getList(TAG_BLOCKS, CompoundTag.TAG_COMPOUND);
            for (int index = 0; index < Math.min(blocks.size(), SIDE_COUNT); index++) {
                final CompoundTag block = blocks.getCompound(index);
                if (block.contains(TAG_NAME) && block.contains(TAG_DATA)) {
                    blockEnvironmentNames[index] = block.getString(TAG_NAME);
                    blockEnvironmentData[index] = block.getCompound(TAG_DATA);
                }
            }
        }
        refreshItemEnvironment();
    }

    @Override
    protected void saveAdditional(final CompoundTag nbt, final HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        saveNode(nbt);
        saveItems(nbt, registries);
        saveItemEnvironment(nbt);
        saveBlockEnvironments(nbt);
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

    public void removeNode() {
        if (node != null) {
            node.remove();
        }
    }

    public void refreshNeighbor(final BlockPos changedPos) {
        if (level == null || changedPos == null) {
            return;
        }
        final BlockPos delta = changedPos.subtract(worldPosition);
        for (Direction direction : Direction.values()) {
            if (direction.getStepX() == delta.getX() && direction.getStepY() == delta.getY() && direction.getStepZ() == delta.getZ()) {
                refreshSide(direction);
                return;
            }
        }
    }

    public void refreshNeighbors() {
        for (Direction direction : Direction.values()) {
            refreshSide(direction);
        }
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final AdapterBlockEntity adapter) {
        if (adapter.itemEnvironment != null && adapter.itemEnvironment.canUpdate()) {
            adapter.itemEnvironment.update();
        }
        for (ManagedEnvironment environment : adapter.blockEnvironments) {
            if (environment != null && environment.canUpdate()) {
                environment.update();
            }
        }
    }

    private void refreshItemEnvironment() {
        final ItemStack stack = items.get(UPGRADE_SLOT);
        final DriverItem driver = stack.isEmpty() ? null : Driver.driverFor(stack, getClass());
        if (itemEnvironment != null && itemDriver == driver) {
            if (node().network() != null && itemEnvironment.node() != null && itemEnvironment.node().network() != node().network()) {
                node().connect(itemEnvironment.node());
            }
            return;
        }

        removeItemEnvironment();
        if (driver == null) {
            return;
        }

        final ManagedEnvironment environment = driver.createEnvironment(stack, this);
        if (environment == null || environment.node() == null) {
            return;
        }
        if (itemEnvironmentData != null) {
            environment.load(itemEnvironmentData);
        }
        itemEnvironment = environment;
        itemDriver = driver;
        if (node().network() != null) {
            node().connect(environment.node());
        }
    }

    private void removeItemEnvironment() {
        if (itemEnvironment != null) {
            final CompoundTag data = itemEnvironmentData != null ? itemEnvironmentData : new CompoundTag();
            itemEnvironment.save(data);
            itemEnvironmentData = data;
            if (itemEnvironment.node() != null) {
                node().disconnect(itemEnvironment.node());
                itemEnvironment.node().remove();
            }
        }
        itemEnvironment = null;
        itemDriver = null;
    }

    private void refreshSide(final Direction direction) {
        if (level == null || node() == null || node().network() == null) {
            return;
        }

        final int index = direction.get3DDataValue();
        final BlockPos targetPos = worldPosition.relative(direction);
        if (level.getBlockEntity(targetPos) instanceof li.cil.oc.api.network.Environment) {
            removeBlockEnvironment(index);
            return;
        }

        final DriverBlock driver = Driver.driverFor(level, targetPos, direction);
        if (driver == null) {
            removeBlockEnvironment(index);
            return;
        }
        if (blockEnvironments[index] != null && blockDrivers[index] == driver) {
            return;
        }

        removeBlockEnvironment(index);
        final ManagedEnvironment environment = driver.createEnvironment(level, targetPos, direction);
        if (environment == null || environment.node() == null) {
            return;
        }
        final String environmentName = environment.getClass().getName();
        final CompoundTag storedData = blockEnvironmentData[index];
        if (storedData != null && environmentName.equals(blockEnvironmentNames[index])) {
            environment.load(storedData);
        }
        blockEnvironments[index] = environment;
        blockDrivers[index] = driver;
        blockEnvironmentNames[index] = environmentName;
        if (blockEnvironmentData[index] == null) {
            blockEnvironmentData[index] = new CompoundTag();
        }
        node().connect(environment.node());
    }

    private void removeBlockEnvironment(final int index) {
        final ManagedEnvironment environment = blockEnvironments[index];
        if (environment != null) {
            final CompoundTag data = blockEnvironmentData[index] != null ? blockEnvironmentData[index] : new CompoundTag();
            environment.save(data);
            blockEnvironmentData[index] = data;
            if (environment.node() != null) {
                node().disconnect(environment.node());
                environment.node().remove();
            }
        }
        blockEnvironments[index] = null;
        blockDrivers[index] = null;
    }

    private void saveNode(final CompoundTag nbt) {
        if (node() == null) {
            return;
        }

        final CompoundTag nodeTag = new CompoundTag();
        if (node().address() == null) {
            Network.joinNewNetwork(node());
            node().save(nodeTag);
            node().remove();
        } else {
            node().save(nodeTag);
        }
        nbt.put(TAG_NODE, nodeTag);
    }

    private void saveItems(final CompoundTag nbt, final HolderLookup.Provider registries) {
        final CompoundTag itemsTag = new CompoundTag();
        ContainerHelper.saveAllItems(itemsTag, items, registries);
        nbt.put(TAG_ITEMS, itemsTag);
    }

    private void saveItemEnvironment(final CompoundTag nbt) {
        if (itemEnvironment != null) {
            final CompoundTag data = itemEnvironmentData != null ? itemEnvironmentData : new CompoundTag();
            itemEnvironment.save(data);
            itemEnvironmentData = data;
        }
        if (itemEnvironmentData != null) {
            nbt.put(TAG_ITEM_COMPONENT, itemEnvironmentData);
        }
    }

    private void saveBlockEnvironments(final CompoundTag nbt) {
        final ListTag blocks = new ListTag();
        for (int index = 0; index < SIDE_COUNT; index++) {
            final CompoundTag block = new CompoundTag();
            final CompoundTag data = blockEnvironmentData[index] != null ? blockEnvironmentData[index] : new CompoundTag();
            if (blockEnvironments[index] != null) {
                blockEnvironments[index].save(data);
                blockEnvironmentNames[index] = blockEnvironments[index].getClass().getName();
                blockEnvironmentData[index] = data;
            }
            if (blockEnvironmentNames[index] != null) {
                block.putString(TAG_NAME, blockEnvironmentNames[index]);
                block.put(TAG_DATA, data);
            }
            blocks.add(block);
        }
        nbt.put(TAG_BLOCKS, blocks);
    }

    private void removeNodes() {
        removeItemEnvironment();
        for (int index = 0; index < SIDE_COUNT; index++) {
            removeBlockEnvironment(index);
        }
        Arrays.fill(blockEnvironmentNames, null);
        Arrays.fill(blockEnvironmentData, null);
        removeNode();
    }

    private static Node createNode(final Adapter adapter) {
        return Network.newNode(adapter, Visibility.Network).create();
    }
}
