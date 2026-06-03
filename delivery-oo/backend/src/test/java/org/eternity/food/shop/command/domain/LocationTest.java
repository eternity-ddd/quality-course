package org.eternity.food.shop.command.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Location 값 객체")
class LocationTest {

    @Nested
    @DisplayName("null 검증")
    class NullInvariant {

        @Test
        @DisplayName("latitude가 null이면 IAE")
        void latitudeNull() {
            assertThatThrownBy(() -> new Location(null, 126.9780))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("좌표는 null");
        }

        @Test
        @DisplayName("longitude가 null이면 IAE")
        void longitudeNull() {
            assertThatThrownBy(() -> new Location(37.5665, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("좌표는 null");
        }

        @Test
        @DisplayName("둘 다 null이면 IAE")
        void bothNull() {
            assertThatThrownBy(() -> new Location(null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("좌표는 null");
        }
    }

    @Nested
    @DisplayName("latitude 범위 (-90 ~ 90)")
    class LatitudeRange {

        @ParameterizedTest(name = "latitude={0}이면 IAE")
        @ValueSource(doubles = {-90.0001, -91.0, 90.0001, 91.0, 180.0})
        @DisplayName("latitude가 -90~90 밖이면 IAE")
        void outOfRangeThrows(double latitude) {
            assertThatThrownBy(() -> new Location(latitude, 0.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("위도는 -90~90");
        }

        @ParameterizedTest(name = "latitude={0}이면 경계 통과")
        @ValueSource(doubles = {-90.0, -89.9999, 0.0, 89.9999, 90.0})
        @DisplayName("latitude가 -90~90 범위 안이면 통과")
        void inRangePasses(double latitude) {
            assertThatCode(() -> new Location(latitude, 0.0))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("longitude 범위 (-180 ~ 180)")
    class LongitudeRange {

        @ParameterizedTest(name = "longitude={0}이면 IAE")
        @ValueSource(doubles = {-180.0001, -181.0, 180.0001, 181.0, 360.0})
        @DisplayName("longitude가 -180~180 밖이면 IAE")
        void outOfRangeThrows(double longitude) {
            assertThatThrownBy(() -> new Location(0.0, longitude))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("경도는 -180~180");
        }

        @ParameterizedTest(name = "longitude={0}이면 경계 통과")
        @ValueSource(doubles = {-180.0, -179.9999, 0.0, 179.9999, 180.0})
        @DisplayName("longitude가 -180~180 범위 안이면 통과")
        void inRangePasses(double longitude) {
            assertThatCode(() -> new Location(0.0, longitude))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("정상 생성")
    class HappyPath {

        @ParameterizedTest(name = "({0}, {1})이면 정상 생성")
        @CsvSource({
                "37.5665, 126.9780",
                "-33.8688, 151.2093",
                "0.0, 0.0"
        })
        @DisplayName("유효 좌표면 latitude/longitude가 그대로 저장된다")
        void validCoordinatesStored(double lat, double lng) {
            Location location = new Location(lat, lng);
            assertThat(location.getLatitude()).isEqualTo(lat);
            assertThat(location.getLongitude()).isEqualTo(lng);
        }
    }
}
