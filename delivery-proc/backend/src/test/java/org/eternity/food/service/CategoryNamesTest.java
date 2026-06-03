package org.eternity.food.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryNamesTest {

    @Test
    @DisplayName("toDisplay: KOREAN → 한식")
    void toDisplay_korean() {
        assertThat(CategoryNames.toDisplay("KOREAN")).isEqualTo("한식");
    }

    @Test
    @DisplayName("toDisplay: CHINESE → 중식")
    void toDisplay_chinese() {
        assertThat(CategoryNames.toDisplay("CHINESE")).isEqualTo("중식");
    }

    @Test
    @DisplayName("toDisplay: JAPANESE → 일식")
    void toDisplay_japanese() {
        assertThat(CategoryNames.toDisplay("JAPANESE")).isEqualTo("일식");
    }

    @Test
    @DisplayName("toDisplay: CHICKEN → 치킨")
    void toDisplay_chicken() {
        assertThat(CategoryNames.toDisplay("CHICKEN")).isEqualTo("치킨");
    }

    @Test
    @DisplayName("toDisplay: PIZZA → 피자")
    void toDisplay_pizza() {
        assertThat(CategoryNames.toDisplay("PIZZA")).isEqualTo("피자");
    }

    @Test
    @DisplayName("toDisplay: SNACK → 분식")
    void toDisplay_snack() {
        assertThat(CategoryNames.toDisplay("SNACK")).isEqualTo("분식");
    }

    @Test
    @DisplayName("toDisplay: 알 수 없는 code → null (현재 구현 spec)")
    void toDisplay_unknownCode_returnsNull() {
        assertThat(CategoryNames.toDisplay("UNKNOWN_CODE")).isNull();
    }

    @Test
    @DisplayName("toDisplay: null 입력 → null")
    void toDisplay_null_returnsNull() {
        assertThat(CategoryNames.toDisplay(null)).isNull();
    }

    @Test
    @DisplayName("toCode: 한식 → KOREAN")
    void toCode_korean() {
        assertThat(CategoryNames.toCode("한식")).isEqualTo("KOREAN");
    }

    @Test
    @DisplayName("toCode: 중식 → CHINESE")
    void toCode_chinese() {
        assertThat(CategoryNames.toCode("중식")).isEqualTo("CHINESE");
    }

    @Test
    @DisplayName("toCode: 일식 → JAPANESE")
    void toCode_japanese() {
        assertThat(CategoryNames.toCode("일식")).isEqualTo("JAPANESE");
    }

    @Test
    @DisplayName("toCode: 치킨 → CHICKEN")
    void toCode_chicken() {
        assertThat(CategoryNames.toCode("치킨")).isEqualTo("CHICKEN");
    }

    @Test
    @DisplayName("toCode: 피자 → PIZZA")
    void toCode_pizza() {
        assertThat(CategoryNames.toCode("피자")).isEqualTo("PIZZA");
    }

    @Test
    @DisplayName("toCode: 분식 → SNACK")
    void toCode_snack() {
        assertThat(CategoryNames.toCode("분식")).isEqualTo("SNACK");
    }

    @Test
    @DisplayName("toCode: 알 수 없는 display → IAE")
    void toCode_unknownDisplay_throwsIAE() {
        assertThatThrownBy(() -> CategoryNames.toCode("양식"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("알 수 없는 카테고리")
                .hasMessageContaining("양식");
    }

    @Test
    @DisplayName("toCode: null 입력 → null")
    void toCode_null_returnsNull() {
        assertThat(CategoryNames.toCode(null)).isNull();
    }

    @Test
    @DisplayName("round-trip: code → display → code 일관성")
    void roundTrip_codeToDisplayToCode() {
        String[] codes = {"KOREAN", "CHINESE", "JAPANESE", "CHICKEN", "PIZZA", "SNACK"};
        for (String code : codes) {
            String display = CategoryNames.toDisplay(code);
            assertThat(CategoryNames.toCode(display))
                    .as("round-trip for %s", code)
                    .isEqualTo(code);
        }
    }
}
