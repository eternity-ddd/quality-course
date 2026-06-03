package org.eternity.food.cart.command.service;

import org.eternity.food.Fixtures;
import org.eternity.food.cart.command.domain.Cart;
import org.eternity.food.cart.command.domain.CartLineItem;
import org.eternity.food.cart.command.domain.CartRepository;
import org.eternity.food.shop.command.domain.Menu;
import org.eternity.food.shop.command.domain.MenuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * CartCommandService의 *위임 검증* — 서비스는 invariant 호출/도메인 위임/save만 책임.
 *
 * <p>Cart의 행위 자체는 {@code CartTest}가 검증하고, 여기서는 *service가 cart 메서드를 정확히 호출하는지*
 * + *Repository 호출 흐름*만 확인한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartCommandService 단위 테스트")
class CartCommandServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private MenuRepository menuRepository;

    @InjectMocks
    private CartCommandService service;

    private Cart cart;
    private Menu menu;

    @BeforeEach
    void setUp() {
        cart = Fixtures.aCart().items(new java.util.ArrayList<>()).shopId(null).build();
        menu = Fixtures.aMenu().build();
    }

    @Nested
    @DisplayName("addCartLineItem")
    class AddCartLineItem {

        @Test
        @DisplayName("정상 흐름: cart 조회 → menu 조회 → cart.addItem 위임 → save 호출")
        void happyPath_delegatesAndSaves() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cart));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.of(menu));

            CartLineItemCommand command = new CartLineItemCommand(
                    menu.getId(), menu.getName(), 1, List.of());

            service.addCartLineItem(Fixtures.USER_ID, command);

            assertThat(cart.getItems()).hasSize(1);
            assertThat(cart.getShopId()).isEqualTo(menu.getShopId());

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(cart);
        }

        @Test
        @DisplayName("cart 미존재 사용자면 IAE")
        void cartNotFound_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.empty());

            CartLineItemCommand command = new CartLineItemCommand(
                    menu.getId(), menu.getName(), 1, List.of());

            assertThatThrownBy(() -> service.addCartLineItem(Fixtures.USER_ID, command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("카트는 존재하지 않는 사용자");
            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("menu 미존재면 IAE + save 호출 안 됨")
        void menuNotFound_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cart));
            given(menuRepository.findById(menu.getId())).willReturn(Optional.empty());

            CartLineItemCommand command = new CartLineItemCommand(
                    menu.getId(), menu.getName(), 1, List.of());

            assertThatThrownBy(() -> service.addCartLineItem(Fixtures.USER_ID, command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메뉴를 찾을 수 없습니다");
            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("다른 shop의 item 추가 → Cart가 자동 clear + 새 shopId 전환")
        void differentShop_clearsExistingItems() {
            Cart existing = Fixtures.aCart().build();
            assertThat(existing.getItems()).isNotEmpty();

            Long otherShopId = existing.getShopId() + 100;
            Menu otherShopMenu = Fixtures.aMenu().id(999L).shopId(otherShopId).build();

            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(existing));
            given(menuRepository.findById(otherShopMenu.getId())).willReturn(Optional.of(otherShopMenu));

            CartLineItemCommand command = new CartLineItemCommand(
                    otherShopMenu.getId(), otherShopMenu.getName(), 1, List.of());

            service.addCartLineItem(Fixtures.USER_ID, command);

            assertThat(existing.getShopId()).isEqualTo(otherShopId);
            assertThat(existing.getItems()).hasSize(1);
            assertThat(existing.getItems().get(0).getMenuId()).isEqualTo(otherShopMenu.getId());
            verify(cartRepository).save(existing);
        }
    }

    @Nested
    @DisplayName("updateItemQuantity")
    class UpdateItemQuantity {

        @Test
        @DisplayName("정상 흐름: cart 조회 → changeItemQuantity 위임 → save 호출")
        void happyPath_delegates() {
            Cart existing = Fixtures.aCart().build();
            Long itemId = existing.getItems().get(0).getId();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(existing));

            service.updateItemQuantity(Fixtures.USER_ID, itemId, 3);

            assertThat(existing.getItems().get(0).getMenuCount()).isEqualTo(3);
            verify(cartRepository).save(existing);
        }

        @Test
        @DisplayName("quantity=0 → Cart가 라인 자동 제거 → save 시 items 비어 있음")
        void zeroQuantity_removesLine() {
            Cart existing = Fixtures.aCart().build();
            Long itemId = existing.getItems().get(0).getId();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(existing));

            service.updateItemQuantity(Fixtures.USER_ID, itemId, 0);

            assertThat(existing.getItems()).extracting(CartLineItem::getId).doesNotContain(itemId);

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            assertThat(captor.getValue().getItems()).isEmpty();
        }

        @Test
        @DisplayName("음수 quantity → IAE + save 호출 안 됨")
        void negativeQuantity_throws() {
            Cart existing = Fixtures.aCart().build();
            Long itemId = existing.getItems().get(0).getId();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.updateItemQuantity(Fixtures.USER_ID, itemId, -1))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("cart 미존재 사용자면 IAE")
        void cartNotFound_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateItemQuantity(Fixtures.USER_ID, 1L, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("카트는 존재하지 않는");
            verify(cartRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeItem")
    class RemoveItem {

        @Test
        @DisplayName("정상 흐름: cart 조회 → removeItem 위임 → save 호출")
        void happyPath_delegates() {
            Cart existing = Fixtures.aCart().build();
            Long itemId = existing.getItems().get(0).getId();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(existing));

            service.removeItem(Fixtures.USER_ID, itemId);

            assertThat(existing.getItems()).extracting(CartLineItem::getId).doesNotContain(itemId);
            verify(cartRepository, times(1)).save(existing);
        }

        @Test
        @DisplayName("존재하지 않는 itemId → 도메인이 IAE → save 호출 안 됨")
        void unknownItemId_throws() {
            Cart existing = Fixtures.aCart().build();
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.removeItem(Fixtures.USER_ID, 9999L))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("cart 미존재 사용자면 IAE")
        void cartNotFound_throws() {
            given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeItem(Fixtures.USER_ID, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("카트는 존재하지 않는");
            verify(cartRepository, never()).save(any());
        }
    }
}
