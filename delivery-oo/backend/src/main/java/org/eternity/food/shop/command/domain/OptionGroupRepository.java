package org.eternity.food.shop.command.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OptionGroupRepository extends JpaRepository<OptionGroup, Long> {
    @Override
    List<OptionGroup> findAllById(Iterable<Long> ids);
}
