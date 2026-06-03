package org.eternity.event.step03;

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
        Schedule schedule = new Schedule("월간회의", LocalTime.of(14, 0), Duration.ofHours(1), new MonthlyPlan(DayOfWeek.MONDAY, 2));
        assertThat(schedule.includes(LocalDate.of(2025, 1, 13))).isTrue();
    }

    @Test
    public void monthly_excludes() {
        Schedule schedule = new Schedule("월간회의", LocalTime.of(14, 0), Duration.ofHours(1), new MonthlyPlan(DayOfWeek.MONDAY, 2));
        assertThat(schedule.includes(LocalDate.of(2025, 1, 14))).isFalse();
    }

    @Test
    public void weekly_includes() {
        Schedule schedule = new Schedule("데일리 스크럼", LocalTime.of(9, 0), Duration.ofMinutes(15), new WeeklyPlan(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)));
        assertThat(schedule.includes(LocalDate.of(2025, 1, 13))).isTrue();
    }

    @Test
    public void weekly_excludes() {
        Schedule schedule = new Schedule("데일리 스크럼", LocalTime.of(9, 0), Duration.ofMinutes(15), new WeeklyPlan(Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)));
        assertThat(schedule.includes(LocalDate.of(2025, 1, 15))).isFalse();
    }
}
