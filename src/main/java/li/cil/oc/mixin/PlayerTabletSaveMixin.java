package li.cil.oc.mixin;

import li.cil.oc.common.item.TabletRuntimeRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerTabletSaveMixin {
    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void neoopencomputers$saveTablets(CompoundTag tag, CallbackInfo ci) {
        TabletRuntimeRegistry.savePlayer((Player) (Object) this);
    }

    @Inject(method = "dropEquipment", at = @At("HEAD"))
    private void neoopencomputers$saveDeathDrops(CallbackInfo ci) {
        final Player player = (Player) (Object) this;
        if (!player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            // Death bypasses ItemTossEvent. Flush before drops can be serialized or picked up.
            TabletRuntimeRegistry.closePlayer(player);
        }
    }
}
