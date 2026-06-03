package org.eternity.food.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.eternity.food.shop.command.domain.OptionGroup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class OptionGroupMutationAccessTest {

    private static final String OPTION_GROUP = "org.eternity.food.shop.command.domain.OptionGroup";
    private static final String OPTION_GROUP_CHANGE_FLOW = "org.eternity.food.shop.command.domain.OptionGroupChangeFlow";

    private static final JavaClasses CLASSES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("org.eternity");

    @Test
    void optionGroup_updateOptions_은_package_private_이어야_한다() {
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().haveFullyQualifiedName(OPTION_GROUP)
                .and().haveName("updateOptions")
                .should().bePackagePrivate();

        rule.check(CLASSES);
    }

    @Test
    void optionGroup_updateOptions_은_OptionGroupChangeFlow_에서만_호출되어야_한다() {
        ArchRule rule = noClasses()
                .that().doNotHaveFullyQualifiedName(OPTION_GROUP_CHANGE_FLOW)
                .and().doNotHaveFullyQualifiedName(OPTION_GROUP)
                .should().callMethod(OptionGroup.class, "updateOptions", List.class);

        rule.check(CLASSES);
    }
}
