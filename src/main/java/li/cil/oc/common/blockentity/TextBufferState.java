package li.cil.oc.common.blockentity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;

final class TextBufferState {
    private static final String TAG_HEIGHT = "height";
    private static final String TAG_ROWS = "rows";
    private static final String TAG_WIDTH = "width";

    private int width;
    private int height;
    private int[][] text;

    TextBufferState(final int width, final int height) {
        resize(width, height);
    }

    void resize(final int width, final int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        text = new int[this.height][this.width];
        fill(0, 0, this.width, this.height, ' ');
    }

    void copy(final int column, final int row, final int width, final int height, final int horizontalTranslation, final int verticalTranslation) {
        final int[][] snapshot = new int[Math.max(0, height)][Math.max(0, width)];
        for (int y = 0; y < snapshot.length; y++) {
            for (int x = 0; x < snapshot[y].length; x++) {
                snapshot[y][x] = getCodePoint(column + x, row + y);
            }
        }
        for (int y = 0; y < snapshot.length; y++) {
            for (int x = 0; x < snapshot[y].length; x++) {
                put(column + x + horizontalTranslation, row + y + verticalTranslation, snapshot[y][x]);
            }
        }
    }

    void fill(final int column, final int row, final int width, final int height, final int value) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                put(column + x, row + y, value);
            }
        }
    }

    void set(final int column, final int row, final String value, final boolean vertical) {
        if (value == null) {
            return;
        }
        final int[] codePoints = value.codePoints().toArray();
        for (int index = 0; index < codePoints.length; index++) {
            final int x = vertical ? column : column + index;
            final int y = vertical ? row + index : row;
            put(x, y, codePoints[index]);
        }
    }

    int getCodePoint(final int column, final int row) {
        if (!isInside(column, row)) {
            return ' ';
        }
        return text[row][column];
    }

    void rawSetText(final int column, final int row, final char[][] text) {
        if (text == null) {
            return;
        }
        for (int y = 0; y < text.length; y++) {
            for (int x = 0; x < text[y].length; x++) {
                put(column + x, row + y, text[y][x]);
            }
        }
    }

    void rawSetText(final int column, final int row, final int[][] text) {
        if (text == null) {
            return;
        }
        for (int y = 0; y < text.length; y++) {
            for (int x = 0; x < text[y].length; x++) {
                put(column + x, row + y, text[y][x]);
            }
        }
    }

    void load(final CompoundTag tag) {
        final int loadedWidth = Math.max(1, tag.getInt(TAG_WIDTH));
        final int loadedHeight = Math.max(1, tag.getInt(TAG_HEIGHT));
        resize(loadedWidth, loadedHeight);
        final ListTag rows = tag.getList(TAG_ROWS, IntArrayTag.TAG_INT_ARRAY);
        for (int y = 0; y < Math.min(rows.size(), height); y++) {
            final int[] row = rows.getIntArray(y);
            for (int x = 0; x < Math.min(row.length, width); x++) {
                text[y][x] = row[x];
            }
        }
    }

    void save(final CompoundTag tag) {
        tag.putInt(TAG_WIDTH, width);
        tag.putInt(TAG_HEIGHT, height);
        final ListTag rows = new ListTag();
        for (int y = 0; y < height; y++) {
            rows.add(new IntArrayTag(text[y]));
        }
        tag.put(TAG_ROWS, rows);
    }

    private void put(final int column, final int row, final int value) {
        if (isInside(column, row)) {
            text[row][column] = value;
        }
    }

    private boolean isInside(final int column, final int row) {
        return column >= 0 && row >= 0 && column < width && row < height;
    }
}
