package org.eternity.food;

import org.eternity.food.base.generic.money.Money;
import org.eternity.food.base.generic.time.TimePeriod;
import org.eternity.food.cart.command.domain.Cart;
import org.eternity.food.cart.command.domain.CartLineItem;
import org.eternity.food.cart.command.domain.CartOption;
import org.eternity.food.cart.command.domain.CartOptionGroup;
import org.eternity.food.order.command.domain.Order;
import org.eternity.food.order.command.domain.OrderLineItem;
import org.eternity.food.shop.command.domain.Category;
import org.eternity.food.shop.command.domain.Location;
import org.eternity.food.shop.command.domain.Menu;
import org.eternity.food.shop.command.domain.MenuConfiguration;
import org.eternity.food.shop.command.domain.MenuOptionGroup;
import org.eternity.food.shop.command.domain.Option;
import org.eternity.food.shop.command.domain.OptionGroup;
import org.eternity.food.shop.command.domain.Shop;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.util.Lists.list;
import static org.assertj.core.util.Sets.set;

public class Fixtures {
    public static final Long SHOP_ID = 1L;
    public static final Long MENU_ID = 1L;
    public static final Long OPTION_GROUP_ID = 1L;
    public static final Long OPTION_ID = 1L;
    public static final Long CART_ID = 1L;
    public static final Long CART_LINE_ITEM_ID = 1L;
    public static final Long CART_OPTION_GROUP_ID = 1L;
    public static final Long ORDER_ID = 1L;
    public static final Long USER_ID = 1L;

    // ─────────────── Shop aggregate ───────────────

    public static Shop.ShopBuilder aShop() {
        return Shop.builder()
                .id(SHOP_ID)
                .name("오겹돼지집")
                .minOrderPrice(Money.wons(13000))
                .operationPeriod(aTimePeriod())
                .category(Category.KOREAN)
                .location(aLocation())
                .deliveryRadius(3.0);
    }

    public static TimePeriod aTimePeriod() {
        return TimePeriod.between(LocalTime.of(9, 0), LocalTime.of(22, 0));
    }

    public static Location aLocation() {
        return new Location(37.5665, 126.9780);
    }

    public static Menu.MenuBuilder aMenu() {
        return Menu.builder()
                .id(MENU_ID)
                .shopId(SHOP_ID)
                .name("삼겹살 1인세트")
                .description("삼겹살 + 야채세트 + 김치찌개")
                .basePrice(Money.wons(10_000))
                .configuration(aMenuConfiguration());
    }

    public static MenuConfiguration aMenuConfiguration() {
        return MenuConfiguration.of(set(aMenuOptionGroup().build()));
    }

    public static MenuOptionGroup.MenuOptionGroupBuilder aMenuOptionGroup() {
        return MenuOptionGroup.builder()
                .optionGroupId(OPTION_GROUP_ID)
                .displayOrder(1);
    }

    public static OptionGroup.OptionGroupBuilder anOptionGroup() {
        return OptionGroup.builder()
                .id(OPTION_GROUP_ID)
                .name("기본")
                .required(true)
                .options(set(
                        anOption().build(),
                        anOption().id(2L).name("대(500g)").price(Money.wons(20000)).build()
                ));
    }

    public static Option.OptionBuilder anOption() {
        return Option.builder()
                .id(OPTION_ID)
                .name("소(250g)")
                .price(Money.wons(12000));
    }

    // ─────────────── Cart aggregate ───────────────

    public static Cart.CartBuilder aCart() {
        return Cart.builder()
                .id(CART_ID)
                .userId(USER_ID)
                .shopId(SHOP_ID)
                .items(list(aCartLineItem().build()));
    }

    public static CartLineItem.CartLineItemBuilder aCartLineItem() {
        return CartLineItem.builder()
                .id(CART_LINE_ITEM_ID)
                .menuId(MENU_ID)
                .menuName("삼겹살 1인세트")
                .count(1)
                .unitPrice(Money.wons(10_000))
                .groups(list(aCartOptionGroup().build()));
    }

    public static CartOptionGroup.CartOptionGroupBuilder aCartOptionGroup() {
        return CartOptionGroup.builder()
                .id(CART_OPTION_GROUP_ID)
                .optionGroupId(OPTION_GROUP_ID)
                .name("기본")
                .options(set(aCartOption().build()));
    }

    public static CartOption.CartOptionBuilder aCartOption() {
        return CartOption.builder()
                .name("소(250g)")
                .price(Money.wons(12000));
    }

    // ─────────────── Order aggregate ───────────────

    public static Order.OrderBuilder anOrder() {
        return Order.builder()
                .id(ORDER_ID)
                .userId(USER_ID)
                .shopId(SHOP_ID)
                .orderedTime(LocalDateTime.of(2020, 1, 1, 12, 0))
                .items(list(anOrderLineItem()))
                .totalPrice(Money.wons(22_000));
    }

    public static OrderLineItem anOrderLineItem() {
        return new OrderLineItem(
                MENU_ID,
                "삼겹살 1인세트",
                1,
                22_000L,
                List.of(anOrderOptionGroup())
        );
    }

    public static OrderLineItem.OrderOptionGroup anOrderOptionGroup() {
        return new OrderLineItem.OrderOptionGroup(
                "기본",
                List.of(anOrderOption())
        );
    }

    public static OrderLineItem.OrderOption anOrderOption() {
        return new OrderLineItem.OrderOption(
                "소(250g)",
                12_000L
        );
    }
}
