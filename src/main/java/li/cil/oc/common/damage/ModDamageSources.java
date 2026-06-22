package li.cil.oc.common.damage;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;

public final class ModDamageSources {
    private static final String NANOMACHINES_HUNGRY_MESSAGE_ID = "oc.nanomachinesHungry";
    private static final int NANOMACHINES_HUNGRY_CAUSES = 3;
    private static final String NANOMACHINES_OVERLOAD_MESSAGE_ID = "oc.nanomachinesOverload";
    private static final int NANOMACHINES_OVERLOAD_CAUSES = 3;

    public static final ResourceKey<DamageType> NANOMACHINES_HUNGRY = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "nanomachines_hungry")
    );
    public static final ResourceKey<DamageType> NANOMACHINES_OVERLOAD = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "nanomachines_overload")
    );

    public static DamageSource nanomachinesHungry(final Player player) {
        return new RandomCauseDamageSource(
            player.level()
                .registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(NANOMACHINES_HUNGRY),
            NANOMACHINES_HUNGRY_MESSAGE_ID,
            NANOMACHINES_HUNGRY_CAUSES
        );
    }

    public static DamageSource nanomachinesOverload(final Player player) {
        return new RandomCauseDamageSource(
            player.level()
                .registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(NANOMACHINES_OVERLOAD),
            NANOMACHINES_OVERLOAD_MESSAGE_ID,
            NANOMACHINES_OVERLOAD_CAUSES
        );
    }

    private ModDamageSources() {
    }
}
