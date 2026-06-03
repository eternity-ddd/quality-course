package org.eternity.food.shop.command.domain;

import org.eternity.food.Fixtures;
import org.eternity.food.base.generic.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Option 도메인")
class OptionTest {

    @Nested
    @DisplayName("name 불변식")
    class NameInvariant {

        @Test
        @DisplayName("name이 null이면 IAE를 던진다")
        void nameNull() {
            assertThatThrownBy(() -> Fixtures.anOption().name(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션명은 2글자 이상");
        }

        @ParameterizedTest(name = "name 길이 {0}이면 IAE")
        @ValueSource(strings = {"", "가"})
        @DisplayName("name이 2글자 미만이면 IAE")
        void nameTooShort(String name) {
            assertThatThrownBy(() -> Fixtures.anOption().name(name).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션명은 2글자 이상");
        }

        @Test
        @DisplayName("name이 정확히 2글자(경계 통과)면 생성된다")
        void nameBoundaryPass() {
            Option option = Fixtures.anOption().name("소소").build();
            assertThat(option.getName()).isEqualTo("소소");
        }

        @Test
        @DisplayName("name이 정상 값이면 생성된다")
        void nameNormal() {
            Option option = Fixtures.anOption().name("소(250g)").build();
            assertThat(option.getName()).isEqualTo("소(250g)");
        }
    }

    @Nested
    @DisplayName("price 불변식")
    class PriceInvariant {

        @Test
        @DisplayName("price가 null이면 IAE를 던진다")
        void priceNull() {
            assertThatThrownBy(() -> Fixtures.anOption().price(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션 가격은 null");
        }

        @Test
        @DisplayName("price가 음수면 IAE를 던진다")
        void priceNegative() {
            assertThatThrownBy(() -> Fixtures.anOption().price(Money.wons(-1)).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션 가격은 0원 이상");
        }

        @Test
        @DisplayName("price가 0원(경계 통과)이면 생성된다")
        void priceZero() {
            Option option = Fixtures.anOption().price(Money.ZERO).build();
            assertThat(option.getPrice()).isEqualTo(Money.ZERO);
        }

        @Test
        @DisplayName("price가 양수 정상 값이면 생성된다")
        void priceNormal() {
            Option option = Fixtures.anOption().price(Money.wons(5000)).build();
            assertThat(option.getPrice()).isEqualTo(Money.wons(5000));
        }
    }

    @Nested
    @DisplayName("rename 행위")
    class RenameBehavior {

        @Test
        @DisplayName("새 이름이 2글자 미만이면 IAE")
        void renameTooShort() {
            Option option = Fixtures.anOption().build();
            assertThatThrownBy(() -> option.rename("가"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션명은 2글자 이상");
        }

        @Test
        @DisplayName("새 이름이 null이면 IAE")
        void renameNull() {
            Option option = Fixtures.anOption().build();
            assertThatThrownBy(() -> option.rename(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션명은 2글자 이상");
        }

        @Test
        @DisplayName("정상적인 새 이름으로 변경 가능")
        void renameNormal() {
            Option option = Fixtures.anOption().build();
            option.rename("중(350g)");
            assertThat(option.getName()).isEqualTo("중(350g)");
        }
    }

    @Nested
    @DisplayName("changePrice 행위")
    class ChangePriceBehavior {

        @Test
        @DisplayName("새 가격이 null이면 IAE")
        void changePriceNull() {
            Option option = Fixtures.anOption().build();
            assertThatThrownBy(() -> option.changePrice(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션 가격은 null");
        }

        @Test
        @DisplayName("새 가격이 음수면 IAE")
        void changePriceNegative() {
            Option option = Fixtures.anOption().build();
            assertThatThrownBy(() -> option.changePrice(Money.wons(-100)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션 가격은 0원 이상");
        }

        @Test
        @DisplayName("새 가격이 0원이면 변경 가능")
        void changePriceZero() {
            Option option = Fixtures.anOption().build();
            option.changePrice(Money.ZERO);
            assertThat(option.getPrice()).isEqualTo(Money.ZERO);
        }

        @Test
        @DisplayName("새 가격이 양수면 변경 가능")
        void changePriceNormal() {
            Option option = Fixtures.anOption().build();
            option.changePrice(Money.wons(15000));
            assertThat(option.getPrice()).isEqualTo(Money.wons(15000));
        }
    }

    @Nested
    @DisplayName("동등성 (id 기반)")
    class Equality {

        @Test
        @DisplayName("동일한 id면 equals=true")
        void sameIdEquals() {
            Option a = Fixtures.anOption().id(100L).name("이름1").build();
            Option b = Fixtures.anOption().id(100L).name("이름2").price(Money.wons(9000)).build();
            assertThat(a).isEqualTo(b);
        }

        @Test
        @DisplayName("id가 다르면 equals=false")
        void differentIdNotEquals() {
            Option a = Fixtures.anOption().id(100L).build();
            Option b = Fixtures.anOption().id(200L).build();
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("id가 null이면 equals=false")
        void nullIdNotEquals() {
            Option a = Fixtures.anOption().id(null).build();
            Option b = Fixtures.anOption().id(null).build();
            assertThat(a).isNotEqualTo(b);
        }
    }
}
