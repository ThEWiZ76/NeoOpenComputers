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
        final TerminalScreenSnapshot snapshot = new TerminalScreenSnapshot(4, 2, new String[]{"neo", "oc"});
        final CompoundTag tag = new CompoundTag();

        snapshot.save(tag);
        final TerminalScreenSnapshot loaded = TerminalScreenSnapshot.load(tag);

        assertEquals(4, loaded.width());
        assertEquals(2, loaded.height());
        assertEquals("neo", loaded.line(0));
        assertEquals("oc", loaded.line(1));
    }

    @Test
    void comparesSnapshotContents() {
        final TerminalScreenSnapshot first = new TerminalScreenSnapshot(4, 2, new String[]{"neo", "oc"});
        final TerminalScreenSnapshot same = new TerminalScreenSnapshot(4, 2, new String[]{"neo", "oc"});
        final TerminalScreenSnapshot different = new TerminalScreenSnapshot(4, 2, new String[]{"neo", "open"});

        assertEquals(true, first.contentEquals(same));
        assertEquals(false, first.contentEquals(different));
    }
}
