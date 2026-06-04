import { test, expect } from '@playwright/test';

// 서버 장바구니 비우기 헬퍼 (DEMO_USER_ID=1 단일 카트)
async function clearCart(page) {
  const res = await page.request.get('http://localhost:8081/api/cart');
  const cart = await res.json();
  for (const item of (cart.items || [])) {
    await page.request.delete(`http://localhost:8081/api/cart/items/${item.id}`);
  }
}

test.describe('장바구니', () => {
  test.beforeEach(async ({ page }) => {
    await clearCart(page);
    await page.goto('/');
  });

  test('빈 장바구니에 적절한 메시지가 표시된다', async ({ page }) => {
    await page.goto('/cart');
    await expect(page.locator('.cart-empty')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('.cart-empty')).toContainText('장바구니가 비어있습니다');
  });

  test('빈 장바구니에서 가게 보러가기 버튼이 동작한다', async ({ page }) => {
    await page.goto('/cart');
    await expect(page.locator('.link-btn')).toBeVisible({ timeout: 5000 });
    await page.locator('.link-btn').click();
    await expect(page).toHaveURL('/');
  });

  test('메뉴를 장바구니에 담을 수 있다', async ({ page }) => {
    // 가게 목록 → 가게 상세 → 김치전(옵션 없는 메뉴) 상세로 탐색
    await page.goto('/');
    await expect(page.locator('.shop-card').first()).toBeVisible({ timeout: 5000 });
    await page.locator('.shop-card').first().click();
    await expect(page).toHaveURL(/\/shops\/\d+/);

    // 김치전 클릭 (옵션 없는 메뉴)
    const menuItem = page.locator('.menu-item', { hasText: '김치전' });
    await expect(menuItem).toBeVisible({ timeout: 5000 });
    await menuItem.click();
    await expect(page).toHaveURL(/\/shops\/\d+\/menus\/\d+/);
    await expect(page.locator('.menu-detail-name')).toHaveText('김치전');

    // 옵션 없는 메뉴이므로 바로 담기 가능
    await expect(page.locator('.add-to-cart-btn')).toBeEnabled();
    await page.locator('.add-to-cart-btn').click();

    // 메뉴 목록으로 돌아감
    await expect(page).toHaveURL(/\/shops\/\d+$/);

    // 플로팅 장바구니 버튼이 보임
    await expect(page.locator('.floating-cart')).toBeVisible({ timeout: 3000 });
  });

  test('장바구니에서 수량 변경이 동작한다', async ({ page }) => {
    // 김치전(옵션 없음) 담기 - API 직접 호출로 정확한 초기 상태 보장
    await page.request.post('http://localhost:8081/api/cart/items', {
      data: { menuId: 4, menuName: '김치전', quantity: 1, selectedOptions: [] },
    });

    await page.goto('/cart');
    await expect(page.locator('.cart-item').first()).toBeVisible({ timeout: 5000 });

    // 수량 확인 (초기 1개)
    const qtyText = page.locator('.cart-item-qty span');
    await expect(qtyText).toHaveText('1');

    // 수량 증가 (+)
    await page.locator('.cart-item-qty button').nth(1).click();
    await page.waitForTimeout(500);
    await expect(qtyText).toHaveText('2');
  });

  test('장바구니에 가게 이름이 표시된다', async ({ page }) => {
    await page.request.post('http://localhost:8081/api/cart/items', {
      data: { menuId: 4, menuName: '김치전', quantity: 1, selectedOptions: [] },
    });

    await page.goto('/cart');
    await expect(page.locator('.cart-shop-name')).toContainText('명동할매칼국수', { timeout: 5000 });
  });

  test('장바구니 요약에 주문금액, 배달비, 총 결제금액이 표시된다', async ({ page }) => {
    await page.request.post('http://localhost:8081/api/cart/items', {
      data: { menuId: 4, menuName: '김치전', quantity: 1, selectedOptions: [] },
    });

    await page.goto('/cart');
    const summary = page.locator('.cart-summary');
    await expect(summary).toBeVisible({ timeout: 5000 });
    await expect(summary).toContainText('주문금액');
    await expect(summary).toContainText('배달비');
    await expect(summary).toContainText('총 결제금액');
  });

  test('최소주문금액 미달 시 주문 버튼이 비활성화된다', async ({ page }) => {
    // 명동할매칼국수 최소주문 12,000원, 김치전 8,000원 → 미달
    await page.request.post('http://localhost:8081/api/cart/items', {
      data: { menuId: 4, menuName: '김치전', quantity: 1, selectedOptions: [] },
    });

    await page.goto('/cart');
    const orderBtn = page.locator('.order-btn');
    await expect(orderBtn).toBeVisible({ timeout: 5000 });
    await expect(orderBtn).toBeDisabled();
    await expect(orderBtn).toContainText('최소주문금액');
  });

  test('수량을 줄여 0이 되면 항목이 삭제된다', async ({ page }) => {
    await page.request.post('http://localhost:8081/api/cart/items', {
      data: { menuId: 4, menuName: '김치전', quantity: 1, selectedOptions: [] },
    });

    await page.goto('/cart');
    await expect(page.locator('.cart-item').first()).toBeVisible({ timeout: 5000 });

    // 수량 감소 (- 버튼) → quantity 0 → 삭제
    await page.locator('.cart-item-qty button').first().click();
    await page.waitForTimeout(500);

    // 빈 장바구니 표시
    await expect(page.locator('.cart-empty')).toBeVisible({ timeout: 3000 });
  });
});
