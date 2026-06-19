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
        super.onChunkUnloaded();
        removeNode();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeNode();
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (node != null && node.host() == this) {
            node.load(tag.getCompound(TAG_NODE));
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
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
