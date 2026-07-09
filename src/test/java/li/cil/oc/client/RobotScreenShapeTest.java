package li.cil.oc.client;

import li.cil.oc.common.menu.RobotMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RobotScreenShapeTest {
    @Test
    void robotScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<RobotScreen> constructor = RobotScreen.class.getConstructor(
            RobotMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(RobotScreen.class));
        assertArrayEquals(new Class<?>[]{RobotMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }

    @Test
    void robotScreenUsesUpstreamRobotTextures() {
        assertEquals(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "textures/gui/robot.png"), RobotScreen.ROBOT_TEXTURE);
        assertEquals(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "textures/gui/robot_noscreen.png"), RobotScreen.ROBOT_NO_SCREEN_TEXTURE);
        assertEquals(256, RobotScreen.robotImageWidth());
        assertEquals(256, RobotScreen.robotImageHeightWithScreen());
    }

    @Test
    void robotScreenUsesRobotTitle() {
        assertTranslationKey("gui.neoopencomputers.robot.title", RobotScreen.screenTitle());
    }

    @Test
    void robotSlotHitTestingUsesMenuPositions() {
        assertEquals(0, RobotScreen.robotSlotAt(170, 232, 0, 0, 0));
        assertEquals(14, RobotScreen.robotSlotAt(206, 192, 0, 0, 1));
        assertEquals(19, RobotScreen.robotSlotAt(224, 210, 0, 0, 3));
        assertEquals(-1, RobotScreen.robotSlotAt(152, 232, 0, 0, 3));
    }

    @Test
    void robotScreenHasComputerStyleStatusControl() {
        assertTrue(RobotScreen.statusControlAt(5, 153, 0, 0));
        assertEquals(0, RobotScreen.statusControlAction(RobotMenu.STATE_READY));
        assertEquals(1, RobotScreen.statusControlAction(RobotMenu.STATE_RUNNING));
    }

    @Test
    void robotScreenDoesNotDrawInventoryLabelOverPowerControls() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/RobotScreen.java"));

        assertTrue(!source.contains("drawString(font, playerInventoryTitle"));
    }

    @Test
    void robotScreenUsesGpuScreenFlagForUpperPanel() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/RobotScreen.java"));

        assertTrue(source.contains("menu.hasScreen()"));
        assertTrue(source.contains("drawScreenPanel"));
    }

    @Test
    void clientRegistersRobotMenuScreen() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(source.contains("ModMenus.ROBOT"));
        assertTrue(source.contains("RobotScreen::new"));
    }

    private static void assertTranslationKey(final String expected, final Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        assertEquals(expected, ((TranslatableContents) component.getContents()).getKey());
    }
}
