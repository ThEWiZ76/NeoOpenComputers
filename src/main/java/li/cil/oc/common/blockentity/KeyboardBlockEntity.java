package li.cil.oc.common.blockentity;

import li.cil.oc.api.internal.Keyboard;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class KeyboardBlockEntity extends BlockEntity implements Keyboard {
    private UsabilityChecker usabilityOverride;

    public KeyboardBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.KEYBOARD.get(), pos, blockState);
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
        return null;
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
    public void load(final CompoundTag nbt) {
    }

    @Override
    public void save(final CompoundTag nbt) {
    }
}
