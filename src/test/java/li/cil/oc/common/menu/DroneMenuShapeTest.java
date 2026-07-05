package li.cil.oc.common.menu;

import li.cil.oc.common.ModContentIds;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DroneMenuShapeTest {
    @Test
    void droneMenuIdIsStableAndRegistered() throws Exception {
        final String menus = Files.readString(Path.of("src/main/java/li/cil/oc/common/ModMenus.java"));

        assertEquals("drone", ModContentIds.DRONE);
        assertEquals("drone_menu", ModContentIds.DRONE_MENU);
        assertTrue(menus.contains("MenuType<DroneMenu>"));
        assertTrue(menus.contains("ModContentIds.DRONE_MENU"));
    }

    @Test
    void droneMenuExposesSlotAndStateHelpers() throws Exception {
        final String menu = Files.readString(Path.of("src/main/java/li/cil/oc/common/menu/DroneMenu.java"));

        assertTrue(menu.contains("public static int droneSlotCountForTier(final int tier)"));
        assertTrue(menu.contains("public static String droneSlotKind(final int tier, final int slot)"));
        assertTrue(menu.contains("public static int droneSlotTierLimit(final int tier, final int slot)"));
        assertTrue(menu.contains("public static int droneStateFor(final Container droneInventory)"));
        assertTrue(menu.contains("public static int missingRequirementsFor(final Container droneInventory)"));
    }
}
