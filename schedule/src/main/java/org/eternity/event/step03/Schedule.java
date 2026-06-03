package org.eternity.event.step03;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

public class Schedule {
    private String title;
    private LocalTime from;
    private Duration duration;
    private RecurringPlan plan;

    public Schedule(String title, LocalTime from, Duration duration, RecurringPlan plan) {
        this.title = title;
        this.from = from;
        this.duration = duration;
        this.plan = plan;
    }

    public boolean includes(LocalDate day) {
        return plan.includes(day);
    }
}
