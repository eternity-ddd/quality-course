package org.eternity.food.order.command.domain;

import org.eternity.food.base.generic.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderLineItemTest {

    @Test
    @DisplayName("subtotal: unitPrice * count")
    void subtotal_unitPriceTimesCount() {
        OrderLineItem line = new OrderLineItem(1L, "삼겹살", 3, 10_000L, List.of());

        assertThat(line.subtotal()).isEqualTo(Money.wons(30_000));
    }

    @Test
    @DisplayName("subtotal: count=1이면 unitPrice 그대로")
    void subtotal_singleCount() {
        OrderLineItem line = new OrderLineItem(1L, "삼겹살", 1, 22_000L, List.of());

        assertThat(line.subtotal()).isEqualTo(Money.wons(22_000));
    }

    @Test
    @DisplayName("subtotal: unitPrice=0이면 0")
    void subtotal_zeroPrice_isZero() {
        OrderLineItem line = new OrderLineItem(1L, "서비스", 5, 0L, List.of());

        assertThat(line.subtotal()).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("OrderOption.asMoney: long price를 Money로 변환")
    void orderOption_asMoney() {
        OrderLineItem.OrderOption opt = new OrderLineItem.OrderOption("소", 12_000L);

        assertThat(opt.asMoney()).isEqualTo(Money.wons(12_000));
    }

    @Test
    @DisplayName("record 동등성: 필드 동일하면 equal")
    void record_equality() {
        OrderLineItem a = new OrderLineItem(1L, "메뉴", 1, 1000L, List.of());
        OrderLineItem b = new OrderLineItem(1L, "메뉴", 1, 1000L, List.of());

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("OrderOptionGroup record 동등성")
    void orderOptionGroup_equality() {
        OrderLineItem.OrderOptionGroup a = new OrderLineItem.OrderOptionGroup("기본", List.of());
        OrderLineItem.OrderOptionGroup b = new OrderLineItem.OrderOptionGroup("기본", List.of());

        assertThat(a).isEqualTo(b);
    }
}
