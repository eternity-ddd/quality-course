import { test, expect } from '@playwright/test';

test.describe('가게 목록 페이지', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('헤더와 기본 UI 요소가 표시된다', async ({ page }) => {
    await expect(page.locator('.header h1')).toHaveText('배달이요');
    await expect(page.locator('.header-link')).toHaveText('내 주문');
    await expect(page.locator('.location-bar')).toContainText('현재 위치 기준');
  });

  test('카테고리 버튼이 모두 표시된다', async ({ page }) => {
    const categories = ['전체', '한식', '중식', '치킨', '피자', '분식', '일식'];
    for (const cat of categories) {
      await expect(page.locator('.category-btn', { hasText: cat })).toBeVisible();
    }
  });

  test('전체 카테고리가 기본 선택되어 있다', async ({ page }) => {
    const allBtn = page.locator('.category-btn', { hasText: '전체' });
    await expect(allBtn).toHaveClass(/active/);
  });

  test('가게 카드가 로드된다', async ({ page }) => {
    const shopCards = page.locator('.shop-card');
    await expect(shopCards.first()).toBeVisible({ timeout: 5000 });
    const count = await shopCards.count();
    expect(count).toBeGreaterThan(0);
  });

  test('가게 카드에 이름, 평점, 거리, 최소주문, 배달비 정보가 있다', async ({ page }) => {
    const firstCard = page.locator('.shop-card').first();
    await expect(firstCard).toBeVisible({ timeout: 5000 });

    await expect(firstCard.locator('h3')).toBeVisible();
    await expect(firstCard.locator('.rating')).toContainText('★');
    await expect(firstCard.locator('.shop-tags')).toContainText('최소주문');
    await expect(firstCard.locator('.shop-tags')).toContainText('배달비');
  });

  test('카테고리 필터링이 동작한다', async ({ page }) => {
    // 먼저 가게 목록이 로드될 때까지 대기
    await expect(page.locator('.shop-card').first()).toBeVisible({ timeout: 5000 });

    // 치킨 카테고리 클릭
    await page.locator('.category-btn', { hasText: '치킨' }).click();
    await expect(page.locator('.category-btn', { hasText: '치킨' })).toHaveClass(/active/);

    // 결과가 로드될 때까지 대기
    await page.waitForTimeout(500);
    const shopCards = page.locator('.shop-card');
    const count = await shopCards.count();
    expect(count).toBeGreaterThan(0);
  });

  test('다른 카테고리로 전환하면 목록이 변경된다', async ({ page }) => {
    await expect(page.locator('.shop-card').first()).toBeVisible({ timeout: 5000 });

    // 피자 카테고리
    await page.locator('.category-btn', { hasText: '피자' }).click();
    await page.waitForTimeout(500);
    const pizzaCount = await page.locator('.shop-card').count();

    // 분식 카테고리
    await page.locator('.category-btn', { hasText: '분식' }).click();
    await page.waitForTimeout(500);
    const snackCount = await page.locator('.shop-card').count();

    // 둘 다 결과가 있어야 함
    expect(pizzaCount).toBeGreaterThan(0);
    expect(snackCount).toBeGreaterThan(0);
  });

  test('가게 카드 클릭 시 메뉴 목록 페이지로 이동한다', async ({ page }) => {
    const firstShop = page.locator('.shop-card').first();
    await expect(firstShop).toBeVisible({ timeout: 5000 });
    await firstShop.click();
    await expect(page).toHaveURL(/\/shops\/\d+/);
  });

  test('내 주문 링크 클릭 시 주문 내역 페이지로 이동한다', async ({ page }) => {
    await page.locator('.header-link').click();
    await expect(page).toHaveURL('/orders');
  });
});
