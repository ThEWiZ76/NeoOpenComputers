package li.cil.oc.client;

import li.cil.oc.common.menu.ServerRackMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        final Method slotAt = ServerRackScreen.class.getMethod("serverSlotAt", int.class, int.class, int.class, int.class);

        assertEquals(Component.class, label.getReturnType());
        assertEquals(Component.class, tierLabel.getReturnType());
        assertEquals(List.class, tooltip.getReturnType());
        assertEquals(int.class, slotAt.getReturnType());
    }

    @Test
    void serverRackScreenMapsMouseToServerSlots() {
        assertEquals(0, ServerRackScreen.serverSlotAt(8, 18, 0, 0));
        assertEquals(8, ServerRackScreen.serverSlotAt(152, 18, 0, 0));
        assertEquals(9, ServerRackScreen.serverSlotAt(8, 36, 0, 0));
        assertEquals(-1, ServerRackScreen.serverSlotAt(170, 18, 0, 0));
        assertEquals(-1, ServerRackScreen.serverSlotAt(8, 58, 0, 0));
    }

    @Test
    void serverRackSlotTooltipContainsSlotLabel() {
        final List<Component> tooltip = ServerRackScreen.slotTooltip(ServerRackMenu.SLOT_KIND_CPU, 2);

        assertEquals(ServerRackScreen.slotLabel(ServerRackMenu.SLOT_KIND_CPU), tooltip.getFirst());
        assertEquals(ServerRackScreen.slotTierLabel(2), tooltip.get(1));
    }

    @Test
    void serverRackSlotTierLabelsDistinguishAnyAndUnused() {
        assertEquals("gui.neoopencomputers.server_rack.slot.max_tier", ServerRackScreen.slotTierLabel(2).getString());
        assertEquals("gui.neoopencomputers.server_rack.slot.any_tier", ServerRackScreen.slotTierLabel(Integer.MAX_VALUE).getString());
        assertEquals("gui.neoopencomputers.server_rack.slot.unavailable", ServerRackScreen.slotTierLabel(-1).getString());
    }
}
