package org.eternity.event.step03;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

public class ScheduleJson {
    private Schedule schedule;
    private ObjectMapper mapper;

    public ScheduleJson(Schedule schedule) {
        this.schedule = schedule;
        this.mapper = initializeMapper();
    }

    private ObjectMapper initializeMapper() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());
        mapper.configOverride(Duration.class).setFormat(JsonFormat.Value.forPattern("MINUTES"));
        mapper.configOverride(LocalTime.class).setFormat(JsonFormat.Value.forPattern("HH:mm"));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        return mapper;
    }

    public String check(LocalDate day) {
        try {
            if (schedule.includes(day)) {
                return mapper.writeValueAsString(schedule);
            }

            return "";
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
