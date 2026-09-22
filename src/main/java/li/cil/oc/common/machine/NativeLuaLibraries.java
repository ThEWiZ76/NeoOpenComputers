package li.cil.oc.common.machine;

import li.cil.oc.api.machine.Machine;
import li.cil.oc.common.util.FontWidths;
import li.cil.repack.com.naef.jnlua.JavaFunction;
import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.repack.com.naef.jnlua.LuaType;

/** Host implementations behind the upstream kernel's Unicode and OS wrappers. */
final class NativeLuaLibraries {
    private NativeLuaLibraries() {}

    static void installUnicode(final LuaState lua) {
        lua.newTable();
        function(lua, "char", state -> {
            final StringBuilder result = new StringBuilder();
            for (int i = 1; i <= state.getTop(); i++) result.appendCodePoint(integer(state, i));
            state.pushString(result.toString()); return 1;
        });
        function(lua, "len", state -> {
            final String value = state.checkString(1);
            state.pushInteger(value.codePointCount(0, value.length())); return 1;
        });
        // The upstream API intentionally follows the server's default locale.
        function(lua, "lower", state -> { state.pushString(state.checkString(1).toLowerCase()); return 1; });
        function(lua, "upper", state -> { state.pushString(state.checkString(1).toUpperCase()); return 1; });
        function(lua, "reverse", state -> { state.pushString(UnicodeStrings.reverseUnicode(state.checkString(1))); return 1; });
        function(lua, "sub", state -> {
            final String value = state.checkString(1);
            final int start = integer(state, 2);
            final int end = state.getTop() > 2 ? integer(state, 3) : Integer.MAX_VALUE;
            state.pushString(UnicodeStrings.subUnicode(value, start, end)); return 1;
        });
        function(lua, "isWide", state -> { state.pushBoolean(charWidth(state.checkString(1)) > 1); return 1; });
        function(lua, "charWidth", state -> { state.pushInteger(charWidth(state.checkString(1))); return 1; });
        function(lua, "wlen", state -> { state.pushInteger(UnicodeStrings.displayWidth(state.checkString(1))); return 1; });
        function(lua, "wtrunc", state -> {
            state.pushString(UnicodeStrings.truncateDisplayWidth(state.checkString(1), integer(state, 2))); return 1;
        });
        lua.setGlobal("unicode");
    }

    static void installOs(final LuaState lua, final Machine machine) {
        lua.newTable();
        function(lua, "clock", state -> { state.pushNumber(machine.cpuTime()); return 1; });
        function(lua, "time", state -> {
            if (state.isNoneOrNil(1)) state.pushNumber(worldTime(machine));
            else {
                state.checkType(1, LuaType.TABLE);
                final int sec = dateField(state, "sec", 0);
                final int min = dateField(state, "min", 0);
                final int hour = dateField(state, "hour", 12);
                final int day = dateField(state, "day", -1);
                final int month = dateField(state, "month", -1);
                final int year = dateField(state, "year", -1);
                final Long timestamp = GameTimeFormatter.mktime(year, month, day, hour, min, sec);
                if (timestamp == null) state.pushNil(); else state.pushNumber(timestamp);
            }
            return 1;
        });
        function(lua, "date", state -> {
            String format = state.isString(1) ? state.toString(1) : "%d/%m/%y %H:%M:%S";
            final double time = state.isNumber(2) ? state.toNumber(2) : worldTime(machine);
            if (format.startsWith("!")) format = format.substring(1);
            final var date = GameTimeFormatter.parse(time);
            if (format.equals("*t")) {
                state.newTable();
                field(state, "year", date.year()); field(state, "month", date.month());
                field(state, "day", date.day()); field(state, "hour", date.hour());
                field(state, "min", date.minute()); field(state, "sec", date.second());
                field(state, "wday", date.weekDay()); field(state, "yday", date.yearDay());
            } else state.pushString(GameTimeFormatter.format(format, date));
            return 1;
        });
        lua.setGlobal("os");
    }

    private static double worldTime(final Machine machine) { return (machine.worldTime() + 6000D) * 3.6D; }

    private static int dateField(final LuaState lua, final String name, final int fallback) {
        lua.getField(1, name);
        final Long value = lua.toIntegerX(-1);
        lua.pop(1);
        if (value != null) return value.intValue();
        if (fallback < 0) throw new IllegalArgumentException("field '" + name + "' missing in date table");
        return fallback;
    }

    private static int integer(final LuaState lua, final int index) {
        final long value = lua.checkInteger(index);
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }

    private static int charWidth(final String value) {
        if (value.isEmpty()) throw new IllegalArgumentException("empty string");
        return FontWidths.wcwidth(value.codePointAt(0));
    }

    private static void field(final LuaState lua, final String name, final int value) {
        lua.pushInteger(value); lua.setField(-2, name);
    }

    private static void function(final LuaState lua, final String name, final JavaFunction function) {
        lua.pushJavaFunction(function); lua.setField(-2, name);
    }
}
