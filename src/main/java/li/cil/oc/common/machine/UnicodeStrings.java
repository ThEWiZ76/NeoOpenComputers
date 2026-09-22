package li.cil.oc.common.machine;

import li.cil.oc.common.util.FontWidths;

/** Shared code-point operations for the LuaJ and native Unicode bindings. */
final class UnicodeStrings {
    private UnicodeStrings() {}

    static String reverseUnicode(final String value) {
        final StringBuilder builder = new StringBuilder(value.length());
        for (int offset = value.length(); offset > 0; ) {
            final int codePoint = value.codePointBefore(offset);
            offset -= Character.charCount(codePoint);
            builder.appendCodePoint(codePoint);
        }
        return builder.toString();
    }

    static String subUnicode(final String value, final int startIndex, final int endIndex) {
        final int codePointLength = value.codePointCount(0, value.length());
        final int start = startIndex < 0
            ? value.offsetByCodePoints(value.length(), Math.max(startIndex, -codePointLength))
            : startIndex == 0 ? 0 : value.offsetByCodePoints(0, Math.min(startIndex - 1, codePointLength));
        final int end = endIndex == Integer.MAX_VALUE
            ? value.length()
            : endIndex < 0
                ? value.offsetByCodePoints(value.length(), Math.max(endIndex + 1, -codePointLength))
                : value.offsetByCodePoints(0, Math.min(endIndex, codePointLength));
        return end <= start ? "" : value.substring(start, end);
    }

    static int displayWidth(final String value) {
        int width = 0;
        for (int offset = 0; offset < value.length(); ) {
            final int codePoint = value.codePointAt(offset);
            width += Math.max(1, FontWidths.wcwidth(codePoint));
            offset += Character.charCount(codePoint);
        }
        return width;
    }

    static String truncateDisplayWidth(final String value, final int count) {
        int width = 0;
        int previous = 0;
        int end = 0;
        while (width < count) {
            previous = end;
            final int codePoint = value.codePointAt(end);
            width += Math.max(1, FontWidths.wcwidth(codePoint));
            end += Character.charCount(codePoint);
        }
        return previous > 0 ? value.substring(0, previous) : "";
    }

}
