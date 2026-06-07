package org.eternity.food.cart.command.domain;

import org.eternity.food.base.generic.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CartTest {

    @Test
    @DisplayName("getTotalPrice: 라인별 subtotal 합")
    void totalPrice_sumOfLines() {
        CartLineItem line1 = CartLineItem.builder()
                .menuId(1L).menuName("삼겹살").count(1).basePrice(Money.wons(10_000))
                .groups(List.of()).build();
        CartLineItem line2 = CartLineItem.builder()
                .menuId(2L).menuName("목살").count(2).basePrice(Money.wons(15_000))
                .groups(List.of()).build();
        Cart cart = Cart.builder().userId(1L).shopId(1L).items(List.of(line1, line2)).build();

        // 10,000×1 + 15,000×2 = 40,000
        assertThat(cart.getTotalPrice()).isEqualTo(Money.wons(40_000));
    }

    @Test
    @DisplayName("addItem: 다른 shopId 진입 → 기존 items.clear() + 새 shopId로 전환")
    void addItem_differentShop_clearsAndSwitches() {
        CartLineItem oldLine = CartLineItem.builder()
                .menuId(1L).menuName("삼겹살").count(1).basePrice(Money.wons(10_000))
                .groups(List.of()).build();
        Cart cart = Cart.builder().userId(1L).shopId(1L).items(List.of(oldLine)).build();

        CartLineItem newLine = CartLineItem.builder()
                .menuId(999L).menuName("초밥").count(1).basePrice(Money.wons(20_000))
                .groups(List.of(CartOptionGroup.builder()
                        .optionGroupId(999L).name("와사비")
                        .options(Set.of(CartOption.builder().name("추가").price(Money.wons(500)).build()))
                        .build()))
                .build();

        cart.addItem(200L, newLine);

        assertThat(cart.getShopId()).isEqualTo(200L);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getMenuId()).isEqualTo(999L);
    }
}
