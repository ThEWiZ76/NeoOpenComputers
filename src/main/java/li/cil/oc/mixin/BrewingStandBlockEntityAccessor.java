package li.cil.oc.mixin;

import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BrewingStandBlockEntity.class)
public interface BrewingStandBlockEntityAccessor {
    @Accessor("brewTime")
    int neoopencomputers$getBrewTime();

    @Accessor("brewTime")
    void neoopencomputers$setBrewTime(int brewTime);
}
