package org.eternity.event.step03;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// 2025년 1월 캘린더
// 월  화  수  목  금  토  일
//           1   2   3   4   5
//  6   7   8   9  10  11  12
// 13  14  15  16  17  18  19
// 20  21  22  23  24  25  26
// 27  28  29  30  31

public class MonthlyPlanTest {
    @Test
    public void includes() {
        MonthlyPlan plan = new MonthlyPlan(DayOfWeek.MONDAY, 2);
        assertThat(plan.includes(LocalDate.of(2025, 1, 13))).isTrue();
    }

    @Test
    public void excludes_wrong_dayOfWeek() {
        MonthlyPlan plan = new MonthlyPlan(DayOfWeek.MONDAY, 2);
        assertThat(plan.includes(LocalDate.of(2025, 1, 14))).isFalse();
    }

    @Test
    public void excludes_wrong_ordinal() {
        MonthlyPlan plan = new MonthlyPlan(DayOfWeek.MONDAY, 2);
        assertThat(plan.includes(LocalDate.of(2025, 1, 20))).isFalse();
    }
}
