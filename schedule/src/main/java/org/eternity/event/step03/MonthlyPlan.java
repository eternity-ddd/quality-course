package org.eternity.event.step03;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class MonthlyPlan implements RecurringPlan {
    private static final int DAYS_IN_WEEK = 7;

    private DayOfWeek dayOfWeek;
    private Integer ordinal;

    public MonthlyPlan(DayOfWeek dayOfWeek, Integer ordinal) {
        this.ordinal = ordinal;
        this.dayOfWeek = dayOfWeek;
    }

    @Override
    public boolean includes(LocalDate day) {
        if (!day.getDayOfWeek().equals(dayOfWeek)) {
            return false;
        }

        return (day.getDayOfMonth() / DAYS_IN_WEEK) + 1 == ordinal;
    }
}
