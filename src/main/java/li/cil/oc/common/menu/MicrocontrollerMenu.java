package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class MicrocontrollerMenu extends AbstractContainerMenu {
    public MicrocontrollerMenu(final int containerId, final Inventory playerInventory) {
        super(ModMenus.MICROCONTROLLER.get(), containerId);
    }

    public MicrocontrollerMenu(final int containerId, final Inventory playerInventory, final RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory);
    }

    public MicrocontrollerMenu(final int containerId, final Inventory playerInventory, final MicrocontrollerBlockEntity microcontroller) {
        this(containerId, playerInventory);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(final Player player) {
        return true;
    }
}
