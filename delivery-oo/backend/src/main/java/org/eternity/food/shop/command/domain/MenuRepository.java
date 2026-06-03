package org.eternity.food.shop.command.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    @Override
    List<Menu> findAllById(Iterable<Long> ids);

    @Query(value = """
        SELECT DISTINCT m.*
        FROM MENU m
        JOIN MENU_OPTION_GROUP mog ON mog.MENU_ID = m.ID
        WHERE mog.OPTION_GROUP_ID = :optionGroupId
          AND m.STATUS = 'OPEN'
        """, nativeQuery = true)
    List<Menu> findSellingMenus(@Param("optionGroupId") Long optionGroupId);
}
