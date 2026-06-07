package org.eternity.food.dto;

import java.util.List;

public final class CartDto {

    private CartDto() {}

    public record CartResponse(
            Long id,
            String sessionId,
            ShopBrief shop,
            List<Item> items,
            long totalPrice
    ) {
        public CartResponse withSessionId(String newSessionId) {
            return new CartResponse(id, newSessionId, shop, items, totalPrice);
        }

        public static CartResponse empty(String sessionId) {
            return new CartResponse(null, sessionId, null, List.of(), 0L);
        }
    }

    public record ShopBrief(
            Long id,
            String name,
            long deliveryFee,
            long minOrderAmount,
            boolean open
    ) {}

    public record Item(
            Long id,
            Long menuId,
            String menuName,
            long basePrice,
            int quantity,
            List<Option> selectedOptions,
            ItemStatus status,
            List<String> messages
    ) {}

    public record Option(
            String groupName,
            String name,
            long price,
            OptionStatus status
    ) {}

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

    /** POST /api/cart/items body. */
    public record AddItemRequest(
            String sessionId,
            Long menuId,
            String menuName,
            Integer quantity,
            List<SelectedOption> selectedOptions
    ) {
        public record SelectedOption(
                Long optionGroupId,
                String optionGroupName,
                Long optionId,
                String name,
                Long price
        ) {}
    }

    public record UpdateQuantityRequest(Integer quantity) {}

    public record OrderPlacedResponse(Long orderId, long totalPrice) {}
}
