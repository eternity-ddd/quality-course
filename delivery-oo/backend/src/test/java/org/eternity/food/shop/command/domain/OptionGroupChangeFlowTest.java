package org.eternity.food.shop.command.domain;

import org.eternity.food.Fixtures;
import org.eternity.food.base.generic.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("OptionGroupChangeFlow — 선행/후행 invariant + declarative patch")
class OptionGroupChangeFlowTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private OptionGroupRepository optionGroupRepository;

    @InjectMocks
    private OptionGroupChangeFlow flow;

    private OptionGroup baseOptionGroup() {
        return Fixtures.anOptionGroup()
                .id(1L)
                .name("기본")
                .required(true)
                .options(new HashSet<>(Set.of(
                        Fixtures.anOption().id(10L).name("소(250g)").price(Money.wons(12000)).build(),
                        Fixtures.anOption().id(11L).name("대(500g)").price(Money.wons(20000)).build()
                )))
                .build();
    }

    @Nested
    @DisplayName("판매중 메뉴 없음 — 검증 우회")
    class NoSellingMenu {

        @Test
        @DisplayName("findSellingMenus 비어 있으면 invariant 확인 없이 patch만 적용된다")
        void shortCircuitsWhenNoSellingMenus() {
            OptionGroup group = baseOptionGroup();
            given(menuRepository.findSellingMenus(group.getId())).willReturn(List.of());

            List<OptionPatch> patches = List.of(
                    new OptionPatch(10L, "소(250g)", Money.wons(12000)),
                    new OptionPatch(11L, "대(500g)", Money.wons(20000)),
                    new OptionPatch(null, "특대(1kg)", Money.wons(40000))
            );

            flow.updateOptions(group, patches);

            assertThat(group.getOptionSize()).isEqualTo(3);
            // optionGroupRepository는 sellingMenu가 없으므로 호출되지 않음
            verifyNoInteractions(optionGroupRepository);
        }
    }

    @Nested
    @DisplayName("판매중 메뉴 있음 — 선행 invariant 통과 후 patch 적용")
    class SellingMenuPreCheck {

        @Test
        @DisplayName("판매중 메뉴의 sellable invariant가 통과하면 patch가 적용된다")
        void appliesPatchWhenInvariantHolds() {
            OptionGroup group = baseOptionGroup();
            Menu sellingMenu = Fixtures.aMenu()
                    .configuration(MenuConfiguration.of(Set.of(
                            Fixtures.aMenuOptionGroup().optionGroupId(group.getId()).build()
                    )))
                    .build();

            given(menuRepository.findSellingMenus(group.getId())).willReturn(List.of(sellingMenu));
            // 메뉴가 참조하는 모든 옵션그룹 = group(자신)뿐. flow는 future 컨텍스트에서 group을 덮어 씀.
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(group));

            List<OptionPatch> patches = List.of(
                    new OptionPatch(10L, "소(250g)", Money.wons(13000)),  // 가격 변경
                    new OptionPatch(11L, "대(500g)", Money.wons(20000))
            );

            flow.updateOptions(group, patches);

            assertThat(group.findOption(10L).orElseThrow().getPrice()).isEqualTo(Money.wons(13000));
            verify(menuRepository).findSellingMenus(group.getId());
        }

        @Test
        @DisplayName("선행 invariant 위반(필수그룹 4개 — MAX_REQUIRED_GROUP=3 초과) — futureOptions 적용 안 됨")
        void preCheckBlocksPatchOnInvariantViolation() {
            OptionGroup group = baseOptionGroup(); // required=true (id=1L)

            // 메뉴가 4개의 필수 옵션그룹 참조 → SellableMenuInvariant C3 fail
            Set<MenuOptionGroup> mogs = Set.of(
                    Fixtures.aMenuOptionGroup().optionGroupId(1L).displayOrder(1).build(),
                    Fixtures.aMenuOptionGroup().optionGroupId(2L).displayOrder(2).build(),
                    Fixtures.aMenuOptionGroup().optionGroupId(3L).displayOrder(3).build(),
                    Fixtures.aMenuOptionGroup().optionGroupId(4L).displayOrder(4).build()
            );
            Menu sellingMenu = Fixtures.aMenu().configuration(MenuConfiguration.of(mogs)).build();

            given(menuRepository.findSellingMenus(group.getId())).willReturn(List.of(sellingMenu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(
                    group,
                    requiredOptionGroup(2L, "그룹2"),
                    requiredOptionGroup(3L, "그룹3"),
                    requiredOptionGroup(4L, "그룹4")
            ));

            List<OptionPatch> patches = List.of(
                    new OptionPatch(10L, "소(250g)", Money.wons(13000)),
                    new OptionPatch(11L, "대(500g)", Money.wons(20000))
            );

            assertThatThrownBy(() -> flow.updateOptions(group, patches))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("필수 옵션그룹의 갯수는 3개 이하");

            // patch가 적용되지 않아서 원래 가격(12000) 유지
            assertThat(group.findOption(10L).orElseThrow().getPrice()).isEqualTo(Money.wons(12000));
        }

        private OptionGroup requiredOptionGroup(long id, String name) {
            return Fixtures.anOptionGroup()
                    .id(id).name(name).required(true)
                    .options(new HashSet<>(Set.of(
                            Fixtures.anOption().id(id * 100 + 1).name("A_" + id).build(),
                            Fixtures.anOption().id(id * 100 + 2).name("B_" + id).build()
                    )))
                    .build();
        }

        @Test
        @DisplayName("future projection 단계에서 OptionGroup invariant 위반 시 IAE — patch 미적용")
        void futureProjectionEnforcesOptionGroupInvariant() {
            OptionGroup group = baseOptionGroup(); // required=true
            Menu sellingMenu = Fixtures.aMenu()
                    .configuration(MenuConfiguration.of(Set.of(
                            Fixtures.aMenuOptionGroup().optionGroupId(group.getId()).build()
                    )))
                    .build();

            given(menuRepository.findSellingMenus(group.getId())).willReturn(List.of(sellingMenu));

            // patch 결과 옵션 1개 → required=true OptionGroup의 buildFutureOptionGroupContext
            // 단계에서 new OptionGroup(...) 생성 시 validateOptions에서 IAE
            List<OptionPatch> singleOptionPatch = List.of(
                    new OptionPatch(10L, "소(250g)", Money.wons(12000))
            );

            assertThatThrownBy(() -> flow.updateOptions(group, singleOptionPatch))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("필수 옵션그룹의 옵션 갯수는 2개 이상");

            // 원본 group은 patch가 적용되지 않아 옵션 2개 유지
            assertThat(group.getOptionSize()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("future projection — id=null vs id 있음")
    class FutureProjection {

        @Test
        @DisplayName("id=null 신규 옵션과 기존 id 옵션이 섞여 있어도 sellable 통과 시 patch 적용")
        void mixedNewAndExistingPatches() {
            OptionGroup group = baseOptionGroup();
            Menu sellingMenu = Fixtures.aMenu()
                    .configuration(MenuConfiguration.of(Set.of(
                            Fixtures.aMenuOptionGroup().optionGroupId(group.getId()).build()
                    )))
                    .build();

            given(menuRepository.findSellingMenus(group.getId())).willReturn(List.of(sellingMenu));
            given(optionGroupRepository.findAllById(any())).willReturn(List.of(group));

            List<OptionPatch> patches = List.of(
                    new OptionPatch(10L, "소(250g)변경", Money.wons(13000)),  // 기존 id, rename
                    new OptionPatch(null, "신규옵션", Money.wons(5000)),       // 신규
                    new OptionPatch(11L, "대(500g)", Money.wons(20000))        // 기존 그대로
            );

            assertThatCode(() -> flow.updateOptions(group, patches)).doesNotThrowAnyException();
            assertThat(group.getOptionSize()).isEqualTo(3);
            assertThat(group.findOption("소(250g)변경")).isPresent();
            assertThat(group.findOption("신규옵션")).isPresent();
        }
    }

    @Nested
    @DisplayName("여러 판매중 메뉴 — 모든 메뉴에 대해 invariant 검증")
    class MultipleSellingMenus {

        @Test
        @DisplayName("여러 메뉴 중 하나라도 invariant 실패면 전체 실패 (C3 필수그룹 초과)")
        void anyMenuFailFailsAll() {
            OptionGroup group = baseOptionGroup(); // required=true (id=1L)

            // Menu A: group만 사용 (정상, 필수 1개)
            Menu menuA = Fixtures.aMenu()
                    .id(100L)
                    .configuration(MenuConfiguration.of(Set.of(
                            Fixtures.aMenuOptionGroup().optionGroupId(group.getId()).build()
                    )))
                    .build();

            // Menu B: 필수 그룹 4개 참조 → C3 fail
            Menu menuB = Fixtures.aMenu()
                    .id(101L)
                    .configuration(MenuConfiguration.of(Set.of(
                            Fixtures.aMenuOptionGroup().optionGroupId(1L).displayOrder(1).build(),
                            Fixtures.aMenuOptionGroup().optionGroupId(2L).displayOrder(2).build(),
                            Fixtures.aMenuOptionGroup().optionGroupId(3L).displayOrder(3).build(),
                            Fixtures.aMenuOptionGroup().optionGroupId(4L).displayOrder(4).build()
                    )))
                    .build();

            given(menuRepository.findSellingMenus(group.getId())).willReturn(List.of(menuA, menuB));
            given(optionGroupRepository.findAllById(any())).willReturn(new ArrayList<>(List.of(
                    group,
                    requiredOptionGroup(2L, "그룹2"),
                    requiredOptionGroup(3L, "그룹3"),
                    requiredOptionGroup(4L, "그룹4")
            )));

            List<OptionPatch> patches = List.of(
                    new OptionPatch(10L, "소(250g)", Money.wons(12000)),
                    new OptionPatch(11L, "대(500g)", Money.wons(20000))
            );

            assertThatThrownBy(() -> flow.updateOptions(group, patches))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("필수 옵션그룹의 갯수는 3개 이하");

            // 원본 group이 patch 적용되지 않음(변경된 가격이 없음 확인)
            assertThat(group.findOption(10L).orElseThrow().getPrice()).isEqualTo(Money.wons(12000));
        }

        private OptionGroup requiredOptionGroup(long id, String name) {
            return Fixtures.anOptionGroup()
                    .id(id).name(name).required(true)
                    .options(new HashSet<>(Set.of(
                            Fixtures.anOption().id(id * 100 + 1).name("A_" + id).build(),
                            Fixtures.anOption().id(id * 100 + 2).name("B_" + id).build()
                    )))
                    .build();
        }
    }
}
