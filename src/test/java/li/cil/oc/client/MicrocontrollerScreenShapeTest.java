package li.cil.oc.client;

import li.cil.oc.common.menu.MicrocontrollerMenu;
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

final class MicrocontrollerScreenShapeTest {
    @Test
    void microcontrollerScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<MicrocontrollerScreen> constructor = MicrocontrollerScreen.class.getConstructor(
            MicrocontrollerMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(MicrocontrollerScreen.class));
        assertArrayEquals(new Class<?>[]{MicrocontrollerMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }

    @Test
    void microcontrollerScreenUsesExistingComputerTextures() {
        assertEquals(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "textures/gui/background.png"), MicrocontrollerScreen.BACKGROUND_TEXTURE);
        assertEquals(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "textures/gui/computer.png"), MicrocontrollerScreen.MICROCONTROLLER_TEXTURE);
    }

    @Test
    void microcontrollerScreenUsesMicrocontrollerTitle() {
        assertTranslationKey("gui.neoopencomputers.microcontroller.title", MicrocontrollerScreen.screenTitle());
    }

    @Test
    void microcontrollerSlotHitTestingUsesMenuPositions() {
        assertEquals(0, MicrocontrollerScreen.microcontrollerSlotAt(48, 16, 0, 0, 0));
        assertEquals(3, MicrocontrollerScreen.microcontrollerSlotAt(48, 34, 0, 0, 1));
        assertEquals(15, MicrocontrollerScreen.microcontrollerSlotAt(156, 52, 0, 0, 3));
        assertEquals(-1, MicrocontrollerScreen.microcontrollerSlotAt(156, 52, 0, 0, 0));
    }

    @Test
    void clientRegistersMicrocontrollerMenuScreen() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(source.contains("ModMenus.MICROCONTROLLER"));
        assertTrue(source.contains("MicrocontrollerScreen::new"));
    }

    private static void assertTranslationKey(final String expected, final Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        assertEquals(expected, ((TranslatableContents) component.getContents()).getKey());
    }
}
