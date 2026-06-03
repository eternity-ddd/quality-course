package org.eternity.food.shop.query.persistence;

import java.util.List;

public final class MenuResponses {
    private MenuResponses() {}

    public record Item(
            Long id,
            String name,
            String description,
            long price,
            String imageUrl
    ) {
    }

    public record Detail(
            Long id,
            String name,
            String description,
            long price,
            String imageUrl,
            List<OptionGroup> optionGroups
    ) {
        public record OptionGroup(
                Long id,
                String name,
                boolean required,
                List<Option> options
        ) {
        }

        public record Option(Long id, String name, long price) {
        }
    }
}
