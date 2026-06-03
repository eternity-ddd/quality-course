package org.eternity.food.base.generic.time;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.ToString;
import org.eternity.food.base.domain.ValueObject;

import java.time.LocalTime;

@Embeddable
@ToString
public class TimePeriod extends ValueObject<TimePeriod> {

    @Column(name = "START_TIME")
    private LocalTime startTime;

    @Column(name = "END_TIME")
    private LocalTime endTime;

    public static TimePeriod between(LocalTime startTime, LocalTime endTime) {
        return new TimePeriod(startTime, endTime);
    }

    public TimePeriod(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("시작 시간과 종료 시간은 null이서는 안됩니다.");
        }

        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new IllegalArgumentException("시작 시간은 종료 시간보다 이전이어야 합니다.");
        }

        this.startTime = startTime;
        this.endTime = endTime;
    }

    protected TimePeriod() {
    }

    public boolean contains(LocalTime datetime) {
        return (datetime.isAfter(startTime) || datetime.equals(startTime)) &&
               (datetime.isBefore(endTime) || datetime.equals(endTime));
    }
}
