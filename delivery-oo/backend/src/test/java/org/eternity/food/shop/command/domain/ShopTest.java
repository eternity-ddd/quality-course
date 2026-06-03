package org.eternity.food.shop.command.domain;

import org.eternity.food.Fixtures;
import org.eternity.food.base.generic.money.Money;
import org.eternity.food.base.generic.time.TimePeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Shop 도메인")
class ShopTest {

    @Nested
    @DisplayName("name 불변식")
    class NameInvariant {

        @Test
        @DisplayName("name이 null이면 IAE를 던진다")
        void nameNull() {
            assertThatThrownBy(() -> Fixtures.aShop().name(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("가게 이름은 5글자 이상");
        }

        @ParameterizedTest(name = "name 길이 {0}글자면 IAE")
        @ValueSource(strings = {"", "가", "가게", "가게1", "가게12"})
        @DisplayName("name이 5글자 미만(경계 미만)이면 IAE")
        void nameTooShort(String name) {
            assertThatThrownBy(() -> Fixtures.aShop().name(name).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("가게 이름은 5글자 이상");
        }

        @Test
        @DisplayName("name이 정확히 5글자(경계 통과)면 생성된다")
        void nameBoundaryPass() {
            Shop shop = Fixtures.aShop().name("오겹돼지집").build();
            assertThat(shop.getName()).isEqualTo("오겹돼지집");
        }

        @Test
        @DisplayName("name이 충분히 긴 정상 값이면 생성된다")
        void nameNormal() {
            Shop shop = Fixtures.aShop().name("매우긴가게이름입니다").build();
            assertThat(shop.getName()).isEqualTo("매우긴가게이름입니다");
        }
    }

    @Nested
    @DisplayName("minOrderPrice 불변식")
    class MinOrderPriceInvariant {

        @Test
        @DisplayName("minOrderPrice가 null이면 IAE를 던진다")
        void minOrderPriceNull() {
            assertThatThrownBy(() -> Fixtures.aShop().minOrderPrice(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("최소주문금액은 0원보다 커야");
        }

        @Test
        @DisplayName("minOrderPrice가 0원(경계 미만)이면 IAE")
        void minOrderPriceZero() {
            assertThatThrownBy(() -> Fixtures.aShop().minOrderPrice(Money.ZERO).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("최소주문금액은 0원보다 커야");
        }

        @Test
        @DisplayName("minOrderPrice가 음수면 IAE")
        void minOrderPriceNegative() {
            assertThatThrownBy(() -> Fixtures.aShop().minOrderPrice(Money.wons(-1)).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("최소주문금액은 0원보다 커야");
        }

        @Test
        @DisplayName("minOrderPrice가 1원(경계 바로 위)이면 생성된다")
        void minOrderPriceBoundaryPass() {
            Shop shop = Fixtures.aShop().minOrderPrice(Money.wons(1)).build();
            assertThat(shop.getMinOrderPrice()).isEqualTo(Money.wons(1));
        }

        @Test
        @DisplayName("minOrderPrice가 충분히 큰 정상 값이면 생성된다")
        void minOrderPriceNormal() {
            Shop shop = Fixtures.aShop().minOrderPrice(Money.wons(15000)).build();
            assertThat(shop.getMinOrderPrice()).isEqualTo(Money.wons(15000));
        }
    }

    @Nested
    @DisplayName("operationPeriod 불변식")
    class OperationPeriodInvariant {

        @ParameterizedTest
        @NullSource
        @DisplayName("operationPeriod가 null이면 IAE")
        void operationPeriodNull(TimePeriod period) {
            assertThatThrownBy(() -> Fixtures.aShop().operationPeriod(period).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("영업시간은 null");
        }

        @Test
        @DisplayName("operationPeriod가 정상 값이면 생성된다")
        void operationPeriodNormal() {
            TimePeriod period = TimePeriod.between(LocalTime.of(10, 0), LocalTime.of(20, 0));
            Shop shop = Fixtures.aShop().operationPeriod(period).build();
            assertThat(shop.getOperationPeriod()).isEqualTo(period);
        }
    }

    @Nested
    @DisplayName("isOpen 행위")
    class IsOpenBehavior {

        @Test
        @DisplayName("영업시간 내 시각이면 isOpen=true")
        void openWithinPeriod() {
            Shop shop = Fixtures.aShop()
                    .operationPeriod(TimePeriod.between(LocalTime.of(9, 0), LocalTime.of(22, 0)))
                    .build();
            assertThat(shop.isOpen(LocalDateTime.of(2026, 5, 29, 12, 0))).isTrue();
        }

        @Test
        @DisplayName("영업시간 시작점이면 isOpen=true (경계 포함)")
        void openOnStart() {
            Shop shop = Fixtures.aShop()
                    .operationPeriod(TimePeriod.between(LocalTime.of(9, 0), LocalTime.of(22, 0)))
                    .build();
            assertThat(shop.isOpen(LocalDateTime.of(2026, 5, 29, 9, 0))).isTrue();
        }

        @Test
        @DisplayName("영업 시작 전이면 isOpen=false")
        void closedBeforeStart() {
            Shop shop = Fixtures.aShop()
                    .operationPeriod(TimePeriod.between(LocalTime.of(9, 0), LocalTime.of(22, 0)))
                    .build();
            assertThat(shop.isOpen(LocalDateTime.of(2026, 5, 29, 8, 59))).isFalse();
        }
    }

    @Nested
    @DisplayName("정상 생성")
    class HappyPath {

        @Test
        @DisplayName("모든 invariant를 통과하면 정상 생성된다")
        void allFieldsValid() {
            assertThatCode(() -> Fixtures.aShop().build()).doesNotThrowAnyException();
        }
    }
}
