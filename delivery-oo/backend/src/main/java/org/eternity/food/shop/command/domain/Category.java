package org.eternity.food.shop.command.domain;

import lombok.Getter;

@Getter
public enum Category {
    KOREAN("한식"),
    CHINESE("중식"),
    JAPANESE("일식"),
    CHICKEN("치킨"),
    PIZZA("피자"),
    SNACK("분식");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public static Category fromDisplayName(String displayName) {
        for (Category each : values()) {
            if (each.displayName.equals(displayName)) {
                return each;
            }
        }

        throw new IllegalArgumentException("알 수 없는 카테고리: " + displayName);
    }
}
