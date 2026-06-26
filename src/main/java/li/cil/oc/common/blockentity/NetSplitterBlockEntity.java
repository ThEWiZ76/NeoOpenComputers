package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;

public class NetSplitterBlockEntity extends BlockEntity implements Environment, SidedEnvironment, DeviceInfo {
    private static final String TAG_NODE = "node";
    private static final String TAG_INVERTED = "oc:isInverted";
    private static final String TAG_OPEN_SIDES = "oc:openSides";
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Network,
        DeviceInfo.DeviceAttribute.Description, "Ethernet controller",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
        DeviceInfo.DeviceAttribute.Product, "NetSplits",
        DeviceInfo.DeviceAttribute.Version, "1.0",
        DeviceInfo.DeviceAttribute.Width, "6"
    );

    private final boolean[] openSides = new boolean[Direction.values().length];
    private Node node;
    private boolean inverted;

    public NetSplitterBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.NET_SPLITTER.get(), pos, blockState);
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
    public Node sidedNode(final Direction side) {
        return isSideOpen(side) ? node() : null;
    }

    @Override
    public boolean canConnect(final Direction side) {
        return isSideOpen(side);
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
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    public boolean isInverted() {
        return inverted;
    }

    public boolean isSideOpen(final Direction side) {
        if (side == null) {
            return false;
        }
        final boolean stored = openSides[side.ordinal()];
        return inverted ? !stored : stored;
    }

    public void updateRedstoneFromWorld() {
        if (level == null || level.isClientSide) {
            return;
        }
        final boolean newInverted = level.hasNeighborSignal(worldPosition);
        if (newInverted != inverted) {
            inverted = newInverted;
            rebuildNetwork(SoundEvents.PISTON_CONTRACT);
        }
    }

    public void removeNode() {
        if (node != null) {
            node.remove();
        }
    }

    @Callback(doc = "function(settings:table):table -- Set open state of all sides, indexed by direction. Returns previous states.")
    public Object[] setSides(final Context context, final Arguments args) {
        final Map<?, ?> settings = args.checkTable(0);
        final Map<Integer, Boolean> previous = currentStatus();
        for (final Direction side : Direction.values()) {
            final Object value = tableValue(settings, side.ordinal());
            setSide(side, value instanceof Boolean typedValue && typedValue);
        }
        return new Object[]{previous};
    }

    @Callback(direct = true, doc = "function():table -- Get current open state of all sides, indexed by direction.")
    public Object[] getSides(final Context context, final Arguments args) {
        return new Object[]{currentStatus()};
    }

    @Callback(doc = "function(side:number):boolean -- Open the side. Returns true if it changed.")
    public Object[] open(final Context context, final Arguments args) {
        return setSideFromArgs(args, true);
    }

    @Callback(doc = "function(side:number):boolean -- Close the side. Returns true if it changed.")
    public Object[] close(final Context context, final Arguments args) {
        return setSideFromArgs(args, false);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_NODE)) {
            node().load(tag.getCompound(TAG_NODE));
        }
        inverted = tag.getBoolean(TAG_INVERTED);
        if (tag.contains(TAG_OPEN_SIDES)) {
            uncompressSides(tag.getByte(TAG_OPEN_SIDES));
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean(TAG_INVERTED, inverted);
        tag.putByte(TAG_OPEN_SIDES, compressSides());
        saveNode(tag);
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

    private Object[] setSideFromArgs(final Arguments args, final boolean value) {
        final int sideIndex = args.checkInteger(0);
        if (sideIndex < 0 || sideIndex > 5) {
            return new Object[]{null, "invalid direction"};
        }
        return new Object[]{setSide(Direction.from3DDataValue(sideIndex), value)};
    }

    private boolean setSide(final Direction side, final boolean effectiveState) {
        final boolean previous = isSideOpen(side);
        openSides[side.ordinal()] = inverted ? !effectiveState : effectiveState;
        if (previous != effectiveState) {
            rebuildNetwork(SoundEvents.PISTON_EXTEND);
        }
        return previous != effectiveState;
    }

    private Map<Integer, Boolean> currentStatus() {
        final Map<Integer, Boolean> status = new LinkedHashMap<>();
        for (final Direction side : Direction.values()) {
            status.put(side.ordinal(), isSideOpen(side));
        }
        return status;
    }

    private void rebuildNetwork(final net.minecraft.sounds.SoundEvent sound) {
        setChanged();
        if (level == null || level.isClientSide) {
            return;
        }
        removeNode();
        Network.joinOrCreateNetwork(level, worldPosition);
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.5F, 0.8F);
    }

    private void saveNode(final CompoundTag tag) {
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

    private byte compressSides() {
        int result = 0;
        for (final Direction side : Direction.values()) {
            if (openSides[side.ordinal()]) {
                result |= 1 << side.ordinal();
            }
        }
        return (byte) result;
    }

    private void uncompressSides(final byte value) {
        for (final Direction side : Direction.values()) {
            openSides[side.ordinal()] = ((1 << side.ordinal()) & value) != 0;
        }
    }

    private static Object tableValue(final Map<?, ?> table, final int ordinal) {
        if (table.containsKey(ordinal)) {
            return table.get(ordinal);
        }
        for (final Map.Entry<?, ?> entry : table.entrySet()) {
            if (entry.getKey() instanceof Number number && number.intValue() == ordinal) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Node createNode(final Environment environment) {
        return Network.newNode(environment, Visibility.Network)
            .withComponent("net_splitter", Visibility.Network)
            .create();
    }
}
