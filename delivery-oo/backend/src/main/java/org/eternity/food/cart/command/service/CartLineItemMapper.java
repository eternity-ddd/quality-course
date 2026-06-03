package org.eternity.food.cart.command.service;

import org.eternity.food.base.generic.money.Money;
import org.eternity.food.cart.command.domain.CartLineItem;
import org.eternity.food.cart.command.domain.CartOption;
import org.eternity.food.cart.command.domain.CartOptionGroup;
import org.eternity.food.cart.command.service.CartLineItemCommand.CartOptionCommand;
import org.eternity.food.cart.command.service.CartLineItemCommand.CartOptionGroupCommand;
import org.eternity.food.shop.command.domain.Menu;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Command → CartLineItem 순수 변환. 검증/조회 책임 없음.
 *
 * <p>담기 시점에 별도 검증 없음. 잘못된 데이터는 조회 시점에 {@code CartReconciler}가 안내하고,
 * 주문 시점에 {@code OrderableCartInvariant}가 strict 검증으로 차단.
 */
public final class CartLineItemMapper {

    private CartLineItemMapper() {
    }

    public static CartLineItem map(Menu menu, CartLineItemCommand command) {
        List<CartOptionGroup> groups = command.optionGroups().stream()
                .map(CartLineItemMapper::toCartOptionGroup)
                .toList();

        Money optionsTotal = groups.stream()
                .map(CartOptionGroup::getTotalPrice)
                .reduce(Money.ZERO, Money::plus);

        Money unitPrice = menu.getBasePrice().plus(optionsTotal);

        return new CartLineItem(menu.getId(), menu.getName(), command.count(), unitPrice, groups);
    }

    private static CartOptionGroup toCartOptionGroup(CartOptionGroupCommand g) {
        Set<CartOption> options = g.options().stream()
                .map(CartLineItemMapper::toCartOption)
                .collect(Collectors.toSet());
        return new CartOptionGroup(g.optionGroupId(), g.optionGroupName(), options);
    }

    private static CartOption toCartOption(CartOptionCommand o) {
        return new CartOption(o.name(), Money.wons(o.price()));
    }
}
