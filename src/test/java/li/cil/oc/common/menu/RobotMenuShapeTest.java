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
    void robotMenuUsesTieredSlotCounts() {
        assertEquals(14, RobotMenu.robotSlotCountForTier(0));
        assertEquals(17, RobotMenu.robotSlotCountForTier(1));
        assertEquals(21, RobotMenu.robotSlotCountForTier(2));
        assertEquals(21, RobotMenu.robotSlotCountForTier(3));
    }

    @Test
    void robotMenuExposesRobotSlotKindsAndTierLimits() {
        assertEquals(Slot.Container, RobotMenu.robotSlotKind(0, 0));
        assertEquals(Slot.Upgrade, RobotMenu.robotSlotKind(1, 3));
        assertEquals(Slot.Card, RobotMenu.robotSlotKind(2, 12));
        assertEquals(Slot.CPU, RobotMenu.robotSlotKind(2, 15));
        assertEquals(Slot.Memory, RobotMenu.robotSlotKind(2, 16));
        assertEquals(EEPROM, RobotMenu.robotSlotKind(2, 18));
        assertEquals(Slot.HDD, RobotMenu.robotSlotKind(2, 19));
        assertEquals(Slot.None, RobotMenu.robotSlotKind(2, 21));
        assertEquals(2, RobotMenu.robotSlotTierLimit(2, 3));
        assertEquals(Integer.MAX_VALUE, RobotMenu.robotSlotTierLimit(2, 18));
    }

    @Test
    void robotMenuLaysOutSlotsOnUpstreamRobotGui() {
        assertEquals(170, RobotMenu.robotSlotX(0, 0));
        assertEquals(232, RobotMenu.robotSlotY(0, 0));
        assertEquals(152, RobotMenu.robotSlotX(2, 20));
        assertEquals(232, RobotMenu.robotSlotY(2, 20));
        assertEquals(-1, RobotMenu.robotSlotX(2, 21));
        assertEquals(-1, RobotMenu.robotSlotY(2, 21));
    }

    @Test
    void robotMenuUsesComputerStateProtocol() {
        assertEquals(ComputerCaseMenu.STATE_EMPTY, RobotMenu.STATE_EMPTY);
        assertEquals(ComputerCaseMenu.STATE_READY, RobotMenu.STATE_READY);
        assertEquals(ComputerCaseMenu.STATE_RUNNING, RobotMenu.STATE_RUNNING);
        assertEquals(ComputerCaseMenu.STATE_INCOMPLETE, RobotMenu.STATE_INCOMPLETE);
        assertEquals(7, RobotMenu.ROBOT_DATA_COUNT);
        assertEquals(5, RobotMenu.ROBOT_ENERGY_INDEX);
        assertEquals(6, RobotMenu.ROBOT_MAX_ENERGY_INDEX);
    }

    @Test
    void robotShiftClickTargetsCompatibleComponentSlotsBeforeInventorySlots() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/menu/RobotMenu.java"));

        assertTrue(source.contains("movePlayerStackToRobot(stack)"),
            "Player shift-click must use robot-aware routing instead of vanilla first-slot transfer.");
        assertTrue(source.contains("movePlayerStackToRobotSlots(stack, menuTier, false)")
                && source.contains("movePlayerStackToRobotSlots(stack, menuTier, true)")
                && source.contains("li.cil.oc.api.driver.item.Slot.Container.equals(RobotBlockEntity.slotType(menuTier, slot))"),
            "Component stacks must be tried in matching component/upgrade slots before generic container slots.");
    }
}
