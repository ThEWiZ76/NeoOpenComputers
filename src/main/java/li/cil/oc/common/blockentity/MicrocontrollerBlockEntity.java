package li.cil.oc.common.blockentity;

import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.menu.MicrocontrollerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.IMenuProviderExtension;

public class MicrocontrollerBlockEntity extends BlockEntity implements MenuProvider, IMenuProviderExtension {
    public MicrocontrollerBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.MICROCONTROLLER.get(), pos, blockState);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.neoopencomputers.microcontroller.title");
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory playerInventory, final Player player) {
        return new MicrocontrollerMenu(containerId, playerInventory, this);
    }

    @Override
    public void writeClientSideData(final AbstractContainerMenu menu, final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(0);
    }
}
