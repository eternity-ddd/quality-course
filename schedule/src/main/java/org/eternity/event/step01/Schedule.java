package org.eternity.event.step01;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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

    public Schedule(String title, LocalTime from, Duration duration,
                    DayOfWeek dayOfWeek, Integer ordinal) {
        this.title = title;
        this.from = from;
        this.duration = duration;
        this.dayOfWeek = dayOfWeek;
        this.ordinal = ordinal;
    }

    public Schedule(String name, LocalTime from, Duration duration,
                    Set<DayOfWeek> dayOfWeeks) {
        this.title = name;
        this.from = from;
        this.duration = duration;
        this.dayOfWeeks = dayOfWeeks;
    }

    public String check(LocalDate day) {
        try {
            if (includes(day)) {
                return toJson();
            }

            return "";
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean includes(LocalDate day) {
        if (ordinal != null) {
            if (!day.getDayOfWeek().equals(dayOfWeek)) {
                return false;
            }

            return (day.getDayOfMonth() / DAYS_IN_WEEK) + 1 == ordinal;
        }

        return dayOfWeeks.contains(day.getDayOfWeek());
    }

    private String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configOverride(Duration.class).setFormat(JsonFormat.Value.forPattern("MINUTES"));
        mapper.configOverride(LocalTime.class).setFormat(JsonFormat.Value.forPattern("HH:mm"));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        return mapper.writeValueAsString(this);
    }
}
