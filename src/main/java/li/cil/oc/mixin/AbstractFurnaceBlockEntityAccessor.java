package li.cil.oc.mixin;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityAccessor {
    @Accessor("litTime")
    int neoopencomputers$getLitTime();

    @Accessor("litTime")
    void neoopencomputers$setLitTime(int litTime);

    @Accessor("litDuration")
    int neoopencomputers$getLitDuration();

    @Accessor("litDuration")
    void neoopencomputers$setLitDuration(int litDuration);

    @Accessor("cookingProgress")
    int neoopencomputers$getCookingProgress();

    @Accessor("cookingProgress")
    void neoopencomputers$setCookingProgress(int cookingProgress);

    @Accessor("cookingTotalTime")
    int neoopencomputers$getCookingTotalTime();

    @Accessor("cookingTotalTime")
    void neoopencomputers$setCookingTotalTime(int cookingTotalTime);
}
