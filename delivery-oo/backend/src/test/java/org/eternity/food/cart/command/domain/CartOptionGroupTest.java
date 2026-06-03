package org.eternity.food.cart.command.domain;

import org.eternity.food.base.generic.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eternity.food.Fixtures.aCartOption;
import static org.eternity.food.Fixtures.aCartOptionGroup;

class CartOptionGroupTest {

    @Test
    @DisplayName("optionGroupId가 null이면 IAE")
    void optionGroupId_null_throws() {
        assertThatThrownBy(() -> aCartOptionGroup().optionGroupId(null).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("optionGroupId");
    }

    @Test
    @DisplayName("optionGroupId가 있으면 정상")
    void optionGroupId_present_ok() {
        CartOptionGroup g = aCartOptionGroup().build();

        assertThat(g.getOptionGroupId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("matchesContent: optionGroupId 다름 → false")
    void matches_differentGroupId_false() {
        CartOptionGroup a = aCartOptionGroup().optionGroupId(1L).build();
        CartOptionGroup b = aCartOptionGroup().optionGroupId(2L).build();

        assertThat(a.matchesContent(b)).isFalse();
    }

    @Test
    @DisplayName("matchesContent: 같은 그룹 ID + 동일 옵션 Set → true")
    void matches_sameGroupAndOptions_true() {
        CartOptionGroup a = aCartOptionGroup().build();
        CartOptionGroup b = aCartOptionGroup().build();

        assertThat(a.matchesContent(b)).isTrue();
    }

    @Test
    @DisplayName("matchesContent: 옵션 Set 다름 → false")
    void matches_differentOptions_false() {
        CartOptionGroup a = aCartOptionGroup()
                .options(Set.of(aCartOption().build()))
                .build();
        CartOptionGroup b = aCartOptionGroup()
                .options(Set.of(aCartOption().name("매움").build()))
                .build();

        assertThat(a.matchesContent(b)).isFalse();
    }

    @Test
    @DisplayName("getTotalPrice: 옵션 가격 합")
    void totalPrice_sumOfOptions() {
        CartOptionGroup g = aCartOptionGroup()
                .options(Set.of(
                        aCartOption().name("소").price(Money.wons(1000)).build(),
                        aCartOption().name("대").price(Money.wons(2000)).build()))
                .build();

        assertThat(g.getTotalPrice()).isEqualTo(Money.wons(3000));
    }

    @Test
    @DisplayName("getTotalPrice: 옵션 없으면 0")
    void totalPrice_empty_isZero() {
        CartOptionGroup g = aCartOptionGroup()
                .options(Set.of())
                .build();

        assertThat(g.getTotalPrice()).isEqualTo(Money.ZERO);
    }
}
