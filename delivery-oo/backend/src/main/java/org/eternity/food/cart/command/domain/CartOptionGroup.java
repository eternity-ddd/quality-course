package org.eternity.food.cart.command.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import org.eternity.food.base.domain.DomainEntity;
import org.eternity.food.base.generic.money.Money;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "CART_OPTION_GROUP")
@Getter
public class CartOptionGroup extends DomainEntity<CartOptionGroup, Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "OPTION_GROUP_ID")
    private Long optionGroupId;

    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "CART_OPTION", joinColumns = @JoinColumn(name = "CART_OPTION_GROUP_ID"))
    private Set<CartOption> options = new HashSet<>();

    public CartOptionGroup(Long optionGroupId, String name, CartOption... options) {
        this(optionGroupId, name, Set.of(options));
    }

    public CartOptionGroup(Long optionGroupId, String name, Set<CartOption> options) {
        this(null, optionGroupId, name, options);
    }

    @Builder
    public CartOptionGroup(Long id, Long optionGroupId, String name, Set<CartOption> options) {
        if (optionGroupId == null) {
            throw new IllegalArgumentException("optionGroupId는 null이어서는 안됩니다.");
        }

        this.id = id;
        this.optionGroupId = optionGroupId;
        this.name = name;
        if (options != null) {
            this.options = new HashSet<>(options);
        }
    }

    protected CartOptionGroup() {
    }

    public boolean matchesContent(CartOptionGroup other) {
        if (!Objects.equals(optionGroupId, other.getOptionGroupId())) {
            return false;
        }

        return Objects.equals(this.options, other.getOptions());
    }

    public Money getTotalPrice() {
        return options.stream().map(CartOption::getPrice).reduce(Money.ZERO, Money::plus);
    }
}
