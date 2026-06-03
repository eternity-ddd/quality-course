package org.eternity.food.order.command.domain;

import org.eternity.food.base.generic.money.Money;

import java.util.List;

public record OrderLineItem(
        Long menuId,
        String menuName,
        int count,
        long unitPrice,
        List<OrderOptionGroup> groups
) {
    public Money subtotal() {
        return Money.wons(unitPrice).times(count);
    }

    public record OrderOptionGroup(
            String name,
            List<OrderOption> options) {
    }

    public record OrderOption(
            String name,
            long price) {
        public Money asMoney() {
            return Money.wons(price);
        }
    }
}
