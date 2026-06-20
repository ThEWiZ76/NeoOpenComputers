package li.cil.oc.common.blockentity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;

final class TextBufferState {
    private static final String TAG_HEIGHT = "height";
    private static final String TAG_BACKGROUND = "background";
    private static final String TAG_BACKGROUND_PALETTE = "backgroundPalette";
    private static final String TAG_FOREGROUND = "foreground";
    private static final String TAG_FOREGROUND_PALETTE = "foregroundPalette";
    private static final String TAG_ROWS = "rows";
    private static final String TAG_WIDTH = "width";

    private int width;
    private int height;
    private int[][] text;
    private int[][] foreground;
    private int[][] background;
    private boolean[][] foregroundPalette;
    private boolean[][] backgroundPalette;

    TextBufferState(final int width, final int height) {
        resize(width, height);
    }

    void resize(final int width, final int height) {
        final int newWidth = Math.max(1, width);
        final int newHeight = Math.max(1, height);
        final int[][] previousText = text;
        final int[][] previousForeground = foreground;
        final int[][] previousBackground = background;
        final boolean[][] previousForegroundPalette = foregroundPalette;
        final boolean[][] previousBackgroundPalette = backgroundPalette;
        final int previousWidth = this.width;
        final int previousHeight = this.height;

        this.width = newWidth;
        this.height = newHeight;
        text = new int[newHeight][newWidth];
        foreground = new int[newHeight][newWidth];
        background = new int[newHeight][newWidth];
        foregroundPalette = new boolean[newHeight][newWidth];
        backgroundPalette = new boolean[newHeight][newWidth];
        fill(0, 0, newWidth, newHeight, ' ', 0xFFFFFF, false, 0x000000, false);

        if (previousText != null) {
            final int rows = Math.min(previousHeight, newHeight);
            final int columns = Math.min(previousWidth, newWidth);
            for (int y = 0; y < rows; y++) {
                System.arraycopy(previousText[y], 0, text[y], 0, columns);
                System.arraycopy(previousForeground[y], 0, foreground[y], 0, columns);
                System.arraycopy(previousBackground[y], 0, background[y], 0, columns);
                System.arraycopy(previousForegroundPalette[y], 0, foregroundPalette[y], 0, columns);
                System.arraycopy(previousBackgroundPalette[y], 0, backgroundPalette[y], 0, columns);
            }
        }
    }

    void copy(final int column, final int row, final int width, final int height, final int horizontalTranslation, final int verticalTranslation) {
        final int rows = Math.max(0, height);
        final int columns = Math.max(0, width);
        final int[][] textSnapshot = new int[rows][columns];
        final int[][] foregroundSnapshot = new int[rows][columns];
        final int[][] backgroundSnapshot = new int[rows][columns];
        final boolean[][] foregroundPaletteSnapshot = new boolean[rows][columns];
        final boolean[][] backgroundPaletteSnapshot = new boolean[rows][columns];
        for (int y = 0; y < textSnapshot.length; y++) {
            for (int x = 0; x < textSnapshot[y].length; x++) {
                final int sourceColumn = column + x;
                final int sourceRow = row + y;
                textSnapshot[y][x] = getCodePoint(sourceColumn, sourceRow);
                foregroundSnapshot[y][x] = getForegroundColor(sourceColumn, sourceRow);
                backgroundSnapshot[y][x] = getBackgroundColor(sourceColumn, sourceRow);
                foregroundPaletteSnapshot[y][x] = isForegroundFromPalette(sourceColumn, sourceRow);
                backgroundPaletteSnapshot[y][x] = isBackgroundFromPalette(sourceColumn, sourceRow);
            }
        }
        for (int y = 0; y < textSnapshot.length; y++) {
            for (int x = 0; x < textSnapshot[y].length; x++) {
                put(
                    column + x + horizontalTranslation,
                    row + y + verticalTranslation,
                    textSnapshot[y][x],
                    foregroundSnapshot[y][x],
                    foregroundPaletteSnapshot[y][x],
                    backgroundSnapshot[y][x],
                    backgroundPaletteSnapshot[y][x]
                );
            }
        }
    }

    void fill(final int column, final int row, final int width, final int height, final int value) {
        fill(column, row, width, height, value, 0xFFFFFF, false, 0x000000, false);
    }

