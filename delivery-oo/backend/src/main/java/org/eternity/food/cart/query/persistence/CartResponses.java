package org.eternity.food.cart.query.persistence;

import java.util.List;

public final class CartResponses {
    private CartResponses() {}

    public record Cart(
            Long id,
            String sessionId,
            Shop shop,
            List<Item> items,
            long totalPrice
    ) {
        public Cart withSessionId(String newSessionId) {
            return new Cart(id, newSessionId, shop, items, totalPrice);
        }

        public static Cart empty(String sessionId) {
            return new Cart(null, sessionId, null, List.of(), 0L);
        }

        public record Shop(
                Long id,
                String name,
                long deliveryFee,
                long minOrderAmount,
                boolean open
        ) {
        }

        public record Item(
                Long id,
                Long menuId,
                String menuName,
                long unitPrice,
                int quantity,
                List<Option> selectedOptions,
                ItemStatus status,
                List<String> messages
        ) {
        }

        public record Option(
                String groupName,
                String name,
                long price,
                OptionStatus status
        ) {
        }
    }

    public enum ItemStatus {
        VALID,
        PRICE_CHANGED,
        INVALID_OPTION,
        MENU_NOT_OPEN,
        MENU_REMOVED
    }

    public enum OptionStatus {
        VALID,
        NAME_UPDATED,
        PRICE_CHANGED,
        INVALID
    }
}
