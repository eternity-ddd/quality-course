package org.eternity.food.cart.command.domain;

import org.eternity.food.base.generic.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eternity.food.Fixtures.aCart;
import static org.eternity.food.Fixtures.aCartLineItem;
import static org.eternity.food.Fixtures.aCartOption;
import static org.eternity.food.Fixtures.aCartOptionGroup;

class CartTest {

    @Test
    @DisplayName("getTotalPrice: 라인별 subtotal 합")
    void totalPrice_sumOfLines() {
        Cart cart = aCart().build();

        Money expected = cart.getItems().stream()
                .map(CartLineItem::subtotal)
                .reduce(Money.ZERO, Money::plus);

        assertThat(cart.getTotalPrice()).isEqualTo(expected);
    }

    @Test
    @DisplayName("addItem: 다른 shopId 진입 → 기존 items.clear() + 새 shopId로 전환")
    void addItem_differentShop_clearsAndSwitches() {
        Cart cart = aCart().build();
        Long oldShopId = cart.getShopId();
        CartLineItem newLine = aCartLineItem()
                .id(99L)
                .menuId(999L)
                .menuName("초밥")
                .groups(List.of(aCartOptionGroup()
                        .id(99L)
                        .optionGroupId(999L)
                        .options(java.util.Set.of(aCartOption().name("특수옵션").build()))
                        .build()))
                .build();
        Long newShopId = oldShopId + 100;

        cart.addItem(newShopId, newLine);

        assertThat(cart.getShopId()).isEqualTo(newShopId);
        assertThat(cart.getItems()).containsExactly(newLine);
    }
}
