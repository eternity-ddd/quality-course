package org.eternity.food.cart.command.domain;

import org.eternity.food.base.domain.validation.Check;
import org.eternity.food.shop.command.domain.Menu;
import org.eternity.food.shop.command.domain.Option;
import org.eternity.food.shop.command.domain.OptionGroup;
import org.eternity.food.shop.command.domain.Shop;

import java.util.Collection;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Cart → Order 변환 자격 검증. *검증 컨텍스트(Shop, Menus, OptionGroups)*만 carry. 타겟은 {@link #check(Cart)}로 받음.
 *
 * <p>호출 예 ({@code OrderFactory} 내부):
 * <pre>{@code
 * new OrderableCartInvariant(shop, menus, optionGroups).check(cart).require();
 * }</pre>
 */
public final class OrderableCartInvariant {
    private final Shop shop;
    private final Collection<Menu> menus;
    private final Collection<OptionGroup> optionGroups;

    public OrderableCartInvariant(Shop shop, Collection<Menu> menus, Collection<OptionGroup> optionGroups) {
        this.shop = shop;
        this.menus = menus;
        this.optionGroups = optionGroups;
    }

    public Check check(Cart cart) {
        if (cart.isEmpty()) {
            return Check.fail("장바구니가 비어 있어 주문할 수 없습니다.");
        }

        if (cart.getShopId() == null || !cart.getShopId().equals(shop.getId())) {
            return Check.fail("주문하려는 가게와 장바구니의 가게가 일치하지 않습니다.");
        }

        if (!shop.isOpen()) {
            return Check.fail("가게가 영업중이어야 합니다.");
        }

        if (cart.getTotalPrice().isLessThan(shop.getMinOrderPrice())) {
            return Check.fail(String.format("최소 주문금액 %s 이상이어야 합니다.", shop.getMinOrderPrice()));
        }

        Map<Long, Menu> menusById = menus.stream().collect(toMap(Menu::getId, identity()));
        Map<Long, OptionGroup> optionGroupsById = optionGroups.stream().collect(toMap(OptionGroup::getId, identity()));

        for (CartLineItem item : cart.getItems()) {
            Menu menu = menusById.get(item.getMenuId());

            if (menu == null) {
                return Check.fail("메뉴를 찾을 수 없습니다: " + item.getMenuName());
            }

            if (!menu.isOpen()) {
                return Check.fail(String.format("판매중이 아닌 메뉴입니다: %s", menu.getName()));
            }

            for (CartOptionGroup cartGroup : item.getGroups()) {
                OptionGroup currentGroup = optionGroupsById.get(cartGroup.getOptionGroupId());

                if (currentGroup == null || !menu.getOptionGroupIds().contains(currentGroup.getId())) {
                    return Check.fail(String.format("옵션 그룹이 더 이상 존재하지 않습니다: %s", cartGroup.getName()));
                }

                for (CartOption cartOption : cartGroup.getOptions()) {
                    Option current = currentGroup.findOption(cartOption.getName()).orElse(null);

                    if (current == null) {
                        return Check.fail(String.format("옵션이 더 이상 존재하지 않습니다: %s", cartOption.getName()));
                    }

                    if (!current.getPrice().equals(cartOption.getPrice())) {
                        return Check.fail(String.format("옵션 가격이 변경되었습니다: %s", cartOption.getName()));
                    }
                }
            }
        }

        return Check.pass();
    }
}
