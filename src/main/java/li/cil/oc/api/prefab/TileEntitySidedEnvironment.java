package li.cil.oc.api.prefab;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class TileEntitySidedEnvironment extends BlockEntity implements SidedEnvironment {
    protected final Node[] nodes = new Node[6];
    protected boolean addedToNetwork = false;

    protected TileEntitySidedEnvironment(final BlockEntityType<?> type, final BlockPos pos, final BlockState blockState, final Node... nodes) {
        super(type, pos, blockState);
        if (nodes != null) {
            System.arraycopy(nodes, 0, this.nodes, 0, Math.min(nodes.length, this.nodes.length));
        }
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final TileEntitySidedEnvironment blockEntity) {
        blockEntity.update();
    }

    @Override
    public Node sidedNode(final Direction side) {
        return side == null ? null : nodes[side.ordinal()];
    }

    public void update() {
        if (!addedToNetwork) {
            addedToNetwork = true;
            Network.joinOrCreateNetwork(this);
        }
    }

    @Override
    public void onChunkUnloaded() {
        onChunkUnload();
    }

    @Override
    public void setRemoved() {
        invalidate();
    }

    /**
     * @deprecated Use {@link #onChunkUnloaded()} in Minecraft 1.21 code.
     */
    @Deprecated
    public void onChunkUnload() {
        super.onChunkUnloaded();
        removeNodes();
    }

    /**
     * @deprecated Use {@link #setRemoved()} in Minecraft 1.21 code.
     */
    @Deprecated
    public void invalidate() {
        super.setRemoved();
        removeNodes();
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readFromNBT(tag);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeToNBT(tag);
    }

    /**
     * @deprecated Use {@link #loadAdditional(CompoundTag, HolderLookup.Provider)} in Minecraft 1.21 code.
     */
    @Deprecated
    public void readFromNBT(final CompoundTag tag) {
        loadNodes(tag);
    }

    /**
     * @deprecated Use {@link #saveAdditional(CompoundTag, HolderLookup.Provider)} in Minecraft 1.21 code.
     */
    @Deprecated
    public CompoundTag writeToNBT(final CompoundTag tag) {
        saveNodes(tag);
        return tag;
    }

    private void loadNodes(final CompoundTag tag) {
        for (int index = 0; index < nodes.length; index++) {
            final Node node = nodes[index];
            if (node != null && node.host() == this) {
                node.load(tag.getCompound("oc:node" + index));
            }
        }
    }

    private void saveNodes(final CompoundTag tag) {
        for (int index = 0; index < nodes.length; index++) {
            final Node node = nodes[index];
            if (node != null && node.host() == this) {
                final CompoundTag nodeTag = new CompoundTag();
                node.save(nodeTag);
                tag.put("oc:node" + index, nodeTag);
            }
        }
    }

    private void removeNodes() {
        for (final Node node : nodes) {
            if (node != null) {
                node.remove();
            }
        }
    }
}
