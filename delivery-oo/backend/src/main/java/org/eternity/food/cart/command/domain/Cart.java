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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eternity.food.base.domain.AggregateRoot;
import org.eternity.food.base.generic.money.Money;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "CART")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends AggregateRoot<Cart, Long> {
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "USER_ID")
    private Long userId;

    @Getter
    @Column(name = "SHOP_ID")
    private Long shopId;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "CART_ID")
    private Set<CartLineItem> items = new LinkedHashSet<>();

    public List<CartLineItem> getItems() {
        return List.copyOf(items);
    }

    public static Cart forUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 null이어서는 안됩니다.");
        }

        return new Cart(userId);
    }

    private Cart(Long userId) {
        this(null, userId, null, null);
    }

    @Builder
    public Cart(Long id, Long userId, Long shopId, Collection<CartLineItem> items) {
        this.id = id;
        this.userId = userId;
        this.shopId = shopId;
        if (items != null) {
            this.items = new LinkedHashSet<>(items);
        }
    }

    public Money getTotalPrice() {
        return items.stream()
                .map(CartLineItem::subtotal)
                .reduce(Money.ZERO, Money::plus);
    }

    public void addItem(Long shopId, CartLineItem cartLineItem) {
        if (this.shopId != null && !this.shopId.equals(shopId)) {
            items.clear();
        }

        this.shopId = shopId;

        findSimilar(cartLineItem).ifPresentOrElse(
                existing -> existing.combine(cartLineItem),
                () -> items.add(cartLineItem)
        );
    }

    public void changeItemQuantity(Long itemId, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("수량은 0 이상이어야 합니다: " + quantity);
        }

        if (quantity == 0) {
            removeItem(itemId);
            return;
        }

        findItem(itemId).changeQuantity(quantity);
    }

    public void removeItem(Long itemId) {
        items.remove(findItem(itemId));
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void clear() {
        items.clear();
        this.shopId = null;
    }

    private CartLineItem findItem(Long itemId) {
        return items.stream()
                .filter(item -> Objects.equals(item.getId(), itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("장바구니 아이템을 찾을 수 없습니다: " + itemId));
    }

    private Optional<CartLineItem> findSimilar(CartLineItem candidate) {
        return items.stream()
                .filter(existing -> existing.matchesContent(candidate))
                .findFirst();
    }
}
