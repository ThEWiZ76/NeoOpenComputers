package li.cil.oc.common.integration.ae2;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

final class CraftingJournalTest {
    @Test
    void savedWorldOutcomeOverridesOlderHeapStateWithoutRetainingTheWholeHeap() {
        final var journal = new CraftingJournal();
        final var id = UUID.fromString("73100000-0000-0000-0000-000000000003");
        final var active = new CompoundTag(); active.putUUID("id", id);
        active.putString("target", "saved-controller"); active.putLong("amount", 8);
        journal.record(active);
        active.putLong("amount", 999);
        assertEquals(8, journal.lookup(id).getLong("amount"));
        assertEquals(1, journal.active().size());
        final var done = journal.lookup(id); done.putBoolean("done", true);
        journal.record(done);
        final var disk = journal.save(new CompoundTag(), null);
        final var fresh = CraftingJournal.load(disk, null);
        assertTrue(fresh.active().isEmpty());
        final var outcome = fresh.lookup(id);
        assertTrue(outcome.getBoolean("done"));
        assertFalse(outcome.contains("target"));
        assertFalse(outcome.contains("amount"));
        outcome.putBoolean("done", false);
        assertTrue(fresh.lookup(id).getBoolean("done"));
    }
}
