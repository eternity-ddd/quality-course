package org.eternity.food.shop.command.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eternity.food.base.domain.DomainEntity;
import org.eternity.food.base.generic.money.Money;

@Entity
@Table(name = "MENU_OPTION")
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Option extends DomainEntity<Option, Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Money price;

    public Option(String name, Money price) {
        this(null, name, price);
    }

    @Builder
    public Option(Long id, String name, Money price) {
        validateName(name);
        validatePrice(price);

        this.id = id;
        this.name = name;
        this.price = price;
    }

    void rename(String newName) {
        validateName(newName);
        this.name = newName;
    }

    void changePrice(Money newPrice) {
        validatePrice(newPrice);
        this.price = newPrice;
    }

    private void validateName(String name) {
        if (name == null || name.length() < 2) {
            throw new IllegalArgumentException("옵션명은 2글자 이상이어야 합니다.");
        }
    }

    private void validatePrice(Money price) {
        if (price == null) {
            throw new IllegalArgumentException("옵션 가격은 null이어서는 안됩니다.");
        }

        if (price.isLessThan(Money.ZERO)) {
            throw new IllegalArgumentException("옵션 가격은 0원 이상이어야 합니다.");
        }
    }
}
