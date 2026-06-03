package org.eternity.food.shop.command.domain;

import org.eternity.food.base.generic.money.Money;

/**
 * OptionGroup의 *원하는 상태* 표현. Style A (declarative full list) 갱신용.
 *
 * - id == null  → 신규 옵션 (add)
 * - id != null  → 기존 옵션 갱신 (rename / changePrice)
 * - patches에 빠진 기존 옵션 → 삭제
 */
public record OptionPatch(Long id, String name, Money price) {
}
