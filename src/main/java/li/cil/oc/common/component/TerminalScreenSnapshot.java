package li.cil.oc.common.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.Arrays;

public record TerminalScreenSnapshot(int width, int height, String[] lines) {
    private static final String TAG_WIDTH = "width";
    private static final String TAG_HEIGHT = "height";
    private static final String TAG_LINES = "lines";

    public TerminalScreenSnapshot {
        width = Math.max(0, width);
        height = Math.max(0, height);
        lines = lines == null ? new String[0] : Arrays.copyOf(lines, lines.length);
        for (int index = 0; index < lines.length; index++) {
            if (lines[index] == null) {
                lines[index] = "";
            }
        }
    }

    @Override
    public String[] lines() {
        return Arrays.copyOf(lines, lines.length);
    }

    public String line(final int row) {
        return row >= 0 && row < lines.length ? lines[row] : "";
    }

    public void save(final CompoundTag tag) {
        tag.putInt(TAG_WIDTH, width);
        tag.putInt(TAG_HEIGHT, height);
        final ListTag lineTags = new ListTag();
        for (final String line : lines) {
            lineTags.add(StringTag.valueOf(line));
        }
        tag.put(TAG_LINES, lineTags);
    }

    public static TerminalScreenSnapshot load(final CompoundTag tag) {
        final int width = Math.max(0, tag.getInt(TAG_WIDTH));
        final int height = Math.max(0, tag.getInt(TAG_HEIGHT));
        final ListTag lineTags = tag.getList(TAG_LINES, StringTag.TAG_STRING);
        final String[] lines = new String[Math.min(height, lineTags.size())];
        for (int index = 0; index < lines.length; index++) {
            lines[index] = lineTags.getString(index);
        }
        return new TerminalScreenSnapshot(width, height, lines);
    }
}
