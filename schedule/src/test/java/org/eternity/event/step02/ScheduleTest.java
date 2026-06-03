package org.eternity.event.step02;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ScheduleTest {
    @Test
    public void monthly_includes() {
        Schedule schedule = new Schedule("월간회의", LocalTime.of(14, 0), Duration.ofHours(1), DayOfWeek.MONDAY, 2);
        assertThat(schedule.includes(LocalDate.of(2025, 1, 13))).isTrue();
    }

    @Test
    public void monthly_excludes_wrong_dayOfWeek() {
        Schedule schedule = new Schedule("월간회의", LocalTime.of(14, 0), Duration.ofHours(1), DayOfWeek.MONDAY, 2);
        assertThat(schedule.includes(LocalDate.of(2025, 1, 14))).isFalse();
    }

    @Test
    public void monthly_excludes_wrong_ordinal() {
        Schedule schedule = new Schedule("월간회의", LocalTime.of(14, 0), Duration.ofHours(1), DayOfWeek.MONDAY, 2);
        assertThat(schedule.includes(LocalDate.of(2025, 1, 20))).isFalse();
    }

    @Test
    public void weekly_includes() {
        Schedule schedule = new Schedule("데일리 스크럼", LocalTime.of(9, 0), Duration.ofMinutes(15), Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY));
        assertThat(schedule.includes(LocalDate.of(2025, 1, 13))).isTrue();
    }

    @Test
    public void weekly_excludes_day_not_in_set() {
        Schedule schedule = new Schedule("데일리 스크럼", LocalTime.of(9, 0), Duration.ofMinutes(15), Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY));
        assertThat(schedule.includes(LocalDate.of(2025, 1, 15))).isFalse();
    }

    @Test
    public void weekly_excludes_empty_dayOfWeeks() {
        Schedule schedule = new Schedule("데일리 스크럼", LocalTime.of(9, 0), Duration.ofMinutes(15), Set.of());
        assertThat(schedule.includes(LocalDate.of(2025, 1, 13))).isFalse();
    }
}
