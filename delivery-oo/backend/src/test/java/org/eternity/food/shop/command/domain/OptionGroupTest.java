package org.eternity.food.shop.command.domain;

import org.eternity.food.Fixtures;
import org.eternity.food.base.generic.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OptionGroup 도메인")
class OptionGroupTest {

    @Nested
    @DisplayName("name 불변식")
    class NameInvariant {

        @Test
        @DisplayName("name이 null이면 IAE")
        void nameNull() {
            assertThatThrownBy(() -> Fixtures.anOptionGroup().name(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션그룹명은 2글자 이상");
        }

        @ParameterizedTest(name = "name 길이 {0}이면 IAE")
        @ValueSource(strings = {"", "가"})
        @DisplayName("name이 2글자 미만이면 IAE")
        void nameTooShort(String name) {
            assertThatThrownBy(() -> Fixtures.anOptionGroup().name(name).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션그룹명은 2글자 이상");
        }

        @Test
        @DisplayName("name이 정확히 2글자(경계 통과)면 생성된다")
        void nameBoundaryPass() {
            OptionGroup group = Fixtures.anOptionGroup().name("기본").build();
            assertThat(group.getName()).isEqualTo("기본");
        }

        @Test
        @DisplayName("name이 정상 값이면 생성된다")
        void nameNormal() {
            OptionGroup group = Fixtures.anOptionGroup().name("사이즈선택").build();
            assertThat(group.getName()).isEqualTo("사이즈선택");
        }
    }

    @Nested
    @DisplayName("options 불변식")
    class OptionsInvariant {

        @Test
        @DisplayName("options가 null이면 IAE")
        void optionsNull() {
            assertThatThrownBy(() -> Fixtures.anOptionGroup().required(false).options(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션은 1개 이상");
        }

        @Test
        @DisplayName("options가 빈 Set이면 IAE")
        void optionsEmpty() {
            assertThatThrownBy(() -> Fixtures.anOptionGroup()
                    .required(false)
                    .options(new HashSet<>())
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션은 1개 이상");
        }

        @Test
        @DisplayName("options가 정확히 1개(경계 통과)이고 required=false면 생성된다")
        void optionsOneNotRequired() {
            OptionGroup group = Fixtures.anOptionGroup()
                    .required(false)
                    .options(Set.of(Fixtures.anOption().build()))
                    .build();
            assertThat(group.getOptionSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("options 이름이 unique하면 생성된다")
        void optionsUniqueName() {
            Set<Option> options = Set.of(
                    Fixtures.anOption().id(1L).name("소(250g)").build(),
                    Fixtures.anOption().id(2L).name("중(350g)").price(Money.wons(15000)).build(),
                    Fixtures.anOption().id(3L).name("대(500g)").price(Money.wons(20000)).build()
            );
            OptionGroup group = Fixtures.anOptionGroup().options(options).build();
            assertThat(group.getOptionSize()).isEqualTo(3);
        }

        @Test
        @DisplayName("options 이름이 중복되면 IAE")
        void optionsDuplicateName() {
            Set<Option> options = Set.of(
                    Fixtures.anOption().id(1L).name("같은이름").build(),
                    Fixtures.anOption().id(2L).name("같은이름").price(Money.wons(15000)).build()
            );
            assertThatThrownBy(() -> Fixtures.anOptionGroup().options(options).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션 이름이 중복");
        }
    }

    @Nested
    @DisplayName("required × options 불변식 (MIN_REQUIRED_OPTION=2)")
    class RequiredOptionsInvariant {

        @Test
        @DisplayName("required=true인데 옵션이 1개(경계 미만)면 IAE")
        void requiredWithOneOption() {
            assertThatThrownBy(() -> Fixtures.anOptionGroup()
                    .required(true)
                    .options(Set.of(Fixtures.anOption().build()))
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("필수 옵션그룹의 옵션 갯수는 2개 이상");
        }

        @Test
        @DisplayName("required=true이고 옵션이 정확히 2개(경계 통과)면 생성된다")
        void requiredWithBoundaryOptions() {
            OptionGroup group = Fixtures.anOptionGroup()
                    .required(true)
                    .options(Set.of(
                            Fixtures.anOption().id(1L).name("소(250g)").build(),
                            Fixtures.anOption().id(2L).name("대(500g)").price(Money.wons(20000)).build()
                    ))
                    .build();
            assertThat(group.isRequired()).isTrue();
            assertThat(group.getOptionSize()).isEqualTo(2);
        }

        @Test
        @DisplayName("required=true이고 옵션이 3개 이상이면 정상 생성")
        void requiredWithManyOptions() {
            OptionGroup group = Fixtures.anOptionGroup()
                    .required(true)
                    .options(Set.of(
                            Fixtures.anOption().id(1L).name("소(250g)").build(),
                            Fixtures.anOption().id(2L).name("중(350g)").price(Money.wons(15000)).build(),
                            Fixtures.anOption().id(3L).name("대(500g)").price(Money.wons(20000)).build()
                    ))
                    .build();
            assertThat(group.getOptionSize()).isEqualTo(3);
        }

        @Test
        @DisplayName("required=false인 경우 옵션이 1개만 있어도 통과한다")
        void notRequiredWithOneOption() {
            assertThatCode(() -> Fixtures.anOptionGroup()
                    .required(false)
                    .options(Set.of(Fixtures.anOption().build()))
                    .build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("findOption 행위")
    class FindOption {

        @Test
        @DisplayName("이름으로 옵션을 찾는다")
        void findByName() {
            OptionGroup group = Fixtures.anOptionGroup().build();
            assertThat(group.findOption("소(250g)")).isPresent();
        }

        @Test
        @DisplayName("없는 이름이면 Optional.empty")
        void findByNameNotFound() {
            OptionGroup group = Fixtures.anOptionGroup().build();
            assertThat(group.findOption("없는이름")).isEmpty();
        }

        @Test
        @DisplayName("id로 옵션을 찾는다")
        void findById() {
            OptionGroup group = Fixtures.anOptionGroup().build();
            assertThat(group.findOption(Fixtures.OPTION_ID)).isPresent();
        }
    }

    @Nested
    @DisplayName("getOptions 불변성")
    class OptionsImmutability {

        @Test
        @DisplayName("반환된 options Set은 수정 불가")
        void optionsUnmodifiable() {
            OptionGroup group = Fixtures.anOptionGroup().build();
            assertThatThrownBy(() -> group.getOptions().add(Fixtures.anOption().build()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("updateOptions(patches) — Style A declarative patch")
    class UpdateOptions {

        private OptionGroup buildGroup() {
            // 기본 옵션 2개: id=1 "소(250g)" 12000원, id=2 "대(500g)" 20000원
            return Fixtures.anOptionGroup()
                    .required(true)
                    .options(new HashSet<>(Set.of(
                            Fixtures.anOption().id(1L).name("소(250g)").price(Money.wons(12000)).build(),
                            Fixtures.anOption().id(2L).name("대(500g)").price(Money.wons(20000)).build()
                    )))
                    .build();
        }

        @Test
        @DisplayName("id=null patch는 신규 옵션으로 추가된다")
        void addsNewOptionWhenIdIsNull() {
            OptionGroup group = buildGroup();
            List<OptionPatch> patches = List.of(
                    new OptionPatch(1L, "소(250g)", Money.wons(12000)),
                    new OptionPatch(2L, "대(500g)", Money.wons(20000)),
                    new OptionPatch(null, "중(350g)", Money.wons(15000))
            );

            group.updateOptions(patches);

            assertThat(group.getOptionSize()).isEqualTo(3);
            assertThat(group.findOption("중(350g)")).isPresent();
        }

        @Test
        @DisplayName("기존 id patch는 rename/changePrice로 갱신 — identity 보존")
        void renamesAndChangesPriceForExisting() {
            OptionGroup group = buildGroup();
            Option original = group.findOption(1L).orElseThrow();

            List<OptionPatch> patches = List.of(
                    new OptionPatch(1L, "라지(750g)", Money.wons(30000)),
                    new OptionPatch(2L, "대(500g)", Money.wons(20000))
            );

            group.updateOptions(patches);

            Option updated = group.findOption(1L).orElseThrow();
            assertThat(updated).isSameAs(original); // identity 보존
            assertThat(updated.getName()).isEqualTo("라지(750g)");
            assertThat(updated.getPrice()).isEqualTo(Money.wons(30000));
            assertThat(group.getOptionSize()).isEqualTo(2);
        }

        @Test
        @DisplayName("patches에 빠진 기존 옵션은 삭제된다")
        void removesMissingOptions() {
            OptionGroup group = buildGroup();
            List<OptionPatch> patches = List.of(
                    // id=1만 유지, id=2 제거, 신규 1개 추가
                    new OptionPatch(1L, "소(250g)", Money.wons(12000)),
                    new OptionPatch(null, "특대(1kg)", Money.wons(40000))
            );

            // 필수=true이고 옵션 2개 유지 → 통과
            group.updateOptions(patches);

            assertThat(group.findOption(2L)).isEmpty();
            assertThat(group.findOption("특대(1kg)")).isPresent();
            assertThat(group.getOptionSize()).isEqualTo(2);
        }

        @Test
        @DisplayName("존재하지 않는 id가 patch에 있으면 IAE")
        void unknownIdThrows() {
            OptionGroup group = buildGroup();
            List<OptionPatch> patches = List.of(
                    new OptionPatch(1L, "소(250g)", Money.wons(12000)),
                    new OptionPatch(999L, "유령", Money.wons(1000))
            );

            assertThatThrownBy(() -> group.updateOptions(patches))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 옵션 id");
        }

        @Test
        @DisplayName("patch 결과로 옵션이 0개가 되면 validate 재실행에서 IAE")
        void emptyAfterPatchThrows() {
            // required=false라 1개만 있어도 valid했던 상태에서, 비어지는 patch
            OptionGroup group = Fixtures.anOptionGroup()
                    .required(false)
                    .options(new HashSet<>(Set.of(
                            Fixtures.anOption().id(1L).name("소(250g)").build()
                    )))
                    .build();

            assertThatThrownBy(() -> group.updateOptions(List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션은 1개 이상");
        }

        @Test
        @DisplayName("patch 결과 옵션명 중복이면 validate 재실행에서 IAE")
        void duplicateNameAfterPatchThrows() {
            OptionGroup group = buildGroup();
            List<OptionPatch> patches = List.of(
                    new OptionPatch(1L, "동일이름", Money.wons(12000)),
                    new OptionPatch(2L, "동일이름", Money.wons(20000))
            );

            assertThatThrownBy(() -> group.updateOptions(patches))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션 이름이 중복");
        }

        @Test
        @DisplayName("required=true에서 patch 결과 옵션이 1개면 validate 재실행에서 IAE")
        void requiredButOnlyOneOptionAfterPatchThrows() {
            OptionGroup group = buildGroup(); // required=true
            List<OptionPatch> patches = List.of(
                    new OptionPatch(1L, "소(250g)", Money.wons(12000))
            );

            assertThatThrownBy(() -> group.updateOptions(patches))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("필수 옵션그룹의 옵션 갯수는 2개 이상");
        }
    }
}
