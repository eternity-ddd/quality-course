package org.eternity.food;

import org.eternity.food.entity.Cart;
import org.eternity.food.entity.CartLineItem;
import org.eternity.food.entity.CartOption;
import org.eternity.food.entity.CartOptionGroup;
import org.eternity.food.entity.Menu;
import org.eternity.food.entity.MenuOptionGroup;
import org.eternity.food.entity.Option;
import org.eternity.food.entity.OptionGroup;
import org.eternity.food.entity.Order;
import org.eternity.food.entity.OrderLineItem;
import org.eternity.food.entity.Shop;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class Fixtures {
    public static final Long SHOP_ID = 1L;
    public static final Long MENU_ID = 1L;
    public static final Long OPTION_GROUP_ID = 1L;
    public static final Long OPTION_ID = 1L;
    public static final Long CART_ID = 1L;
    public static final Long CART_LINE_ITEM_ID = 1L;
    public static final Long CART_OPTION_GROUP_ID = 1L;
    public static final Long CART_OPTION_ID = 1L;
    public static final Long ORDER_ID = 1L;
    public static final Long USER_ID = 1L;

    public static Shop.ShopBuilder aShop() {
        return Shop.builder()
                .id(SHOP_ID)
                .name("오겹돼지")
                .minOrderPrice(13_000L)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(22, 0))
                .category("KOREAN")
                .latitude(37.5665)
                .longitude(126.9780)
                .deliveryRadius(3.0);
    }

    public static Menu.MenuBuilder aMenu() {
        return Menu.builder()
                .id(MENU_ID)
                .shopId(SHOP_ID)
                .name("삼겹살 1인세트")
                .description("삼겹살 + 야채세트 + 김치찌개")
                .basePrice(10_000L)
                .status("OPEN");
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
                .required(true);
    }

    public static Option.OptionBuilder anOption() {
        return Option.builder()
                .id(OPTION_ID)
                .optionGroupId(OPTION_GROUP_ID)
                .name("소(250g)")
                .price(12_000L);
    }

    public static Cart.CartBuilder aCart() {
        return Cart.builder()
                .id(CART_ID)
                .userId(USER_ID)
                .shopId(SHOP_ID);
    }

    public static CartLineItem.CartLineItemBuilder aCartLineItem() {
        return CartLineItem.builder()
                .id(CART_LINE_ITEM_ID)
                .menuId(MENU_ID)
                .menuName("삼겹살 1인세트")
                .menuCount(1)
                .unitPrice(10_000L);
    }

    public static CartOptionGroup.CartOptionGroupBuilder aCartOptionGroup() {
        return CartOptionGroup.builder()
                .id(CART_OPTION_GROUP_ID)
                .optionGroupId(OPTION_GROUP_ID)
                .name("기본");
    }

    public static CartOption.CartOptionBuilder aCartOption() {
        return CartOption.builder()
                .id(CART_OPTION_ID)
                .optionId(OPTION_ID)
                .name("소(250g)")
                .price(12_000L);
    }

    public static Order.OrderBuilder anOrder() {
        return Order.builder()
                .id(ORDER_ID)
                .userId(USER_ID)
                .shopId(SHOP_ID)
                .orderedTime(LocalDateTime.of(2020, 1, 1, 12, 0))
                .totalPrice(22_000L);
    }

    public static OrderLineItem.OrderLineItemBuilder anOrderLineItem() {
        return OrderLineItem.builder()
                .menuId(MENU_ID)
                .menuName("삼겹살 1인세트")
                .count(1)
                .unitPrice(22_000L);
    }

    public static OrderLineItem.OrderOptionGroup.OrderOptionGroupBuilder anOrderOptionGroup() {
        return OrderLineItem.OrderOptionGroup.builder()
                .name("기본");
    }

    public static OrderLineItem.OrderOption.OrderOptionBuilder anOrderOption() {
        return OrderLineItem.OrderOption.builder()
                .name("소(250g)")
                .price(12_000L);
    }
}
