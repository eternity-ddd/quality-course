package org.eternity.food.shop.command.domain;

import org.eternity.food.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SellableMenuInvariant (C1~C3)")
class SellableMenuInvariantTest {

    @Nested
    @DisplayName("C1: 옵션그룹 1개 이상")
    class C1OptionGroupAtLeastOne {

        @Test
        @DisplayName("configuration이 비어 있으면 Check.fail")
        void emptyConfigurationFails() {
            SellableMenuInvariant invariant = new SellableMenuInvariant(List.of());
            assertThatThrownBy(() -> invariant.check(MenuConfiguration.empty()).require())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("옵션그룹이 1개 이상");
        }

        @Test
        @DisplayName("configuration에 그룹이 1개 있으면 C1 통과 (다음 검증으로 진행)")
        void oneOptionGroupPasses() {
            MenuOptionGroup mog = Fixtures.aMenuOptionGroup().optionGroupId(1L).build();
            MenuConfiguration cfg = MenuConfiguration.of(Set.of(mog));
            OptionGroup og = Fixtures.anOptionGroup().id(1L).build();
            SellableMenuInvariant invariant = new SellableMenuInvariant(List.of(og));
            assertThat(invariant.check(cfg).passed()).isTrue();
        }
    }

    @Nested
    @DisplayName("C2: 메뉴 옵션그룹 ID = OptionGroup ID 일치")
    class C2IdAlignment {

        @Test
        @DisplayName("config의 optionGroupId가 실제 OptionGroup의 id와 일치하지 않으면 fail")
        void mismatchFails() {
            MenuOptionGroup mog = Fixtures.aMenuOptionGroup().optionGroupId(1L).build();
            MenuConfiguration cfg = MenuConfiguration.of(Set.of(mog));
            OptionGroup wrongGroup = Fixtures.anOptionGroup().id(999L).build();
            SellableMenuInvariant invariant = new SellableMenuInvariant(List.of(wrongGroup));
            assertThatThrownBy(() -> invariant.check(cfg).require())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("옵션 그룹 구성이 일치하지 않");
        }

        @Test
        @DisplayName("config에 있는데 optionGroups에 없으면 fail")
        void configHasMoreFails() {
            Set<MenuOptionGroup> mogs = Set.of(
                    Fixtures.aMenuOptionGroup().optionGroupId(1L).displayOrder(1).build(),
                    Fixtures.aMenuOptionGroup().optionGroupId(2L).displayOrder(2).build()
            );
            MenuConfiguration cfg = MenuConfiguration.of(mogs);
            OptionGroup g = Fixtures.anOptionGroup().id(1L).build();
            SellableMenuInvariant invariant = new SellableMenuInvariant(List.of(g));
            assertThatThrownBy(() -> invariant.check(cfg).require())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("일치하지 않");
        }

        @Test
        @DisplayName("optionGroups에 있는데 config에 없으면 fail")
        void optionGroupsHasMoreFails() {
            MenuOptionGroup mog = Fixtures.aMenuOptionGroup().optionGroupId(1L).build();
            MenuConfiguration cfg = MenuConfiguration.of(Set.of(mog));
            List<OptionGroup> groups = List.of(
                    Fixtures.anOptionGroup().id(1L).required(false).build(),
                    Fixtures.anOptionGroup().id(2L).name("추가").required(false).build()
            );
            SellableMenuInvariant invariant = new SellableMenuInvariant(groups);
            assertThatThrownBy(() -> invariant.check(cfg).require())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("일치하지 않");
        }

        @Test
        @DisplayName("ID 집합이 완전히 일치하면 통과")
        void exactMatchPasses() {
            Set<MenuOptionGroup> mogs = Set.of(
                    Fixtures.aMenuOptionGroup().optionGroupId(1L).displayOrder(1).build(),
                    Fixtures.aMenuOptionGroup().optionGroupId(2L).displayOrder(2).build()
            );
            MenuConfiguration cfg = MenuConfiguration.of(mogs);
            List<OptionGroup> groups = List.of(
                    Fixtures.anOptionGroup().id(1L).required(false).build(),
                    Fixtures.anOptionGroup().id(2L).name("추가").required(false).build()
            );
            SellableMenuInvariant invariant = new SellableMenuInvariant(groups);
            assertThat(invariant.check(cfg).passed()).isTrue();
        }
    }

