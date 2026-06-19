package li.cil.oc.common.blockentity;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ComputerCaseSideMappingTest {
    @Test
    void northFacingUsesGlobalDirectionsDirectly() {
        assertEquals(Direction.NORTH, ComputerCaseBlockEntity.toGlobal(Direction.NORTH, Direction.NORTH));
        assertEquals(Direction.EAST, ComputerCaseBlockEntity.toGlobal(Direction.NORTH, Direction.EAST));
        assertEquals(Direction.SOUTH, ComputerCaseBlockEntity.toGlobal(Direction.NORTH, Direction.SOUTH));
        assertEquals(Direction.WEST, ComputerCaseBlockEntity.toGlobal(Direction.NORTH, Direction.WEST));
        assertEquals(Direction.UP, ComputerCaseBlockEntity.toGlobal(Direction.NORTH, Direction.UP));
    }

    @Test
    void eastFacingRotatesHorizontalSidesClockwise() {
        assertEquals(Direction.EAST, ComputerCaseBlockEntity.toGlobal(Direction.EAST, Direction.NORTH));
        assertEquals(Direction.SOUTH, ComputerCaseBlockEntity.toGlobal(Direction.EAST, Direction.EAST));
        assertEquals(Direction.WEST, ComputerCaseBlockEntity.toGlobal(Direction.EAST, Direction.SOUTH));
        assertEquals(Direction.NORTH, ComputerCaseBlockEntity.toGlobal(Direction.EAST, Direction.WEST));
        assertEquals(Direction.DOWN, ComputerCaseBlockEntity.toGlobal(Direction.EAST, Direction.DOWN));
    }

    @Test
    void localMappingIsInverseOfGlobalMapping() {
        for (final Direction facing : Direction.Plane.HORIZONTAL) {
            for (final Direction side : Direction.values()) {
                final Direction global = ComputerCaseBlockEntity.toGlobal(facing, side);

                assertEquals(side, ComputerCaseBlockEntity.toLocal(facing, global));
            }
        }
    }
}
