package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.block.WaypointBlock;
import li.cil.oc.common.menu.WaypointMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.IMenuProviderExtension;

public class WaypointBlockEntity extends BlockEntity implements Environment, EnvironmentHost, MenuProvider, IMenuProviderExtension {
    private static final String TAG_NODE = "node";
    private static final String TAG_LABEL = "oc:label";
    private static final String COMPONENT_NAME = "waypoint";
    public static final int MAX_LABEL_LENGTH = 32;

    private Node node;
    String label = "";

    public WaypointBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.WAYPOINT.get(), pos, blockState);
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

    @Callback(doc = "function():string -- Get the current waypoint label.")
    public Object[] getLabel(final Context context, final Arguments args) {
        return new Object[]{label};
    }

    public String label() {
        return label;
    }

    public void setLabelValue(final String value) {
        label = truncateLabel(value);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static String truncateLabel(final String value) {
        final String safeValue = value == null ? "" : value;
        return safeValue.length() > MAX_LABEL_LENGTH ? safeValue.substring(0, MAX_LABEL_LENGTH) : safeValue;
    }

    public int redstoneInput() {
        return level == null ? 0 : level.getBestNeighborSignal(worldPosition);
    }

    public double targetXPosition() {
        return targetPosition().getX() + 0.5D;
    }

    public double targetYPosition() {
        return targetPosition().getY() + 0.5D;
    }

    public double targetZPosition() {
        return targetPosition().getZ() + 0.5D;
    }

    private BlockPos targetPosition() {
        return getBlockPos().relative(facing());
    }

    private Direction facing() {
        final BlockState state = getBlockState();
        return state.hasProperty(WaypointBlock.FACING) ? state.getValue(WaypointBlock.FACING) : Direction.NORTH;
    }

    @Callback(doc = "function(value:string) -- Set the waypoint label.")
    public Object[] setLabel(final Context context, final Arguments args) {
        setLabelValue(args.checkString(0));
        if (context != null) {
            context.pause(0.5D);
        }
        return null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.neoopencomputers.waypoint.title");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory playerInventory, final Player player) {
        return new WaypointMenu(containerId, playerInventory, this);
    }

    @Override
    public void writeClientSideData(final AbstractContainerMenu menu, final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        buffer.writeUtf(label, 32767);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_NODE)) {
            node().load(tag.getCompound(TAG_NODE));
        }
        label = tag.getString(TAG_LABEL);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveNode(tag);
        tag.putString(TAG_LABEL, label);
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

    private void saveNode(final CompoundTag tag) {
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
        tag.put(TAG_NODE, nodeTag);
    }

    private static Node createNode(final Environment environment) {
        return Network.newNode(environment, Visibility.Network)
            .withComponent(COMPONENT_NAME, Visibility.Network)
            .create();
    }
}
