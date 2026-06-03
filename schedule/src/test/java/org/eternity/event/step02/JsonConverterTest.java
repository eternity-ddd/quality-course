package org.eternity.event.step02;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;

public class JsonConverterTest {
    @Test
    public void toJson() throws JSONException {
        Schedule schedule = new Schedule("월간회의", LocalTime.of(14, 0), Duration.ofHours(1), DayOfWeek.MONDAY, 2);

        JSONAssert.assertEquals("""
                {"title":"월간회의","from":"14:00","duration":60,"dayOfWeek":"MONDAY","ordinal":2}""",
                new JsonConverter().toJson(schedule),
                JSONCompareMode.LENIENT);
    }
}
