package org.eternity.food.shop.command.service;

import org.eternity.food.shop.command.domain.Menu;
import org.eternity.food.shop.command.domain.MenuConfiguration;
import org.eternity.food.shop.command.domain.MenuRepository;
import org.eternity.food.shop.command.domain.MenuStatus;
import org.eternity.food.shop.command.domain.OptionGroup;
import org.eternity.food.shop.command.domain.OptionGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.util.Sets.set;
import static org.eternity.food.Fixtures.MENU_ID;
import static org.eternity.food.Fixtures.OPTION_GROUP_ID;
import static org.eternity.food.Fixtures.aMenu;
import static org.eternity.food.Fixtures.aMenuOptionGroup;
import static org.eternity.food.Fixtures.anOption;
import static org.eternity.food.Fixtures.anOptionGroup;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MenuCommandService")
class MenuCommandServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private OptionGroupRepository optionGroupRepository;

    @InjectMocks
    private MenuCommandService service;

    /**
     * Fixtures.anOptionGroup의 기본 옵션 2개가 같은 id로 collapse되는 이슈를 회피하기 위한 헬퍼.
     * required=true 유효한 OptionGroup을 안전하게 생성.
     */
    private static OptionGroup validOptionGroup(Long id) {
        return anOptionGroup()
                .id(id)
                .options(set(
                        anOption().id(1L).name("소(250g)").build(),
                        anOption().id(2L).name("대(500g)").build()
                ))
                .build();
    }

    @Nested
    @DisplayName("open")
    class Open {

        @Test
        @DisplayName("메뉴를 찾을 수 없으면 IAE를 던진다")
        void throws_when_menu_not_found() {
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.open(MENU_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메뉴를 찾을 수 없습니다");

            verify(menuRepository, never()).save(any());
            verify(optionGroupRepository, never()).findAllById(anyIterable());
        }

        @Test
        @DisplayName("이미 OPEN 상태면 멱등 — save/optionGroup 조회 모두 호출하지 않는다")
        void idempotent_when_already_open() {
            Menu menu = aMenu().status(MenuStatus.OPEN).build();
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));

            service.open(MENU_ID);

            assertThat(menu.isOpen()).isTrue();
            verify(menuRepository, never()).save(any());
            verify(optionGroupRepository, never()).findAllById(anyIterable());
        }

        @Test
        @DisplayName("정상 흐름 — invariant 검증을 거쳐 OPEN으로 전이하고 save를 호출한다")
        void opens_menu_and_saves() {
            Menu menu = aMenu().status(MenuStatus.READY).build();
            OptionGroup optionGroup = validOptionGroup(OPTION_GROUP_ID);
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));
            given(optionGroupRepository.findAllById(menu.getOptionGroupIds()))
                    .willReturn(List.of(optionGroup));

            service.open(MENU_ID);

            ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
            verify(menuRepository).save(captor.capture());
            assertThat(captor.getValue().isOpen()).isTrue();
            verify(optionGroupRepository).findAllById(menu.getOptionGroupIds());
        }

        @Test
        @DisplayName("C2 위반 — 옵션그룹 ID 불일치 시 ISE가 service를 통과해 전파된다")
        void propagates_ise_when_option_group_ids_mismatch() {
            Menu menu = aMenu().status(MenuStatus.READY).build();
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));
            given(optionGroupRepository.findAllById(menu.getOptionGroupIds()))
                    .willReturn(List.of());

            assertThatThrownBy(() -> service.open(MENU_ID))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(menu.isOpen()).isFalse();
            verify(menuRepository, never()).save(any());
        }

        @Test
        @DisplayName("C1 위반 — empty 구성에서 open 시 ISE 전파 (configuration이 비어있음)")
        void propagates_ise_when_configuration_empty() {
            Menu menu = aMenu()
                    .status(MenuStatus.READY)
                    .configuration(MenuConfiguration.empty())
                    .build();
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));
            given(optionGroupRepository.findAllById(menu.getOptionGroupIds()))
                    .willReturn(List.of());

            assertThatThrownBy(() -> service.open(MENU_ID))
                    .isInstanceOf(IllegalStateException.class);

            verify(menuRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("close")
    class Close {

        @Test
        @DisplayName("메뉴를 찾을 수 없으면 IAE를 던진다")
        void throws_when_menu_not_found() {
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.close(MENU_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메뉴를 찾을 수 없습니다");

            verify(menuRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 READY 상태면 멱등 — save를 호출하지 않는다")
        void idempotent_when_already_ready() {
            Menu menu = aMenu().status(MenuStatus.READY).build();
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));

            service.close(MENU_ID);

            assertThat(menu.isOpen()).isFalse();
            verify(menuRepository, never()).save(any());
        }

        @Test
        @DisplayName("정상 흐름 — OPEN → READY 전이 후 save 호출")
        void closes_menu_and_saves() {
            Menu menu = aMenu().status(MenuStatus.OPEN).build();
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));

            service.close(MENU_ID);

            ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
            verify(menuRepository).save(captor.capture());
            assertThat(captor.getValue().isOpen()).isFalse();
        }
    }

    @Nested
    @DisplayName("changeMenuConfiguration")
    class ChangeMenuConfiguration {

        @Test
        @DisplayName("메뉴를 찾을 수 없으면 IAE를 던진다")
        void throws_when_menu_not_found() {
            ChangeMenuConfigurationCommand command = new ChangeMenuConfigurationCommand(
                    MENU_ID,
                    MenuConfiguration.of(set(aMenuOptionGroup().build()))
            );
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.changeMenuConfiguration(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메뉴를 찾을 수 없습니다");

            verify(menuRepository, never()).save(any());
            verify(optionGroupRepository, never()).findAllById(anyIterable());
        }

        @Test
        @DisplayName("READY 상태에서는 invariant를 건너뛰고 configuration을 교체한 뒤 save")
        void replaces_configuration_without_invariant_when_ready() {
            Menu menu = aMenu().status(MenuStatus.READY).build();
            MenuConfiguration newConfig = MenuConfiguration.of(set(
                    aMenuOptionGroup().optionGroupId(999L).displayOrder(1).build()
            ));
            ChangeMenuConfigurationCommand command = new ChangeMenuConfigurationCommand(MENU_ID, newConfig);
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));
            given(optionGroupRepository.findAllById(command.optionGroupIds()))
                    .willReturn(List.of());

            service.changeMenuConfiguration(command);

            ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
            verify(menuRepository).save(captor.capture());
            assertThat(captor.getValue().getConfiguration()).isEqualTo(newConfig);
        }

        @Test
        @DisplayName("OPEN 상태에서 정상 흐름 — invariant 통과 후 configuration 교체 + save")
        void replaces_configuration_with_invariant_when_open() {
            Menu menu = aMenu().status(MenuStatus.OPEN).build();
            MenuConfiguration newConfig = MenuConfiguration.of(set(aMenuOptionGroup().build()));
            ChangeMenuConfigurationCommand command = new ChangeMenuConfigurationCommand(MENU_ID, newConfig);
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));
            given(optionGroupRepository.findAllById(command.optionGroupIds()))
                    .willReturn(List.of(validOptionGroup(OPTION_GROUP_ID)));

            service.changeMenuConfiguration(command);

            verify(menuRepository).save(menu);
            assertThat(menu.getConfiguration()).isEqualTo(newConfig);
        }

        @Test
        @DisplayName("OPEN 상태에서 invariant(C2) 위반 — ISE 전파, save 미호출")
        void propagates_ise_when_open_and_invariant_violated() {
            Menu menu = aMenu().status(MenuStatus.OPEN).build();
            MenuConfiguration newConfig = MenuConfiguration.of(set(
                    aMenuOptionGroup().optionGroupId(999L).displayOrder(1).build()
            ));
            ChangeMenuConfigurationCommand command = new ChangeMenuConfigurationCommand(MENU_ID, newConfig);
            given(menuRepository.findById(MENU_ID)).willReturn(Optional.of(menu));
            given(optionGroupRepository.findAllById(command.optionGroupIds()))
                    .willReturn(List.of());

            assertThatThrownBy(() -> service.changeMenuConfiguration(command))
                    .isInstanceOf(IllegalStateException.class);

            verify(menuRepository, never()).save(any());
        }
    }
}
