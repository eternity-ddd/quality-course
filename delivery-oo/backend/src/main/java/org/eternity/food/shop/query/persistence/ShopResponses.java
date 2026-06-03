package org.eternity.food.shop.query.persistence;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ShopResponses {
    private ShopResponses() {}

    public record Nearby(
            Long id,
            String name,
            String category,
            Double latitude,
            Double longitude,
            long minOrderAmount,
            long deliveryFee,
            double rating,
            String imageUrl,
            double distance,
            boolean open
    ) {
    }

    public record Detail(
            Long id,
            String name,
            String category,
            Double latitude,
            Double longitude,
            long minOrderAmount,
            long deliveryFee,
            double rating,
            String imageUrl,
            boolean open
    ) {
    }

    public record Brief(
            Long id,
            String name,
            long minOrderAmount,
            boolean open
    ) {
    }

    public static final class CategoryNames {
        private static final Map<String, String> CODE_TO_DISPLAY = new LinkedHashMap<>();
        private static final Map<String, String> DISPLAY_TO_CODE = new LinkedHashMap<>();

        static {
            put("KOREAN", "한식");
            put("CHINESE", "중식");
            put("JAPANESE", "일식");
            put("CHICKEN", "치킨");
            put("PIZZA", "피자");
            put("SNACK", "분식");
        }

        private static void put(String code, String display) {
            CODE_TO_DISPLAY.put(code, display);
            DISPLAY_TO_CODE.put(display, code);
        }

        private CategoryNames() {}

        public static String toDisplay(String code) {
            return code == null ? null : CODE_TO_DISPLAY.get(code);
        }

        public static String toCode(String display) {
            if (display == null) return null;
            String code = DISPLAY_TO_CODE.get(display);
            if (code == null) {
                throw new IllegalArgumentException("알 수 없는 카테고리: " + display);
            }
            return code;
        }
    }
}
