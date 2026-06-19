package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.item.Slot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComputerCaseBootValidationTest {
    private static final String EEPROM = "eeprom";

    @Test
    void tierOneCaseCanBootWithCpuTwoMemorySticksHardDriveAndEeprom() {
        assertTrue(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.Memory, Slot.Memory, Slot.HDD, EEPROM));
    }

    @Test
    void tierOneCaseCannotBootWithoutCpu() {
        assertFalse(ComputerCaseBlockEntity.hasRequiredComponents(Slot.None, Slot.Memory, Slot.Memory, Slot.HDD, EEPROM));
    }

    @Test
    void tierOneCaseCannotBootWithoutBothMemorySticks() {
        assertFalse(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.Memory, Slot.None, Slot.HDD, EEPROM));
        assertFalse(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.None, Slot.Memory, Slot.HDD, EEPROM));
    }

    @Test
    void tierOneCaseCannotBootWithoutHardDiskDrive() {
        assertFalse(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.Memory, Slot.Memory, Slot.None, EEPROM));
    }

    @Test
    void tierOneCaseCannotBootWithoutEeprom() {
        assertFalse(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.Memory, Slot.Memory, Slot.HDD, Slot.None));
    }
}
