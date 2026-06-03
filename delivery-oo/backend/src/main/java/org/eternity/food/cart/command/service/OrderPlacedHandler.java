package org.eternity.food.cart.command.service;

import lombok.AllArgsConstructor;
import org.eternity.food.cart.command.domain.CartRepository;
import org.eternity.food.order.command.domain.OrderPlacedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 주문 완료 시 해당 사용자의 장바구니를 비운다.
 *
 * <p>{@link OrderPlacedEvent}는 {@code orderRepository.save(order)} 시 Spring Data의
 * {@code @DomainEvents}로 발행되며, 같은 트랜잭션 안에서 이 핸들러가 동기로 실행된다.
 * 결과적으로 주문 저장과 장바구니 비우기는 원자적으로 commit/rollback.
 *
 * <p>Spring Data JDBC는 dirty checking이 없으므로 {@code cart.clear()} 후
 * {@code cartRepository.save(cart)} 명시 호출 필수.
 */
@Component
@AllArgsConstructor
public class OrderPlacedHandler {
    private final CartRepository cartRepository;

    @EventListener
    public void clearCart(OrderPlacedEvent event) {
        cartRepository.findByUserId(event.userId()).ifPresent(cart -> {
            cart.clear();
            cartRepository.save(cart);
        });
    }
}
