package li.cil.oc.common.machine;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.function.Function;

import org.luaj.vm2.LuaError;

final class GameTimeFormatter {
    private static final String[] WEEK_DAYS = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private static final String[] SHORT_WEEK_DAYS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    private static final String[] MONTHS = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    private static final String[] SHORT_MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private static final String[] AM_PM = {"AM", "PM"};

    private static final Map<Character, Function<DateTime, String>> SPECIFIERS = Map.ofEntries(
        Map.entry('a', time -> SHORT_WEEK_DAYS[time.weekDay() - 1]),
        Map.entry('A', time -> WEEK_DAYS[time.weekDay() - 1]),
        Map.entry('b', time -> SHORT_MONTHS[time.month() - 1]),
        Map.entry('B', time -> MONTHS[time.month() - 1]),
        Map.entry('c', time -> format("%a %b %e %H:%M:%S %Y", time)),
        Map.entry('C', time -> String.format("%02d", time.year() / 100)),
        Map.entry('d', time -> String.format("%02d", time.day())),
        Map.entry('D', time -> format("%m/%d/%y", time)),
        Map.entry('e', time -> String.format("%2d", time.day())),
        Map.entry('F', time -> format("%Y-%m-%d", time)),
        Map.entry('h', time -> format("%b", time)),
        Map.entry('H', time -> String.format("%02d", time.hour())),
        Map.entry('I', time -> String.format("%02d", (time.hour() + 11) % 12 + 1)),
        Map.entry('j', time -> String.format("%03d", time.yearDay())),
        Map.entry('m', time -> String.format("%02d", time.month())),
        Map.entry('M', time -> String.format("%02d", time.minute())),
        Map.entry('n', time -> "\n"),
        Map.entry('p', time -> AM_PM[time.hour() < 12 ? 0 : 1]),
        Map.entry('r', time -> format("%I:%M:%S %p", time)),
        Map.entry('R', time -> format("%H:%M", time)),
        Map.entry('S', time -> String.format("%02d", time.second())),
        Map.entry('t', time -> "\t"),
        Map.entry('T', time -> format("%H:%M:%S", time)),
        Map.entry('w', time -> Integer.toString(time.weekDay() - 1)),
        Map.entry('x', time -> format("%D", time)),
        Map.entry('X', time -> format("%T", time)),
        Map.entry('y', time -> String.format("%02d", time.year() % 100)),
        Map.entry('Y', time -> String.format("%04d", time.year())),
        Map.entry('%', time -> "%"));

    private GameTimeFormatter() {
    }

    static DateTime parse(final double time) {
        final ZonedDateTime dateTime = Instant.ofEpochMilli((long) (time * 1000D)).atZone(ZoneOffset.UTC);
        return new DateTime(
            dateTime.getYear(),
            dateTime.getMonthValue(),
            dateTime.getDayOfMonth(),
            dateTime.getDayOfWeek().getValue() % 7 + 1,
            dateTime.getDayOfYear(),
            dateTime.getHour(),
            dateTime.getMinute(),
            dateTime.getSecond());
    }

    static String format(final String format, final DateTime time) {
        final StringBuilder result = new StringBuilder();
        for (int index = 0; index < format.length(); index++) {
            final char value = format.charAt(index);
            if (value == '%' && index + 1 < format.length()) {
                final char specifierKey = format.charAt(++index);
                final Function<DateTime, String> specifier = SPECIFIERS.get(specifierKey);
                if (specifier != null) {
                    result.append(specifier.apply(time));
                }
            } else {
                result.append(value);
            }
        }
        return result.toString();
    }

    static Long mktime(final int year, final int month, final int day, final int hour, final int minute, final int second) {
        try {
            return LocalDateTime.of(year, 1, 1, 0, 0, 0)
                .plusMonths(month - 1L)
                .plusDays(day - 1L)
                .plusHours(hour)
                .plusMinutes(minute)
                .plusSeconds(second)
                .toEpochSecond(ZoneOffset.UTC);
        } catch (DateTimeException e) {
            return null;
        }
    }

    record DateTime(int year, int month, int day, int weekDay, int yearDay, int hour, int minute, int second) {
    }
}
