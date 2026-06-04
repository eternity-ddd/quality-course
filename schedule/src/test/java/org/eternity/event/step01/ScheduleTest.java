package org.eternity.event.step01;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ScheduleTest {
    @Test
    public void monthly_includes_2nd_week() {
        Schedule schedule = new Schedule("월간회의",
                LocalTime.of(14, 0),
                Duration.ofHours(1),
                DayOfWeek.MONDAY, 2);

        assertThat(schedule.check(LocalDate.of(2025, 1, 13))).isNotEmpty();
    }

    @Test
    public void monthly_excludes_wrong_ordinal() {
        Schedule schedule = new Schedule("월간회의",
                LocalTime.of(14, 0),
                Duration.ofHours(1),
                DayOfWeek.MONDAY, 2);

        assertThat(schedule.check(LocalDate.of(2025, 1, 20))).isEmpty();
    }

    @Test
    public void monthly_excludes_wrong_dayOfWeek() {
        Schedule schedule = new Schedule("월간회의",
                LocalTime.of(14, 0),
                Duration.ofHours(1),
                DayOfWeek.MONDAY, 2);

        assertThat(schedule.check(LocalDate.of(2025, 1, 14))).isEmpty();
    }

    @Test
    public void weekly_includes() {
        Schedule schedule = new Schedule("데일리 스크럼",
                LocalTime.of(9, 0),
                Duration.ofMinutes(15),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY));

        assertThat(schedule.check(LocalDate.of(2025, 1, 13))).isNotEmpty();
    }

    @Test
    public void weekly_excludes_day_not_in_set() {
        Schedule schedule = new Schedule("데일리 스크럼",
                LocalTime.of(9, 0),
                Duration.ofMinutes(15),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY));

        assertThat(schedule.check(LocalDate.of(2025, 1, 15))).isEmpty();
    }

    @Test
    public void weekly_excludes_empty_dayOfWeeks() {
        Schedule schedule = new Schedule("데일리 스크럼",
                LocalTime.of(9, 0),
                Duration.ofMinutes(15),
                Set.of());

        assertThat(schedule.check(LocalDate.of(2025, 1, 13))).isEmpty();
    }

    @Test
    public void check() throws JSONException {
        Schedule schedule = new Schedule("월간회의", LocalTime.of(14, 0),
                Duration.ofHours(1), DayOfWeek.MONDAY, 2);

        JSONAssert.assertEquals("""
                {"title":"월간회의","from":"14:00","duration":60,
                "dayOfWeek":"MONDAY","ordinal":2}""",
                schedule.check(LocalDate.of(2025, 1, 13)),
                JSONCompareMode.LENIENT);
    }
}
