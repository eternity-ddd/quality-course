import { test, expect } from '@playwright/test';

test.describe('주문 골든 패스', () => {
  test('가게 목록 → 메뉴 상세 → 장바구니 → 주문 → 주문 내역', async ({ page }) => {
    // 1. 가게 목록 진입
    await page.goto('/');
    await expect(page.locator('.header h1')).toHaveText('배달이요');

    // 첫 번째 가게 카드가 보일 때까지 대기
    const firstShop = page.locator('a[href^="/shops/"]').first();
    await expect(firstShop).toBeVisible();
    const shopName = await firstShop.locator('h3, .shop-name, *').first().textContent();

    // 2. 첫 가게 클릭
    await firstShop.click();
    await expect(page).toHaveURL(/\/shops\/\d+/);

    // 3. 첫 번째 메뉴 클릭
    const firstMenu = page.locator('a[href*="/menus/"]').first();
    await expect(firstMenu).toBeVisible({ timeout: 5000 });
    await firstMenu.click();
    await expect(page).toHaveURL(/\/shops\/\d+\/menus\/\d+/);
    await expect(page.locator('.menu-detail-name')).toBeVisible();

    // 4. 필수 옵션 첫 번째 선택 (radio)
    const radios = page.locator('input[type="radio"]');
    const radioCount = await radios.count();
    for (let i = 0; i < radioCount; i++) {
      const name = await radios.nth(i).getAttribute('name');
      const sameGroup = page.locator(`input[name="${name}"]`);
      if (await sameGroup.first().isChecked() === false) {
        await sameGroup.first().check();
      }
    }

    // 5. 담기 버튼 클릭
    await page.locator('.add-to-cart-btn').click();

    // 카트 충돌 다이얼로그가 나오면 담기 선택
    const confirmBtn = page.locator('.dialog-confirm');
    if (await confirmBtn.isVisible({ timeout: 1000 }).catch(() => false)) {
      await confirmBtn.click();
    }

    // 6. 가게 목록으로 돌아왔는지 + 플로팅 카트 버튼이 보이는지
    await expect(page).toHaveURL(/\/shops\/\d+$/);
    await expect(page.locator('.floating-cart')).toBeVisible({ timeout: 3000 });

    // 7. 카트 페이지로 이동
    await page.locator('.floating-cart').click();
    await expect(page).toHaveURL(/\/cart/);
    await expect(page.locator('.cart-item').first()).toBeVisible();

    // 8. 주문 버튼이 활성화면 클릭, 아니면 수량 늘려서 최소 주문액 맞추기
    const orderBtn = page.locator('.order-btn');
    let attempts = 0;
    while (await orderBtn.isDisabled() && attempts < 10) {
      const plusBtn = page.locator('.cart-item-qty button').nth(1);
      await plusBtn.click();
      await page.waitForTimeout(300);
      attempts++;
    }

    // alert 처리
    page.once('dialog', async (d) => {
      expect(d.message()).toContain('주문');
      await d.accept();
    });

    await orderBtn.click();

    // 9. 홈으로 돌아옴
    await expect(page).toHaveURL('/');

    // 10. 주문 내역 페이지
    await page.locator('.header-link').click();
    await expect(page).toHaveURL('/orders');
    await expect(page.locator('.order-card').first()).toBeVisible({ timeout: 5000 });

    // 주문 1건 이상
    const orderCount = await page.locator('.order-card').count();
    expect(orderCount).toBeGreaterThanOrEqual(1);
  });
});
