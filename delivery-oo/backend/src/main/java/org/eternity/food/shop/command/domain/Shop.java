package org.eternity.food.shop.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.eternity.food.base.generic.time.TimePeriod;

import java.time.LocalDateTime;

@Entity
@Table(name = "SHOP")
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Shop extends AggregateRoot<Shop, Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "MIN_ORDER_PRICE")
    private Money minOrderPrice;

    @Embedded
    private TimePeriod operationPeriod;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Embedded
    private Location location;

    @Column(name = "DELIVERY_RADIUS")
    private Double deliveryRadius;

    @Builder
    public Shop(Long id, String name, Money minOrderPrice, TimePeriod operationPeriod,
                Category category, Location location, Double deliveryRadius) {
        if (name == null || name.length() < 5) {
            throw new IllegalArgumentException("가게 이름은 5글자 이상이어야 합니다.");
        }

        if (minOrderPrice == null || minOrderPrice.isLessThanOrEqual(Money.ZERO)) {
            throw new IllegalArgumentException("최소주문금액은 0원보다 커야 합니다.");
        }

        if (operationPeriod == null) {
            throw new IllegalArgumentException("영업시간은 null이어서는 안됩니다.");
        }

        this.id = id;
        this.name = name;
        this.minOrderPrice = minOrderPrice;
        this.operationPeriod = operationPeriod;
        this.category = category;
        this.location = location;
        this.deliveryRadius = deliveryRadius;
    }

    public boolean isOpen() {
        return isOpen(LocalDateTime.now());
    }

    public boolean isOpen(LocalDateTime time) {
        return operationPeriod.contains(time.toLocalTime());
    }
}
