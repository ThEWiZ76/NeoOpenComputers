package li.cil.oc.common;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class PresentCalendarTest {
    @Test
    void allDaysMatchTheEighteenUpstreamCelebrationDatesIncludingLeapYears() {
        final Set<MonthDay> holidays = Set.of(
            MonthDay.of(1, 1), MonthDay.of(1, 2), MonthDay.of(1, 3), MonthDay.of(1, 4), MonthDay.of(1, 5), MonthDay.of(1, 6),
            MonthDay.of(2, 14), MonthDay.of(4, 22), MonthDay.of(5, 1), MonthDay.of(10, 3), MonthDay.of(12, 14),
            MonthDay.of(12, 25), MonthDay.of(12, 26), MonthDay.of(12, 27), MonthDay.of(12, 28), MonthDay.of(12, 29), MonthDay.of(12, 30), MonthDay.of(12, 31));
        for (int year : new int[]{2024, 2026}) {
            for (LocalDate date = LocalDate.of(year, 1, 1); date.getYear() == year; date = date.plusDays(1)) {
                assertEquals(holidays.contains(MonthDay.from(date)), PresentHandler.isHoliday(date), date.toString());
            }
        }
    }
}
