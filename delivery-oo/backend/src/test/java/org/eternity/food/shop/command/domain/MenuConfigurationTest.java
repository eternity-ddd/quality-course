package org.eternity.food.shop.command.domain;

import org.eternity.food.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MenuConfiguration VO")
class MenuConfigurationTest {

    @Nested
    @DisplayName("menuOptionGroups 불변식")
    class MenuOptionGroupsInvariant {

        @Test
        @DisplayName("menuOptionGroups가 null이면 IAE")
        void menuOptionGroupsNull() {
            assertThatThrownBy(() -> MenuConfiguration.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션그룹은 null");
        }

        @Test
        @DisplayName("같은 optionGroupId가 중복되면 IAE")
        void duplicateOptionGroupId() {
            Set<MenuOptionGroup> groups = new HashSet<>(Set.of(
                    Fixtures.aMenuOptionGroup().optionGroupId(1L).displayOrder(1).build(),
                    Fixtures.aMenuOptionGroup().optionGroupId(1L).displayOrder(2).build()
            ));
            assertThatThrownBy(() -> MenuConfiguration.of(groups))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("중복된 옵션 그룹");
        }

        @Test
        @DisplayName("빈 Set이면 통과 (empty configuration 허용)")
        void emptySetAllowed() {
            MenuConfiguration cfg = MenuConfiguration.of(new HashSet<>());
            assertThat(cfg.isEmpty()).isTrue();
            assertThat(cfg.menuOptionGroups()).isEmpty();
        }

        @Test
        @DisplayName("optionGroupId가 모두 unique한 정상 케이스")
        void allUnique() {
            Set<MenuOptionGroup> groups = Set.of(
                    Fixtures.aMenuOptionGroup().optionGroupId(1L).displayOrder(1).build(),
                    Fixtures.aMenuOptionGroup().optionGroupId(2L).displayOrder(2).build(),
                    Fixtures.aMenuOptionGroup().optionGroupId(3L).displayOrder(3).build()
            );
            MenuConfiguration cfg = MenuConfiguration.of(groups);
            assertThat(cfg.menuOptionGroups()).hasSize(3);
            assertThat(cfg.optionGroupIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
        }
    }

    @Nested
    @DisplayName("empty() 팩토리")
    class EmptyFactory {

        @Test
        @DisplayName("empty()는 빈 configuration을 만든다")
        void emptyIsEmpty() {
            MenuConfiguration cfg = MenuConfiguration.empty();
            assertThat(cfg.isEmpty()).isTrue();
            assertThat(cfg.menuOptionGroups()).isEmpty();
        }
    }

    @Nested
    @DisplayName("menuOptionGroups 불변성")
    class Immutability {

        @Test
        @DisplayName("반환된 menuOptionGroups는 수정 불가")
        void unmodifiable() {
            MenuConfiguration cfg = Fixtures.aMenuConfiguration();
            assertThatThrownBy(() ->
                    cfg.menuOptionGroups().add(Fixtures.aMenuOptionGroup().build())
            ).isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
