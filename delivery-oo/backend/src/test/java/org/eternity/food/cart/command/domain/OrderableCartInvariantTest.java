package org.eternity.food.cart.command.domain;

import org.eternity.food.base.domain.validation.Check;
import org.eternity.food.base.generic.money.Money;
import org.eternity.food.shop.command.domain.Menu;
import org.eternity.food.shop.command.domain.MenuStatus;
import org.eternity.food.shop.command.domain.Option;
import org.eternity.food.shop.command.domain.OptionGroup;
import org.eternity.food.shop.command.domain.Shop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eternity.food.Fixtures.aCart;
import static org.eternity.food.Fixtures.aCartLineItem;
import static org.eternity.food.Fixtures.aCartOption;
import static org.eternity.food.Fixtures.aCartOptionGroup;
import static org.eternity.food.Fixtures.aMenu;
import static org.eternity.food.Fixtures.aShop;
import static org.eternity.food.Fixtures.anOption;
import static org.eternity.food.Fixtures.anOptionGroup;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderableCartInvariantTest {

    private Shop openShop;
    private Menu openMenu;
    private OptionGroup optionGroup;

    @BeforeEach
    void setUp() {
        openShop = mock(Shop.class);
        given(openShop.getId()).willReturn(1L);
        given(openShop.isOpen()).willReturn(true);
        given(openShop.getMinOrderPrice()).willReturn(Money.wons(13_000));

        openMenu = aMenu().status(MenuStatus.OPEN).build();
        optionGroup = anOptionGroup()
                .options(Set.of(
                        anOption().id(1L).name("소(250g)").price(Money.wons(12000)).build(),
                        anOption().id(2L).name("대(500g)").price(Money.wons(20000)).build()))
                .build();
    }

    private OrderableCartInvariant invariant(Shop shop) {
        return new OrderableCartInvariant(shop, List.of(openMenu), List.of(optionGroup));
    }

    private OrderableCartInvariant invariant(Shop shop, Menu menu) {
        return new OrderableCartInvariant(shop, List.of(menu), List.of(optionGroup));
    }

    private OrderableCartInvariant invariant(Shop shop, Menu menu, OptionGroup og) {
        return new OrderableCartInvariant(shop, List.of(menu), List.of(og));
    }

    @Nested
    @DisplayName("Rule 1: Cart 비어있지 않음")
    class CartNotEmpty {

        @Test
        @DisplayName("빈 Cart → 실패")
        void emptyCart_fails() {
            Cart cart = Cart.forUser(1L);
            Check result = invariant(openShop).check(cart);

            assertThat(result.passed()).isFalse();
            assertThat(result.reason()).contains("비어");
            assertThatThrownBy(result::require)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("아이템이 있는 정상 Cart → 통과")
        void nonEmptyCart_passes() {
            Cart cart = orderableCart();
            Check result = invariant(openShop).check(cart);

            assertThat(result.passed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Rule 2: shopId 일치")
    class ShopIdMatch {

        @Test
        @DisplayName("cart.shopId null → 실패")
        void nullShopId_fails() {
            Cart cart = orderableCart();
            cart.clear();
            cart.addItem(null, aCartLineItem().id(null).build());

            Check result = invariant(openShop).check(cart);

            assertThat(result.passed()).isFalse();
            assertThat(result.reason()).contains("가게");
        }

        @Test
        @DisplayName("cart.shopId != shop.id → 실패")
        void differentShopId_fails() {
            Cart cart = aCart().shopId(999L).build();

            Check result = invariant(openShop).check(cart);

            assertThat(result.passed()).isFalse();
        }

        @Test
        @DisplayName("cart.shopId == shop.id → 통과")
        void sameShopId_passes() {
            Cart cart = orderableCart();
            Check result = invariant(openShop).check(cart);

            assertThat(result.passed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Rule 3: shop.isOpen()")
    class ShopOpen {

        @Test
        @DisplayName("shop이 닫힘 → 실패")
        void closedShop_fails() {
            Shop closedShop = mock(Shop.class);
            given(closedShop.getId()).willReturn(1L);
            given(closedShop.isOpen()).willReturn(false);

            Cart cart = orderableCart();
            Check result = invariant(closedShop).check(cart);

            assertThat(result.passed()).isFalse();
            assertThat(result.reason()).contains("영업");
        }

        @Test
        @DisplayName("shop이 열림 → 통과")
        void openShop_passes() {
            Cart cart = orderableCart();
            Check result = invariant(openShop).check(cart);

            assertThat(result.passed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Rule 4: totalPrice ≥ minOrderPrice")
    class MinOrderPrice {

        @Test
        @DisplayName("totalPrice < minOrderPrice → 실패")
        void belowMin_fails() {
            given(openShop.getMinOrderPrice()).willReturn(Money.wons(100_000));
            Cart cart = orderableCart();

            Check result = invariant(openShop).check(cart);

            assertThat(result.passed()).isFalse();
            assertThat(result.reason()).contains("최소");
        }

        @Test
        @DisplayName("totalPrice == minOrderPrice → 경계 통과")
        void atMin_passes() {
            Cart cart = orderableCart();
            given(openShop.getMinOrderPrice()).willReturn(cart.getTotalPrice());

            Check result = invariant(openShop).check(cart);

            assertThat(result.passed()).isTrue();
        }

        @Test
        @DisplayName("totalPrice > minOrderPrice → 통과")
        void aboveMin_passes() {
            Cart cart = orderableCart();
            given(openShop.getMinOrderPrice()).willReturn(Money.wons(1));

            Check result = invariant(openShop).check(cart);

            assertThat(result.passed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Rule 5: 메뉴/옵션 id 기반 매칭 + 가격 일치")
    class CatalogMatching {

        @Test
        @DisplayName("메뉴 ID가 카탈로그에 없음 → 실패")
        void menuNotFound_fails() {
            Cart cart = aCart()
                    .items(List.of(aCartLineItem()
                            .menuId(999L)
                            .basePrice(Money.wons(15_000))
                            .build()))
                    .build();

            Check result = invariant(openShop).check(cart);

            assertThat(result.passed()).isFalse();
            assertThat(result.reason()).contains("메뉴");
        }

        @Test
        @DisplayName("메뉴가 판매중이 아님 (READY) → 실패")
        void menuNotOpen_fails() {
            Menu readyMenu = aMenu().status(MenuStatus.READY).build();
            Cart cart = orderableCart();

            Check result = invariant(openShop, readyMenu).check(cart);

            assertThat(result.passed()).isFalse();
            assertThat(result.reason()).contains("판매중");
        }

        @Test
        @DisplayName("옵션그룹이 카탈로그에 없음 → 실패")
        void optionGroupNotFound_fails() {
            Cart cart = aCart()
                    .items(List.of(aCartLineItem()
                            .basePrice(Money.wons(15_000))
                            .groups(List.of(aCartOptionGroup().optionGroupId(999L).build()))
                            .build()))
                    .build();

            Check result = invariant(openShop).check(cart);

            assertThat(result.passed()).isFalse();
            assertThat(result.reason()).contains("옵션 그룹");
        }

        @Test
        @DisplayName("옵션그룹이 메뉴의 OptionGroupIds에 없음 → 실패")
        void groupNotInMenu_fails() {
            OptionGroup foreignGroup = anOptionGroup()
                    .id(777L)
                    .options(Set.of(
                            anOption().id(1L).name("소(250g)").price(Money.wons(12000)).build(),
                            anOption().id(2L).name("대(500g)").price(Money.wons(20000)).build()))
                    .build();
            Cart cart = aCart()
                    .items(List.of(aCartLineItem()
                            .basePrice(Money.wons(15_000))
                            .groups(List.of(aCartOptionGroup().optionGroupId(777L).build()))
                            .build()))
                    .build();

            Check result = invariant(openShop, openMenu, foreignGroup).check(cart);

            assertThat(result.passed()).isFalse();
        }

        @Test
        @DisplayName("옵션 이름이 옵션그룹에 없음 → 실패")
        void optionNameNotFound_fails() {
            Cart cart = aCart()
                    .items(List.of(aCartLineItem()
                            .basePrice(Money.wons(15_000))
                            .groups(List.of(aCartOptionGroup()
                                    .options(Set.of(aCartOption().name("존재하지않는옵션").build()))
                                    .build()))
                            .build()))
                    .build();

            Check result = invariant(openShop).check(cart);

            assertThat(result.passed()).isFalse();
            assertThat(result.reason()).contains("옵션");
        }

        @Test
        @DisplayName("옵션 가격이 다름 → 실패")
        void optionPriceMismatch_fails() {
            Cart cart = aCart()
                    .items(List.of(aCartLineItem()
                            .basePrice(Money.wons(15_000))
                            .groups(List.of(aCartOptionGroup()
                                    .options(Set.of(aCartOption()
                                            .price(Money.wons(99999))
                                            .build()))
                                    .build()))
                            .build()))
                    .build();

            Check result = invariant(openShop).check(cart);

            assertThat(result.passed()).isFalse();
            assertThat(result.reason()).contains("가격");
        }

        @Test
        @DisplayName("모든 카탈로그 매칭 + 가격 일치 → 통과")
        void allMatch_passes() {
            Cart cart = orderableCart();
            Check result = invariant(openShop).check(cart);

            assertThat(result.passed()).isTrue();
            assertThatCode(result::require).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Check → require()")
    class CheckRequire {

        @Test
        @DisplayName("실패면 require()가 ISE throw")
        void failedRequire_throws() {
            Cart empty = Cart.forUser(1L);

            Check result = invariant(openShop).check(empty);

            assertThatThrownBy(result::require)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("통과면 require() 정상")
        void passedRequire_ok() {
            Cart cart = orderableCart();

            assertThatCode(() -> invariant(openShop).check(cart).require())
                    .doesNotThrowAnyException();
        }
    }

    // Helper: 모든 invariant을 통과하는 카트
    private Cart orderableCart() {
        return aCart()
                .items(List.of(aCartLineItem()
                        .menuId(1L)
                        .basePrice(Money.wons(15_000))
                        .count(1)
                        .groups(List.of(aCartOptionGroup()
                                .optionGroupId(1L)
                                .options(Set.of(aCartOption()
                                        .name("소(250g)")
                                        .price(Money.wons(12000))
                                        .build()))
                                .build()))
                        .build()))
                .build();
    }
}
