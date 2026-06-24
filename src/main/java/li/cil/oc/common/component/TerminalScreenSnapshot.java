package li.cil.oc.common.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.Arrays;

public record TerminalScreenSnapshot(int width, int height, String[] lines, int[][] foreground, int[][] background) {
    private static final String TAG_WIDTH = "width";
    private static final String TAG_HEIGHT = "height";
    private static final String TAG_LINES = "lines";
    private static final String TAG_FOREGROUND = "foreground";
    private static final String TAG_BACKGROUND = "background";
    private static final int DEFAULT_FOREGROUND = 0xFFFFFF;
    private static final int DEFAULT_BACKGROUND = 0x000000;

    public TerminalScreenSnapshot(final int width, final int height, final String[] lines) {
        this(width, height, lines, null, null);
    }

    public TerminalScreenSnapshot {
        width = Math.max(0, width);
        height = Math.max(0, height);
        lines = lines == null ? new String[0] : Arrays.copyOf(lines, lines.length);
        for (int index = 0; index < lines.length; index++) {
            if (lines[index] == null) {
                lines[index] = "";
            }
        }
        foreground = normalizeColors(foreground, width, height, DEFAULT_FOREGROUND);
        background = normalizeColors(background, width, height, DEFAULT_BACKGROUND);
    }

    @Override
    public String[] lines() {
        return Arrays.copyOf(lines, lines.length);
    }

    @Override
    public int[][] foreground() {
        return copyColors(foreground);
    }

    @Override
    public int[][] background() {
        return copyColors(background);
    }

    public String line(final int row) {
        return row >= 0 && row < lines.length ? lines[row] : "";
    }

    public int foregroundColor(final int column, final int row) {
        return colorAt(foreground, column, row, DEFAULT_FOREGROUND);
    }

    public int backgroundColor(final int column, final int row) {
        return colorAt(background, column, row, DEFAULT_BACKGROUND);
    }

    public boolean contentEquals(final TerminalScreenSnapshot other) {
        return other != null &&
            width == other.width &&
            height == other.height &&
            Arrays.equals(lines, other.lines) &&
            Arrays.deepEquals(foreground, other.foreground) &&
            Arrays.deepEquals(background, other.background);
    }

    public void save(final CompoundTag tag) {
        tag.putInt(TAG_WIDTH, width);
        tag.putInt(TAG_HEIGHT, height);
        final ListTag lineTags = new ListTag();
        for (final String line : lines) {
            lineTags.add(StringTag.valueOf(line));
        }
        tag.put(TAG_LINES, lineTags);
        tag.put(TAG_FOREGROUND, saveColors(foreground));
        tag.put(TAG_BACKGROUND, saveColors(background));
    }

    public static TerminalScreenSnapshot load(final CompoundTag tag) {
        final int width = Math.max(0, tag.getInt(TAG_WIDTH));
        final int height = Math.max(0, tag.getInt(TAG_HEIGHT));
        final ListTag lineTags = tag.getList(TAG_LINES, StringTag.TAG_STRING);
        final String[] lines = new String[height];
        for (int index = 0; index < lines.length; index++) {
            lines[index] = index < lineTags.size() ? lineTags.getString(index) : "";
        }
        return new TerminalScreenSnapshot(
            width,
            height,
            lines,
            loadColors(tag.getList(TAG_FOREGROUND, IntArrayTag.TAG_INT_ARRAY), width, height, DEFAULT_FOREGROUND),
            loadColors(tag.getList(TAG_BACKGROUND, IntArrayTag.TAG_INT_ARRAY), width, height, DEFAULT_BACKGROUND));
    }

    private static int colorAt(final int[][] colors, final int column, final int row, final int fallback) {
        if (row < 0 || row >= colors.length || column < 0 || column >= colors[row].length) {
            return fallback;
        }
        return colors[row][column];
    }

    private static int[][] normalizeColors(final int[][] source, final int width, final int height, final int fallback) {
        final int[][] colors = new int[height][width];
        for (final int[] row : colors) {
            Arrays.fill(row, fallback);
        }
        if (source == null) {
            return colors;
        }
        for (int row = 0; row < Math.min(height, source.length); row++) {
            if (source[row] != null) {
                System.arraycopy(source[row], 0, colors[row], 0, Math.min(width, source[row].length));
            }
        }
        return colors;
    }

    private static int[][] copyColors(final int[][] source) {
        final int[][] copy = new int[source.length][];
        for (int row = 0; row < source.length; row++) {
            copy[row] = Arrays.copyOf(source[row], source[row].length);
        }
        return copy;
    }

    private static ListTag saveColors(final int[][] colors) {
        final ListTag rows = new ListTag();
        for (final int[] row : colors) {
            rows.add(new IntArrayTag(row));
        }
        return rows;
    }

    private static int[][] loadColors(final ListTag rows, final int width, final int height, final int fallback) {
        final int[][] colors = new int[height][width];
        for (final int[] row : colors) {
            Arrays.fill(row, fallback);
        }
        for (int row = 0; row < Math.min(height, rows.size()); row++) {
            final int[] source = rows.getIntArray(row);
            System.arraycopy(source, 0, colors[row], 0, Math.min(width, source.length));
        }
        return colors;
    }
}
