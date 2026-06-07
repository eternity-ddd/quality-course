package org.eternity.food.cart.query.persistence;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record CartRaw(
        Long id,
        Long userId,
        Long shopId,
        List<CartItemRaw> items
) {
    public Set<Long> menuIds() {
        return items.stream().map(CartItemRaw::menuId).collect(Collectors.toSet());
    }

    public record CartItemRaw(
            Long id,
            Long menuId,
            String menuName,
            int quantity,
            long basePrice,
            List<CartOptionGroupRaw> groups
    ) {
    }

    public record CartOptionGroupRaw(
            Long optionGroupId,
            String name,
            List<CartOptionRaw> options
    ) {
    }

    public record CartOptionRaw(
            String name,
            long price
    ) {
    }
}
