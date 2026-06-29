package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Keyboard;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.block.KeyboardBlock;
import li.cil.oc.common.component.KeyboardEnvironment;
import li.cil.oc.common.component.KeyboardInputState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class KeyboardBlockEntity extends BlockEntity implements Keyboard, DeviceInfo, SidedEnvironment {
    private static final String TAG_NODE = "node";
    private static final double DEFAULT_USABLE_DISTANCE_SQUARED = 64D;

    private UsabilityChecker usabilityOverride;
    private final KeyboardInputState inputState = new KeyboardInputState();
    private Node node;

    public KeyboardBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.KEYBOARD.get(), pos, blockState);
        node = KeyboardEnvironment.createNode(this);
    }

    @Override
    public void setUsableOverride(final UsabilityChecker callback) {
        usabilityOverride = callback;
    }

    public boolean isUsableByPlayer(final Player player) {
        if (usabilityOverride != null) {
            return usabilityOverride.isUsableByPlayer(this, player);
        }
        return player == null || player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= DEFAULT_USABLE_DISTANCE_SQUARED;
    }

    @Override
    public Node node() {
        if (node == null) {
            node = KeyboardEnvironment.createNode(this);
        }
        return node;
    }

    @Override
    public Node sidedNode(final Direction side) {
        return canConnect(side) ? node() : null;
    }

    @Override
    public boolean canConnect(final Direction side) {
        if (side == null || !(getBlockState().getBlock() instanceof KeyboardBlock)) {
            return false;
        }
        final BlockState state = getBlockState();
        return hasNodeOnSide(state.getValue(KeyboardBlock.ATTACH_FACE), state.getValue(KeyboardBlock.FACING), side);
    }

    @Override
    public void onConnect(final Node node) {
    }

    @Override
    public void onDisconnect(final Node node) {
    }

    @Override
    public void onMessage(final Message message) {
        inputState.onMessage(node(), message, this::isUsableByPlayer);
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return KeyboardEnvironment.deviceInfo();
    }

    @Override
    public void load(final CompoundTag nbt) {
        if (nbt.contains(TAG_NODE) && node() != null) {
            node().load(nbt.getCompound(TAG_NODE));
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
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

    @Override
    protected void loadAdditional(final CompoundTag nbt, final HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        load(nbt);
    }

    @Override
    protected void saveAdditional(final CompoundTag nbt, final HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        save(nbt);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            Network.joinOrCreateNetwork(this);
        }
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

    private void removeNode() {
        if (node != null) {
            node.remove();
        }
    }

    private static boolean hasNodeOnSide(final Direction attachFace, final Direction facing, final Direction side) {
        if (attachFace == null || side == null) {
            return false;
        }
        final boolean onWall = !attachFace.getAxis().isVertical();
        final Direction forward = onWall ? Direction.UP : facing;
        return side != attachFace && (onWall || side.getOpposite() != forward);
    }
}
