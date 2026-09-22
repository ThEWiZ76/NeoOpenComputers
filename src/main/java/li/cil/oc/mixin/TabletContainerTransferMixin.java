package li.cil.oc.mixin;

import li.cil.oc.common.item.TabletRuntimeRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class TabletContainerTransferMixin {
    @Inject(method = "clicked", at = @At("HEAD"))
    private void neoopencomputers$saveBeforeClick(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        TabletRuntimeRegistry.beforeContainerClick((AbstractContainerMenu) (Object) this, player);
    }

    @Inject(method = "clicked", at = @At("RETURN"))
    private void neoopencomputers$rebindAfterClick(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        TabletRuntimeRegistry.afterContainerClick((AbstractContainerMenu) (Object) this, player);
    }
}
