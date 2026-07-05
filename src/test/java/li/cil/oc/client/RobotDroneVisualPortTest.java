package li.cil.oc.client;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class RobotDroneVisualPortTest {
    @Test
    void clientRegistersRobotAndDroneModelLayers() throws Exception {
        final Method method = NeoOpenComputersClient.class.getDeclaredMethod(
            "registerLayerDefinitions",
            EntityRenderersEvent.RegisterLayerDefinitions.class);
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue((method.getModifiers() & java.lang.reflect.Modifier.STATIC) != 0);
        assertTrue(source.contains("RobotModel.LAYER_LOCATION"));
        assertTrue(source.contains("RobotModel::createBodyLayer"));
        assertTrue(source.contains("DroneModel.LAYER_LOCATION"));
        assertTrue(source.contains("DroneModel::createBodyLayer"));
    }

    @Test
    void clientRegistersRobotAndDroneRenderers() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(source.contains("event.registerBlockEntityRenderer(ModBlockEntities.ROBOT.get(), RobotBlockEntityRenderer::new)"));
        assertTrue(source.contains("event.registerEntityRenderer(ModEntities.DRONE.get(), DroneEntityRenderer::new)"));
    }

    @Test
    void robotRendererUsesDedicatedModelAndTexture() throws Exception {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/RobotBlockEntityRenderer.java"));

        assertTrue(renderer.contains("BlockEntityRenderer<RobotBlockEntity>"));
        assertTrue(renderer.contains("new RobotModel(context.bakeLayer(RobotModel.LAYER_LOCATION))"));
        assertTrue(renderer.contains("RobotModel.TEXTURE"));
        assertTrue(renderer.contains("RenderType.entityCutout"));
    }

    @Test
    void droneRendererUsesDedicatedModelAndTexture() throws Exception {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/DroneEntityRenderer.java"));

        assertTrue(renderer.contains("new DroneModel(context.bakeLayer(DroneModel.LAYER_LOCATION))"));
        assertTrue(renderer.contains("DroneModel.TEXTURE"));
        assertTrue(renderer.contains("model.renderToBuffer"));
        assertTrue(!renderer.contains("textures/item/tablet.png"));
    }
}
