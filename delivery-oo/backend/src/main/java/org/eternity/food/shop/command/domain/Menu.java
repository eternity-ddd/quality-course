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

import java.util.List;

@Entity
@Table(name = "MENU")
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Menu extends AggregateRoot<Menu, Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "SHOP_ID")
    private Long shopId;

    private String name;
    private String description;

    @Column(name = "BASE_PRICE")
    private Money basePrice;

    @Enumerated(EnumType.STRING)
    private MenuStatus status;

    @Embedded
    private MenuConfiguration configuration;

    public Menu(Long shopId, String name, String description, Money basePrice) {
        this(null, shopId, name, description, basePrice, MenuStatus.READY, MenuConfiguration.empty());
    }

    @Builder
    public Menu(Long id, Long shopId, String name, String description, Money basePrice, MenuStatus status, MenuConfiguration configuration) {
        validateBasePrice(basePrice);
        if (configuration == null) {
            throw new IllegalArgumentException("configuration은 null이어서는 안됩니다.");
        }

        this.id = id;
        this.shopId = shopId;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.status = status;
        this.configuration = configuration;
    }

    private void validateBasePrice(Money basePrice) {
        if (basePrice == null) {
            throw new IllegalArgumentException("basePrice는 null이어서는 안됩니다.");
        }

        if (basePrice.isLessThanOrEqual(Money.ZERO)) {
            throw new IllegalArgumentException("기본가는 0원보다는 커야합니다.");
        }
    }

    public void open(SellableMenuInvariant invariant) {
        if (!canOpen()) {
            throw new IllegalStateException("이미 판매중인 메뉴입니다.");
        }
        invariant.check(this.configuration).require();
        status = MenuStatus.OPEN;
    }

    public void close() {
        if (!canClose()) {
            throw new IllegalStateException("판매중이 아닌 메뉴입니다.");
        }
        status = MenuStatus.READY;
    }

    public boolean isOpen() {
        return this.status.equals(MenuStatus.OPEN);
    }

    public boolean canOpen() {
        return !isOpen();
    }

    public boolean canClose() {
        return isOpen();
    }

    public void changeConfiguration(MenuConfiguration newConfiguration, SellableMenuInvariant invariant) {
        if (newConfiguration == null) {
            throw new IllegalArgumentException("configuration은 null이어서는 안됩니다.");
        }

        if (isOpen()) {
            invariant.check(newConfiguration).require();
        }

        this.configuration = newConfiguration;
    }

    public List<Long> getOptionGroupIds() {
        return configuration.optionGroupIds();
    }
}
