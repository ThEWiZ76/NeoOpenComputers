package li.cil.oc.mixin;

import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nullable;

@Mixin(BaseSpawner.class)
public interface BaseSpawnerAccessor {
    @Nullable
    @Accessor("nextSpawnData")
    SpawnData neoopencomputers$getNextSpawnData();
}
