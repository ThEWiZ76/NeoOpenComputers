package li.cil.oc.common.blockentity;

import li.cil.oc.api.driver.item.Slot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComputerCaseBootValidationTest {
    @Test
    void tierOneCaseCanBootWithCpuAndTwoMemorySticks() {
        assertTrue(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.Memory, Slot.Memory));
    }

    @Test
    void tierOneCaseCannotBootWithoutCpu() {
        assertFalse(ComputerCaseBlockEntity.hasRequiredComponents(Slot.None, Slot.Memory, Slot.Memory));
    }

    @Test
    void tierOneCaseCannotBootWithoutBothMemorySticks() {
        assertFalse(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.Memory, Slot.None));
        assertFalse(ComputerCaseBlockEntity.hasRequiredComponents(Slot.CPU, Slot.None, Slot.Memory));
    }
}
