package li.cil.oc.common.component;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TerminalScreenSnapshotTest {
    @Test
    void storesRowsByViewportSize() {
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(4, 2, new String[]{"neo", "oc"});

        assertEquals(4, snapshot.width());
        assertEquals(2, snapshot.height());
        assertEquals("neo", snapshot.line(0));
        assertEquals("oc", snapshot.line(1));
        assertEquals("", snapshot.line(2));
    }

    @Test
    void roundTripsThroughNbt() {
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(
            4,
            2,
            new String[]{"neo", "oc"},
            new int[][]{{0x112233, 0x223344, 0x334455, 0x445566}, {0x556677, 0x667788, 0x778899, 0x8899AA}},
            new int[][]{{0x010203, 0x020304, 0x030405, 0x040506}, {0x050607, 0x060708, 0x070809, 0x08090A}});
        final CompoundTag tag = new CompoundTag();

        snapshot.save(tag);
        final TerminalScreenSnapshot loaded = TerminalScreenSnapshot.load(tag);

        assertEquals(4, loaded.width());
        assertEquals(2, loaded.height());
        assertEquals("neo", loaded.line(0));
        assertEquals("oc", loaded.line(1));
        assertEquals(0x334455, loaded.foregroundColor(2, 0));
        assertEquals(0x070809, loaded.backgroundColor(2, 1));
    }

    @Test
    void roundTripsSparseRowsAsBlankRows() {
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(3, 2, new String[]{"neo"});
        final CompoundTag tag = new CompoundTag();

        snapshot.save(tag);
        final TerminalScreenSnapshot loaded = TerminalScreenSnapshot.load(tag);

        assertEquals("neo", loaded.line(0));
        assertEquals("", loaded.line(1));
        assertEquals(true, snapshot.contentEquals(loaded));
    }

    @Test
    void loadsMissingSavedRowsAsBlankRows() {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("width", 3);
        tag.putInt("height", 2);

        final TerminalScreenSnapshot loaded = TerminalScreenSnapshot.load(tag);
        final TerminalScreenSnapshot blank = new TerminalScreenSnapshot(3, 2, new String[]{"", ""});

        assertEquals("", loaded.line(0));
        assertEquals("", loaded.line(1));
        assertEquals(true, blank.contentEquals(loaded));
    }

    @Test
    void comparesSnapshotContents() {
        final TerminalScreenSnapshot first = new TerminalScreenSnapshot(4, 2, new String[]{"neo", "oc"});
        final TerminalScreenSnapshot same = new TerminalScreenSnapshot(4, 2, new String[]{"neo", "oc"});
        final TerminalScreenSnapshot different = new TerminalScreenSnapshot(4, 2, new String[]{"neo", "open"});

        assertEquals(true, first.contentEquals(same));
        assertEquals(false, first.contentEquals(different));
    }

    @Test
    void comparesSnapshotColors() {
        final TerminalScreenSnapshot first = new TerminalScreenSnapshot(
            2,
            1,
            new String[]{"ab"},
            new int[][]{{0x111111, 0x222222}},
            new int[][]{{0x000000, 0x333333}});
        final TerminalScreenSnapshot same = new TerminalScreenSnapshot(
            2,
            1,
            new String[]{"ab"},
            new int[][]{{0x111111, 0x222222}},
            new int[][]{{0x000000, 0x333333}});
        final TerminalScreenSnapshot different = new TerminalScreenSnapshot(
            2,
            1,
            new String[]{"ab"},
            new int[][]{{0x111111, 0xFFFFFF}},
            new int[][]{{0x000000, 0x333333}});

        assertEquals(true, first.contentEquals(same));
        assertEquals(false, first.contentEquals(different));
    }
}
