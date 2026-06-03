package org.eternity.food.cart.command.service;

import org.eternity.food.Fixtures;
import org.eternity.food.cart.command.domain.Cart;
import org.eternity.food.cart.command.domain.CartRepository;
import org.eternity.food.order.command.domain.OrderPlacedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * OrderPlacedHandler — OrderPlacedEvent 수신 시 해당 사용자의 cart를 비운다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderPlacedHandler 단위 테스트")
class OrderPlacedHandlerTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private OrderPlacedHandler handler;

    @Test
    @DisplayName("이벤트 수신 → cart.clear() → save 호출 (cart 존재 케이스)")
    void clearCart_whenCartPresent_clearsAndSaves() {
        Cart cart = Fixtures.aCart().build();
        assertThat(cart.getShopId()).isNotNull();
        assertThat(cart.getItems()).isNotEmpty();

        given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.of(cart));

        handler.clearCart(new OrderPlacedEvent(Fixtures.USER_ID));

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.getShopId()).isNull();

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(cart);
    }

    @Test
    @DisplayName("cart 없는 사용자 이벤트 → 아무 일도 일어나지 않음 (save 호출 없음)")
    void clearCart_whenCartAbsent_noop() {
        given(cartRepository.findByUserId(Fixtures.USER_ID)).willReturn(Optional.empty());

        handler.clearCart(new OrderPlacedEvent(Fixtures.USER_ID));

        verify(cartRepository, never()).save(any());
    }
}
