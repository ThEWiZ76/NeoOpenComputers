package li.cil.oc.client;

import li.cil.oc.common.menu.RobotMenu;
import li.cil.oc.common.menu.TerminalMenu;
import li.cil.oc.common.component.TerminalScreenSnapshot;
import li.cil.oc.common.network.TerminalMousePayload;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import sun.misc.Unsafe;
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
        assertTrue(TerminalScreen.class.isAssignableFrom(RobotScreen.class), "Robot must render and forward input through the terminal screen");
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
    void robotScreenUsesInstalledScreenFlagForUpperPanel() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/RobotScreen.java"));

        assertTrue(source.contains("menu.hasScreen()"));
        assertTrue(source.contains("drawScreenPanel"));
    }

    @Test
    void compactRobotControlsAndSlotsUseTheSameVerticalOffset() {
        final int compactTop = 100 - RobotMenu.layoutOffset(false);
        assertEquals(148, RobotMenu.layoutOffset(false));
        assertEquals(0, RobotMenu.layoutOffset(true));
        assertTrue(RobotScreen.statusControlAt(5, 105, 0, compactTop));
        assertEquals(false, RobotScreen.statusControlAt(5, 104, 0, compactTop));
        assertEquals(false, RobotScreen.statusControlAt(5, 253, 0, compactTop));
        assertEquals(4, RobotScreen.robotSlotAt(170, 108, 0, compactTop, 0));
        assertEquals(0, RobotScreen.robotSlotAt(170, 184, 0, compactTop, 0));
        assertEquals(-1, RobotScreen.robotSlotAt(170, 107, 0, compactTop, 0));
    }

    @Test
    void robotTerminalMouseCoordinatesMatchItsScaledUpperPanel() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final Unsafe unsafe = (Unsafe) unsafeField.get(null);
        final RobotScreen screen = (RobotScreen) unsafe.allocateInstance(RobotScreen.class);
        for (final String name : new String[]{"leftPos", "topPos"}) {
            final Field field = AbstractContainerScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            field.setInt(screen, name.equals("leftPos") ? 100 : 40);
        }
        final TerminalMenu menu = (TerminalMenu) unsafe.allocateInstance(TerminalMenu.class);
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(80, 25, new String[25]);
        final TerminalScreen.TerminalFrame frame = screen.terminalFrame();
        final double scale = TerminalScreen.terminalScale(snapshot, frame.width(), frame.height());
        assertEquals(0.32D, scale, 1.0E-9);
        final TerminalMousePayload input = TerminalScreen.mousePayload(menu, TerminalMousePayload.MOUSE_DOWN,
            108 + 10 * 8 * scale, 59 + 4 * 16 * scale, 0,
            frame.left(), frame.top(), snapshot, frame.width(), frame.height());
        assertEquals(10, input.x(), 1.0E-9);
        assertEquals(4, input.y(), 1.0E-9);
        assertTrue(TerminalScreen.terminalContains(snapshot, frame, 108, 59));
        assertEquals(false, TerminalScreen.terminalContains(snapshot, frame, 107, 59));
        assertEquals(false, TerminalScreen.terminalContains(snapshot, frame, 108, 187));
        assertEquals(false, TerminalScreen.terminalContains(snapshot, frame, 110, 194));
        // Power controls and the inventory must not generate terminal clicks.
        assertEquals(null, TerminalScreen.mousePayload(menu, TerminalMousePayload.MOUSE_DOWN,
            110, 194, 0, frame.left(), frame.top(), snapshot, frame.width(), frame.height()));
        assertEquals(null, TerminalScreen.mousePayload(menu, TerminalMousePayload.MOUSE_DOWN,
            107, 60, 0, frame.left(), frame.top(), snapshot, frame.width(), frame.height()));
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
