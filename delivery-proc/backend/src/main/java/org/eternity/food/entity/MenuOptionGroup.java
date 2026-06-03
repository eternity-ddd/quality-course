package org.eternity.food.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * menu → option_group 매핑 행 (PK = menu_id + option_group_id).
 * Menu의 @ElementCollection 으로 매핑된다.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuOptionGroup {

    @Column(name = "option_group_id")
    private Long optionGroupId;

    @Column(name = "display_order")
    private Integer displayOrder;
}
