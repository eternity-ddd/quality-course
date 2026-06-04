import { test, expect } from '@playwright/test';

async function clearCart(page) {
  const res = await page.request.get('http://localhost:8081/api/cart');
  const cart = await res.json();
  for (const item of (cart.items || [])) {
    await page.request.delete(`http://localhost:8081/api/cart/items/${item.id}`);
  }
}

test.describe('주문 및 주문 내역', () => {
  test.beforeEach(async ({ page }) => {
    await clearCart(page);
    await page.goto('/');
  });

  test('최소주문금액 이상이면 주문할 수 있다', async ({ page }) => {
    // 해물파전 12,000원 (최소주문 12,000원 충족)
    await page.request.post('http://localhost:8081/api/cart/items', {
      data: { menuId: 5, menuName: '해물파전', quantity: 1, selectedOptions: [] },
    });

    await page.goto('/cart');
    const orderBtn = page.locator('.order-btn');
    await expect(orderBtn).toBeEnabled({ timeout: 5000 });

    page.once('dialog', async (d) => {
      expect(d.message()).toContain('주문');
      await d.accept();
    });

    await orderBtn.click();
    await expect(page).toHaveURL('/');
  });

  test('주문 후 주문 내역에 표시된다', async ({ page }) => {
    await page.request.post('http://localhost:8081/api/cart/items', {
      data: { menuId: 5, menuName: '해물파전', quantity: 1, selectedOptions: [] },
    });

    await page.goto('/cart');
    page.once('dialog', async (d) => await d.accept());
    await page.locator('.order-btn').click();
    await expect(page).toHaveURL('/');

    await page.locator('.header-link').click();
    await expect(page).toHaveURL('/orders');

    await expect(page.locator('.order-card').first()).toBeVisible({ timeout: 5000 });
    const orderCount = await page.locator('.order-card').count();
    expect(orderCount).toBeGreaterThanOrEqual(1);
  });

  test('주문 내역에 가게명, 주문시간, 메뉴, 결제금액이 표시된다', async ({ page }) => {
    await page.request.post('http://localhost:8081/api/cart/items', {
      data: { menuId: 5, menuName: '해물파전', quantity: 1, selectedOptions: [] },
    });

    await page.goto('/cart');
    page.once('dialog', async (d) => await d.accept());
    await page.locator('.order-btn').click();
    await expect(page).toHaveURL('/');

    await page.locator('.header-link').click();
    const orderCard = page.locator('.order-card').first();
    await expect(orderCard).toBeVisible({ timeout: 5000 });

    await expect(orderCard.locator('.order-shop')).toBeVisible();
    await expect(orderCard.locator('.order-time')).toBeVisible();
    await expect(orderCard.locator('.order-item-name')).toContainText('해물파전');
    await expect(orderCard.locator('.order-total')).toContainText('원');
  });

  test('주문 내역이 없으면 빈 상태 메시지가 표시된다', async ({ page }) => {
    await page.goto('/orders');
    await expect(page.locator('.header h1')).toHaveText('내 주문 내역');
    await expect(page.locator('.back-btn')).toBeVisible();
  });
});
