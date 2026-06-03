package org.eternity.food.order.command.domain;

import org.eternity.food.base.domain.DomainEvent;

/**
 * 주문이 생성되었음을 알리는 도메인 이벤트.
 *
 * <p>{@link Order} 생성 시점에 registerEvent로 등록되고, {@code orderRepository.save(order)} 시
 * Spring Data의 {@code @DomainEvents} 메커니즘으로 {@code ApplicationEventPublisher}를 통해
 * 발행. 같은 트랜잭션 안에서 동기로 핸들러가 실행됨.
 */
public record OrderPlacedEvent(Long userId) implements DomainEvent {
}
