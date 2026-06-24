package li.cil.oc.common.component;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TerminalScreenDeltaTest {
    @Test
    void capturesOnlyChangedRowsAndAppliesToSnapshot() {
        final TerminalScreenSnapshot previous = new TerminalScreenSnapshot(
            4,
            3,
            new String[]{"aaaa", "bbbb", "cccc"},
            new int[][]{{1, 1, 1, 1}, {2, 2, 2, 2}, {3, 3, 3, 3}},
            new int[][]{{4, 4, 4, 4}, {5, 5, 5, 5}, {6, 6, 6, 6}});
        final TerminalScreenSnapshot current = new TerminalScreenSnapshot(
            4,
            3,
            new String[]{"aaaa", "neo ", "cccc"},
            new int[][]{{1, 1, 1, 1}, {7, 7, 7, 7}, {3, 3, 3, 3}},
            new int[][]{{4, 4, 4, 4}, {8, 8, 8, 8}, {6, 6, 6, 6}});

        final TerminalScreenDelta delta = TerminalScreenDelta.between(previous, current);
        final TerminalScreenSnapshot applied = delta.applyTo(previous);

        assertEquals(1, delta.rowCount());
        assertEquals(1, delta.rowIndex(0));
        assertEquals(true, current.contentEquals(applied));
    }

    @Test
    void roundTripsThroughNbt() {
        final TerminalScreenSnapshot previous = new TerminalScreenSnapshot(3, 2, new String[]{"old", "row"});
        final TerminalScreenSnapshot current = new TerminalScreenSnapshot(3, 2, new String[]{"new", "row"});
        final TerminalScreenDelta delta = TerminalScreenDelta.between(previous, current);
        final CompoundTag tag = new CompoundTag();

        delta.save(tag);
        final TerminalScreenDelta loaded = TerminalScreenDelta.load(tag);

        assertEquals(1, loaded.rowCount());
        assertEquals(true, current.contentEquals(loaded.applyTo(previous)));
    }
}
