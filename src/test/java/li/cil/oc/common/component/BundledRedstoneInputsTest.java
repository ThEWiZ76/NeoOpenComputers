package li.cil.oc.common.component;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class BundledRedstoneInputsTest {
    @Test
    void restoredChangesMarkHostDirtyOnceAndSnapshotsStayIndependent() {
        final var cache = new BundledRedstoneInputs();
        final var host = new Host();
        final var values = new int[96];
        values[31] = 15;
        values[80] = 255;
        final var data = new CompoundTag();
        data.putIntArray("oc:bundledRedstoneInputs", values);
        cache.load(data);
        final var snapshot = new CompoundTag();
        cache.save(snapshot);
        final var changes = new ArrayList<BundledRedstoneInputs.Change>();
        cache.update(host, changes::add);
        assertEquals(List.of(new BundledRedstoneInputs.Change(Direction.UP, 15, 15, 0),
            new BundledRedstoneInputs.Change(Direction.EAST, 0, 255, 0)), changes);
        assertEquals(1, host.dirty);
        assertEquals(15, snapshot.getIntArray("oc:bundledRedstoneInputs")[31]);
        assertEquals(255, snapshot.getIntArray("oc:bundledRedstoneInputs")[80]);
        cache.update(host, changes::add);
        assertEquals(2, changes.size());
        assertEquals(1, host.dirty);

        cache.load(new CompoundTag());
        final var unknown = new CompoundTag();
        cache.save(unknown);
        assertTrue(Arrays.stream(unknown.getIntArray("oc:bundledRedstoneInputs")).allMatch(value -> value == -1));
        changes.clear();
        cache.update(host, changes::add);
        assertTrue(changes.isEmpty(), "First sample of an older save is not a signal transition");
        assertEquals(2, host.dirty, "First sampling still changes persistent state");
    }

    @Test
    void malformedShortArraysAreClampedAndCopiedWithoutRetainingOldCells() {
        final var cache = new BundledRedstoneInputs();
        final var values = new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE, 42, -1};
        final var data = new CompoundTag();
        data.putIntArray("oc:bundledRedstoneInputs", values);
        cache.load(data);
        values[2] = 0;
        assertEquals(0, cache.get(Direction.DOWN, 0));
        assertEquals(255, cache.get(Direction.DOWN, 1));
        assertEquals(42, cache.get(Direction.DOWN, 2));
        assertEquals(0, cache.get(Direction.EAST, 15));
        final var saved = new CompoundTag();
        cache.save(saved);
        assertEquals(96, saved.getIntArray("oc:bundledRedstoneInputs").length);
        assertEquals(-1, saved.getIntArray("oc:bundledRedstoneInputs")[95]);
        saved.getIntArray("oc:bundledRedstoneInputs")[2] = 0;
        assertEquals(42, cache.get(Direction.DOWN, 2));
    }

    private static final class Host implements RedstoneControllerHost {
        private int dirty;
        public Level world() { return null; }
        public double xPosition() { return 0; }
        public double yPosition() { return 0; }
        public double zPosition() { return 0; }
        public void markChanged() { dirty++; }
        public int redstoneOutput(Direction side) { return 0; }
        public int redstoneInput(Direction side) { return 0; }
        public void setRedstoneOutput(Direction side, int value) {}
        public Direction toGlobal(Direction side) { return side; }
    }
}
