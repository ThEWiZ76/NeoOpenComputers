package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Keyboard;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.component.KeyboardEnvironment;
import li.cil.oc.common.component.KeyboardInputState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class KeyboardBlockEntity extends BlockEntity implements Keyboard, DeviceInfo {
    private static final String TAG_NODE = "node";

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
        return usabilityOverride == null || usabilityOverride.isUsableByPlayer(this, player);
    }

    @Override
    public Node node() {
        if (node == null) {
            node = KeyboardEnvironment.createNode(this);
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
}
