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

    @Test
    void appliesFullSnapshotWhenTerminalDimensionsChange() {
        final TerminalScreenSnapshot previous = new TerminalScreenSnapshot(2, 1, new String[]{"oc"});
        final TerminalScreenSnapshot current = new TerminalScreenSnapshot(3, 2, new String[]{"neo", "oc "});

        final TerminalScreenDelta delta = TerminalScreenDelta.between(previous, current);
        final TerminalScreenSnapshot applied = delta.applyTo(previous);

        assertEquals(2, delta.rowCount());
        assertEquals(3, applied.width());
        assertEquals(2, applied.height());
        assertEquals(true, current.contentEquals(applied));
    }

    @Test
    void appliesDefaultColorsWhenBaseSnapshotIsMissing() {
        final TerminalScreenDelta delta = new TerminalScreenDelta(2, 1, new TerminalScreenDelta.Row[0]);

        final TerminalScreenSnapshot applied = delta.applyTo(null);

        assertEquals("  ", applied.line(0));
        assertEquals(0xFFFFFF, applied.foregroundColor(0, 0));
        assertEquals(0xFFFFFF, applied.foregroundColor(1, 0));
        assertEquals(0x000000, applied.backgroundColor(0, 0));
        assertEquals(0x000000, applied.backgroundColor(1, 0));
    }

    @Test
    void appliesMalformedRowsAsFixedWidthTerminalRows() {
        final TerminalScreenSnapshot previous = new TerminalScreenSnapshot(4, 1, new String[]{"base"});
        final TerminalScreenDelta delta = new TerminalScreenDelta(
            4,
            1,
            new TerminalScreenDelta.Row[]{
                new TerminalScreenDelta.Row(0, "xy", new int[]{0x112233}, new int[]{0x445566})
            });

        final TerminalScreenSnapshot applied = delta.applyTo(previous);

        assertEquals("xy  ", applied.line(0));
        assertEquals(0x112233, applied.foregroundColor(0, 0));
        assertEquals(0xFFFFFF, applied.foregroundColor(1, 0));
        assertEquals(0xFFFFFF, applied.foregroundColor(3, 0));
        assertEquals(0x445566, applied.backgroundColor(0, 0));
        assertEquals(0x000000, applied.backgroundColor(1, 0));
        assertEquals(0x000000, applied.backgroundColor(3, 0));
    }

    @Test
    void appliesRowsWithoutSplittingSupplementaryCodePoints() {
        final String glyph = new String(Character.toChars(0x10400));
        final TerminalScreenDelta delta = new TerminalScreenDelta(
            1,
            1,
            new TerminalScreenDelta.Row[]{
                new TerminalScreenDelta.Row(0, glyph, new int[]{0x112233}, new int[]{0x445566})
            });

        final TerminalScreenSnapshot applied = delta.applyTo(null);

        assertEquals(glyph, applied.line(0));
    }
}
