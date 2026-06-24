package li.cil.oc.client;

import li.cil.oc.common.menu.ServerRackMenu;
import li.cil.oc.common.network.RackControlPayload;
import li.cil.oc.common.network.ServerRackControlPayload;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ServerRackScreenShapeTest {
    @Test
    void serverRackScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<ServerRackScreen> constructor = ServerRackScreen.class.getConstructor(
            ServerRackMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(ServerRackScreen.class));
        assertArrayEquals(new Class<?>[]{ServerRackMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }

    @Test
    void serverRackScreenExposesSlotLabels() throws NoSuchMethodException {
        final Method label = ServerRackScreen.class.getMethod("slotLabel", int.class);
        final Method tierLabel = ServerRackScreen.class.getMethod("slotTierLabel", int.class);
        final Method tooltip = ServerRackScreen.class.getMethod("slotTooltip", int.class, int.class);
        final Method occupiedTooltip = ServerRackScreen.class.getMethod("slotTooltip", int.class, int.class, boolean.class);
        final Method statusLabel = ServerRackScreen.class.getMethod("statusLabel", int.class);
        final Method statusTooltip = ServerRackScreen.class.getMethod("statusTooltip", int.class, int.class);
        final Method capacityStatusTooltip = ServerRackScreen.class.getMethod("statusTooltip", int.class, int.class, int.class, int.class);
        final Method slotAt = ServerRackScreen.class.getMethod("serverSlotAt", int.class, int.class, int.class, int.class);
        final Method controlAt = ServerRackScreen.class.getDeclaredMethod("statusControlAt", int.class, int.class, int.class, int.class);
        final Method controlPayload = ServerRackScreen.class.getDeclaredMethod("controlPayload", ServerRackMenu.class, int.class);

        assertEquals(Component.class, label.getReturnType());
        assertEquals(Component.class, tierLabel.getReturnType());
        assertEquals(List.class, tooltip.getReturnType());
        assertEquals(List.class, occupiedTooltip.getReturnType());
        assertEquals(Component.class, statusLabel.getReturnType());
        assertEquals(List.class, statusTooltip.getReturnType());
        assertEquals(List.class, capacityStatusTooltip.getReturnType());
        assertEquals(int.class, slotAt.getReturnType());
        assertEquals(boolean.class, controlAt.getReturnType());
        assertEquals(ServerRackControlPayload.class, controlPayload.getReturnType());
    }

    @Test
    void serverRackScreenMapsMouseToServerSlots() {
        assertEquals(0, ServerRackScreen.serverSlotAt(76, 7, 0, 0));
        assertEquals(13, ServerRackScreen.serverSlotAt(148, 61, 0, 0));
        assertEquals(16, ServerRackScreen.serverSlotAt(26, 34, 0, 0));
        assertEquals(-1, ServerRackScreen.serverSlotAt(8, 18, 0, 0));
        assertEquals(-1, ServerRackScreen.serverSlotAt(170, 18, 0, 0));
    }

    @Test
    void serverRackScreenUsesUpstreamTieredSlotLayout() {
        assertEquals(new ServerRackMenu.ServerSlotPosition(76, 7), ServerRackScreen.slotPositionForTier(1, 0));
        assertEquals(new ServerRackMenu.ServerSlotPosition(100, 43), ServerRackScreen.slotPositionForTier(1, 4));
        assertEquals(new ServerRackMenu.ServerSlotPosition(76, 43), ServerRackScreen.slotPositionForTier(1, 11));
        assertEquals(new ServerRackMenu.ServerSlotPosition(26, 34), ServerRackScreen.slotPositionForTier(1, 12));
        assertNull(ServerRackScreen.slotPositionForTier(1, 13));

        assertEquals(new ServerRackMenu.ServerSlotPosition(148, 61), ServerRackScreen.slotPositionForTier(2, 13));
        assertEquals(new ServerRackMenu.ServerSlotPosition(76, 61), ServerRackScreen.slotPositionForTier(2, 15));
        assertEquals(new ServerRackMenu.ServerSlotPosition(26, 34), ServerRackScreen.slotPositionForTier(2, 16));
    }

    @Test
    void serverRackScreenMapsMouseToTieredServerSlotsLikeUpstream() {
        assertEquals(0, ServerRackScreen.serverSlotAt(76, 7, 0, 0, 1));
        assertEquals(12, ServerRackScreen.serverSlotAt(26, 34, 0, 0, 1));
        assertEquals(16, ServerRackScreen.serverSlotAt(26, 34, 0, 0, 2));
        assertEquals(-1, ServerRackScreen.serverSlotAt(8, 18, 0, 0, 1));
        assertEquals(-1, ServerRackScreen.serverSlotAt(148, 61, 0, 0, 1));
    }

    @Test
    void serverRackSlotTooltipContainsSlotLabel() {
        final List<Component> tooltip = ServerRackScreen.slotTooltip(ServerRackMenu.SLOT_KIND_CPU, 2);

        assertEquals(ServerRackScreen.slotLabel(ServerRackMenu.SLOT_KIND_CPU), tooltip.getFirst());
        assertEquals(ServerRackScreen.slotTierLabel(2), tooltip.get(1));
    }

    @Test
    void serverRackSlotTooltipShowsOccupancy() {
        final List<Component> occupied = ServerRackScreen.slotTooltip(ServerRackMenu.SLOT_KIND_CPU, 2, true);
        final List<Component> empty = ServerRackScreen.slotTooltip(ServerRackMenu.SLOT_KIND_CPU, 2, false);

        assertTranslationKey("gui.neoopencomputers.server_rack.slot.installed", occupied.get(2));
        assertTranslationKey("gui.neoopencomputers.server_rack.slot.empty_state", empty.get(2));
    }

    @Test
    void serverRackSlotTierLabelsDistinguishAnyAndUnused() {
        assertEquals("gui.neoopencomputers.server_rack.slot.max_tier", ServerRackScreen.slotTierLabel(2).getString());
        assertEquals("gui.neoopencomputers.server_rack.slot.any_tier", ServerRackScreen.slotTierLabel(Integer.MAX_VALUE).getString());
        assertEquals("gui.neoopencomputers.server_rack.slot.unavailable", ServerRackScreen.slotTierLabel(-1).getString());
    }

    @Test
    void serverRackStatusTooltipShowsStateAndMissingRequirements() {
        assertTranslationKey("gui.neoopencomputers.server_rack.state.empty", ServerRackScreen.statusLabel(ServerRackMenu.STATE_EMPTY));
        assertTranslationKey("gui.neoopencomputers.server_rack.state.ready", ServerRackScreen.statusLabel(ServerRackMenu.STATE_READY));
        assertTranslationKey("gui.neoopencomputers.server_rack.state.running", ServerRackScreen.statusLabel(ServerRackMenu.STATE_RUNNING));
        assertTranslationKey("gui.neoopencomputers.server_rack.state.incomplete", ServerRackScreen.statusLabel(ServerRackMenu.STATE_INCOMPLETE));

        final List<Component> tooltip = ServerRackScreen.statusTooltip(
            ServerRackMenu.STATE_INCOMPLETE,
            ServerRackMenu.MISSING_CPU | ServerRackMenu.MISSING_MEMORY | ServerRackMenu.MISSING_EEPROM);

        assertEquals(5, tooltip.size());
        assertTranslationKey("gui.neoopencomputers.server_rack.status", tooltip.get(0));
        assertTranslationKey("gui.neoopencomputers.server_rack.state.incomplete", tooltip.get(1));
        assertTranslationKey("gui.neoopencomputers.rack.missing.cpu", tooltip.get(2));
        assertTranslationKey("gui.neoopencomputers.rack.missing.memory", tooltip.get(3));
        assertTranslationKey("gui.neoopencomputers.rack.missing.eeprom", tooltip.get(4));
    }

    @Test
    void serverRackStatusTooltipShowsComponentCapacity() {
        final List<Component> tooltip = ServerRackScreen.statusTooltip(
            ServerRackMenu.STATE_READY,
            0,
            5,
            12);

        assertEquals(3, tooltip.size());
        assertTranslationKey("gui.neoopencomputers.server_rack.status", tooltip.get(0));
        assertTranslationKey("gui.neoopencomputers.server_rack.state.ready", tooltip.get(1));
        assertTranslationKey("gui.neoopencomputers.server_rack.components", tooltip.get(2));
        assertEquals(5, ((TranslatableContents) tooltip.get(2).getContents()).getArgs()[0]);
        assertEquals(12, ((TranslatableContents) tooltip.get(2).getContents()).getArgs()[1]);
    }

    @Test
    void serverRackPowerControlTooltipMatchesUpstreamTurnAction() {
        final List<Component> readyTooltip = ServerRackScreen.statusControlTooltip(ServerRackMenu.STATE_READY);
        final List<Component> runningTooltip = ServerRackScreen.statusControlTooltip(ServerRackMenu.STATE_RUNNING);

        assertEquals(1, readyTooltip.size());
        assertEquals(1, runningTooltip.size());
        assertTranslationKey("gui.neoopencomputers.server_rack.power.turn_on", readyTooltip.getFirst());
        assertTranslationKey("gui.neoopencomputers.server_rack.power.turn_off", runningTooltip.getFirst());
    }

    @Test
    void serverRackScreenBuildsControlPayloadForMenu() throws ReflectiveOperationException {
        final ServerRackMenu menu = allocateMenu(11);

        final ServerRackControlPayload payload = ServerRackScreen.controlPayload(menu, RackControlPayload.TOGGLE);

        assertEquals(11, payload.containerId());
        assertEquals(RackControlPayload.TOGGLE, payload.action());
    }

    @Test
    void serverRackScreenMapsMouseToStatusControl() {
        assertEquals(true, ServerRackScreen.statusControlAt(152, 62, 0, 0));
        assertEquals(false, ServerRackScreen.statusControlAt(151, 62, 0, 0));
        assertEquals(false, ServerRackScreen.statusControlAt(164, 62, 0, 0));
        assertEquals(false, ServerRackScreen.statusControlAt(152, 74, 0, 0));
    }

    private static void assertTranslationKey(final String expected, final Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        assertEquals(expected, ((TranslatableContents) component.getContents()).getKey());
    }

    private static ServerRackMenu allocateMenu(final int containerId) throws ReflectiveOperationException {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final ServerRackMenu menu = (ServerRackMenu) ((Unsafe) unsafeField.get(null)).allocateInstance(ServerRackMenu.class);
        final Field containerIdField = net.minecraft.world.inventory.AbstractContainerMenu.class.getDeclaredField("containerId");
        containerIdField.setAccessible(true);
        containerIdField.setInt(menu, containerId);
        return menu;
    }
}
