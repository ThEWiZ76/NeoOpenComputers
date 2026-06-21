package li.cil.oc.client;

import li.cil.oc.common.menu.RaidMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RaidScreenShapeTest {
    @Test
    void raidScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<RaidScreen> constructor = RaidScreen.class.getConstructor(
            RaidMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(RaidScreen.class));
        assertArrayEquals(new Class<?>[]{RaidMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }

    @Test
    void raidScreenExposesStatusLabels() {
        assertTranslationKey("gui.neoopencomputers.raid.state.empty", RaidScreen.statusLabel(RaidMenu.STATE_EMPTY));
        assertTranslationKey("gui.neoopencomputers.raid.state.incomplete", RaidScreen.statusLabel(RaidMenu.STATE_INCOMPLETE));
        assertTranslationKey("gui.neoopencomputers.raid.state.ready", RaidScreen.statusLabel(RaidMenu.STATE_READY));
    }

    @Test
    void raidScreenStatusTooltipIncludesCapacity() {
        final List<Component> tooltip = RaidScreen.statusTooltip(RaidMenu.STATE_READY, 7 * 1024 * 1024);

        assertTranslationKey("gui.neoopencomputers.raid.status", tooltip.getFirst());
        assertTranslationKey("gui.neoopencomputers.raid.state.ready", tooltip.get(1));
        assertTranslationKey("gui.neoopencomputers.raid.capacity", tooltip.get(2));
    }

    private static void assertTranslationKey(final String expectedKey, final Component component) {
        assertTrue(component instanceof net.minecraft.network.chat.MutableComponent);
        final net.minecraft.network.chat.MutableComponent mutable = (net.minecraft.network.chat.MutableComponent) component;
        assertEquals(expectedKey, mutable.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents contents ? contents.getKey() : "");
    }
}
