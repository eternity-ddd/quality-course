package org.eternity.food.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.eternity.food.cart.command.domain.Cart;
import org.eternity.food.cart.command.domain.CartLineItem;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Cart 변경 작업의 *호출 범위 제한*.
 *
 * <p>Cart의 mutation 메서드(addItem, changeItemQuantity, removeItem, clear)는
 * 외부에서 직접 호출하면 안 되고 반드시 지정된 service를 거쳐야 한다.
 */
class CartMutationAccessTest {

    private static final String CART = "org.eternity.food.cart.command.domain.Cart";
    private static final String CART_COMMAND_SERVICE = "org.eternity.food.cart.command.service.CartCommandService";
    private static final String PLACE_ORDER_SERVICE = "org.eternity.food.cart.command.service.PlaceOrderService";
    private static final String ORDER_PLACED_HANDLER = "org.eternity.food.cart.command.service.OrderPlacedHandler";

    private static final JavaClasses CLASSES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("org.eternity");

    @Test
    void cart_addItem_은_CartCommandService_에서만_호출되어야_한다() {
        ArchRule rule = noClasses()
                .that().doNotHaveFullyQualifiedName(CART_COMMAND_SERVICE)
                .and().doNotHaveFullyQualifiedName(CART)
                .should().callMethod(Cart.class, "addItem", Long.class, CartLineItem.class);

        rule.check(CLASSES);
    }

    @Test
    void cart_changeItemQuantity_는_CartCommandService_에서만_호출되어야_한다() {
        ArchRule rule = noClasses()
                .that().doNotHaveFullyQualifiedName(CART_COMMAND_SERVICE)
                .and().doNotHaveFullyQualifiedName(CART)
                .should().callMethod(Cart.class, "changeItemQuantity", Long.class, int.class);

        rule.check(CLASSES);
    }

    @Test
    void cart_removeItem_은_CartCommandService_에서만_호출되어야_한다() {
        ArchRule rule = noClasses()
                .that().doNotHaveFullyQualifiedName(CART_COMMAND_SERVICE)
                .and().doNotHaveFullyQualifiedName(CART)
                .should().callMethod(Cart.class, "removeItem", Long.class);

        rule.check(CLASSES);
    }

    @Test
    void cart_clear_는_PlaceOrderService_또는_OrderPlacedHandler_에서만_호출되어야_한다() {
        ArchRule rule = noClasses()
                .that().doNotHaveFullyQualifiedName(PLACE_ORDER_SERVICE)
                .and().doNotHaveFullyQualifiedName(ORDER_PLACED_HANDLER)
                .and().doNotHaveFullyQualifiedName(CART_COMMAND_SERVICE)
                .and().doNotHaveFullyQualifiedName(CART)
                .should().callMethod(Cart.class, "clear");

        rule.check(CLASSES);
    }
}
