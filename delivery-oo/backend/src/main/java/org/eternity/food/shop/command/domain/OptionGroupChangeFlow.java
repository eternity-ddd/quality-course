package org.eternity.food.shop.command.domain;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Component
@AllArgsConstructor
public class OptionGroupChangeFlow {
    private final MenuRepository menuRepository;
    private final OptionGroupRepository optionGroupRepository;

    public void updateOptions(OptionGroup optionGroup, List<OptionPatch> patches) {
        Set<Option> futureOptions = projectFutureOptions(patches);
        verifyMenusStaySellable(optionGroup, futureOptions);
        optionGroup.updateOptions(patches);
    }

    private Set<Option> projectFutureOptions(List<OptionPatch> patches) {
        Set<Option> future = new HashSet<>();
        for (OptionPatch patch : patches) {
            if (patch.id() == null) {
                future.add(new Option(patch.name(), patch.price()));
            } else {
                future.add(Option.builder()
                        .id(patch.id())
                        .name(patch.name())
                        .price(patch.price())
                        .build());
            }
        }
        return future;
    }

    private void verifyMenusStaySellable(OptionGroup current, Set<Option> futureOptions) {
        List<Menu> sellingMenus = menuRepository.findSellingMenus(current.getId());
        if (sellingMenus.isEmpty()) {
            return;
        }

        Map<Long, OptionGroup> futureById = buildFutureOptionGroupContext(current, futureOptions, sellingMenus);

        for (Menu menu : sellingMenus) {
            List<OptionGroup> futureGroups = menu.getOptionGroupIds().stream()
                    .map(futureById::get)
                    .toList();
            new SellableMenuInvariant(futureGroups).check(menu.getConfiguration()).require();
        }
    }

    private Map<Long, OptionGroup> buildFutureOptionGroupContext(OptionGroup current, Set<Option> futureOptions, Collection<Menu> sellingMenus) {
        OptionGroup updated = new OptionGroup(current.getId(), current.getName(), current.isRequired(), futureOptions);
        Map<Long, OptionGroup> byId = collectOptionGroupsOf(sellingMenus);
        byId.put(updated.getId(), updated);
        return byId;
    }

    private Map<Long, OptionGroup> collectOptionGroupsOf(Collection<Menu> menus) {
        Set<Long> ids = menus.stream()
                .flatMap(m -> m.getOptionGroupIds().stream())
                .collect(toSet());
        return optionGroupRepository.findAllById(ids).stream()
                .collect(toMap(OptionGroup::getId, identity()));
    }
}
