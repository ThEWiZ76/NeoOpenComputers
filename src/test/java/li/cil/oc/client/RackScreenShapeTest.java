package li.cil.oc.client;

import li.cil.oc.common.menu.RackMenu;
import li.cil.oc.common.network.RackControlPayload;
import li.cil.oc.common.network.RackOpenServerPayload;
import net.minecraft.core.Direction;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RackScreenShapeTest {
    @Test
    void rackScreenHasMenuConstructor() throws NoSuchMethodException {
        final Constructor<RackScreen> constructor = RackScreen.class.getConstructor(
            RackMenu.class,
            Inventory.class,
            Component.class);

        assertTrue(AbstractContainerScreen.class.isAssignableFrom(RackScreen.class));
        assertArrayEquals(new Class<?>[]{RackMenu.class, Inventory.class, Component.class}, constructor.getParameterTypes());
    }

    @Test
    void rackScreenBuildsControlPayloadForMenu() throws ReflectiveOperationException {
        final RackMenu menu = allocateMenu(14);

        final RackControlPayload payload = RackScreen.controlPayload(menu, 2, RackControlPayload.TOGGLE);

        assertEquals(14, payload.containerId());
        assertEquals(2, payload.slot());
        assertEquals(RackControlPayload.TOGGLE, payload.action());
    }

    @Test
    void rackScreenBuildsMappingPayloadForMenu() throws ReflectiveOperationException {
        final RackMenu menu = allocateMenu(14);

        final RackControlPayload payload = RackScreen.mappingPayload(menu, 2, 0, Direction.SOUTH);
        final RackControlPayload cleared = RackScreen.mappingPayload(menu, 2, -1, null);

        assertEquals(14, payload.containerId());
        assertEquals(2, payload.slot());
        assertEquals(RackControlPayload.MAP, payload.action());
        assertEquals(0, payload.connectableIndex());
        assertEquals(Direction.SOUTH.ordinal(), payload.side());
        assertEquals(RackControlPayload.NO_SIDE, cleared.side());
    }

    @Test
    void rackScreenMapsBusGridCoordinatesLikeUpstreamButtons() {
        final RackScreen.MappingControl first = RackScreen.mappingControlAt(53, 64, 0, 0);
        final RackScreen.MappingControl last = RackScreen.mappingControlAt(53 + 3 * 18 + 4 * 3 + 2, 64 + 3 * 3 + 2, 0, 0);

        assertEquals(new RackScreen.MappingControl(0, 0, 0), first);
        assertEquals(new RackScreen.MappingControl(3, 3, 4), last);
        assertNull(RackScreen.mappingControlAt(52, 64, 0, 0));
    }

    @Test
    void rackScreenBuildsMappingPayloadForClickedBusAndClearsSelectedBus() throws ReflectiveOperationException {
        final RackMenu menu = allocateMenu(14, rackDataWithMappingAndPresence(1, 2, Direction.UP));
        final RackScreen.MappingControl selected = new RackScreen.MappingControl(1, 2, RackScreen.busIndex(Direction.UP));
        final RackScreen.MappingControl remapped = new RackScreen.MappingControl(1, 2, RackScreen.busIndex(Direction.SOUTH));

        final RackControlPayload cleared = RackScreen.mappingPayload(menu, selected);
        final RackControlPayload changed = RackScreen.mappingPayload(menu, remapped);

        assertEquals(14, cleared.containerId());
        assertEquals(1, cleared.slot());
        assertEquals(1, cleared.connectableIndex());
        assertEquals(RackControlPayload.NO_SIDE, cleared.side());

        assertEquals(14, changed.containerId());
        assertEquals(1, changed.slot());
        assertEquals(1, changed.connectableIndex());
        assertEquals(Direction.SOUTH.ordinal(), changed.side());
    }

    @Test
    void rackScreenMapsBusesRelativeToRackFacingLikeUpstream() throws ReflectiveOperationException {
        final RackMenu menu = allocateMenu(14, rackDataWithFacingMappingAndPresence(Direction.SOUTH, 1, 2, Direction.NORTH));
        final RackScreen.MappingControl selected = new RackScreen.MappingControl(1, 2, RackScreen.busIndex(Direction.SOUTH, Direction.NORTH));
        final RackScreen.MappingControl remapped = new RackScreen.MappingControl(1, 2, RackScreen.busIndex(Direction.SOUTH, Direction.WEST));

        final RackControlPayload cleared = RackScreen.mappingPayload(menu, selected);
        final RackControlPayload changed = RackScreen.mappingPayload(menu, remapped);

        assertEquals(Direction.NORTH, RackScreen.busSide(Direction.SOUTH, RackScreen.busIndex(Direction.SOUTH, Direction.NORTH)));
        assertEquals(-1, RackScreen.busIndex(Direction.SOUTH, Direction.SOUTH));
        assertEquals(RackControlPayload.NO_SIDE, cleared.side());
        assertEquals(Direction.WEST.ordinal(), changed.side());
    }

    @Test
    void rackScreenDoesNotBuildMappingPayloadForAbsentConnectable() throws ReflectiveOperationException {
        final RackMenu menu = allocateMenu(14, rackDataWithMappingAndPresence(1, 2, Direction.UP));

        assertNull(RackScreen.mappingPayload(menu, new RackScreen.MappingControl(1, 3, RackScreen.busIndex(Direction.SOUTH))));
    }

    @Test
    void rackScreenBuildsOpenServerPayloadForMenu() throws ReflectiveOperationException {
        final RackMenu menu = allocateMenu(14);

        final RackOpenServerPayload payload = RackScreen.openServerPayload(menu, 3);

        assertEquals(14, payload.containerId());
        assertEquals(3, payload.slot());
    }

    @Test
    void rackScreenUsesRackStateForControlColor() {
        assertEquals(0xFF4C566A, RackScreen.controlColor(RackMenu.STATE_EMPTY));
        assertEquals(0xFFA3BE8C, RackScreen.controlColor(RackMenu.STATE_READY));
        assertEquals(0xFF88C0D0, RackScreen.controlColor(RackMenu.STATE_RUNNING));
        assertEquals(0xFFD08770, RackScreen.controlColor(RackMenu.STATE_INCOMPLETE));
    }

    @Test
    void rackScreenExposesStateTooltipKeys() {
        assertTranslationKey("gui.neoopencomputers.rack.state.empty", RackScreen.stateLabel(RackMenu.STATE_EMPTY));
        assertTranslationKey("gui.neoopencomputers.rack.state.ready", RackScreen.stateLabel(RackMenu.STATE_READY));
        assertTranslationKey("gui.neoopencomputers.rack.state.running", RackScreen.stateLabel(RackMenu.STATE_RUNNING));
        assertTranslationKey("gui.neoopencomputers.rack.state.incomplete", RackScreen.stateLabel(RackMenu.STATE_INCOMPLETE));

        final List<Component> tooltip = RackScreen.controlTooltip(RackMenu.STATE_READY);

        assertEquals(2, tooltip.size());
        assertTranslationKey("gui.neoopencomputers.rack.control", tooltip.get(0));
        assertTranslationKey("gui.neoopencomputers.rack.state.ready", tooltip.get(1));

        final List<Component> missingTooltip = RackScreen.controlTooltip(
            RackMenu.STATE_INCOMPLETE,
            RackMenu.MISSING_CPU | RackMenu.MISSING_MEMORY | RackMenu.MISSING_EEPROM);

        assertEquals(5, missingTooltip.size());
        assertTranslationKey("gui.neoopencomputers.rack.missing.cpu", missingTooltip.get(2));
        assertTranslationKey("gui.neoopencomputers.rack.missing.memory", missingTooltip.get(3));
        assertTranslationKey("gui.neoopencomputers.rack.missing.eeprom", missingTooltip.get(4));
    }

    @Test
    void rackScreenExposesMappingTooltipKeys() throws ReflectiveOperationException {
        final RackMenu menu = allocateMenu(14, rackDataWithFacingMappingAndPresence(Direction.SOUTH, 1, 2, Direction.NORTH));
        final RackScreen.MappingControl selected = new RackScreen.MappingControl(1, 2, RackScreen.busIndex(Direction.SOUTH, Direction.NORTH));
        final RackScreen.MappingControl unselected = new RackScreen.MappingControl(1, 2, RackScreen.busIndex(Direction.SOUTH, Direction.WEST));

        final List<Component> selectedTooltip = RackScreen.mappingTooltip(menu, selected);
        final List<Component> unselectedTooltip = RackScreen.mappingTooltip(menu, unselected);

        assertEquals(3, selectedTooltip.size());
        assertTranslationKey("gui.neoopencomputers.rack.bus", selectedTooltip.get(0));
        assertTranslationKey("gui.neoopencomputers.rack.bus.side.north", selectedTooltip.get(1));
        assertTranslationKey("gui.neoopencomputers.rack.bus.clear", selectedTooltip.get(2));

        assertEquals(3, unselectedTooltip.size());
        assertTranslationKey("gui.neoopencomputers.rack.bus", unselectedTooltip.get(0));
        assertTranslationKey("gui.neoopencomputers.rack.bus.side.west", unselectedTooltip.get(1));
        assertTranslationKey("gui.neoopencomputers.rack.bus.map", unselectedTooltip.get(2));

        assertTrue(RackScreen.mappingTooltip(menu, new RackScreen.MappingControl(1, 3, RackScreen.busIndex(Direction.SOUTH, Direction.WEST))).isEmpty());
    }

    private static RackMenu allocateMenu(final int containerId) throws ReflectiveOperationException {
        return allocateMenu(containerId, null);
    }

    private static RackMenu allocateMenu(final int containerId, final ContainerData rackData) throws ReflectiveOperationException {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        final RackMenu menu = (RackMenu) ((Unsafe) unsafeField.get(null)).allocateInstance(RackMenu.class);
        final Field containerIdField = net.minecraft.world.inventory.AbstractContainerMenu.class.getDeclaredField("containerId");
        containerIdField.setAccessible(true);
        containerIdField.setInt(menu, containerId);
        if (rackData != null) {
            final Field rackDataField = RackMenu.class.getDeclaredField("rackData");
            rackDataField.setAccessible(true);
            rackDataField.set(menu, rackData);
        }
        return menu;
    }

    private static ContainerData rackDataWithMappingAndPresence(final int slot, final int connectable, final Direction mappedSide) {
        return rackDataWithFacingMappingAndPresence(Direction.NORTH, slot, connectable, mappedSide);
    }

    private static ContainerData rackDataWithFacingMappingAndPresence(final Direction facing, final int slot, final int connectable, final Direction mappedSide) {
        return new ContainerData() {
            @Override
            public int get(final int index) {
                final int mappingIndex = RackMenu.RACK_NODE_MAPPING_OFFSET + slot * 4 + connectable;
                final int presenceIndex = RackMenu.RACK_NODE_PRESENCE_OFFSET + slot * 4 + connectable;
                if (index == RackMenu.RACK_FACING_OFFSET) {
                    return facing.ordinal() + 1;
                }
                if (index == mappingIndex) {
                    return mappedSide.ordinal();
                }
                if (index == presenceIndex) {
                    return 1;
                }
                return 0;
            }

            @Override
            public void set(final int index, final int value) {
            }

            @Override
            public int getCount() {
                return RackMenu.RACK_DATA_COUNT;
            }
        };
    }

    private static void assertTranslationKey(final String expected, final Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        assertEquals(expected, ((TranslatableContents) component.getContents()).getKey());
    }
}
