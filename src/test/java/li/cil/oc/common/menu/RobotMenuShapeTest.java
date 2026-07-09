package li.cil.oc.common.menu;

import li.cil.oc.api.driver.item.Slot;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RobotMenuShapeTest {
    private static final String EEPROM = "eeprom";

    @Test
    void robotMenuUsesMutableSlotCountsLikeUpstream() {
        assertEquals(20, RobotMenu.robotSlotCountForTier(0));
        assertEquals(20, RobotMenu.robotSlotCountForTier(1));
        assertEquals(20, RobotMenu.robotSlotCountForTier(2));
        assertEquals(20, RobotMenu.robotSlotCountForTier(3));
    }

    @Test
    void robotMenuDoesNotExposeAssembledComponentSlots() {
        assertEquals("tool", RobotMenu.robotSlotKind(0, 0));
        assertEquals(Slot.Any, RobotMenu.robotSlotKind(0, 4));
        assertEquals(Slot.Any, RobotMenu.robotSlotKind(2, 19));
        assertEquals(Slot.None, RobotMenu.robotSlotKind(2, 20));
        assertEquals(Slot.None, RobotMenu.robotSlotKind(2, 21));
        assertEquals(Integer.MAX_VALUE, RobotMenu.robotSlotTierLimit(2, 4));
    }

    @Test
    void robotMenuLaysOutSlotsOnUpstreamRobotGui() {
        assertEquals(170, RobotMenu.robotSlotX(0, 0));
        assertEquals(232, RobotMenu.robotSlotY(0, 0));
        assertEquals(224, RobotMenu.robotSlotX(2, 3));
        assertEquals(232, RobotMenu.robotSlotY(2, 3));
        assertEquals(170, RobotMenu.robotSlotX(2, 4));
        assertEquals(156, RobotMenu.robotSlotY(2, 4));
        assertEquals(224, RobotMenu.robotSlotX(2, 19));
        assertEquals(210, RobotMenu.robotSlotY(2, 19));
        assertEquals(-1, RobotMenu.robotSlotX(2, 20));
        assertEquals(-1, RobotMenu.robotSlotY(2, 20));
    }

    @Test
    void robotMenuUsesComputerStateProtocol() {
        assertEquals(ComputerCaseMenu.STATE_EMPTY, RobotMenu.STATE_EMPTY);
        assertEquals(ComputerCaseMenu.STATE_READY, RobotMenu.STATE_READY);
        assertEquals(ComputerCaseMenu.STATE_RUNNING, RobotMenu.STATE_RUNNING);
        assertEquals(ComputerCaseMenu.STATE_INCOMPLETE, RobotMenu.STATE_INCOMPLETE);
        assertEquals(8, RobotMenu.ROBOT_DATA_COUNT);
        assertEquals(5, RobotMenu.ROBOT_ENERGY_INDEX);
        assertEquals(6, RobotMenu.ROBOT_MAX_ENERGY_INDEX);
        assertEquals(7, RobotMenu.ROBOT_HAS_SCREEN_INDEX);
    }

    @Test
    void robotShiftClickDoesNotInstallAssembledComputerComponents() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/menu/RobotMenu.java"));

        assertTrue(source.contains("movePlayerStackToRobot(stack)"),
            "Player shift-click must use robot-aware routing instead of vanilla first-slot transfer.");
        assertTrue(source.contains("RobotBlockEntity.isRuntimeMutableSlot(slot)"),
            "Menu must route only into tool/cargo/runtime mutable slots.");
        assertTrue(source.contains("RobotBlockEntity.mutableSlotAcceptsStack(menuTier, slot, stack)"),
            "Menu must delegate mutable slot validation to robot runtime rules.");
        assertTrue(!source.contains("Slot.Container.equals(RobotBlockEntity.slotType(menuTier, slot))"),
            "Menu must not expose assembler hardware slots for direct component insertion.");
    }

    @Test
    void robotMenuReportsInstalledGpuAsScreenCapable() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/menu/RobotMenu.java"));

        assertTrue(source.contains("hasScreenFor"));
        assertTrue(source.contains("robot.hasScreenHardware()"));
        assertTrue(source.contains("case ROBOT_HAS_SCREEN_INDEX -> hasScreenFor(robotInventory) ? 1 : 0"));
    }
}
