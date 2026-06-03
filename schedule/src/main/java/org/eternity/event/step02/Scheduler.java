package org.eternity.event.step02;

import java.time.LocalDate;

public class Scheduler {
    private JsonConverter converter;

    public Scheduler(JsonConverter converter) {
        this.converter = converter;
    }

    public String check(Schedule schedule, LocalDate day) {
        if (schedule.includes(day)) {
            return converter.toJson(schedule);
        }

        return "";
    }
}
