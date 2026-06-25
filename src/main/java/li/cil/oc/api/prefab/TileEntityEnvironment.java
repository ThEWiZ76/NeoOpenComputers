package li.cil.oc.api.prefab;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class TileEntityEnvironment extends BlockEntity implements Environment {
    private static final String TAG_NODE = "oc:node";

    protected Node node;

    protected TileEntityEnvironment(final BlockEntityType<?> type, final BlockPos pos, final BlockState blockState) {
        super(type, pos, blockState);
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
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Network.joinOrCreateNetwork(this);
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
        removeNode();
    }

    /**
     * @deprecated Use {@link #setRemoved()} in Minecraft 1.21 code.
     */
    @Deprecated
    public void invalidate() {
        super.setRemoved();
        removeNode();
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
        loadNode(tag);
    }

    /**
     * @deprecated Use {@link #saveAdditional(CompoundTag, HolderLookup.Provider)} in Minecraft 1.21 code.
     */
    @Deprecated
    public CompoundTag writeToNBT(final CompoundTag tag) {
        saveNode(tag);
        return tag;
    }

    private void loadNode(final CompoundTag tag) {
        if (node != null && node.host() == this) {
            node.load(tag.getCompound(TAG_NODE));
        }
    }

    private void saveNode(final CompoundTag tag) {
        if (node != null && node.host() == this) {
            final CompoundTag nodeTag = new CompoundTag();
            node.save(nodeTag);
            tag.put(TAG_NODE, nodeTag);
        }
    }

    private void removeNode() {
        if (node != null) {
            node.remove();
        }
    }
}
