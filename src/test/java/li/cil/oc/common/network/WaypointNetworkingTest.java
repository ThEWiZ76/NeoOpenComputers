package li.cil.oc.common.network;

import li.cil.oc.common.blockentity.WaypointBlockEntity;
import li.cil.oc.common.menu.WaypointMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WaypointNetworkingTest {
    @Test
    void waypointLabelPayloadTruncatesToUpstreamLimit() {
        final WaypointLabelPayload payload = new WaypointLabelPayload(7, "abcdefghijklmnopqrstuvwxyz0123456789");

        assertEquals("abcdefghijklmnopqrstuvwxyz012345", payload.label());
    }

    @Test
    void applyWaypointLabelUpdatesOpenWaypointMenu() throws Exception {
        final WaypointBlockEntity waypoint = allocateWaypoint();
        final WaypointMenu menu = allocateMenu(7, waypoint);

        assertTrue(WaypointNetworking.applyWaypointLabel(menu, new WaypointLabelPayload(7, "Sorting Input")));

        assertEquals("Sorting Input", waypoint.label());
    }

    @Test
    void applyWaypointLabelRejectsWrongContainer() throws Exception {
        final WaypointBlockEntity waypoint = allocateWaypoint();
        final WaypointMenu menu = allocateMenu(7, waypoint);

        assertFalse(WaypointNetworking.applyWaypointLabel(menu, new WaypointLabelPayload(8, "Other")));

        assertEquals("", waypoint.label());
    }

    private static WaypointBlockEntity allocateWaypoint() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final WaypointBlockEntity waypoint = (WaypointBlockEntity) ((Unsafe) unsafeField.get(null)).allocateInstance(WaypointBlockEntity.class);
        final Field labelField = WaypointBlockEntity.class.getDeclaredField("label");
        labelField.setAccessible(true);
        labelField.set(waypoint, "");
        return waypoint;
    }

    private static WaypointMenu allocateMenu(final int containerId, final WaypointBlockEntity waypoint) throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final WaypointMenu menu = (WaypointMenu) ((Unsafe) unsafeField.get(null)).allocateInstance(WaypointMenu.class);
        setField(AbstractContainerMenu.class, menu, "containerId", containerId);
        setField(WaypointMenu.class, menu, "waypoint", waypoint);
        setField(WaypointMenu.class, menu, "pos", BlockPos.ZERO);
        setField(WaypointMenu.class, menu, "label", "");
        return menu;
    }

    private static void setField(final Class<?> type, final Object target, final String name, final Object value) throws Exception {
        final Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
