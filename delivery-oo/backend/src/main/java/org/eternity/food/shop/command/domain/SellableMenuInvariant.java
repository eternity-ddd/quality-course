package org.eternity.food.shop.command.domain;

import org.eternity.food.base.domain.validation.Check;

import java.util.Collection;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * 메뉴 셀러블 자격 검증. *검증 컨텍스트(OptionGroups)*만 carry. 타겟은 {@link #check(MenuConfiguration)}으로 받음.
 *
 * <p>호출 예:
 * <pre>{@code
 * menu.open(new SellableMenuInvariant(optionGroups));            // 내부에서 check(this.configuration)
 * menu.changeConfiguration(newConfig, new SellableMenuInvariant(optionGroups));
 * }</pre>
 */
public final class SellableMenuInvariant {
    public static final int MAX_REQUIRED_GROUP = 3;

    private final Collection<OptionGroup> optionGroups;

    public SellableMenuInvariant(Collection<OptionGroup> optionGroups) {
        this.optionGroups = optionGroups;
    }

    public Check check(MenuConfiguration configuration) {
        if (configuration.menuOptionGroups().isEmpty()) {
            return Check.fail("메뉴에는 옵션그룹이 1개 이상 포함되어야 합니다.");
        }

        Set<Long> configOptionGroupIds = configuration.menuOptionGroups().stream()
                .map(MenuOptionGroup::getOptionGroupId)
                .collect(toSet());
        Set<Long> optionGroupIds = optionGroups.stream()
                .map(OptionGroup::getId)
                .collect(toSet());

        if (!configOptionGroupIds.equals(optionGroupIds)) {
            return Check.fail("옵션 그룹 구성이 일치하지 않습니다.");
        }

        long requiredCount = optionGroups.stream()
                .filter(OptionGroup::isRequired)
                .count();

        if (requiredCount > MAX_REQUIRED_GROUP) {
            return Check.fail(String.format("필수 옵션그룹의 갯수는 %d개 이하여야 합니다.", MAX_REQUIRED_GROUP));
        }

        return Check.pass();
    }
}
