package org.eternity.food.cart.command.service;

import org.eternity.food.Fixtures;
import org.eternity.food.base.generic.money.Money;
import org.eternity.food.cart.command.domain.CartLineItem;
import org.eternity.food.cart.command.domain.CartOptionGroup;
import org.eternity.food.cart.command.service.CartLineItemCommand.CartOptionCommand;
import org.eternity.food.cart.command.service.CartLineItemCommand.CartOptionGroupCommand;
import org.eternity.food.shop.command.domain.Menu;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CartLineItemMapper — Command → CartLineItem 순수 변환")
class CartLineItemMapperTest {

    private static CartLineItemCommand commandWithOptions(int count, long optionPrice) {
        return new CartLineItemCommand(
                Fixtures.MENU_ID,
                "삼겹살 1인세트",
                count,
                List.of(new CartOptionGroupCommand(
                        Fixtures.OPTION_GROUP_ID,
                        "기본",
                        List.of(new CartOptionCommand("소(250g)", optionPrice))
                ))
        );
    }

    @Nested
    @DisplayName("basePrice 계산")
    class UnitPriceCalculation {

        @Test
        @DisplayName("basePrice = menu.basePrice (옵션 미포함)")
        void basePriceIsMenuBasePrice() {
            Menu menu = Fixtures.aMenu().basePrice(Money.wons(10_000)).build();
            CartLineItemCommand command = commandWithOptions(1, 12_000L);

            CartLineItem item = CartLineItemMapper.map(menu, command);

            assertThat(item.getBasePrice()).isEqualTo(Money.wons(10_000));
        }

        @Test
        @DisplayName("옵션 그룹이 비어 있으면 basePrice = basePrice")
        void basePriceEqualsBasePriceWhenNoOptions() {
            Menu menu = Fixtures.aMenu().basePrice(Money.wons(8_500)).build();
            CartLineItemCommand command = new CartLineItemCommand(
                    Fixtures.MENU_ID, "기본메뉴", 2, List.of()
            );

            CartLineItem item = CartLineItemMapper.map(menu, command);

            assertThat(item.getBasePrice()).isEqualTo(Money.wons(8_500));
            assertThat(item.getGroups()).isEmpty();
        }

        @Test
        @DisplayName("옵션이 여러 개여도 basePrice는 menu.basePrice만")
        void multipleOptions_basePriceUnchanged() {
            Menu menu = Fixtures.aMenu().basePrice(Money.wons(5_000)).build();
            CartLineItemCommand command = new CartLineItemCommand(
                    Fixtures.MENU_ID, "메뉴", 1,
                    List.of(
                            new CartOptionGroupCommand(1L, "그룹1", List.of(
                                    new CartOptionCommand("옵션A", 1_000L),
                                    new CartOptionCommand("옵션B", 2_000L)
                            )),
                            new CartOptionGroupCommand(2L, "그룹2", List.of(
                                    new CartOptionCommand("옵션C", 3_000L)
                            ))
                    )
            );

            CartLineItem item = CartLineItemMapper.map(menu, command);

            assertThat(item.getBasePrice()).isEqualTo(Money.wons(5_000));
        }
    }

    @Nested
    @DisplayName("필드 전사")
    class FieldMapping {

        @Test
        @DisplayName("menu.id와 menu.name이 CartLineItem에 복사된다")
        void menuIdAndNameAreCopied() {
            Menu menu = Fixtures.aMenu().id(42L).name("뼈해장국").basePrice(Money.wons(9_000)).build();
            CartLineItemCommand command = commandWithOptions(1, 0L);

            CartLineItem item = CartLineItemMapper.map(menu, command);

            assertThat(item.getMenuId()).isEqualTo(42L);
            assertThat(item.getMenuName()).isEqualTo("뼈해장국");
        }

        @Test
        @DisplayName("count가 그대로 menuCount로 전사된다")
        void countIsCopiedAsMenuCount() {
            Menu menu = Fixtures.aMenu().build();
            CartLineItemCommand command = commandWithOptions(7, 1_000L);

            CartLineItem item = CartLineItemMapper.map(menu, command);

            assertThat(item.getMenuCount()).isEqualTo(7);
        }

        @Test
        @DisplayName("CartOptionGroup의 optionGroupId와 name이 복사된다")
        void cartOptionGroupFieldsAreCopied() {
            Menu menu = Fixtures.aMenu().build();
            CartLineItemCommand command = new CartLineItemCommand(
                    Fixtures.MENU_ID, "메뉴", 1,
                    List.of(new CartOptionGroupCommand(77L, "사이즈선택",
                            List.of(new CartOptionCommand("특대", 3_000L))))
            );

            CartLineItem item = CartLineItemMapper.map(menu, command);

            assertThat(item.getGroups()).hasSize(1);
            CartOptionGroup group = item.getGroups().iterator().next();
            assertThat(group.getOptionGroupId()).isEqualTo(77L);
            assertThat(group.getName()).isEqualTo("사이즈선택");
            assertThat(group.getOptions()).hasSize(1);
            assertThat(group.getOptions().iterator().next().getName()).isEqualTo("특대");
            assertThat(group.getOptions().iterator().next().getPrice()).isEqualTo(Money.wons(3_000));
        }
    }

    @Nested
    @DisplayName("subtotal과 통합")
    class SubtotalIntegration {

        @Test
        @DisplayName("매핑된 결과의 subtotal = basePrice × count")
        void mappedSubtotal() {
            Menu menu = Fixtures.aMenu().basePrice(Money.wons(10_000)).build();
            CartLineItemCommand command = commandWithOptions(3, 2_000L);

            CartLineItem item = CartLineItemMapper.map(menu, command);

            // (10000 + 2000) * 3
            assertThat(item.subtotal()).isEqualTo(Money.wons(36_000));
        }
    }
}
