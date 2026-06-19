package li.cil.oc.common.blockentity;

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
}