    @Nested
    @DisplayName("C3: 필수 옵션그룹 ≤ MAX_REQUIRED_GROUP (3)")
    class C3MaxRequired {

        @Test
        @DisplayName("필수 옵션그룹이 정확히 3개(경계 통과)면 통과")
        void exactlyThreeRequiredPasses() {
            Set<MenuOptionGroup> mogs = Set.of(
                    Fixtures.aMenuOptionGroup().optionGroupId(1L).displayOrder(1).build(),
                    Fixtures.aMenuOptionGroup().optionGroupId(2L).displayOrder(2).build(),
                    Fixtures.aMenuOptionGroup().optionGroupId(3L).displayOrder(3).build()
            );
            MenuConfiguration cfg = MenuConfiguration.of(mogs);
            List<OptionGroup> groups = List.of(
                    requiredGroup(1L, "그룹1"),
                    requiredGroup(2L, "그룹2"),
                    requiredGroup(3L, "그룹3")
            );
            SellableMenuInvariant invariant = new SellableMenuInvariant(groups);
            assertThat(invariant.check(cfg).passed()).isTrue();
        }

        @Test
        @DisplayName("필수 옵션그룹이 4개(경계 초과)면 fail")
        void fourRequiredFails() {
            List<MenuOptionGroup> mogList = new ArrayList<>();
            List<OptionGroup> groups = new ArrayList<>();
            for (long i = 1; i <= 4; i++) {
                mogList.add(Fixtures.aMenuOptionGroup().optionGroupId(i).displayOrder((int) i).build());
                groups.add(requiredGroup(i, "그룹" + i));
            }
            MenuConfiguration cfg = MenuConfiguration.of(new java.util.HashSet<>(mogList));
            SellableMenuInvariant invariant = new SellableMenuInvariant(groups);
            assertThatThrownBy(() -> invariant.check(cfg).require())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("필수 옵션그룹의 갯수는 3개 이하");
        }

        @Test
        @DisplayName("required=false 그룹은 카운트되지 않음 (필수 0개 → 통과)")
        void allOptionalPasses() {
            Set<MenuOptionGroup> mogs = Set.of(
                    Fixtures.aMenuOptionGroup().optionGroupId(1L).displayOrder(1).build(),
                    Fixtures.aMenuOptionGroup().optionGroupId(2L).displayOrder(2).build()
            );
            MenuConfiguration cfg = MenuConfiguration.of(mogs);
            List<OptionGroup> groups = List.of(
                    Fixtures.anOptionGroup().id(1L).required(false).build(),
                    Fixtures.anOptionGroup().id(2L).name("추가").required(false).build()
            );
            SellableMenuInvariant invariant = new SellableMenuInvariant(groups);
            assertThat(invariant.check(cfg).passed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Check 반환 형태")
    class CheckResult {

        @Test
        @DisplayName("성공 시 Check.passed=true")
        void passReturnsPassedTrue() {
            MenuOptionGroup mog = Fixtures.aMenuOptionGroup().optionGroupId(1L).build();
            MenuConfiguration cfg = MenuConfiguration.of(Set.of(mog));
            OptionGroup og = Fixtures.anOptionGroup().id(1L).build();
            SellableMenuInvariant invariant = new SellableMenuInvariant(List.of(og));
            assertThat(invariant.check(cfg).passed()).isTrue();
        }

        @Test
        @DisplayName("실패 시 Check.passed=false + reason 채워짐 (throw하지 않음)")
        void failReturnsCheckWithReason() {
            SellableMenuInvariant invariant = new SellableMenuInvariant(List.of());
            var result = invariant.check(MenuConfiguration.empty());
            assertThat(result.passed()).isFalse();
            assertThat(result.reason()).isNotBlank();
        }
    }

    private OptionGroup requiredGroup(long id, String name) {
        return OptionGroup.builder()
                .id(id)
                .name(name)
                .required(true)
                .options(Set.of(
                        Fixtures.anOption().id(id * 10 + 1).name("옵션A_" + id).build(),
                        Fixtures.anOption().id(id * 10 + 2).name("옵션B_" + id).build()
                ))
                .build();
    }
}
