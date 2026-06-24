package li.cil.oc.common.blockentity;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TextBufferStateTest {
    @Test
    void setWritesTextHorizontallyAndVertically() {
        TextBufferState buffer = new TextBufferState(4, 3);

        buffer.set(0, 0, "ab", false, 0x112233, false, 0x445566, false);
        buffer.set(3, 0, "xy", true, 0x778899, true, 0xAABBCC, true);

        assertEquals('a', buffer.getCodePoint(0, 0));
        assertEquals('b', buffer.getCodePoint(1, 0));
        assertEquals('x', buffer.getCodePoint(3, 0));
        assertEquals('y', buffer.getCodePoint(3, 1));
        assertEquals(0x112233, buffer.getForegroundColor(0, 0));
        assertEquals(0x445566, buffer.getBackgroundColor(1, 0));
        assertEquals(0x778899, buffer.getForegroundColor(3, 0));
        assertEquals(0xAABBCC, buffer.getBackgroundColor(3, 1));
        assertEquals(false, buffer.isForegroundFromPalette(0, 0));
        assertEquals(true, buffer.isForegroundFromPalette(3, 1));
        assertEquals(false, buffer.isBackgroundFromPalette(1, 0));
        assertEquals(true, buffer.isBackgroundFromPalette(3, 0));
    }

    @Test
    void setRespectsWideCharactersLikeUpstream() {
        TextBufferState buffer = new TextBufferState(4, 2);
        String wide = new String(Character.toChars(0x6c34));

        buffer.set(0, 0, wide + "b", false, 0x112233, false, 0x445566, false);
        buffer.set(3, 0, wide, false, 0x778899, false, 0xAABBCC, false);
        buffer.set(3, 1, wide, true, 0x778899, false, 0xAABBCC, false);

        assertEquals(0x6c34, buffer.getCodePoint(0, 0));
        assertEquals(' ', buffer.getCodePoint(1, 0));
        assertEquals('b', buffer.getCodePoint(2, 0));
        assertEquals(' ', buffer.getCodePoint(3, 0));
        assertEquals(' ', buffer.getCodePoint(3, 1));
        assertEquals(0x112233, buffer.getForegroundColor(1, 0));
        assertEquals(0x445566, buffer.getBackgroundColor(1, 0));
    }

    @Test
    void fillRespectsWideCharactersLikeUpstream() {
        TextBufferState buffer = new TextBufferState(4, 1);
        String wide = new String(Character.toChars(0x6c34));

        buffer.fill(0, 0, 4, 1, wide.codePointAt(0), 0x112233, false, 0x445566, false);

        assertEquals(0x6c34, buffer.getCodePoint(0, 0));
        assertEquals(' ', buffer.getCodePoint(1, 0));
        assertEquals(0x6c34, buffer.getCodePoint(2, 0));
        assertEquals(' ', buffer.getCodePoint(3, 0));
    }

    @Test
    void fillWritesRectWithinBounds() {
        TextBufferState buffer = new TextBufferState(4, 3);

        buffer.fill(1, 1, 5, 5, 'z', 0x112233, true, 0x445566, false);

        assertEquals(' ', buffer.getCodePoint(0, 0));
        assertEquals('z', buffer.getCodePoint(1, 1));
        assertEquals('z', buffer.getCodePoint(3, 2));
        assertEquals(0x112233, buffer.getForegroundColor(1, 1));
        assertEquals(0x445566, buffer.getBackgroundColor(3, 2));
        assertEquals(true, buffer.isForegroundFromPalette(3, 2));
        assertEquals(false, buffer.isBackgroundFromPalette(1, 1));
    }

    @Test
    void copyMovesRectWithOverlap() {
        TextBufferState buffer = new TextBufferState(5, 1);
        buffer.set(0, 0, "abcd", false, 0x112233, true, 0x445566, false);

        buffer.copy(0, 0, 4, 1, 1, 0);

        assertEquals('a', buffer.getCodePoint(1, 0));
        assertEquals('b', buffer.getCodePoint(2, 0));
        assertEquals('c', buffer.getCodePoint(3, 0));
        assertEquals('d', buffer.getCodePoint(4, 0));
        assertEquals(0x112233, buffer.getForegroundColor(4, 0));
        assertEquals(0x445566, buffer.getBackgroundColor(4, 0));
        assertEquals(true, buffer.isForegroundFromPalette(4, 0));
        assertEquals(false, buffer.isBackgroundFromPalette(4, 0));
    }

    @Test
    void copySkipsCellsWithoutValidSourceLikeUpstream() {
        TextBufferState buffer = new TextBufferState(4, 1);
        buffer.set(0, 0, "abcd", false, 0x112233, false, 0x445566, false);

        buffer.copy(-1, 0, 3, 1, 1, 0);

        assertEquals('a', buffer.getCodePoint(0, 0));
        assertEquals('a', buffer.getCodePoint(1, 0));
        assertEquals('b', buffer.getCodePoint(2, 0));
        assertEquals('d', buffer.getCodePoint(3, 0));
    }

    @Test
    void saveAndLoadPreservesText() {
        TextBufferState saved = new TextBufferState(3, 2);
        saved.set(0, 0, "abc", false, 0xFFFFFF, false, 0x000000, false);
        saved.set(0, 1, "xy", false, 0xFFFFFF, false, 0x000000, false);
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
    void resizePreservesOverlappingTextAndColors() {
        TextBufferState buffer = new TextBufferState(2, 2);
        buffer.set(0, 0, "A", false, 0x112233, true, 0x445566, false);

        buffer.resize(3, 3);

        assertEquals('A', buffer.getCodePoint(0, 0));
        assertEquals(0x112233, buffer.getForegroundColor(0, 0));
        assertEquals(0x445566, buffer.getBackgroundColor(0, 0));
        assertEquals(true, buffer.isForegroundFromPalette(0, 0));
        assertEquals(false, buffer.isBackgroundFromPalette(0, 0));
        assertEquals(' ', buffer.getCodePoint(2, 2));
        assertEquals(0xFFFFFF, buffer.getForegroundColor(2, 2));
        assertEquals(0x000000, buffer.getBackgroundColor(2, 2));
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

    @Test
    void saveAndLoadPreservesColors() {
        TextBufferState saved = new TextBufferState(2, 1);
        saved.rawSetForeground(0, 0, new int[][]{{0xABCDEF, 0x123456}});
        saved.rawSetBackground(0, 0, new int[][]{{0x010203, 0x040506}});
        CompoundTag tag = new CompoundTag();

        saved.save(tag);

        TextBufferState loaded = new TextBufferState(1, 1);
        loaded.load(tag);

        assertEquals(0xABCDEF, loaded.getForegroundColor(0, 0));
        assertEquals(0x123456, loaded.getForegroundColor(1, 0));
        assertEquals(0x010203, loaded.getBackgroundColor(0, 0));
        assertEquals(0x040506, loaded.getBackgroundColor(1, 0));
    }
}
