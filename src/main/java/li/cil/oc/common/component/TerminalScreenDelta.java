package li.cil.oc.common.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;

import java.util.Arrays;

public record TerminalScreenDelta(int width, int height, Row[] rows) {
    private static final String TAG_WIDTH = "width";
    private static final String TAG_HEIGHT = "height";
    private static final String TAG_ROWS = "rows";
    private static final String TAG_INDEX = "index";
    private static final String TAG_LINE = "line";
    private static final String TAG_FOREGROUND = "foreground";
    private static final String TAG_BACKGROUND = "background";

    public TerminalScreenDelta {
        width = Math.max(0, width);
        height = Math.max(0, height);
        rows = rows == null ? new Row[0] : Arrays.copyOf(rows, rows.length);
    }

    @Override
    public Row[] rows() {
        return Arrays.copyOf(rows, rows.length);
    }

    public int rowCount() {
        return rows.length;
    }

    public int rowIndex(final int index) {
        return index >= 0 && index < rows.length ? rows[index].index() : -1;
    }

    public TerminalScreenSnapshot applyTo(final TerminalScreenSnapshot base) {
        if (base == null || base.width() != width || base.height() != height) {
            return base;
        }
        final String[] lines = new String[height];
        for (int row = 0; row < height; row++) {
            lines[row] = base.line(row);
        }
        final int[][] foreground = base.foreground();
        final int[][] background = base.background();
        for (final Row row : rows) {
            if (row.index() >= 0 && row.index() < height) {
                lines[row.index()] = row.line();
                foreground[row.index()] = Arrays.copyOf(row.foreground(), width);
                background[row.index()] = Arrays.copyOf(row.background(), width);
            }
        }
        return new TerminalScreenSnapshot(width, height, lines, foreground, background);
    }

    public void save(final CompoundTag tag) {
        tag.putInt(TAG_WIDTH, width);
        tag.putInt(TAG_HEIGHT, height);
        final ListTag rowTags = new ListTag();
        for (final Row row : rows) {
            final CompoundTag rowTag = new CompoundTag();
            rowTag.putInt(TAG_INDEX, row.index());
            rowTag.putString(TAG_LINE, row.line());
            rowTag.put(TAG_FOREGROUND, new IntArrayTag(row.foreground()));
            rowTag.put(TAG_BACKGROUND, new IntArrayTag(row.background()));
            rowTags.add(rowTag);
        }
        tag.put(TAG_ROWS, rowTags);
    }

    public static TerminalScreenDelta between(final TerminalScreenSnapshot previous, final TerminalScreenSnapshot current) {
        if (current == null) {
            return new TerminalScreenDelta(0, 0, new Row[0]);
        }
        final Row[] changed = new Row[current.height()];
        int count = 0;
        final int[][] currentForeground = current.foreground();
        final int[][] currentBackground = current.background();
        final int[][] previousForeground = previous == null ? null : previous.foreground();
        final int[][] previousBackground = previous == null ? null : previous.background();
        for (int row = 0; row < current.height(); row++) {
            if (previous == null
                || previous.width() != current.width()
                || previous.height() != current.height()
                || !current.line(row).equals(previous.line(row))
                || !Arrays.equals(currentForeground[row], previousForeground[row])
                || !Arrays.equals(currentBackground[row], previousBackground[row])) {
                changed[count++] = new Row(row, current.line(row), currentForeground[row], currentBackground[row]);
            }
        }
        return new TerminalScreenDelta(current.width(), current.height(), Arrays.copyOf(changed, count));
    }

    public static TerminalScreenDelta load(final CompoundTag tag) {
        final int width = Math.max(0, tag.getInt(TAG_WIDTH));
        final int height = Math.max(0, tag.getInt(TAG_HEIGHT));
        final ListTag rowTags = tag.getList(TAG_ROWS, CompoundTag.TAG_COMPOUND);
        final Row[] rows = new Row[rowTags.size()];
        for (int index = 0; index < rows.length; index++) {
            final CompoundTag rowTag = rowTags.getCompound(index);
            rows[index] = new Row(
                rowTag.getInt(TAG_INDEX),
                rowTag.getString(TAG_LINE),
                rowTag.getIntArray(TAG_FOREGROUND),
                rowTag.getIntArray(TAG_BACKGROUND));
        }
        return new TerminalScreenDelta(width, height, rows);
    }

    public record Row(int index, String line, int[] foreground, int[] background) {
        public Row {
            line = line == null ? "" : line;
            foreground = foreground == null ? new int[0] : Arrays.copyOf(foreground, foreground.length);
            background = background == null ? new int[0] : Arrays.copyOf(background, background.length);
        }

        @Override
        public int[] foreground() {
            return Arrays.copyOf(foreground, foreground.length);
        }

        @Override
        public int[] background() {
            return Arrays.copyOf(background, background.length);
        }
    }
}
