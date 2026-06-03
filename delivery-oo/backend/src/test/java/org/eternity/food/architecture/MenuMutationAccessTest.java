package org.eternity.food.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.eternity.food.shop.command.domain.Menu;
import org.eternity.food.shop.command.domain.MenuConfiguration;
import org.eternity.food.shop.command.domain.SellableMenuInvariant;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Menu 변경 작업의 *호출 범위 제한*.
 *
 * <p>Menu의 mutation 메서드(open, close, changeConfiguration)는 외부에서 직접 호출하면 안 되고
 * 반드시 {@link org.eternity.food.shop.command.service.MenuCommandService} 또는
 * {@link org.eternity.food.shop.command.domain.OptionGroupChangeFlow}를 거쳐야 한다.
 */
class MenuMutationAccessTest {

    private static final String MENU = "org.eternity.food.shop.command.domain.Menu";
    private static final String MENU_COMMAND_SERVICE = "org.eternity.food.shop.command.service.MenuCommandService";
    private static final String OPTION_GROUP_CHANGE_FLOW = "org.eternity.food.shop.command.domain.OptionGroupChangeFlow";

    private static final JavaClasses CLASSES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("org.eternity");

    @Test
    void menu_open_은_MenuCommandService_에서만_호출되어야_한다() {
        ArchRule rule = noClasses()
                .that().doNotHaveFullyQualifiedName(MENU_COMMAND_SERVICE)
                .and().doNotHaveFullyQualifiedName(MENU)
                .should().callMethod(Menu.class, "open", SellableMenuInvariant.class);

        rule.check(CLASSES);
    }

    @Test
    void menu_close_는_MenuCommandService_에서만_호출되어야_한다() {
        ArchRule rule = noClasses()
                .that().doNotHaveFullyQualifiedName(MENU_COMMAND_SERVICE)
                .and().doNotHaveFullyQualifiedName(MENU)
                .should().callMethod(Menu.class, "close");

        rule.check(CLASSES);
    }

    @Test
    void menu_changeConfiguration_은_MenuCommandService_또는_OptionGroupChangeFlow_에서만_호출되어야_한다() {
        ArchRule rule = noClasses()
                .that().doNotHaveFullyQualifiedName(MENU_COMMAND_SERVICE)
                .and().doNotHaveFullyQualifiedName(OPTION_GROUP_CHANGE_FLOW)
                .and().doNotHaveFullyQualifiedName(MENU)
                .should().callMethod(Menu.class, "changeConfiguration", MenuConfiguration.class, SellableMenuInvariant.class);

        rule.check(CLASSES);
    }
}
