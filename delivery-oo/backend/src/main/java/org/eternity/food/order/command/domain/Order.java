package org.eternity.food.order.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eternity.food.base.domain.AggregateRoot;
import org.eternity.food.base.generic.money.Money;
import org.eternity.food.order.command.persistence.converter.OrderItemsConverters;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ORDERS")
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Order extends AggregateRoot<Order, Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "SHOP_ID")
    private Long shopId;

    @Column(name = "ORDERED_TIME")
    private LocalDateTime orderedTime;

    @Column(name = "TOTAL_PRICE")
    private Money totalPrice;

    @Column(name = "ITEMS_SNAPSHOT", nullable = false)
    @Convert(converter = OrderItemsConverters.class)
    private OrderItems items;

    public Order(Long userId, Long shopId, List<OrderLineItem> items) {
        this(null, userId, shopId, items, computeTotal(items), LocalDateTime.now());
        registerEvent(new OrderPlacedEvent(userId));
    }

    @Builder
    public Order(Long id, Long userId, Long shopId, List<OrderLineItem> items, Money totalPrice, LocalDateTime orderedTime) {
        this.id = id;
        this.userId = userId;
        this.shopId = shopId;
        this.items = OrderItems.of(items);
        this.totalPrice = totalPrice;
        this.orderedTime = orderedTime;
    }

    private static Money computeTotal(List<OrderLineItem> items) {
        return items.stream()
                .map(OrderLineItem::subtotal)
                .reduce(Money.ZERO, Money::plus);
    }

    public Money getPrice() {
        return totalPrice;
    }

    public List<OrderLineItem> getItems() {
        return items.list();
    }
}
