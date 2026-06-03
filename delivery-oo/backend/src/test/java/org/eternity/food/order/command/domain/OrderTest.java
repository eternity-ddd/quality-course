package org.eternity.food.order.command.domain;

import org.eternity.food.base.generic.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eternity.food.Fixtures.anOrder;
import static org.eternity.food.Fixtures.anOrderLineItem;
import static org.eternity.food.Fixtures.anOrderOptionGroup;

class OrderTest {

    @Nested
    @DisplayName("new Order(userId, shopId, items) — placement 생성자")
    class PlacementConstructor {

        @Test
        @DisplayName("totalPrice가 items.subtotal()의 합으로 자동 계산")
        void totalPrice_isAutoComputed() {
            OrderLineItem line1 = new OrderLineItem(1L, "삼겹살", 2, 10_000L, List.of(anOrderOptionGroup()));
            OrderLineItem line2 = new OrderLineItem(2L, "김치찌개", 1, 8_000L, List.of(anOrderOptionGroup()));

            Order order = new Order(100L, 1L, List.of(line1, line2));

            assertThat(order.getPrice()).isEqualTo(Money.wons(28_000));
        }

        @Test
        @DisplayName("orderedTime이 LocalDateTime.now() 부근으로 자동 설정")
        void orderedTime_isNow() {
            LocalDateTime before = LocalDateTime.now();
            Order order = new Order(100L, 1L, List.of(anOrderLineItem()));
            LocalDateTime after = LocalDateTime.now();

            assertThat(order.getOrderedTime()).isBetween(before, after);
        }

        @Test
        @DisplayName("userId/shopId가 그대로 보존")
        void preservesUserIdShopId() {
            Order order = new Order(100L, 1L, List.of(anOrderLineItem()));

            assertThat(order.getUserId()).isEqualTo(100L);
            assertThat(order.getShopId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("items는 OrderItems.of(...)로 wrap되고 getItems()로 노출")
        void items_wrappedAsOrderItems() {
            OrderLineItem line = anOrderLineItem();

            Order order = new Order(100L, 1L, List.of(line));

            assertThat(order.getItems()).containsExactly(line);
        }

        @Test
        @DisplayName("totalPrice: 빈 items면 ZERO")
        void totalPrice_empty_isZero() {
            Order order = new Order(100L, 1L, List.of());

            assertThat(order.getPrice()).isEqualTo(Money.ZERO);
        }
    }

    @Nested
    @DisplayName("@Builder build()")
    class BuilderConstructor {

        @Test
        @DisplayName("totalPrice를 직접 지정 (자동 계산 안 함)")
        void totalPrice_explicit() {
            Order order = anOrder().totalPrice(Money.wons(99_999)).build();

            assertThat(order.getPrice()).isEqualTo(Money.wons(99_999));
        }

        @Test
        @DisplayName("orderedTime을 직접 지정")
        void orderedTime_explicit() {
            LocalDateTime fixed = LocalDateTime.of(2020, 1, 1, 12, 0);
            Order order = anOrder().orderedTime(fixed).build();

            assertThat(order.getOrderedTime()).isEqualTo(fixed);
        }

        @Test
        @DisplayName("items가 그대로 OrderItems로 wrap")
        void items_wrapped() {
            OrderLineItem line = anOrderLineItem();
            Order order = anOrder().items(List.of(line)).build();

            assertThat(order.getItems()).containsExactly(line);
        }

        @Test
        @DisplayName("id가 builder로 set 가능")
        void id_set() {
            Order order = anOrder().id(7L).build();

            assertThat(order.getId()).isEqualTo(7L);
        }
    }

    @Test
    @DisplayName("getItems()는 unmodifiable view")
    void getItems_unmodifiable() {
        Order order = anOrder().build();
        List<OrderLineItem> items = order.getItems();

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                items.add(anOrderLineItem()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
