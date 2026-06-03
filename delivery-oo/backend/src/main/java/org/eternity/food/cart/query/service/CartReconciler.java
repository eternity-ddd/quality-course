package org.eternity.food.cart.query.service;

import org.eternity.food.cart.query.persistence.CartRaw;
import org.eternity.food.cart.query.persistence.CartRaw.CartItemRaw;
import org.eternity.food.cart.query.persistence.CartRaw.CartOptionGroupRaw;
import org.eternity.food.cart.query.persistence.CartRaw.CartOptionRaw;
import org.eternity.food.cart.query.persistence.CartResponses;
import org.eternity.food.cart.query.persistence.CartResponses.Cart;
import org.eternity.food.cart.query.persistence.CartResponses.Cart.Item;
import org.eternity.food.cart.query.persistence.CartResponses.Cart.Option;
import org.eternity.food.cart.query.persistence.CartResponses.ItemStatus;
import org.eternity.food.cart.query.persistence.CartResponses.OptionStatus;
import org.eternity.food.cart.query.persistence.CatalogSnapshot;
import org.eternity.food.cart.query.persistence.CatalogSnapshot.MenuInfo;
import org.eternity.food.cart.query.persistence.CatalogSnapshot.OptionGroupInfo;
import org.eternity.food.cart.query.persistence.CatalogSnapshot.OptionInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class CartReconciler {

    public Cart reconcile(CartRaw raw, CatalogSnapshot catalog, Cart.Shop shop) {
        List<Item> items = raw.items().stream()
                .map(item -> reconcileItem(item, catalog))
                .toList();

        long totalPrice = items.stream()
                .mapToLong(it -> it.unitPrice() * it.quantity())
                .sum();

        return new Cart(raw.id(), null, shop, items, totalPrice);
    }

    private Item reconcileItem(CartItemRaw item, CatalogSnapshot catalog) {
        MenuInfo menu = catalog.menusById().get(item.menuId());
        if (menu == null) {
            return menuRemoved(item);
        }

        Set<Long> validGroupIds = Set.copyOf(menu.optionGroupIds());
        List<String> messages = new ArrayList<>();

        if (!menu.name().equals(item.menuName())) {
            messages.add(String.format("메뉴 이름이 변경되었습니다: %s → %s",
                    item.menuName(), menu.name()));
        }

        boolean anyOptionInvalid = false;
        boolean anyPriceChanged = false;
        long unitTotal = menu.basePrice();
        List<Option> reconciledOptions = new ArrayList<>();

        for (CartOptionGroupRaw cartGroup : item.groups()) {
            OptionGroupInfo currentGroup = catalog.optionGroupsById().get(cartGroup.optionGroupId());

            if (currentGroup == null || !validGroupIds.contains(currentGroup.id())) {
                for (CartOptionRaw cartOption : cartGroup.options()) {
                    reconciledOptions.add(new Option(
                            cartGroup.name(), cartOption.name(), cartOption.price(), OptionStatus.INVALID));
                    unitTotal += cartOption.price();
                }
                anyOptionInvalid = true;
                continue;
            }

            String displayGroupName = currentGroup.name();
            if (!displayGroupName.equals(cartGroup.name())) {
                messages.add(String.format("옵션 그룹 이름이 변경되었습니다: %s → %s",
                        cartGroup.name(), displayGroupName));
            }

            for (CartOptionRaw cartOption : cartGroup.options()) {
                OptionInfo current = currentGroup.optionsByName().get(cartOption.name());

                if (current == null) {
                    reconciledOptions.add(new Option(
                            displayGroupName, cartOption.name(), cartOption.price(), OptionStatus.INVALID));
                    anyOptionInvalid = true;
                    unitTotal += cartOption.price();
                    continue;
                }

                OptionStatus status;
                if (current.price() != cartOption.price()) {
                    status = OptionStatus.PRICE_CHANGED;
                    anyPriceChanged = true;
                } else {
                    status = OptionStatus.VALID;
                }

                reconciledOptions.add(new Option(
                        displayGroupName, current.name(), current.price(), status));
                unitTotal += current.price();
            }
        }

        boolean priceChanged = anyPriceChanged || unitTotal != item.unitPrice();

        ItemStatus status;

        if (!"OPEN".equals(menu.status())) {
            status = ItemStatus.MENU_NOT_OPEN;
            messages.add("판매중이 아닌 메뉴입니다.");
        } else if (anyOptionInvalid) {
            status = ItemStatus.INVALID_OPTION;
            messages.add("일부 옵션이 더 이상 제공되지 않습니다. 다시 선택해주세요.");
        } else if (priceChanged) {
            status = ItemStatus.PRICE_CHANGED;
            messages.add("가격이 변경되었습니다.");
        } else {
            status = ItemStatus.VALID;
        }

        return new Item(
                item.id(),
                item.menuId(),
                menu.name(),
                unitTotal,
                item.quantity(),
                reconciledOptions,
                status,
                List.copyOf(messages)
        );
    }

    private Item menuRemoved(CartItemRaw item) {
        List<Option> options = item.groups().stream()
                .flatMap(g -> g.options().stream()
                        .map(o -> new Option(g.name(), o.name(), o.price(), OptionStatus.INVALID)))
                .toList();

        return new Item(
                item.id(),
                item.menuId(),
                item.menuName(),
                item.unitPrice(),
                item.quantity(),
                options,
                ItemStatus.MENU_REMOVED,
                List.of("이 메뉴는 더 이상 판매되지 않습니다.")
        );
    }
}
