package org.eternity.food.shop.command.service;

import org.eternity.food.base.generic.money.Money;
import org.eternity.food.shop.command.domain.OptionGroup;
import org.eternity.food.shop.command.domain.OptionGroupChangeFlow;
import org.eternity.food.shop.command.domain.OptionGroupRepository;
import org.eternity.food.shop.command.domain.OptionPatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.util.Sets.set;
import static org.eternity.food.Fixtures.OPTION_GROUP_ID;
import static org.eternity.food.Fixtures.anOption;
import static org.eternity.food.Fixtures.anOptionGroup;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OptionGroupCommandService")
class OptionGroupCommandServiceTest {

    @Mock
    private OptionGroupRepository optionGroupRepository;

    @Mock
    private OptionGroupChangeFlow optionGroupChangeFlow;

    @InjectMocks
    private OptionGroupCommandService service;

    private static OptionGroup validOptionGroup() {
        return anOptionGroup()
                .id(OPTION_GROUP_ID)
                .options(set(
                        anOption().id(1L).name("소(250g)").build(),
                        anOption().id(2L).name("대(500g)").build()
                ))
                .build();
    }

    @Nested
    @DisplayName("updateOptions")
    class UpdateOptions {

        @Test
        @DisplayName("OptionGroup을 찾을 수 없으면 IAE를 던지고 위임/저장을 하지 않는다")
        void throws_when_option_group_not_found() {
            UpdateOptionsCommand command = new UpdateOptionsCommand(
                    OPTION_GROUP_ID,
                    List.of(new OptionPatch(null, "신규", Money.wons(1000)))
            );
            given(optionGroupRepository.findById(OPTION_GROUP_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateOptions(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OptionGroup not found");

            verify(optionGroupChangeFlow, never()).updateOptions(any(), any());
            verify(optionGroupRepository, never()).save(any());
        }

        @Test
        @DisplayName("정상 흐름 — flow.updateOptions에 위임한 뒤 save를 호출한다 (호출 순서 보장)")
        void delegates_to_flow_then_saves() {
            OptionGroup optionGroup = validOptionGroup();
            List<OptionPatch> patches = List.of(
                    new OptionPatch(null, "추가옵션", Money.wons(3000))
            );
            UpdateOptionsCommand command = new UpdateOptionsCommand(OPTION_GROUP_ID, patches);
            given(optionGroupRepository.findById(OPTION_GROUP_ID)).willReturn(Optional.of(optionGroup));

            service.updateOptions(command);

            InOrder ordered = inOrder(optionGroupChangeFlow, optionGroupRepository);
            ordered.verify(optionGroupChangeFlow).updateOptions(optionGroup, patches);
            ordered.verify(optionGroupRepository).save(optionGroup);
        }

        @Test
        @DisplayName("flow가 ISE를 던지면(셀러블 invariant 위반) service 통과해 전파되고 save를 하지 않는다")
        void propagates_ise_from_flow_and_does_not_save() {
            OptionGroup optionGroup = validOptionGroup();
            List<OptionPatch> patches = List.of(
                    new OptionPatch(null, "추가옵션", Money.wons(3000))
            );
            UpdateOptionsCommand command = new UpdateOptionsCommand(OPTION_GROUP_ID, patches);
            given(optionGroupRepository.findById(OPTION_GROUP_ID)).willReturn(Optional.of(optionGroup));
            doThrow(new IllegalStateException("옵션 그룹 구성이 일치하지 않습니다."))
                    .when(optionGroupChangeFlow).updateOptions(optionGroup, patches);

            assertThatThrownBy(() -> service.updateOptions(command))
                    .isInstanceOf(IllegalStateException.class);

            verify(optionGroupRepository, never()).save(any());
        }

        @Test
        @DisplayName("flow가 IAE를 던지면(존재하지 않는 옵션 id) service 통과해 전파되고 save를 하지 않는다")
        void propagates_iae_from_flow_and_does_not_save() {
            OptionGroup optionGroup = validOptionGroup();
            List<OptionPatch> patches = List.of(
                    new OptionPatch(99999L, "이름변경", Money.wons(5000))
            );
            UpdateOptionsCommand command = new UpdateOptionsCommand(OPTION_GROUP_ID, patches);
            given(optionGroupRepository.findById(OPTION_GROUP_ID)).willReturn(Optional.of(optionGroup));
            doThrow(new IllegalArgumentException("존재하지 않는 옵션 id: [99999]"))
                    .when(optionGroupChangeFlow).updateOptions(optionGroup, patches);

            assertThatThrownBy(() -> service.updateOptions(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("존재하지 않는 옵션");

            verify(optionGroupRepository, never()).save(any());
        }
    }
}
