package org.eternity.food.shop.command.domain;

import org.eternity.food.Fixtures;
import org.eternity.food.base.generic.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Menu 도메인")
class MenuTest {

    @Nested
    @DisplayName("basePrice 불변식")
    class BasePriceInvariant {

        @Test
        @DisplayName("basePrice가 null이면 IAE")
        void basePriceNull() {
            assertThatThrownBy(() -> Fixtures.aMenu().basePrice(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("basePrice는 null");
        }

        @Test
        @DisplayName("basePrice가 0원(경계 미만)이면 IAE")
        void basePriceZero() {
            assertThatThrownBy(() -> Fixtures.aMenu().basePrice(Money.ZERO).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("기본가는 0원보다는 커야");
        }

        @Test
        @DisplayName("basePrice가 음수이면 IAE")
        void basePriceNegative() {
            assertThatThrownBy(() -> Fixtures.aMenu().basePrice(Money.wons(-100)).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("기본가는 0원보다는 커야");
        }

        @Test
        @DisplayName("basePrice가 1원(경계 통과)이면 생성된다")
        void basePriceBoundaryPass() {
            Menu menu = Fixtures.aMenu().basePrice(Money.wons(1)).build();
            assertThat(menu.getBasePrice()).isEqualTo(Money.wons(1));
        }

        @Test
        @DisplayName("basePrice가 정상 값이면 생성된다")
        void basePriceNormal() {
            Menu menu = Fixtures.aMenu().basePrice(Money.wons(10_000)).build();
            assertThat(menu.getBasePrice()).isEqualTo(Money.wons(10_000));
        }
    }

    @Nested
    @DisplayName("configuration 불변식")
    class ConfigurationInvariant {

        @Test
        @DisplayName("configuration이 null이면 IAE")
        void configurationNull() {
            assertThatThrownBy(() -> Fixtures.aMenu().configuration(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("configuration은 null");
        }

        @Test
        @DisplayName("configuration이 empty여도 생성된다 (구조 invariant 통과)")
        void configurationEmptyAllowed() {
            Menu menu = Fixtures.aMenu().configuration(MenuConfiguration.empty()).build();
            assertThat(menu.getConfiguration().isEmpty()).isTrue();
        }

        @Test
        @DisplayName("정상 configuration이면 생성된다")
        void configurationNormal() {
            Menu menu = Fixtures.aMenu().build();
            assertThat(menu.getConfiguration().isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("간이 생성자 (Menu(shopId, name, description, basePrice))")
    class SimpleConstructor {

        @Test
        @DisplayName("간이 생성자는 status=READY, configuration=empty")
        void simpleConstructorDefaults() {
            Menu menu = new Menu(1L, "메뉴이름", "설명", Money.wons(10000));
            assertThat(menu.getStatus()).isEqualTo(MenuStatus.READY);
            assertThat(menu.getConfiguration().isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("상태 머신: open()")
    class OpenStateMachine {

        @Test
        @DisplayName("READY 상태에서 open()하면 OPEN으로 전이")
        void readyToOpen() {
            Menu menu = Fixtures.aMenu().status(MenuStatus.READY).build();
            SellableMenuInvariant invariant = sellableInvariant();
            menu.open(invariant);
            assertThat(menu.isOpen()).isTrue();
            assertThat(menu.getStatus()).isEqualTo(MenuStatus.OPEN);
        }

        @Test
        @DisplayName("이미 OPEN 상태에서 open()을 다시 호출하면 ISE (멱등 아님 — terminal)")
        void openWhenAlreadyOpenThrows() {
            Menu menu = Fixtures.aMenu().status(MenuStatus.OPEN).build();
            SellableMenuInvariant invariant = sellableInvariant();
            assertThatThrownBy(() -> menu.open(invariant))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 판매중");
        }

        @Test
        @DisplayName("open() 시 SellableMenuInvariant 위반(빈 configuration)이면 ISE")
        void openFailsOnEmptyConfiguration() {
            Menu menu = Fixtures.aMenu()
                    .status(MenuStatus.READY)
                    .configuration(MenuConfiguration.empty())
                    .build();
            SellableMenuInvariant invariant = new SellableMenuInvariant(List.of());
            assertThatThrownBy(() -> menu.open(invariant))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("옵션그룹이 1개 이상");
        }
    }

    @Nested
    @DisplayName("상태 머신: close()")
    class CloseStateMachine {

        @Test
        @DisplayName("OPEN 상태에서 close()하면 READY로 전이")
        void openToReady() {
            Menu menu = Fixtures.aMenu().status(MenuStatus.OPEN).build();
            menu.close();
            assertThat(menu.isOpen()).isFalse();
            assertThat(menu.getStatus()).isEqualTo(MenuStatus.READY);
        }

        @Test
        @DisplayName("이미 READY 상태에서 close()를 호출하면 ISE")
        void closeWhenReadyThrows() {
            Menu menu = Fixtures.aMenu().status(MenuStatus.READY).build();
            assertThatThrownBy(menu::close)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("판매중이 아닌");
        }
    }

    @Nested
    @DisplayName("canOpen / canClose 쿼리")
    class StateQueries {

        @Test
        @DisplayName("READY → canOpen=true, canClose=false")
        void readyState() {
            Menu menu = Fixtures.aMenu().status(MenuStatus.READY).build();
            assertThat(menu.canOpen()).isTrue();
            assertThat(menu.canClose()).isFalse();
        }

        @Test
        @DisplayName("OPEN → canOpen=false, canClose=true")
        void openState() {
            Menu menu = Fixtures.aMenu().status(MenuStatus.OPEN).build();
            assertThat(menu.canOpen()).isFalse();
            assertThat(menu.canClose()).isTrue();
        }
    }

    @Nested
    @DisplayName("changeConfiguration 행위")
    class ChangeConfiguration {

        @Test
        @DisplayName("newConfiguration이 null이면 IAE")
        void changeConfigurationNull() {
            Menu menu = Fixtures.aMenu().status(MenuStatus.READY).build();
            SellableMenuInvariant invariant = sellableInvariant();
            assertThatThrownBy(() -> menu.changeConfiguration(null, invariant))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("configuration은 null");
        }

        @Test
        @DisplayName("READY 상태에서는 invariant 검증을 건너뛰고 변경된다 (empty도 허용)")
        void changeConfigurationWhenReadySkipInvariant() {
            Menu menu = Fixtures.aMenu().status(MenuStatus.READY).build();
            SellableMenuInvariant invariant = new SellableMenuInvariant(List.of());
            menu.changeConfiguration(MenuConfiguration.empty(), invariant);
            assertThat(menu.getConfiguration().isEmpty()).isTrue();
        }

        @Test
        @DisplayName("OPEN 상태에서 invariant 위반 시 ISE를 던지고 configuration이 보존된다")
        void changeConfigurationWhenOpenInvariantFails() {
            Menu menu = Fixtures.aMenu().status(MenuStatus.OPEN).build();
            MenuConfiguration original = menu.getConfiguration();
            SellableMenuInvariant invariant = new SellableMenuInvariant(List.of());
            assertThatThrownBy(() -> menu.changeConfiguration(MenuConfiguration.empty(), invariant))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(menu.getConfiguration()).isSameAs(original);
        }

        @Test
        @DisplayName("OPEN 상태에서 invariant 통과 시 변경된다")
        void changeConfigurationWhenOpenInvariantPasses() {
            Menu menu = Fixtures.aMenu().status(MenuStatus.OPEN).build();
            MenuOptionGroup newGroup = Fixtures.aMenuOptionGroup().optionGroupId(2L).build();
            MenuConfiguration newCfg = MenuConfiguration.of(Set.of(newGroup));
            OptionGroup group = Fixtures.anOptionGroup().id(2L).required(false).build();
            SellableMenuInvariant invariant = new SellableMenuInvariant(List.of(group));
            menu.changeConfiguration(newCfg, invariant);
            assertThat(menu.getConfiguration()).isEqualTo(newCfg);
        }
    }

    @Nested
    @DisplayName("getOptionGroupIds")
    class GetOptionGroupIds {

        @Test
        @DisplayName("configuration의 optionGroupIds를 위임 반환")
        void delegateToConfiguration() {
            Menu menu = Fixtures.aMenu().build();
            assertThat(menu.getOptionGroupIds()).containsExactly(Fixtures.OPTION_GROUP_ID);
        }
    }

    @Nested
    @DisplayName("정상 생성")
    class HappyPath {

        @Test
        @DisplayName("모든 invariant 통과 시 정상 생성")
        void allValid() {
            assertThatCode(() -> Fixtures.aMenu().build()).doesNotThrowAnyException();
        }
    }

    private SellableMenuInvariant sellableInvariant() {
        OptionGroup group = Fixtures.anOptionGroup().build();
        return new SellableMenuInvariant(List.of(group));
    }
}
