package li.cil.oc.client;

import li.cil.oc.common.menu.DiskDriveMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    void diskDriveScreenExposesStatusLabels() throws NoSuchMethodException {
        final Method statusLabel = DiskDriveScreen.class.getMethod("statusLabel", int.class);
        final Method statusTooltip = DiskDriveScreen.class.getMethod("statusTooltip", int.class);

        assertEquals(Component.class, statusLabel.getReturnType());
        assertEquals(List.class, statusTooltip.getReturnType());
    }

    @Test
    void diskDriveScreenUsesUpstreamBackgroundAndSlotTextures() throws Exception {
        assertEquals(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "textures/gui/background.png"), DiskDriveScreen.BACKGROUND_TEXTURE);
        assertEquals(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "textures/gui/slot.png"), DiskDriveScreen.SLOT_TEXTURE);
        assertTrue(Files.exists(Path.of("src/main/resources/assets/neoopencomputers/textures/gui/background.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/assets/neoopencomputers/textures/gui/slot.png")));
        assertEquals(18, DiskDriveScreen.slotTextureWidth());
        assertEquals(18, DiskDriveScreen.slotTextureHeight());

        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/DiskDriveScreen.java"));
        assertTrue(!source.contains("0xFF2E3440"));
        assertTrue(!source.contains("0xFF3B4252"));
    }

    @Test
    void diskDriveStatusTooltipShowsMediaState() {
        assertTranslationKey("gui.neoopencomputers.disk_drive.state.empty", DiskDriveScreen.statusLabel(DiskDriveMenu.STATE_EMPTY));
        assertTranslationKey("gui.neoopencomputers.disk_drive.state.loaded", DiskDriveScreen.statusLabel(DiskDriveMenu.STATE_LOADED));

        final List<Component> tooltip = DiskDriveScreen.statusTooltip(DiskDriveMenu.STATE_LOADED);

        assertEquals(2, tooltip.size());
        assertTranslationKey("gui.neoopencomputers.disk_drive.status", tooltip.get(0));
        assertTranslationKey("gui.neoopencomputers.disk_drive.state.loaded", tooltip.get(1));
    }

    private static void assertTranslationKey(final String expected, final Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        assertEquals(expected, ((TranslatableContents) component.getContents()).getKey());
    }
}
