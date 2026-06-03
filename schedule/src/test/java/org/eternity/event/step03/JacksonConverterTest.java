package org.eternity.event.step03;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;

public class JacksonConverterTest {
    @Test
    public void toJson() throws JSONException {
        Schedule schedule = new Schedule("월간회의", LocalTime.of(14, 0), Duration.ofHours(1), new MonthlyPlan(DayOfWeek.MONDAY, 2));

        JSONAssert.assertEquals("""
                {"title":"월간회의","from":"14:00","duration":60,"plan":{"dayOfWeek":"MONDAY","ordinal":2}}""",
                new JacksonConverter().toJson(schedule),
                JSONCompareMode.LENIENT);
    }
}
