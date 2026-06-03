package org.eternity.food.shop.command.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eternity.food.base.domain.ValueObject;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class MenuOptionGroup extends ValueObject<MenuOptionGroup> implements Comparable<MenuOptionGroup> {

    @Column(name = "OPTION_GROUP_ID")
    private Long optionGroupId;

    @Column(name = "DISPLAY_ORDER")
    private int displayOrder;

    @Builder
    public MenuOptionGroup(Long optionGroupId, int displayOrder) {
        if (optionGroupId == null) {
            throw new IllegalArgumentException("optionGroupId는 null이어서는 안됩니다.");
        }

        if (displayOrder <= 0) {
            throw new IllegalArgumentException("displayOrder는 1 이상이어야 합니다.");
        }

        this.optionGroupId = optionGroupId;
        this.displayOrder = displayOrder;
    }

    @Override
    public int compareTo(MenuOptionGroup other) {
        return this.displayOrder - other.displayOrder;
    }
}
