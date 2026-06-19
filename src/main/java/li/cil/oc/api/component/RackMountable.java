package li.cil.oc.api.component;

import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.util.StateAware;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface RackMountable extends ManagedEnvironment, StateAware {
    CompoundTag getData();

    int getConnectableCount();

    RackBusConnectable getConnectableAt(int index);

    boolean onActivate(Player player, InteractionHand hand, ItemStack heldItem, float hitX, float hitY);
}
