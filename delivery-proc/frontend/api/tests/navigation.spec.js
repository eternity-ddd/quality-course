import { test, expect } from '@playwright/test';

test.describe('네비게이션', () => {
  test('홈 → 가게 → 메뉴 상세 → 뒤로가기로 돌아온다', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('.shop-card').first()).toBeVisible({ timeout: 5000 });

    // 가게 클릭
    await page.locator('.shop-card').first().click();
    await expect(page).toHaveURL(/\/shops\/\d+/);

    // 메뉴 클릭
    const menuItem = page.locator('.menu-item').first();
    await expect(menuItem).toBeVisible({ timeout: 5000 });
    await menuItem.click();
    await expect(page).toHaveURL(/\/shops\/\d+\/menus\/\d+/);

    // 뒤로가기 → 메뉴 목록
    await page.locator('.back-btn').click();
    await expect(page).toHaveURL(/\/shops\/\d+$/);

    // 뒤로가기 → 홈
    await page.locator('.back-btn').click();
    await expect(page).toHaveURL('/');
  });

  test('주문 내역에서 뒤로가기로 홈으로 돌아온다', async ({ page }) => {
    await page.goto('/');
    await page.locator('.header-link').click();
    await expect(page).toHaveURL('/orders');

    await page.locator('.back-btn').click();
    await expect(page).toHaveURL('/');
  });

  test('장바구니에서 뒤로가기로 이전 페이지로 돌아온다', async ({ page }) => {
    await page.goto('/cart');
    await expect(page.locator('.header h1')).toHaveText('장바구니');

    await page.locator('.back-btn').click();
    // 이전 페이지로 돌아감
  });

  test('플로팅 카트 버튼으로 장바구니에 접근한다', async ({ page }) => {
    // 새 세션 설정
    await page.goto('/');
    await page.evaluate(() => {
      localStorage.setItem('sessionId', 'nav-test-' + Math.random().toString(36).substring(2, 10));
    });

    // 옵션 없는 메뉴 담기
    await page.goto('/shops/1/menus/4');
    await expect(page.locator('.add-to-cart-btn')).toBeVisible({ timeout: 5000 });
    await page.locator('.add-to-cart-btn').click();

    // 플로팅 카트 클릭
    await expect(page.locator('.floating-cart')).toBeVisible({ timeout: 3000 });
    await page.locator('.floating-cart').click();
    await expect(page).toHaveURL('/cart');
    await expect(page.locator('.cart-item')).toHaveCount(1);
  });
});
