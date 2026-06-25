package li.cil.oc.mixin;

import li.cil.oc.common.blockentity.PrintBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {
    @Redirect(
        method = "updateBase",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
        )
    )
    private static BlockState neoopencomputers$getBeaconBaseBlockState(final Level level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (level.getBlockEntity(pos) instanceof PrintBlockEntity print && print.data().isBeaconBase()) {
            return Blocks.DIAMOND_BLOCK.defaultBlockState();
        }
        return state;
    }
}
