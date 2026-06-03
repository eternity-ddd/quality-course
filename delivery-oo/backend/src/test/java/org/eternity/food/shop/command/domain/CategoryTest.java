package org.eternity.food.shop.command.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Category 매핑")
class CategoryTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "한식, KOREAN",
            "중식, CHINESE",
            "일식, JAPANESE",
            "치킨, CHICKEN",
            "피자, PIZZA",
            "분식, SNACK"
    })
    @DisplayName("displayName으로부터 모든 enum 값을 찾을 수 있다")
    void fromDisplayName_allMatches(String displayName, Category expected) {
        assertThat(Category.fromDisplayName(displayName)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\"이면 IAE")
    @ValueSource(strings = {"양식", "디저트", "", "KOREAN"})
    @DisplayName("매칭되는 displayName이 없으면 IAE")
    void fromDisplayName_unknown(String displayName) {
        assertThatThrownBy(() -> Category.fromDisplayName(displayName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("알 수 없는 카테고리");
    }

    @Test
    @DisplayName("displayName이 null이면 NPE (equals 비교 시점)")
    void fromDisplayName_null() {
        // null은 displayName.equals(null)에서 false → 결국 IAE로 종결.
        // (enum.displayName.equals(null) → false 이므로 NPE 아닌 IAE)
        assertThatThrownBy(() -> Category.fromDisplayName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("알 수 없는 카테고리");
    }

    @Test
    @DisplayName("각 카테고리의 displayName getter가 동작한다")
    void getDisplayName() {
        assertThat(Category.KOREAN.getDisplayName()).isEqualTo("한식");
        assertThat(Category.PIZZA.getDisplayName()).isEqualTo("피자");
    }
}
