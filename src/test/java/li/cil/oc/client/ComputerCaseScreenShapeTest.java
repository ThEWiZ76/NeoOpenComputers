package li.cil.oc.client;

import li.cil.oc.common.menu.ComputerCaseMenu;
import li.cil.oc.common.network.ComputerCaseControlPayload;
import li.cil.oc.common.network.RackControlPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerData;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComputerCaseScreenShapeTest {
    @Test
    void computerCaseScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<ComputerCaseScreen> constructor = ComputerCaseScreen.class.getConstructor(
            ComputerCaseMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(ComputerCaseScreen.class));
        assertArrayEquals(new Class<?>[]{ComputerCaseMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }

    @Test
    void computerCaseScreenUsesUpstreamTextureAsset() {
        assertEquals(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "textures/gui/background.png"), ComputerCaseScreen.BACKGROUND_TEXTURE);
        assertEquals(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "textures/gui/computer.png"), ComputerCaseScreen.COMPUTER_TEXTURE);
        assertTrue(Files.exists(Path.of("src/main/resources/assets/neoopencomputers/textures/gui/background.png")));
        assertTrue(Files.exists(Path.of("src/main/resources/assets/neoopencomputers/textures/gui/computer.png")));
    }

    @Test
    void computerCasePowerControlUsesUpstreamTextureAtlas() {
        assertEquals(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "textures/gui/button_power.png"), ComputerCaseScreen.POWER_BUTTON_TEXTURE);
        assertTrue(Files.exists(Path.of("src/main/resources/assets/neoopencomputers/textures/gui/button_power.png")));
        assertEquals(0, ComputerCaseScreen.powerButtonTextureX(ComputerCaseMenu.STATE_READY));
        assertEquals(18, ComputerCaseScreen.powerButtonTextureX(ComputerCaseMenu.STATE_RUNNING));
        assertEquals(0, ComputerCaseScreen.powerButtonTextureY(false));
        assertEquals(18, ComputerCaseScreen.powerButtonTextureY(true));
        assertEquals(36, ComputerCaseScreen.powerButtonTextureWidth());
        assertEquals(36, ComputerCaseScreen.powerButtonTextureHeight());
        assertEquals(18, ComputerCaseScreen.slotTextureWidth());
        assertEquals(18, ComputerCaseScreen.slotTextureHeight());
    }

    @Test
    void computerCaseScreenExposesStatusLabels() throws NoSuchMethodException {
        final Method screenTitle = ComputerCaseScreen.class.getMethod("screenTitle");
        final Method statusLabel = ComputerCaseScreen.class.getMethod("statusLabel", int.class);
        final Method statusTooltip = ComputerCaseScreen.class.getMethod("statusTooltip", int.class, int.class, int.class, int.class);
        final Method statusControlTooltip = ComputerCaseScreen.class.getMethod("statusControlTooltip", int.class);
        final Method slotAt = ComputerCaseScreen.class.getMethod("computerSlotAt", int.class, int.class, int.class, int.class, int.class);
        final Method controlAt = ComputerCaseScreen.class.getDeclaredMethod("statusControlAt", int.class, int.class, int.class, int.class);
        final Method controlPayload = ComputerCaseScreen.class.getDeclaredMethod("controlPayload", ComputerCaseMenu.class, int.class);

        assertEquals(Component.class, screenTitle.getReturnType());
        assertEquals(Component.class, statusLabel.getReturnType());
        assertEquals(List.class, statusTooltip.getReturnType());
        assertEquals(List.class, statusControlTooltip.getReturnType());
        assertEquals(int.class, slotAt.getReturnType());
        assertEquals(boolean.class, controlAt.getReturnType());
        assertEquals(ComputerCaseControlPayload.class, controlPayload.getReturnType());
    }

    @Test
    void computerCaseScreenUsesShortTitleAndNoInlineStatusText() throws Exception {
        final Method renderLabels = ComputerCaseScreen.class.getDeclaredMethod(
            "renderLabels",
            net.minecraft.client.gui.GuiGraphics.class,
            int.class,
            int.class);
        assertEquals(void.class, renderLabels.getReturnType());
        assertTranslationKey("gui.neoopencomputers.computer_case.title", ComputerCaseScreen.screenTitle());

        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/ComputerCaseScreen.java"));
        assertTrue(!source.contains("drawString(font, statusLabel(menu.computerState())"));
    }

    @Test
    void computerCaseStatusTooltipShowsStateAndMissingRequirements() {
        assertTranslationKey("gui.neoopencomputers.computer_case.state.empty", ComputerCaseScreen.statusLabel(ComputerCaseMenu.STATE_EMPTY));
        assertTranslationKey("gui.neoopencomputers.computer_case.state.ready", ComputerCaseScreen.statusLabel(ComputerCaseMenu.STATE_READY));
        assertTranslationKey("gui.neoopencomputers.computer_case.state.running", ComputerCaseScreen.statusLabel(ComputerCaseMenu.STATE_RUNNING));
        assertTranslationKey("gui.neoopencomputers.computer_case.state.incomplete", ComputerCaseScreen.statusLabel(ComputerCaseMenu.STATE_INCOMPLETE));

        final List<Component> tooltip = ComputerCaseScreen.statusTooltip(
            ComputerCaseMenu.STATE_INCOMPLETE,
            ComputerCaseMenu.MISSING_CPU | ComputerCaseMenu.MISSING_MEMORY | ComputerCaseMenu.MISSING_EEPROM,
            0,
            0);

        assertEquals(5, tooltip.size());
        assertTranslationKey("gui.neoopencomputers.computer_case.status", tooltip.get(0));
        assertTranslationKey("gui.neoopencomputers.computer_case.state.incomplete", tooltip.get(1));
        assertTranslationKey("gui.neoopencomputers.rack.missing.cpu", tooltip.get(2));
        assertTranslationKey("gui.neoopencomputers.rack.missing.memory", tooltip.get(3));
        assertTranslationKey("gui.neoopencomputers.rack.missing.eeprom", tooltip.get(4));
    }

    @Test
    void computerCaseStatusTooltipShowsComponentCapacity() {
        final List<Component> tooltip = ComputerCaseScreen.statusTooltip(
            ComputerCaseMenu.STATE_READY,
            0,
            3,
            8);

        assertEquals(3, tooltip.size());
        assertTranslationKey("gui.neoopencomputers.computer_case.status", tooltip.get(0));
        assertTranslationKey("gui.neoopencomputers.computer_case.state.ready", tooltip.get(1));
        assertTranslationKey("gui.neoopencomputers.computer_case.components", tooltip.get(2));
        assertEquals(3, ((TranslatableContents) tooltip.get(2).getContents()).getArgs()[0]);
        assertEquals(8, ((TranslatableContents) tooltip.get(2).getContents()).getArgs()[1]);
    }

    @Test
    void computerCasePowerControlTooltipMatchesUpstreamTurnAction() {
        final List<Component> readyTooltip = ComputerCaseScreen.statusControlTooltip(ComputerCaseMenu.STATE_READY);
        final List<Component> runningTooltip = ComputerCaseScreen.statusControlTooltip(ComputerCaseMenu.STATE_RUNNING);

        assertEquals(1, readyTooltip.size());
        assertEquals(1, runningTooltip.size());
        assertTranslationKey("gui.neoopencomputers.computer_case.power.turn_on", readyTooltip.getFirst());
        assertTranslationKey("gui.neoopencomputers.computer_case.power.turn_off", runningTooltip.getFirst());
    }

    @Test
    void computerCasePowerControlPayloadUsesDesiredStateLikeUpstream() throws ReflectiveOperationException {
        final ComputerCaseMenu menu = allocateMenu(13, 0);

        final ComputerCaseControlPayload startPayload = ComputerCaseScreen.controlPayload(menu, ComputerCaseScreen.statusControlAction(ComputerCaseMenu.STATE_READY));
        final ComputerCaseControlPayload stopPayload = ComputerCaseScreen.controlPayload(menu, ComputerCaseScreen.statusControlAction(ComputerCaseMenu.STATE_RUNNING));

        assertEquals(13, startPayload.containerId());
        assertEquals(RackControlPayload.START, startPayload.action());
        assertEquals(13, stopPayload.containerId());
        assertEquals(RackControlPayload.STOP, stopPayload.action());
    }

    @Test
    void computerCaseScreenMapsMouseToUpstreamTieredSlots() {
        assertEquals(0, ComputerCaseScreen.computerSlotAt(98, 16, 0, 0, 0));
        assertEquals(6, ComputerCaseScreen.computerSlotAt(48, 34, 0, 0, 0));
        assertEquals(-1, ComputerCaseScreen.computerSlotAt(142, 52, 0, 0, 0));
        assertEquals(9, ComputerCaseScreen.computerSlotAt(48, 34, 0, 0, 2));
        assertEquals(-1, ComputerCaseScreen.computerSlotAt(35, 17, 0, 0, 0));
    }

    @Test
    void computerCaseScreenMapsMouseToStatusControl() {
        assertTrue(ComputerCaseScreen.statusControlAt(70, 33, 0, 0));
        assertTrue(ComputerCaseScreen.statusControlAt(87, 50, 0, 0));
        assertEquals(false, ComputerCaseScreen.statusControlAt(69, 33, 0, 0));
        assertEquals(false, ComputerCaseScreen.statusControlAt(88, 33, 0, 0));
        assertEquals(false, ComputerCaseScreen.statusControlAt(70, 51, 0, 0));
    }

    private static void assertTranslationKey(final String expected, final Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        assertEquals(expected, ((TranslatableContents) component.getContents()).getKey());
    }

    private static ComputerCaseMenu allocateMenu(final int containerId, final int tier) throws ReflectiveOperationException {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final ComputerCaseMenu menu = (ComputerCaseMenu) ((Unsafe) unsafeField.get(null)).allocateInstance(ComputerCaseMenu.class);
        final Field containerIdField = net.minecraft.world.inventory.AbstractContainerMenu.class.getDeclaredField("containerId");
        containerIdField.setAccessible(true);
        containerIdField.setInt(menu, containerId);
        final Field computerDataField = ComputerCaseMenu.class.getDeclaredField("computerData");
        computerDataField.setAccessible(true);
        computerDataField.set(menu, new ContainerData() {
            @Override
            public int get(final int index) {
                return index == ComputerCaseMenu.COMPUTER_TIER_INDEX ? tier : 0;
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return ComputerCaseMenu.COMPUTER_DATA_COUNT;
            }
        });
        return menu;
    }
}
