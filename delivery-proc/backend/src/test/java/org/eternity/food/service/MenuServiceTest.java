package org.eternity.food.service;

import org.eternity.food.dto.MenuDto;
import org.eternity.food.entity.Menu;
import org.eternity.food.entity.MenuOptionGroup;
import org.eternity.food.entity.Option;
import org.eternity.food.entity.OptionGroup;
import org.eternity.food.repository.OptionRepository;
import org.eternity.food.repository.MenuRepository;
import org.eternity.food.repository.OptionGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eternity.food.Fixtures.MENU_ID;
import static org.eternity.food.Fixtures.OPTION_GROUP_ID;
import static org.eternity.food.Fixtures.SHOP_ID;
import static org.eternity.food.Fixtures.aMenu;
import static org.eternity.food.Fixtures.anOption;
import static org.eternity.food.Fixtures.aMenuOptionGroup;
import static org.eternity.food.Fixtures.anOptionGroup;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MenuService")
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private OptionGroupRepository optionGroupRepository;

    @Mock
    private OptionRepository optionRepository;

    @InjectMocks
    private MenuService service;

    private static OptionGroup validOptionalGroup(Long id) {
        OptionGroup og = anOptionGroup()
                .id(id)
                .required(false)
                .options(new ArrayList<>(List.of(
                        anOption().id(1L).name("소(250g)").build()
                )))
                .build();
        return og;
    }

    private static OptionGroup validRequiredGroup(Long id) {
        return anOptionGroup()
                .id(id)
                .required(true)
                .options(new ArrayList<>(List.of(
                        anOption().id(1L).name("소(250g)").build(),
                        anOption().id(2L).name("대(500g)").build()
                )))
                .build();
    }

    @Nested
    @DisplayName("findByShopId")
    class FindByShopId {

        @Test
        @DisplayName("정상 — Menu 리스트를 DTO Item으로 매핑한다")
        void maps_menus_to_items() {
            Menu m1 = aMenu().id(1L).name("삼겹살").basePrice(10_000L).description("desc1").build();
            Menu m2 = aMenu().id(2L).name("목살").basePrice(12_000L).description("desc2").build();
            given(menuRepository.findByShopIdOrderById(SHOP_ID)).willReturn(List.of(m1, m2));

            List<MenuDto.Item> result = service.findByShopId(SHOP_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(0).name()).isEqualTo("삼겹살");
            assertThat(result.get(0).price()).isEqualTo(10_000L);
            assertThat(result.get(1).id()).isEqualTo(2L);
            assertThat(result.get(1).price()).isEqualTo(12_000L);
        }

        @Test
        @DisplayName("정상 — 결과가 없으면 빈 리스트를 반환한다 (예외 아님)")
        void returns_empty_when_no_menus() {
            given(menuRepository.findByShopIdOrderById(SHOP_ID)).willReturn(List.of());

            List<MenuDto.Item> result = service.findByShopId(SHOP_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findDetail")
    class FindDetail {

        @Test
        @DisplayName("부재 — 메뉴를 찾을 수 없으면 IAE")
        void throws_when_menu_not_found() {
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.findDetail(MENU_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메뉴를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("정상 — 옵션그룹/옵션을 펼쳐 Detail DTO로 반환")
        void returns_detail_with_option_groups() {
            Menu menu = aMenu()
                    .optionGroups(new ArrayList<>(List.of(aMenuOptionGroup().build())))
                    .build();
            OptionGroup og = validRequiredGroup(OPTION_GROUP_ID);
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));
            given(optionGroupRepository.findAllById(List.of(OPTION_GROUP_ID)))
                    .willReturn(List.of(og));

            MenuDto.Detail detail = service.findDetail(MENU_ID);

            assertThat(detail.id()).isEqualTo(MENU_ID);
            assertThat(detail.name()).isEqualTo("삼겹살 1인세트");
            assertThat(detail.optionGroups()).hasSize(1);
            assertThat(detail.optionGroups().get(0).id()).isEqualTo(OPTION_GROUP_ID);
            assertThat(detail.optionGroups().get(0).required()).isTrue();
            assertThat(detail.optionGroups().get(0).options()).hasSize(2);
            assertThat(detail.optionGroups().get(0).options().get(0).name()).isEqualTo("소(250g)");
        }

        @Test
        @DisplayName("정상 — optionGroups가 비어있어도 정상 반환 (조회는 비고)")
        void returns_detail_with_empty_groups() {
            Menu menu = aMenu().optionGroups(new ArrayList<>()).build();
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));

            MenuDto.Detail detail = service.findDetail(MENU_ID);

            assertThat(detail.optionGroups()).isEmpty();
            verify(optionGroupRepository, never()).findAllById(anyIterable());
        }

        @Test
        @DisplayName("정상 — menu가 참조하는 OptionGroup이 DB에 없으면 silent skip")
        void silently_skips_when_option_group_missing_in_db() {
            Menu menu = aMenu()
                    .optionGroups(new ArrayList<>(List.of(aMenuOptionGroup().build())))
                    .build();
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));
            given(optionGroupRepository.findAllById(List.of(OPTION_GROUP_ID)))
                    .willReturn(List.of());

            MenuDto.Detail detail = service.findDetail(MENU_ID);

            assertThat(detail.optionGroups()).isEmpty();
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        private final List<MenuOptionGroup> validMogs = new ArrayList<>(
                List.of(aMenuOptionGroup().optionGroupId(OPTION_GROUP_ID).displayOrder(1).build()));

        @Test
        @DisplayName("정상 — 저장 후 Menu 반환, status=READY")
        void saves_menu_with_ready_status() {
            given(menuRepository.save(any(Menu.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            Menu saved = service.create(SHOP_ID, "삼겹살", "맛있는 삼겹살", 10_000L, validMogs);

            ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
            verify(menuRepository).save(captor.capture());
            Menu sent = captor.getValue();
            assertThat(sent.getShopId()).isEqualTo(SHOP_ID);
            assertThat(sent.getName()).isEqualTo("삼겹살");
            assertThat(sent.getBasePrice()).isEqualTo(10_000L);
            assertThat(sent.getStatus()).isEqualTo("READY");
            assertThat(sent.getOptionGroups()).hasSize(1);
            assertThat(saved).isSameAs(sent);
        }

        @Test
        @DisplayName("실패 — basePrice null이면 IAE")
        void throws_when_basePrice_null() {
            assertThatThrownBy(() ->
                    service.create(SHOP_ID, "삼겹살", "desc", null, validMogs))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("basePrice");

            verify(menuRepository, never()).save(any());
        }

        @Test
        @DisplayName("경계 실패 — basePrice 0이면 IAE")
        void throws_when_basePrice_zero() {
            assertThatThrownBy(() ->
                    service.create(SHOP_ID, "삼겹살", "desc", 0L, validMogs))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("기본가는 0원보다는 커야합니다");
        }

        @Test
        @DisplayName("실패 — basePrice 음수이면 IAE")
        void throws_when_basePrice_negative() {
            assertThatThrownBy(() ->
                    service.create(SHOP_ID, "삼겹살", "desc", -1L, validMogs))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("기본가는 0원보다는 커야합니다");
        }

        @Test
        @DisplayName("경계 통과 — basePrice 1이면 통과")
        void accepts_basePrice_one() {
            given(menuRepository.save(any(Menu.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            service.create(SHOP_ID, "삼겹살", "desc", 1L, validMogs);

            verify(menuRepository).save(any(Menu.class));
        }

        @Test
        @DisplayName("실패 — optionGroups null이면 IAE")
        void throws_when_optionGroups_null() {
            assertThatThrownBy(() ->
                    service.create(SHOP_ID, "삼겹살", "desc", 10_000L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("configuration");
        }

        @Test
        @DisplayName("실패 — 같은 optionGroupId 중복이면 IAE")
        void throws_when_duplicate_optionGroupId() {
            List<MenuOptionGroup> dup = List.of(
                    aMenuOptionGroup().optionGroupId(OPTION_GROUP_ID).displayOrder(1).build(),
                    aMenuOptionGroup().optionGroupId(OPTION_GROUP_ID).displayOrder(2).build()
            );

            assertThatThrownBy(() ->
                    service.create(SHOP_ID, "삼겹살", "desc", 10_000L, dup))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("중복된 옵션 그룹");

            verify(menuRepository, never()).save(any());
        }

        @Test
        @DisplayName("경계 통과 — 빈 리스트는 configuration null 검증을 통과 (생성 시점)")
        void accepts_empty_optionGroups() {
            given(menuRepository.save(any(Menu.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            service.create(SHOP_ID, "삼겹살", "desc", 10_000L, new ArrayList<>());

            verify(menuRepository).save(any(Menu.class));
        }

        @Test
        @DisplayName("실패 — optionGroupId null이면 IAE (validateMenuOptionGroup 직접)")
        void throws_when_mog_optionGroupId_null() {
            MenuOptionGroup mog = aMenuOptionGroup().optionGroupId(null).displayOrder(1).build();

            assertThatThrownBy(() -> service.validateMenuOptionGroup(mog))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("optionGroupId");
        }

        @Test
        @DisplayName("실패 — mog 자체가 null이면 IAE")
        void throws_when_mog_is_null() {
            assertThatThrownBy(() -> service.validateMenuOptionGroup(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("menuOptionGroup");
        }

        @Test
        @DisplayName("실패 — menuOptionGroups 자체가 null이면 IAE")
        void throws_when_configuration_is_null_direct() {
            assertThatThrownBy(() -> service.validateMenuConfiguration(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션그룹은 null");
        }

        @Test
        @DisplayName("create 경유 시 MenuConfiguration 검증에 의해 IAE로 차단 (방어선 중복)")
        void create_blocks_null_optionGroupId_via_configuration() {
            List<MenuOptionGroup> mogs = List.of(
                    aMenuOptionGroup().optionGroupId(null).displayOrder(1).build()
            );

            assertThatThrownBy(() ->
                    service.create(SHOP_ID, "삼겹살", "desc", 10_000L, mogs))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(menuRepository, never()).save(any());
        }

        @Test
        @DisplayName("경계 실패 — displayOrder 0이면 IAE")
        void throws_when_displayOrder_zero() {
            List<MenuOptionGroup> mogs = List.of(
                    aMenuOptionGroup().optionGroupId(OPTION_GROUP_ID).displayOrder(0).build()
            );

            assertThatThrownBy(() ->
                    service.create(SHOP_ID, "삼겹살", "desc", 10_000L, mogs))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("displayOrder");
        }

        @Test
        @DisplayName("실패 — displayOrder null이면 IAE")
        void throws_when_displayOrder_null() {
            List<MenuOptionGroup> mogs = List.of(
                    aMenuOptionGroup().optionGroupId(OPTION_GROUP_ID).displayOrder(null).build()
            );

            assertThatThrownBy(() ->
                    service.create(SHOP_ID, "삼겹살", "desc", 10_000L, mogs))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("displayOrder");
        }

        @Test
        @DisplayName("경계 통과 — displayOrder 1이면 통과")
        void accepts_displayOrder_one() {
            given(menuRepository.save(any(Menu.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            List<MenuOptionGroup> mogs = List.of(
                    aMenuOptionGroup().optionGroupId(OPTION_GROUP_ID).displayOrder(1).build()
            );

            service.create(SHOP_ID, "삼겹살", "desc", 10_000L, mogs);

            verify(menuRepository).save(any(Menu.class));
        }
    }

    @Nested
    @DisplayName("createOptionGroup")
    class CreateOptionGroup {

        private List<Option> validOptions() {
            return new ArrayList<>(List.of(
                    anOption().id(1L).name("소(250g)").price(12_000L).build(),
                    anOption().id(2L).name("대(500g)").price(20_000L).build()
            ));
        }

        @Test
        @DisplayName("정상 — 검증 후 저장 (required=true, 옵션 2개)")
        void saves_option_group_when_valid() {
            given(optionGroupRepository.save(any(OptionGroup.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            OptionGroup result = service.createOptionGroup("기본", true, validOptions());

            ArgumentCaptor<OptionGroup> captor = ArgumentCaptor.forClass(OptionGroup.class);
            verify(optionGroupRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("기본");
            assertThat(captor.getValue().getRequired()).isTrue();
            assertThat(captor.getValue().getOptions()).hasSize(2);
            assertThat(result).isSameAs(captor.getValue());
        }

        @Test
        @DisplayName("실패 — name null이면 IAE")
        void throws_when_name_null() {
            assertThatThrownBy(() ->
                    service.createOptionGroup(null, false, validOptions()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션그룹명");
        }

        @Test
        @DisplayName("경계 실패 — name 1글자면 IAE")
        void throws_when_name_one_char() {
            assertThatThrownBy(() ->
                    service.createOptionGroup("A", false, validOptions()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("2글자 이상");
        }

        @Test
        @DisplayName("경계 통과 — name 2글자면 통과")
        void accepts_name_two_chars() {
            given(optionGroupRepository.save(any(OptionGroup.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            service.createOptionGroup("AB", false, validOptions());

            verify(optionGroupRepository).save(any(OptionGroup.class));
        }

        @Test
        @DisplayName("실패 — options null이면 IAE")
        void throws_when_options_null() {
            assertThatThrownBy(() ->
                    service.createOptionGroup("기본", false, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션은 1개 이상");
        }

        @Test
        @DisplayName("경계 실패 — options 빈 리스트면 IAE")
        void throws_when_options_empty() {
            assertThatThrownBy(() ->
                    service.createOptionGroup("기본", false, new ArrayList<>()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션은 1개 이상");
        }

        @Test
        @DisplayName("경계 통과 — options 1개 + required=false면 통과")
        void accepts_one_option_when_not_required() {
            given(optionGroupRepository.save(any(OptionGroup.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            List<Option> one = new ArrayList<>(List.of(
                    anOption().id(1L).name("소(250g)").price(12_000L).build()
            ));

            service.createOptionGroup("기본", false, one);

            verify(optionGroupRepository).save(any(OptionGroup.class));
        }

        @Test
        @DisplayName("실패 — 옵션 이름이 중복이면 IAE")
        void throws_when_option_names_duplicate() {
            List<Option> dup = new ArrayList<>(List.of(
                    anOption().id(1L).name("소(250g)").price(12_000L).build(),
                    anOption().id(2L).name("소(250g)").price(12_000L).build()
            ));

            assertThatThrownBy(() ->
                    service.createOptionGroup("기본", false, dup))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션 이름이 중복");
        }

        @Test
        @DisplayName("경계 실패 — required=true, options 1개면 IAE")
        void throws_when_required_with_one_option() {
            List<Option> one = new ArrayList<>(List.of(
                    anOption().id(1L).name("소(250g)").price(12_000L).build()
            ));

            assertThatThrownBy(() ->
                    service.createOptionGroup("기본", true, one))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("필수 옵션그룹의 옵션 갯수는 2개 이상");
        }

        @Test
        @DisplayName("경계 통과 — required=true, options 2개면 통과")
        void accepts_required_with_two_options() {
            given(optionGroupRepository.save(any(OptionGroup.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            service.createOptionGroup("기본", true, validOptions());

            verify(optionGroupRepository).save(any(OptionGroup.class));
        }

        @Test
        @DisplayName("Option 검증 위임 — option name이 1글자면 IAE (loop 검증)")
        void throws_when_option_name_invalid_inside_group() {
            List<Option> bad = new ArrayList<>(List.of(
                    anOption().id(1L).name("A").price(12_000L).build(),
                    anOption().id(2L).name("대(500g)").price(20_000L).build()
            ));

            assertThatThrownBy(() ->
                    service.createOptionGroup("기본", false, bad))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션명");
        }
    }

    @Nested
    @DisplayName("createOption")
    class CreateOption {

        @Test
        @DisplayName("정상 — 검증 통과 후 저장 (price 0 경계 포함)")
        void saves_option_when_valid() {
            given(optionRepository.save(any(Option.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            Option result = service.createOption(OPTION_GROUP_ID, "기본맛", 0L);

            ArgumentCaptor<Option> captor = ArgumentCaptor.forClass(Option.class);
            verify(optionRepository).save(captor.capture());
            assertThat(captor.getValue().getOptionGroupId()).isEqualTo(OPTION_GROUP_ID);
            assertThat(captor.getValue().getName()).isEqualTo("기본맛");
            assertThat(captor.getValue().getPrice()).isEqualTo(0L);
            assertThat(result).isSameAs(captor.getValue());
        }

        @Test
        @DisplayName("실패 — name null이면 IAE")
        void throws_when_name_null() {
            assertThatThrownBy(() ->
                    service.createOption(OPTION_GROUP_ID, null, 12_000L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션명");
        }

        @Test
        @DisplayName("경계 실패 — name 1글자면 IAE")
        void throws_when_name_one_char() {
            assertThatThrownBy(() ->
                    service.createOption(OPTION_GROUP_ID, "A", 12_000L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("2글자 이상");
        }

        @Test
        @DisplayName("경계 통과 — name 2글자면 통과")
        void accepts_name_two_chars() {
            given(optionRepository.save(any(Option.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            service.createOption(OPTION_GROUP_ID, "AB", 12_000L);

            verify(optionRepository).save(any(Option.class));
        }

        @Test
        @DisplayName("실패 — price null이면 IAE")
        void throws_when_price_null() {
            assertThatThrownBy(() ->
                    service.createOption(OPTION_GROUP_ID, "소(250g)", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("옵션 가격");
        }

        @Test
        @DisplayName("경계 실패 — price 음수이면 IAE")
        void throws_when_price_negative() {
            assertThatThrownBy(() ->
                    service.createOption(OPTION_GROUP_ID, "소(250g)", -1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("0원 이상");
        }

        @Test
        @DisplayName("경계 통과 — price 0이면 통과")
        void accepts_price_zero() {
            given(optionRepository.save(any(Option.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            service.createOption(OPTION_GROUP_ID, "기본맛", 0L);

            verify(optionRepository).save(any(Option.class));
        }

        @Test
        @DisplayName("optionGroupId 실패 — null이면 IAE")
        void throws_when_optionGroupId_null() {
            assertThatThrownBy(() ->
                    service.createOption(null, "소(250g)", 12_000L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("optionGroupId");

            verify(optionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("openMenu")
    class OpenMenu {

        @Test
        @DisplayName("부재 — 메뉴를 찾을 수 없으면 IAE")
        void throws_when_menu_not_found() {
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.openMenu(MENU_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메뉴를 찾을 수 없습니다");

            verify(menuRepository, never()).save(any());
            verify(optionGroupRepository, never()).findAllById(anyIterable());
        }

        @Test
        @DisplayName("멱등 — 이미 OPEN이면 검증/save 모두 건너뜀")
        void idempotent_when_already_open() {
            Menu menu = aMenu().status("OPEN").build();
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));

            service.openMenu(MENU_ID);

            assertThat(menu.getStatus()).isEqualTo("OPEN");
            verify(menuRepository, never()).save(any());
            verify(optionGroupRepository, never()).findAllById(anyIterable());
        }

        @Test
        @DisplayName("정상 — READY → OPEN 전이 + save 호출")
        void opens_menu_when_sellable() {
            Menu menu = aMenu()
                    .status("READY")
                    .optionGroups(new ArrayList<>(List.of(
                            aMenuOptionGroup().optionGroupId(OPTION_GROUP_ID).displayOrder(1).build())))
                    .build();
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));
            given(optionGroupRepository.findAllById(List.of(OPTION_GROUP_ID)))
                    .willReturn(List.of(validOptionalGroup(OPTION_GROUP_ID)));

            service.openMenu(MENU_ID);

            ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
            verify(menuRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("OPEN");
        }

        @Test
        @DisplayName("실패 — validateSellable에 mogs=null인 menu 직접 주면 ISE")
        void throws_ise_when_mogs_null_direct() {
            Menu menu = aMenu().status("READY").build();
            menu.setOptionGroups(null);

            assertThatThrownBy(() -> service.validateSellable(menu, List.of()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("옵션그룹이 1개 이상");
        }

        @Test
        @DisplayName("실패 — optionGroups 비어있으면 ISE, save 미호출")
        void throws_ise_when_no_option_groups() {
            Menu menu = aMenu()
                    .status("READY")
                    .optionGroups(new ArrayList<>())
                    .build();
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));

            assertThatThrownBy(() -> service.openMenu(MENU_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("옵션그룹이 1개 이상");

            assertThat(menu.getStatus()).isEqualTo("READY");
            verify(menuRepository, never()).save(any());
            verify(optionGroupRepository, never()).findAllById(anyIterable());
        }

        @Test
        @DisplayName("경계 통과 — optionGroups 1개면 통과")
        void accepts_one_option_group() {
            Menu menu = aMenu()
                    .status("READY")
                    .optionGroups(new ArrayList<>(List.of(
                            aMenuOptionGroup().optionGroupId(OPTION_GROUP_ID).displayOrder(1).build())))
                    .build();
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));
            given(optionGroupRepository.findAllById(List.of(OPTION_GROUP_ID)))
                    .willReturn(List.of(validOptionalGroup(OPTION_GROUP_ID)));

            service.openMenu(MENU_ID);

            verify(menuRepository).save(any(Menu.class));
        }

        @Test
        @DisplayName("실패 — optionGroupId가 실제 OptionGroup ID와 불일치 시 ISE")
        void throws_ise_when_ids_mismatch() {
            Menu menu = aMenu()
                    .status("READY")
                    .optionGroups(new ArrayList<>(List.of(
                            aMenuOptionGroup().optionGroupId(OPTION_GROUP_ID).displayOrder(1).build())))
                    .build();
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));
            given(optionGroupRepository.findAllById(List.of(OPTION_GROUP_ID)))
                    .willReturn(List.of());

            assertThatThrownBy(() -> service.openMenu(MENU_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("옵션 그룹 구성이 일치하지 않습니다");

            verify(menuRepository, never()).save(any());
        }

        @Test
        @DisplayName("경계 실패 — 필수 옵션그룹 4개면 ISE")
        void throws_ise_when_too_many_required_groups() {
            List<MenuOptionGroup> mogs = new ArrayList<>(List.of(
                    aMenuOptionGroup().optionGroupId(1L).displayOrder(1).build(),
                    aMenuOptionGroup().optionGroupId(2L).displayOrder(2).build(),
                    aMenuOptionGroup().optionGroupId(3L).displayOrder(3).build(),
                    aMenuOptionGroup().optionGroupId(4L).displayOrder(4).build()
            ));
            Menu menu = aMenu().status("READY").optionGroups(mogs).build();
            List<Long> ids = List.of(1L, 2L, 3L, 4L);
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));
            given(optionGroupRepository.findAllById(ids))
                    .willReturn(List.of(
                            validRequiredGroup(1L),
                            validRequiredGroup(2L),
                            validRequiredGroup(3L),
                            validRequiredGroup(4L)
                    ));

            assertThatThrownBy(() -> service.openMenu(MENU_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("필수 옵션그룹의 갯수는 3개 이하");

            verify(menuRepository, never()).save(any());
        }

        @Test
        @DisplayName("경계 통과 — 필수 옵션그룹 3개면 통과")
        void accepts_three_required_groups() {
            List<MenuOptionGroup> mogs = new ArrayList<>(List.of(
                    aMenuOptionGroup().optionGroupId(1L).displayOrder(1).build(),
                    aMenuOptionGroup().optionGroupId(2L).displayOrder(2).build(),
                    aMenuOptionGroup().optionGroupId(3L).displayOrder(3).build()
            ));
            Menu menu = aMenu().status("READY").optionGroups(mogs).build();
            List<Long> ids = List.of(1L, 2L, 3L);
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));
            given(optionGroupRepository.findAllById(ids))
                    .willReturn(List.of(
                            validRequiredGroup(1L),
                            validRequiredGroup(2L),
                            validRequiredGroup(3L)
                    ));

            service.openMenu(MENU_ID);

            verify(menuRepository).save(any(Menu.class));
        }
    }

    @Nested
    @DisplayName("closeMenu")
    class CloseMenu {

        @Test
        @DisplayName("부재 — 메뉴를 찾을 수 없으면 IAE")
        void throws_when_menu_not_found() {
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.closeMenu(MENU_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메뉴를 찾을 수 없습니다");

            verify(menuRepository, never()).save(any());
        }

        @Test
        @DisplayName("정상 — OPEN → READY 전이 + save 호출")
        void closes_menu() {
            Menu menu = aMenu().status("OPEN").build();
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));

            service.closeMenu(MENU_ID);

            ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
            verify(menuRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("READY");
        }

        @Test
        @DisplayName("멱등 — 이미 READY면 save 미호출")
        void idempotent_when_already_ready() {
            Menu menu = aMenu().status("READY").build();
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));

            service.closeMenu(MENU_ID);

            assertThat(menu.getStatus()).isEqualTo("READY");
            verify(menuRepository, never()).save(any());
        }
    }
}
