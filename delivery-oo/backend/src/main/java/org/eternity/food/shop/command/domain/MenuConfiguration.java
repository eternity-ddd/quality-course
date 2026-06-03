package org.eternity.food.shop.command.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eternity.food.base.domain.ValueObject;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class MenuConfiguration extends ValueObject<MenuConfiguration> {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "MENU_OPTION_GROUP", joinColumns = @JoinColumn(name = "MENU_ID"))
    private Set<MenuOptionGroup> menuOptionGroups = new HashSet<>();

    public static MenuConfiguration empty() {
        return new MenuConfiguration();
    }

    public static MenuConfiguration of(Set<MenuOptionGroup> menuOptionGroups) {
        return new MenuConfiguration(menuOptionGroups);
    }

    private MenuConfiguration(Set<MenuOptionGroup> menuOptionGroups) {
        validate(menuOptionGroups);
        this.menuOptionGroups = new HashSet<>(menuOptionGroups);
    }

    private void validate(Set<MenuOptionGroup> menuOptionGroups) {
        if (menuOptionGroups == null) {
            throw new IllegalArgumentException("옵션그룹은 null이어서는 안됩니다.");
        }

        long distinctCount = menuOptionGroups.stream()
                .map(MenuOptionGroup::getOptionGroupId)
                .distinct()
                .count();

        if (menuOptionGroups.size() != distinctCount) {
            throw new IllegalArgumentException("중복된 옵션 그룹을 포함합니다.");
        }
    }

    public boolean isEmpty() {
        return menuOptionGroups.isEmpty();
    }

    public Collection<MenuOptionGroup> menuOptionGroups() {
        return Collections.unmodifiableSet(menuOptionGroups);
    }

    public List<Long> optionGroupIds() {
        return menuOptionGroups.stream().map(MenuOptionGroup::getOptionGroupId).toList();
    }
}
