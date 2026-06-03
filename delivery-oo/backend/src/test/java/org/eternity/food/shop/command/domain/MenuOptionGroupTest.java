package org.eternity.food.shop.command.domain;

import org.eternity.food.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MenuOptionGroup VO")
class MenuOptionGroupTest {

    @Nested
    @DisplayName("optionGroupId 불변식")
    class OptionGroupIdInvariant {

        @Test
        @DisplayName("optionGroupId가 null이면 IAE")
        void optionGroupIdNull() {
            assertThatThrownBy(() -> Fixtures.aMenuOptionGroup().optionGroupId(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("optionGroupId는 null");
        }

        @Test
        @DisplayName("optionGroupId가 정상 값이면 생성된다")
        void optionGroupIdNormal() {
            MenuOptionGroup g = Fixtures.aMenuOptionGroup().optionGroupId(100L).build();
            assertThat(g.getOptionGroupId()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("displayOrder 불변식")
    class DisplayOrderInvariant {

        @ParameterizedTest(name = "displayOrder={0}이면 IAE")
        @ValueSource(ints = {0, -1, -10})
        @DisplayName("displayOrder가 1 미만이면 IAE")
        void displayOrderTooSmall(int displayOrder) {
            assertThatThrownBy(() -> Fixtures.aMenuOptionGroup().displayOrder(displayOrder).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("displayOrder는 1 이상");
        }

        @Test
        @DisplayName("displayOrder가 정확히 1(경계 통과)이면 생성된다")
        void displayOrderBoundaryPass() {
            MenuOptionGroup g = Fixtures.aMenuOptionGroup().displayOrder(1).build();
            assertThat(g.getDisplayOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("displayOrder가 큰 값이면 정상 생성")
        void displayOrderNormal() {
            MenuOptionGroup g = Fixtures.aMenuOptionGroup().displayOrder(99).build();
            assertThat(g.getDisplayOrder()).isEqualTo(99);
        }
    }

    @Nested
    @DisplayName("compareTo (displayOrder 기준)")
    class CompareTo {

        @Test
        @DisplayName("displayOrder가 작은 쪽이 앞")
        void compareByDisplayOrderAsc() {
            MenuOptionGroup a = Fixtures.aMenuOptionGroup().displayOrder(1).build();
            MenuOptionGroup b = Fixtures.aMenuOptionGroup().displayOrder(2).build();
            assertThat(a.compareTo(b)).isNegative();
            assertThat(b.compareTo(a)).isPositive();
        }

        @Test
        @DisplayName("displayOrder가 같으면 0")
        void compareEqualOrder() {
            MenuOptionGroup a = Fixtures.aMenuOptionGroup().displayOrder(3).build();
            MenuOptionGroup b = Fixtures.aMenuOptionGroup().displayOrder(3).optionGroupId(2L).build();
            assertThat(a.compareTo(b)).isZero();
        }
    }
}
