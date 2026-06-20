package li.cil.oc.common.blockentity;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class AdapterBlockEntity extends BlockEntity implements Adapter {
    private static final String TAG_NODE = "node";
    private static final String TAG_BLOCKS = "oc:adapter.blocks";
    private static final String TAG_NAME = "name";
    private static final String TAG_DATA = "data";
    private static final int SIDE_COUNT = 6;

    private Node node;
    private final ManagedEnvironment[] blockEnvironments = new ManagedEnvironment[SIDE_COUNT];
    private final DriverBlock[] blockDrivers = new DriverBlock[SIDE_COUNT];
    private final String[] blockEnvironmentNames = new String[SIDE_COUNT];
    private final CompoundTag[] blockEnvironmentData = new CompoundTag[SIDE_COUNT];

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
    public int getContainerSize() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(final int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(final int slot, final int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
    }

    @Override
    public boolean stillValid(final Player player) {
        return !isRemoved();
    }

    @Override
    public void clearContent() {
    }

    @Override
    protected void loadAdditional(final CompoundTag nbt, final HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        if (nbt.contains(TAG_NODE)) {
            node().load(nbt.getCompound(TAG_NODE));
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
    }

    @Override
    protected void saveAdditional(final CompoundTag nbt, final HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        saveNode(nbt);
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
        for (ManagedEnvironment environment : adapter.blockEnvironments) {
            if (environment != null && environment.canUpdate()) {
                environment.update();
            }
        }
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
