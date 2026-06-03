package org.eternity.food.cart.command.domain;

import org.eternity.food.base.generic.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eternity.food.Fixtures.aCartOption;

class CartOptionTest {

    @Test
    @DisplayName("name이 null이면 IAE")
    void name_null_throws() {
        assertThatThrownBy(() -> aCartOption().name(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    @DisplayName("name이 blank이면 IAE")
    void name_blank_throws() {
        assertThatThrownBy(() -> aCartOption().name("  ").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    @DisplayName("name이 있으면 정상 생성")
    void name_present_ok() {
        CartOption opt = aCartOption().name("소(250g)").build();

        assertThat(opt.getName()).isEqualTo("소(250g)");
    }

    @Test
    @DisplayName("동등성: (name, price) 기반 — 둘 다 같으면 동등")
    void equality_byPair() {
        CartOption a = aCartOption().name("소").price(Money.wons(12000)).build();
        CartOption b = aCartOption().name("소").price(Money.wons(12000)).build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("동등성: name이 다르면 not equal")
    void equality_differentName() {
        CartOption a = aCartOption().name("소").build();
        CartOption b = aCartOption().name("대").build();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("동등성: price가 다르면 not equal")
    void equality_differentPrice() {
        CartOption a = aCartOption().price(Money.wons(1000)).build();
        CartOption b = aCartOption().price(Money.wons(2000)).build();

        assertThat(a).isNotEqualTo(b);
    }
}