    void fill(final int column, final int row, final int width, final int height, final int value, final int foregroundColor, final boolean foregroundFromPalette, final int backgroundColor, final boolean backgroundFromPalette) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                put(column + x, row + y, value, foregroundColor, foregroundFromPalette, backgroundColor, backgroundFromPalette);
            }
        }
    }

    void set(final int column, final int row, final String value, final boolean vertical) {
        set(column, row, value, vertical, 0xFFFFFF, false, 0x000000, false);
    }

    void set(final int column, final int row, final String value, final boolean vertical, final int foregroundColor, final boolean foregroundFromPalette, final int backgroundColor, final boolean backgroundFromPalette) {
        if (value == null) {
            return;
        }
        final int[] codePoints = value.codePoints().toArray();
        for (int index = 0; index < codePoints.length; index++) {
            final int x = vertical ? column : column + index;
            final int y = vertical ? row + index : row;
            put(x, y, codePoints[index], foregroundColor, foregroundFromPalette, backgroundColor, backgroundFromPalette);
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
                putText(column + x, row + y, text[y][x]);
            }
        }
    }

    void rawSetText(final int column, final int row, final int[][] text) {
        if (text == null) {
            return;
        }
        for (int y = 0; y < text.length; y++) {
            for (int x = 0; x < text[y].length; x++) {
                putText(column + x, row + y, text[y][x]);
            }
        }
    }

    void rawSetForeground(final int column, final int row, final int[][] color) {
        rawSetColor(foreground, foregroundPalette, column, row, color);
    }

    void rawSetBackground(final int column, final int row, final int[][] color) {
        rawSetColor(background, backgroundPalette, column, row, color);
    }

    int getForegroundColor(final int column, final int row) {
        return isInside(column, row) ? foreground[row][column] : 0;
    }

    boolean isForegroundFromPalette(final int column, final int row) {
        return isInside(column, row) && foregroundPalette[row][column];
    }

    int getBackgroundColor(final int column, final int row) {
        return isInside(column, row) ? background[row][column] : 0;
    }

    boolean isBackgroundFromPalette(final int column, final int row) {
        return isInside(column, row) && backgroundPalette[row][column];
    }

    void load(final CompoundTag tag) {
        final int loadedWidth = Math.max(1, tag.getInt(TAG_WIDTH));
        final int loadedHeight = Math.max(1, tag.getInt(TAG_HEIGHT));
        resize(loadedWidth, loadedHeight);
        final ListTag rows = tag.getList(TAG_ROWS, IntArrayTag.TAG_INT_ARRAY);
        loadRows(rows, text);
        loadRows(tag.getList(TAG_FOREGROUND, IntArrayTag.TAG_INT_ARRAY), foreground);
        loadRows(tag.getList(TAG_BACKGROUND, IntArrayTag.TAG_INT_ARRAY), background);
        loadBooleanRows(tag.getList(TAG_FOREGROUND_PALETTE, IntArrayTag.TAG_INT_ARRAY), foregroundPalette);
        loadBooleanRows(tag.getList(TAG_BACKGROUND_PALETTE, IntArrayTag.TAG_INT_ARRAY), backgroundPalette);
    }

    void save(final CompoundTag tag) {
        tag.putInt(TAG_WIDTH, width);
        tag.putInt(TAG_HEIGHT, height);
        tag.put(TAG_ROWS, saveRows(text));
        tag.put(TAG_FOREGROUND, saveRows(foreground));
        tag.put(TAG_BACKGROUND, saveRows(background));
        tag.put(TAG_FOREGROUND_PALETTE, saveBooleanRows(foregroundPalette));
        tag.put(TAG_BACKGROUND_PALETTE, saveBooleanRows(backgroundPalette));
    }

    private void put(final int column, final int row, final int value) {
        put(column, row, value, 0xFFFFFF, false, 0x000000, false);
    }

    private void put(final int column, final int row, final int value, final int foregroundColor, final boolean foregroundFromPalette, final int backgroundColor, final boolean backgroundFromPalette) {
        if (isInside(column, row)) {
            text[row][column] = value;
            foreground[row][column] = foregroundColor;
            this.foregroundPalette[row][column] = foregroundFromPalette;
            background[row][column] = backgroundColor;
            this.backgroundPalette[row][column] = backgroundFromPalette;
        }
    }

    private void putText(final int column, final int row, final int value) {
        if (isInside(column, row)) {
            text[row][column] = value;
        }
    }

    private void rawSetColor(final int[][] target, final boolean[][] paletteTarget, final int column, final int row, final int[][] color) {
        if (color == null) {
            return;
        }
        for (int y = 0; y < color.length; y++) {
            for (int x = 0; x < color[y].length; x++) {
                final int targetX = column + x;
                final int targetY = row + y;
                if (isInside(targetX, targetY)) {
                    target[targetY][targetX] = color[y][x];
                    paletteTarget[targetY][targetX] = false;
                }
            }
        }
    }

    private void loadRows(final ListTag rows, final int[][] target) {
        for (int y = 0; y < Math.min(rows.size(), height); y++) {
            final int[] row = rows.getIntArray(y);
            for (int x = 0; x < Math.min(row.length, width); x++) {
                target[y][x] = row[x];
            }
        }
    }

    private void loadBooleanRows(final ListTag rows, final boolean[][] target) {
        for (int y = 0; y < Math.min(rows.size(), height); y++) {
            final int[] row = rows.getIntArray(y);
            for (int x = 0; x < Math.min(row.length, width); x++) {
                target[y][x] = row[x] != 0;
            }
        }
    }

    private ListTag saveRows(final int[][] source) {
        final ListTag rows = new ListTag();
        for (int y = 0; y < height; y++) {
            rows.add(new IntArrayTag(source[y]));
        }
        return rows;
    }

    private ListTag saveBooleanRows(final boolean[][] source) {
        final ListTag rows = new ListTag();
        for (int y = 0; y < height; y++) {
            final int[] row = new int[width];
            for (int x = 0; x < width; x++) {
                row[x] = source[y][x] ? 1 : 0;
            }
            rows.add(new IntArrayTag(row));
        }
        return rows;
    }

    private boolean isInside(final int column, final int row) {
        return column >= 0 && row >= 0 && column < width && row < height;
    }
}
