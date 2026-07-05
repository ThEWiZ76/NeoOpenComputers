package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DroneRuntimeShapeTest {
    @Test
    void registersDroneEntityTypeFromMainMod() throws Exception {
        final String modEntities = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModEntities.java"));
        final String modSource = Files.readString(Path.of("src/main/java/li/cil/oc/NeoOpenComputers.java"));

        assertTrue(modEntities.contains("public static final DeferredHolder<EntityType<?>, EntityType<DroneEntity>> DRONE"));
        assertTrue(modEntities.contains("ModContentIds.DRONE"));
        assertTrue(modSource.contains("ModEntities.register(modEventBus);"));
    }

    @Test
    void droneEntityImplementsRuntimeHostContracts() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/entity/DroneEntity.java"));

        assertTrue(source.contains("extends Entity implements Drone, Environment, Container, MenuProvider, IMenuProviderExtension"));
        assertTrue(source.contains("public Machine machine()"));
        assertTrue(source.contains("public Iterable<ItemStack> internalComponents()"));
        assertTrue(source.contains("public Container mainInventory()"));
        assertTrue(source.contains("public AbstractContainerMenu createMenu"));
    }

    @Test
    void droneEntityExposesRuntimeStateAndCallbacks() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/entity/DroneEntity.java"));

        assertTrue(source.contains("public boolean toggleMachine()"));
        assertTrue(source.contains("public void loadFromItemStack(final ItemStack stack, final Player player)"));
        assertTrue(source.contains("public static int slotCount(final int tier)"));
        assertTrue(source.contains("public static String slotType(final int tier, final int slot)"));
        assertTrue(source.contains("public static int slotTier(final int tier, final int slot)"));
        assertTrue(source.contains("public Object[] getStatusText(final Context context, final Arguments arguments)"));
        assertTrue(source.contains("public Object[] setStatusText(final Context context, final Arguments arguments)"));
        assertTrue(source.contains("public Object[] getLightColor(final Context context, final Arguments arguments)"));
        assertTrue(source.contains("public Object[] setLightColor(final Context context, final Arguments arguments)"));
        assertTrue(source.contains("public Object[] move(final Context context, final Arguments arguments)"));
        assertTrue(source.contains("public Object[] getOffset(final Context context, final Arguments arguments)"));
        assertTrue(source.contains("public Object[] getVelocity(final Context context, final Arguments arguments)"));
        assertTrue(source.contains("public Object[] getMaxVelocity(final Context context, final Arguments arguments)"));
        assertTrue(source.contains("public Object[] getAcceleration(final Context context, final Arguments arguments)"));
        assertTrue(source.contains("public Object[] setAcceleration(final Context context, final Arguments arguments)"));
    }
}
