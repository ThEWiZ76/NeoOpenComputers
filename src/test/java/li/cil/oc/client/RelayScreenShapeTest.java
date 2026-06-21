package li.cil.oc.client;

import li.cil.oc.common.menu.RelayMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RelayScreenShapeTest {
    @Test
    void relayScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<RelayScreen> constructor = RelayScreen.class.getConstructor(
            RelayMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(RelayScreen.class));
        assertArrayEquals(new Class<?>[]{RelayMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }

    @Test
    void relayScreenExposesStatusLabels() {
        assertTranslationKey("gui.neoopencomputers.relay.mode.wired", RelayScreen.statusLabel(RelayMenu.MODE_WIRED));
        assertTranslationKey("gui.neoopencomputers.relay.mode.wireless", RelayScreen.statusLabel(RelayMenu.MODE_WIRELESS));
        assertTranslationKey("gui.neoopencomputers.relay.mode.linked", RelayScreen.statusLabel(RelayMenu.MODE_LINKED));
    }

    @Test
    void relayScreenStatusTooltipIncludesDelayAndQueue() {
        final List<Component> tooltip = RelayScreen.statusTooltip(RelayMenu.MODE_WIRELESS, 3, 2, 20);

        assertTranslationKey("gui.neoopencomputers.relay.status", tooltip.getFirst());
        assertTranslationKey("gui.neoopencomputers.relay.mode.wireless", tooltip.get(1));
        assertTranslationKey("gui.neoopencomputers.relay.delay", tooltip.get(2));
        assertTranslationKey("gui.neoopencomputers.relay.queue", tooltip.get(3));
    }

    private static void assertTranslationKey(final String expectedKey, final Component component) {
        assertTrue(component instanceof net.minecraft.network.chat.MutableComponent);
        final net.minecraft.network.chat.MutableComponent mutable = (net.minecraft.network.chat.MutableComponent) component;
        assertEquals(expectedKey, mutable.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents ? contents.getKey() : "");
    }
}
