package li.cil.oc.client;

import li.cil.oc.common.menu.DiskDriveMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DiskDriveScreenShapeTest {
    @Test
    void diskDriveScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<DiskDriveScreen> constructor = DiskDriveScreen.class.getConstructor(
            DiskDriveMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(DiskDriveScreen.class));
        assertArrayEquals(new Class<?>[]{DiskDriveMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }

    @Test
    void diskDriveScreenUsesUpstreamBackgroundAndSlotTextures() throws Exception {
        assertEquals(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "textures/gui/background.png"), DiskDriveScreen.BACKGROUND_TEXTURE);
        assertEquals(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "textures/gui/slot.png"), DiskDriveScreen.SLOT_TEXTURE);
        assertTrue(Files.exists(Path.of("src/main/resources/assets/neoopencomputers/textures/gui/background.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/assets/neoopencomputers/textures/gui/slot.png")));
        assertEquals(18, DiskDriveScreen.slotTextureWidth());
        assertEquals(18, DiskDriveScreen.slotTextureHeight());
        assertEquals(18, DiskDriveScreen.slotDrawnWidth());
        assertEquals(18, DiskDriveScreen.slotDrawnHeight());
        assertEquals(8, DiskDriveScreen.titleTextX());
        assertEquals(6, DiskDriveScreen.titleTextY());
        assertEquals(8, DiskDriveScreen.inventoryTextX());
        assertEquals(72, DiskDriveScreen.inventoryTextY());

        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/DiskDriveScreen.java"));
        assertTrue(!source.contains("0xFF2E3440"));
        assertTrue(!source.contains("0xFF3B4252"));
        assertTrue(!source.contains("statusLabel"));
        assertTrue(!source.contains("statusTooltip"));
        assertTrue(!source.contains("mediaState"));
        assertTrue(!source.contains("renderComponentTooltip(font, status"));
        assertTrue(source.contains("slotTextureWidth(),"));
        assertTrue(source.contains("slotTextureHeight()"));
        assertTrue(!source.contains("guiGraphics.blit(SLOT_TEXTURE, left, top, 0, 0, SLOT_TEXTURE_SIZE, SLOT_TEXTURE_SIZE);"));
    }

    @Test
    void diskDriveRegistersInsertedMediaRenderer() throws Exception {
        assertTrue(Files.exists(Path.of("src/main/java/li/cil/oc/client/DiskDriveBlockEntityRenderer.java")));

        final String rendererSource = Files.readString(Path.of("src/main/java/li/cil/oc/client/DiskDriveBlockEntityRenderer.java"));
        assertTrue(rendererSource.contains("BlockEntityRenderer<DiskDriveBlockEntity>"));
        assertTrue(rendererSource.contains("getItem(DiskDriveBlockEntity.SLOT_FLOPPY)"));
        assertTrue(rendererSource.contains("ItemDisplayContext.FIXED"));
        assertTrue(rendererSource.contains("renderStatic"));

        final String clientSource = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));
        assertTrue(clientSource.contains("DiskDriveBlockEntityRenderer::new"));
    }

    @Test
    void diskDriveRendererUsesUpstreamActivityOverlay() throws Exception {
        final String rendererSource = Files.readString(Path.of("src/main/java/li/cil/oc/client/DiskDriveBlockEntityRenderer.java"));

        assertTrue(rendererSource.contains("diskdrive_front_activity"));
        assertTrue(rendererSource.contains("getLastAccess"));
        assertTrue(rendererSource.contains("400L"));
    }

    @Test
    void diskDriveRendererUsesUpstreamFrontCoordinateSystem() throws Exception {
        final String rendererSource = Files.readString(Path.of("src/main/java/li/cil/oc/client/DiskDriveBlockEntityRenderer.java"));

        assertTrue(rendererSource.contains("poseStack.translate(0D, 3.5D / 16D, 6D / 16D);"));
        assertTrue(rendererSource.contains("case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(-90F));"));
        assertTrue(rendererSource.contains("case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180F));"));
        assertTrue(rendererSource.contains("case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90F));"));
        assertTrue(!rendererSource.contains("case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180F));"));
        assertTrue(rendererSource.contains("0.505F"));
        assertTrue(rendererSource.contains("setNormal(pose, 0F, 0F, 1F)"));
    }

}
