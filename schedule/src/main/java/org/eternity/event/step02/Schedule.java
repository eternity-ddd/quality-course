package org.eternity.event.step02;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public class Schedule {
    private static final int DAYS_IN_WEEK = 7;

    private String title;
    private LocalTime from;
    private Duration duration;

    private Integer ordinal;
    private DayOfWeek dayOfWeek;

    private Set<DayOfWeek> dayOfWeeks;

    public Schedule(String title, LocalTime from, Duration duration, DayOfWeek dayOfWeek, Integer ordinal) {
        this.title = title;
        this.from = from;
        this.duration = duration;
        this.dayOfWeek = dayOfWeek;
        this.ordinal = ordinal;
    }

    public Schedule(String title, LocalTime from, Duration duration, Set<DayOfWeek> dayOfWeeks) {
        this.title = title;
        this.from = from;
        this.duration = duration;
        this.dayOfWeeks = dayOfWeeks;
    }

    public boolean includes(LocalDate day) {
        if (ordinal != null) {
            if (!day.getDayOfWeek().equals(dayOfWeek)) {
                return false;
            }

            return (day.getDayOfMonth() / DAYS_IN_WEEK) + 1 == ordinal;
        }

        return dayOfWeeks.contains(day.getDayOfWeek());
    }
}
