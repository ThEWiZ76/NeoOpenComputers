package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AdapterBlockEntity extends BlockEntity implements Adapter {
    private static final String TAG_NODE = "node";

    private Node node;

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
    }

    @Override
    protected void saveAdditional(final CompoundTag nbt, final HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        saveNode(nbt);
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

    public void removeNode() {
        if (node != null) {
            node.remove();
        }
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

    private static Node createNode(final Adapter adapter) {
        return Network.newNode(adapter, Visibility.None).create();
    }
}
