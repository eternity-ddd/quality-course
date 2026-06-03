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

    @Nested
    @DisplayName("구조 invariant")
    class Structural {

        @Test
        @DisplayName("forUser: userId가 null이면 IAE")
        void forUser_nullUserId_throws() {
            assertThatThrownBy(() -> Cart.forUser(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자 ID");
        }

        @Test
        @DisplayName("forUser: userId가 있으면 정상 생성 + items 비어 있음 + shopId null")
        void forUser_valid_ok() {
            Cart cart = Cart.forUser(1L);

            assertThat(cart.getUserId()).isEqualTo(1L);
            assertThat(cart.isEmpty()).isTrue();
            assertThat(cart.getShopId()).isNull();
        }

        @Test
        @DisplayName("addItem: cartLineItem이 null이면 NPE (현재 코드는 명시적 검증 없음 — null이 들어가면 후속 호출에서 NPE)")
        void addItem_nullLineItem_throws() {
            Cart cart = aCart().build();

            // 빈 카트가 아닌 경우 findSimilar에서 matchesContent(null) 호출 시 NPE
            assertThatThrownBy(() -> cart.addItem(cart.getShopId(), null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("addItem: shopId=null로 빈 카트에 진입 → shopId=null로 set + line 추가됨")
        void addItem_nullShopId_clearsAndSetsNull() {
            // forUser로 빈 카트 시작
            Cart cart = Cart.forUser(1L);
            CartLineItem line = aCartLineItem().id(7L).build();

            cart.addItem(null, line);

            assertThat(cart.getShopId()).isNull();
            assertThat(cart.getItems()).hasSize(1);
            assertThat(cart.getItems().get(0).getId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("changeItemQuantity: quantity가 음수면 IAE")
        void changeItemQuantity_negative_throws() {
            Cart cart = aCart().build();
            Long itemId = cart.getItems().get(0).getId();

            assertThatThrownBy(() -> cart.changeItemQuantity(itemId, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("0 이상");
        }

        @Test
        @DisplayName("changeItemQuantity: quantity=0은 경계 통과 (자동 제거 트리거)")
        void changeItemQuantity_zero_ok_removes() {
            Cart cart = aCart().build();
            Long itemId = cart.getItems().get(0).getId();

            assertThatCode(() -> cart.changeItemQuantity(itemId, 0))
                    .doesNotThrowAnyException();
            assertThat(cart.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("changeItemQuantity: quantity=1은 정상")
        void changeItemQuantity_one_ok() {
            Cart cart = aCart().build();
            Long itemId = cart.getItems().get(0).getId();

            cart.changeItemQuantity(itemId, 1);

            assertThat(cart.getItems().get(0).getMenuCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("행위 invariant")
    class Behavioral {

        @Test
        @DisplayName("addItem: 같은 shopId면 기존 items 유지")
        void addItem_sameShop_keepsItems() {
            Cart cart = aCart().build();
            int sizeBefore = cart.getItems().size();
            CartLineItem newLine = aCartLineItem()
                    .id(99L)
                    .menuId(999L)
                    .menuName("불고기")
                    .groups(List.of(aCartOptionGroup()
                            .id(99L)
                            .optionGroupId(999L)
                            .options(java.util.Set.of(aCartOption().name("특수옵션").build()))
                            .build()))
                    .build();

            cart.addItem(cart.getShopId(), newLine);

            assertThat(cart.getItems()).hasSize(sizeBefore + 1);
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

        @Test
        @DisplayName("addItem: 동일 내용 라인 추가 → combine으로 count 누적")
        void addItem_sameContent_combines() {
            Cart cart = aCart().build();
            CartLineItem existing = cart.getItems().get(0);
            int beforeCount = existing.getMenuCount();
            // 같은 menuId/optionGroup/option 으로 만든 새 라인
            CartLineItem duplicate = aCartLineItem()
                    .id(null)
                    .count(2)
                    .build();

            cart.addItem(cart.getShopId(), duplicate);

            assertThat(cart.getItems()).hasSize(1);
            assertThat(cart.getItems().get(0).getMenuCount()).isEqualTo(beforeCount + 2);
        }

        @Test
        @DisplayName("clear(): items와 shopId 모두 reset")
        void clear_resetsItemsAndShopId() {
            Cart cart = aCart().build();
            assertThat(cart.isEmpty()).isFalse();
            assertThat(cart.getShopId()).isNotNull();

            cart.clear();

            assertThat(cart.isEmpty()).isTrue();
            assertThat(cart.getShopId()).isNull();
        }

        @Test
        @DisplayName("changeItemQuantity(itemId, 0): 해당 라인 자동 제거")
        void changeItemQuantity_zero_removesItem() {
            Cart cart = aCart().build();
            Long itemId = cart.getItems().get(0).getId();

            cart.changeItemQuantity(itemId, 0);

            assertThat(cart.getItems()).extracting(CartLineItem::getId).doesNotContain(itemId);
        }

        @Test
        @DisplayName("changeItemQuantity(itemId, n>0): 해당 라인 quantity 변경")
        void changeItemQuantity_positive_updates() {
            Cart cart = aCart().build();
            Long itemId = cart.getItems().get(0).getId();

            cart.changeItemQuantity(itemId, 5);

            assertThat(cart.getItems().get(0).getMenuCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("removeItem: 존재하지 않는 itemId는 IAE")
        void removeItem_unknownId_throws() {
            Cart cart = aCart().build();

            assertThatThrownBy(() -> cart.removeItem(9999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("getTotalPrice: 빈 카트는 0원")
        void totalPrice_empty_isZero() {
            Cart cart = Cart.forUser(1L);

            assertThat(cart.getTotalPrice()).isEqualTo(Money.ZERO);
        }

        @Test
        @DisplayName("getTotalPrice: 라인별 subtotal 합")
        void totalPrice_sumOfLines() {
            Cart cart = aCart().build();

            Money expected = cart.getItems().stream()
                    .map(CartLineItem::subtotal)
                    .reduce(Money.ZERO, Money::plus);

            assertThat(cart.getTotalPrice()).isEqualTo(expected);
        }
    }
}
