package org.eternity.food.cart.command.domain;

import org.eternity.food.base.generic.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eternity.food.Fixtures.aCartLineItem;
import static org.eternity.food.Fixtures.aCartOption;
import static org.eternity.food.Fixtures.aCartOptionGroup;

class CartLineItemTest {

    @Nested
    @DisplayName("구조 invariant - count")
    class Count {

        @Test
        @DisplayName("count=0은 IAE (경계 실패)")
        void count_zero_throws() {
            assertThatThrownBy(() -> aCartLineItem().count(0).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("1 이상");
        }

        @Test
        @DisplayName("count=-1은 IAE")
        void count_negative_throws() {
            assertThatThrownBy(() -> aCartLineItem().count(-1).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("count=1은 경계 통과")
        void count_one_ok() {
            CartLineItem line = aCartLineItem().count(1).build();
            assertThat(line.getMenuCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("count=10은 정상")
        void count_normal_ok() {
            CartLineItem line = aCartLineItem().count(10).build();
            assertThat(line.getMenuCount()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("changeQuantity - quantity ≥ 1")
    class ChangeQuantity {

        @Test
        @DisplayName("quantity=0은 IAE (경계 실패) — Cart가 0을 자동 제거로 처리하므로 라인엔 도달 안 함")
        void changeQuantity_zero_throws() {
            CartLineItem line = aCartLineItem().build();

            assertThatThrownBy(() -> line.changeQuantity(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("1 이상");
        }

        @Test
        @DisplayName("quantity=-1은 IAE")
        void changeQuantity_negative_throws() {
            CartLineItem line = aCartLineItem().build();

            assertThatThrownBy(() -> line.changeQuantity(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("quantity=1은 경계 통과")
        void changeQuantity_one_ok() {
            CartLineItem line = aCartLineItem().count(3).build();

            line.changeQuantity(1);

            assertThat(line.getMenuCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("quantity=5는 정상")
        void changeQuantity_positive_ok() {
            CartLineItem line = aCartLineItem().count(1).build();

            line.changeQuantity(5);

            assertThat(line.getMenuCount()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("matchesContent")
    class MatchesContent {

        @Test
        @DisplayName("menuId 다름 → false")
        void differentMenuId_false() {
            CartLineItem a = aCartLineItem().menuId(1L).build();
            CartLineItem b = aCartLineItem().menuId(2L).build();

            assertThat(a.matchesContent(b)).isFalse();
        }

        @Test
        @DisplayName("옵션 그룹 수 다름 → false")
        void differentGroupSize_false() {
            CartLineItem a = aCartLineItem()
                    .groups(List.of(aCartOptionGroup().id(1L).build()))
                    .build();
            CartLineItem b = aCartLineItem()
                    .groups(List.of(
                            aCartOptionGroup().id(1L).build(),
                            aCartOptionGroup().id(2L).optionGroupId(2L).name("매운맛")
                                    .options(Set.of(aCartOption().name("매움").build()))
                                    .build()))
                    .build();

            assertThat(a.matchesContent(b)).isFalse();
        }

        @Test
        @DisplayName("정확히 일치 → true")
        void exactMatch_true() {
            CartLineItem a = aCartLineItem().build();
            CartLineItem b = aCartLineItem().build();

            assertThat(a.matchesContent(b)).isTrue();
        }

        @Test
        @DisplayName("옵션 그룹 순서 무관(Set이므로) → true")
        void orderIndependent_true() {
            // groups Set은 DomainEntity id-based equality이므로 id 분리 필요
            CartOptionGroup g1a = aCartOptionGroup().id(1L).optionGroupId(1L).build();
            CartOptionGroup g2a = aCartOptionGroup()
                    .id(2L)
                    .optionGroupId(2L)
                    .name("매운맛")
                    .options(Set.of(aCartOption().name("매움").price(Money.wons(500)).build()))
                    .build();

            CartOptionGroup g1b = aCartOptionGroup().id(3L).optionGroupId(1L).build();
            CartOptionGroup g2b = aCartOptionGroup()
                    .id(4L)
                    .optionGroupId(2L)
                    .name("매운맛")
                    .options(Set.of(aCartOption().name("매움").price(Money.wons(500)).build()))
                    .build();

            CartLineItem a = aCartLineItem().groups(List.of(g1a, g2a)).build();
            CartLineItem b = aCartLineItem().groups(List.of(g2b, g1b)).build();

            assertThat(a.matchesContent(b)).isTrue();
        }

        @Test
        @DisplayName("그룹 내 옵션 가격 다름 → false")
        void differentOptionPrice_false() {
            CartLineItem a = aCartLineItem()
                    .groups(List.of(aCartOptionGroup()
                            .options(Set.of(aCartOption().price(Money.wons(12000)).build()))
                            .build()))
                    .build();
            CartLineItem b = aCartLineItem()
                    .groups(List.of(aCartOptionGroup()
                            .options(Set.of(aCartOption().price(Money.wons(15000)).build()))
                            .build()))
                    .build();

            assertThat(a.matchesContent(b)).isFalse();
        }
    }

    @Nested
    @DisplayName("combine")
    class Combine {

        @Test
        @DisplayName("같은 내용 합치기 → count 누적")
        void combine_accumulatesCount() {
            CartLineItem base = aCartLineItem().count(2).build();
            CartLineItem other = aCartLineItem().count(3).build();

            base.combine(other);

            assertThat(base.getMenuCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("combine 후 기존 라인 identity는 그대로(id 보존)")
        void combine_preservesIdentity() {
            CartLineItem base = aCartLineItem().id(7L).count(1).build();
            CartLineItem other = aCartLineItem().id(null).count(4).build();

            base.combine(other);

            assertThat(base.getId()).isEqualTo(7L);
            assertThat(base.getMenuCount()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("subtotal")
    class Subtotal {

        @Test
        @DisplayName("(basePrice + 옵션) × count")
        void subtotal_basePricePlusOptionsTimesCount() {
            // aCartLineItem 기본: basePrice=10,000 + aCartOption 가격=12,000
            CartLineItem line = aCartLineItem()
                    .basePrice(Money.wons(10_000))
                    .count(3)
                    .build();

            // (10,000 + 12,000) × 3 = 66,000
            assertThat(line.subtotal()).isEqualTo(Money.wons(66_000));
        }
    }
}
