package org.eternity.food.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DB 코드 ↔ 디스플레이명 변환 유틸. anti-pattern 그대로: static method + private map.
 */
public final class CategoryNames {

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
        if (display == null) {
            return null;
        }
        String code = DISPLAY_TO_CODE.get(display);
        if (code == null) {
            throw new IllegalArgumentException("알 수 없는 카테고리: " + display);
        }
        return code;
    }
}
