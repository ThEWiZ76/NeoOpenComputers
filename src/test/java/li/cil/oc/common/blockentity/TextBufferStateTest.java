package li.cil.oc.common.blockentity;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TextBufferStateTest {
    @Test
    void setWritesTextHorizontallyAndVertically() {
        TextBufferState buffer = new TextBufferState(4, 3);

        buffer.set(0, 0, "ab", false);
        buffer.set(3, 0, "xy", true);

        assertEquals('a', buffer.getCodePoint(0, 0));
        assertEquals('b', buffer.getCodePoint(1, 0));
        assertEquals('x', buffer.getCodePoint(3, 0));
        assertEquals('y', buffer.getCodePoint(3, 1));
    }

    @Test
    void fillWritesRectWithinBounds() {
        TextBufferState buffer = new TextBufferState(4, 3);

        buffer.fill(1, 1, 5, 5, 'z');

        assertEquals(' ', buffer.getCodePoint(0, 0));
        assertEquals('z', buffer.getCodePoint(1, 1));
        assertEquals('z', buffer.getCodePoint(3, 2));
    }

    @Test
    void copyMovesRectWithOverlap() {
        TextBufferState buffer = new TextBufferState(5, 1);
        buffer.set(0, 0, "abcd", false);

        buffer.copy(0, 0, 4, 1, 1, 0);

        assertEquals('a', buffer.getCodePoint(1, 0));
        assertEquals('b', buffer.getCodePoint(2, 0));
        assertEquals('c', buffer.getCodePoint(3, 0));
        assertEquals('d', buffer.getCodePoint(4, 0));
    }

    @Test
    void saveAndLoadPreservesText() {
        TextBufferState saved = new TextBufferState(3, 2);
        saved.set(0, 0, "abc", false);
        saved.set(0, 1, "xy", false);
        CompoundTag tag = new CompoundTag();

        saved.save(tag);

        TextBufferState loaded = new TextBufferState(1, 1);
        loaded.load(tag);

        assertEquals('a', loaded.getCodePoint(0, 0));
        assertEquals('c', loaded.getCodePoint(2, 0));
        assertEquals('x', loaded.getCodePoint(0, 1));
        assertEquals('y', loaded.getCodePoint(1, 1));
    }

    @Test
    void rawColorWritesAffectCells() {
        TextBufferState buffer = new TextBufferState(3, 2);

        buffer.rawSetForeground(1, 0, new int[][]{{0x123456, 0x654321}});
        buffer.rawSetBackground(0, 1, new int[][]{{0x111111, 0x222222}});

        assertEquals(0x123456, buffer.getForegroundColor(1, 0));
        assertEquals(0x654321, buffer.getForegroundColor(2, 0));
        assertEquals(0x111111, buffer.getBackgroundColor(0, 1));
        assertEquals(0x222222, buffer.getBackgroundColor(1, 1));
    }
}
