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
    void robotRendererUsesLegacyChassisMesh() throws Exception {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/RobotBlockEntityRenderer.java"));

        assertTrue(renderer.contains("BlockEntityRenderer<RobotBlockEntity>"));
        assertTrue(renderer.contains("renderLegacyChassis"));
        assertTrue(renderer.contains("renderTopChassis"));
        assertTrue(renderer.contains("renderBottomChassis"));
        assertTrue(renderer.contains("renderSelectedStack"));
        assertTrue(!renderer.contains("new RobotModel(context.bakeLayer(RobotModel.LAYER_LOCATION))"));
        assertTrue(renderer.contains("RobotModel.TEXTURE"));
        assertTrue(renderer.contains("RenderType.entityCutout"));
        assertTrue(renderer.contains("CHASSIS_HALF = 0.40F"));
        assertTrue(renderer.contains("TOP_SEAM_Y = 0.54F"));
        assertTrue(renderer.contains("BOTTOM_SEAM_Y = 0.46F"));
        assertTrue(renderer.contains("TOP_APEX_Y = 0.96F"));
        assertTrue(renderer.contains("BOTTOM_APEX_Y = 0.04F"));
        assertTrue(!renderer.contains("renderFrontChest"));
        assertTrue(!renderer.contains("0xFFE2E2E2"));
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
