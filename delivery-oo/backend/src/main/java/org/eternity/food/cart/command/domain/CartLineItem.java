package org.eternity.food.cart.command.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import org.eternity.food.base.domain.DomainEntity;
import org.eternity.food.base.generic.money.Money;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "CART_LINE_ITEM")
@Getter
public class CartLineItem extends DomainEntity<CartLineItem, Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "MENU_ID")
    private Long menuId;

    @Column(name = "MENU_NAME")
    private String menuName;

    @Column(name = "MENU_COUNT")
    private int menuCount;

    @Column(name = "UNIT_PRICE")
    private Money unitPrice;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "CART_LINE_ITEM_ID")
    private Set<CartOptionGroup> groups = new HashSet<>();

    public CartLineItem(Long menuId, String menuName, int count, Money unitPrice, List<CartOptionGroup> groups) {
        this(null, menuId, menuName, count, unitPrice, groups);
    }

    @Builder
    public CartLineItem(Long id, Long menuId, String menuName, int count, Money unitPrice, List<CartOptionGroup> groups) {
        if (count < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다: " + count);
        }

        this.id = id;
        this.menuId = menuId;
        this.menuName = menuName;
        this.menuCount = count;
        this.unitPrice = unitPrice;
        if (groups != null) {
            this.groups.addAll(groups);
        }
    }

    protected CartLineItem() {
    }

    public void combine(CartLineItem other) {
        this.menuCount += other.getMenuCount();
    }

    public void changeQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다: " + quantity);
        }

        this.menuCount = quantity;
    }

    public boolean matchesContent(CartLineItem other) {
        if (!Objects.equals(this.menuId, other.menuId)) {
            return false;
        }

        if (this.groups.size() != other.groups.size()) {
            return false;
        }

        for (CartOptionGroup thisGroup : groups) {
            if (other.groups.stream().noneMatch(thatGroup -> thatGroup.matchesContent(thisGroup))) {
                return false;
            }
        }

        return true;
    }

    public Money subtotal() {
        return unitPrice.times(menuCount);
    }
}
