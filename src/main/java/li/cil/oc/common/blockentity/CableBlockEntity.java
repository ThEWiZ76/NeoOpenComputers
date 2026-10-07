package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.internal.Colored;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CableBlockEntity extends BlockEntity implements Environment, SidedEnvironment, Colored {
    private static final String TAG_NODE = "node";
    private static final String TAG_COLOR = "oc:renderColorRGB";
    public static final int DEFAULT_COLOR = DyeColor.LIGHT_GRAY.getTextureDiffuseColor();

    private Node node;
    private int color = DEFAULT_COLOR;

    public CableBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.CABLE.get(), pos, blockState);
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
    public int getColor() {
        return color;
    }

    @Override
    public void setColor(final int value) {
        if (color == value) {
            return;
        }
        color = value;
        setChanged();
        if (level != null && !level.isClientSide && !isRemoved()) {
            // Joining only adds edges; remove old edges before applying the new color rules.
            removeNode();
            Network.joinNewNetwork(node());
            Network.joinOrCreateNetwork(this);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public boolean controlsConnectivity() {
        return true;
    }

    @Override
    public Node sidedNode(final Direction side) {
        return node();
    }

    @Override
    public boolean canConnect(final Direction side) {
        if (level == null || isRemoved()) {
            return false;
        }
        final var neighbor = level.getBlockEntity(worldPosition.relative(side));
        final int otherColor = neighbor instanceof Colored colored && colored.controlsConnectivity()
            ? colored.getColor() : DEFAULT_COLOR;
        return color == otherColor || color == DEFAULT_COLOR || otherColor == DEFAULT_COLOR;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            if (node().network() == null) {
                Network.joinNewNetwork(node());
            }
            Network.joinOrCreateNetwork(this);
        }
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
    protected void loadAdditional(final CompoundTag nbt, final HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        color = nbt.contains(TAG_COLOR) ? nbt.getInt(TAG_COLOR)
            : nbt.contains("oc:renderColor") ? DyeColor.byId(nbt.getInt("oc:renderColor")).getTextureDiffuseColor()
            : DEFAULT_COLOR;
        if (nbt.contains(TAG_NODE)) {
            node().load(nbt.getCompound(TAG_NODE));
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag nbt, final HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.putInt(TAG_COLOR, color);
        saveNode(nbt);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final var tag = new CompoundTag();
        tag.putInt(TAG_COLOR, color);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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

    private static Node createNode(final Environment environment) {
        return Network.newNode(environment, Visibility.None).create();
    }
}
