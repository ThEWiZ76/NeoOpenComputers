package li.cil.oc.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nullable;

@Mixin(BeaconBlockEntity.class)
public interface BeaconBlockEntityAccessor {
    @Accessor("levels")
    int neoopencomputers$getLevels();

    @Accessor("levels")
    void neoopencomputers$setLevels(int levels);

    @Accessor("primaryPower")
    @Nullable
    Holder<MobEffect> neoopencomputers$getPrimaryPower();

    @Accessor("primaryPower")
    void neoopencomputers$setPrimaryPower(@Nullable Holder<MobEffect> effect);

    @Accessor("secondaryPower")
    @Nullable
    Holder<MobEffect> neoopencomputers$getSecondaryPower();

    @Accessor("secondaryPower")
    void neoopencomputers$setSecondaryPower(@Nullable Holder<MobEffect> effect);
}
