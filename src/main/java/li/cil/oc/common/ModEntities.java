package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.entity.DroneEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, NeoOpenComputers.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DroneEntity>> DRONE =
        ENTITY_TYPES.register(
            ModContentIds.DRONE,
            () -> EntityType.Builder.<DroneEntity>of(DroneEntity::new, MobCategory.MISC)
                .sized(0.75F, 0.375F)
                .clientTrackingRange(80)
                .updateInterval(1)
                .fireImmune()
                .build(ModContentIds.DRONE));

    public static void register(final IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }

    private ModEntities() {
    }
}
