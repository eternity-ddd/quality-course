package org.eternity.food.repository;

import org.eternity.food.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByShopIdOrderById(Long shopId);
}
