package org.eternity.event.step03;

import java.time.LocalDate;

public interface RecurringPlan {
    boolean includes(LocalDate day);
}
