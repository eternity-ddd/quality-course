package org.eternity.event.step02;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

public class SchedulerTest {
    private final Scheduler scheduler = new Scheduler(new JsonConverter());

    @Test
    public void check_includes() {
        Schedule schedule = new Schedule("월간회의", LocalTime.of(14, 0), Duration.ofHours(1), DayOfWeek.MONDAY, 2);
        assertThat(scheduler.check(schedule, LocalDate.of(2025, 1, 13))).isNotEmpty();
    }

    @Test
    public void check_excludes() {
        Schedule schedule = new Schedule("월간회의", LocalTime.of(14, 0), Duration.ofHours(1), DayOfWeek.MONDAY, 2);
        assertThat(scheduler.check(schedule, LocalDate.of(2025, 1, 14))).isEmpty();
    }
}
