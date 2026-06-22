package li.cil.oc.common.damage;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;

public final class ModDamageSources {
    public static final ResourceKey<DamageType> NANOMACHINES_HUNGRY = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "nanomachines_hungry")
    );

    public static DamageSource nanomachinesHungry(final Player player) {
        return new DamageSource(
            player.level()
                .registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(NANOMACHINES_HUNGRY),
            player
        );
    }

    private ModDamageSources() {
    }
}
