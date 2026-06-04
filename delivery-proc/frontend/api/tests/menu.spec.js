import { test, expect } from '@playwright/test';

test.describe('메뉴 목록 페이지', () => {
  test.beforeEach(async ({ page }) => {
    // 첫 번째 가게(명동할매칼국수)로 이동
    await page.goto('/shops/1');
  });

  test('가게 이름이 헤더에 표시된다', async ({ page }) => {
    await expect(page.locator('.header h1')).toHaveText('명동할매칼국수');
  });

  test('뒤로가기 버튼이 있다', async ({ page }) => {
    await expect(page.locator('.back-btn')).toBeVisible();
  });

  test('가게 상세 정보가 표시된다', async ({ page }) => {
    const detailBar = page.locator('.shop-detail-bar');
    await expect(detailBar).toBeVisible({ timeout: 5000 });
    await expect(detailBar).toContainText('최소주문');
    await expect(detailBar).toContainText('★');
  });

  test('메뉴 항목들이 표시된다', async ({ page }) => {
    const menuItems = page.locator('.menu-item');
    await expect(menuItems.first()).toBeVisible({ timeout: 5000 });
    const count = await menuItems.count();
    expect(count).toBeGreaterThan(0);
  });

  test('메뉴 항목에 이름과 가격이 있다', async ({ page }) => {
    const firstMenu = page.locator('.menu-item').first();
    await expect(firstMenu).toBeVisible({ timeout: 5000 });
    await expect(firstMenu.locator('h4')).toBeVisible();
    await expect(firstMenu.locator('.price')).toContainText('원');
  });

  test('메뉴 클릭 시 메뉴 상세 페이지로 이동한다', async ({ page }) => {
    const firstMenu = page.locator('.menu-item').first();
    await expect(firstMenu).toBeVisible({ timeout: 5000 });
    await firstMenu.click();
    await expect(page).toHaveURL(/\/shops\/1\/menus\/\d+/);
  });

  test('뒤로가기 버튼으로 이전 페이지로 돌아간다', async ({ page }) => {
    // 홈에서 가게로 진입해야 navigate(-1)이 홈으로 돌아감
    await page.goto('/');
    await expect(page.locator('.shop-card').first()).toBeVisible({ timeout: 5000 });
    await page.locator('.shop-card').first().click();
    await expect(page).toHaveURL(/\/shops\/\d+/);

    await page.locator('.back-btn').click();
    await expect(page).toHaveURL('/');
  });
});

test.describe('메뉴 상세 페이지', () => {
  test('메뉴 이름과 가격이 표시된다', async ({ page }) => {
    // 칼국수 (메뉴 ID 1) - 옵션 그룹이 있는 메뉴
    await page.goto('/shops/1/menus/1');
    await expect(page.locator('.menu-detail-name')).toHaveText('칼국수');
    await expect(page.locator('.menu-detail-price')).toContainText('9,000원');
  });

  test('필수 옵션 그룹에 필수 뱃지가 표시된다', async ({ page }) => {
    await page.goto('/shops/1/menus/1');
    await expect(page.locator('.required-badge').first()).toBeVisible({ timeout: 5000 });
    await expect(page.locator('.required-badge').first()).toHaveText('필수');
  });

  test('필수 옵션 미선택 시 담기 버튼이 비활성화된다', async ({ page }) => {
    await page.goto('/shops/1/menus/1');
    await expect(page.locator('.add-to-cart-btn')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('.add-to-cart-btn')).toBeDisabled();
    await expect(page.locator('.add-to-cart-btn')).toContainText('필수 옵션을 선택해주세요');
  });

  test('필수 옵션 선택 후 담기 버튼이 활성화된다', async ({ page }) => {
    await page.goto('/shops/1/menus/1');
    // 면 양 필수 옵션 선택 (보통)
    const radios = page.locator('input[type="radio"]');
    await expect(radios.first()).toBeVisible({ timeout: 5000 });
    await radios.first().check();

    await expect(page.locator('.add-to-cart-btn')).toBeEnabled();
    await expect(page.locator('.add-to-cart-btn')).toContainText('원 담기');
  });

  test('수량 조절이 동작한다', async ({ page }) => {
    await page.goto('/shops/1/menus/1');
    await expect(page.locator('.qty-num')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('.qty-num')).toHaveText('1');

    // 수량 증가
    await page.locator('.qty-btn').nth(1).click();
    await expect(page.locator('.qty-num')).toHaveText('2');

    // 수량 감소
    await page.locator('.qty-btn').first().click();
    await expect(page.locator('.qty-num')).toHaveText('1');
  });

  test('수량 1 미만으로 줄어들지 않는다', async ({ page }) => {
    await page.goto('/shops/1/menus/1');
    await expect(page.locator('.qty-num')).toBeVisible({ timeout: 5000 });
    const minusBtn = page.locator('.qty-btn').first();
    await expect(minusBtn).toBeDisabled();
  });

  test('선택 옵션(체크박스)을 여러 개 선택할 수 있다', async ({ page }) => {
    // 칼국수에는 토핑 추가(선택 옵션)가 있다
    await page.goto('/shops/1/menus/1');
    const checkboxes = page.locator('input[type="checkbox"]');
    await expect(checkboxes.first()).toBeVisible({ timeout: 5000 });

    await checkboxes.nth(0).check();
    await checkboxes.nth(1).check();

    await expect(checkboxes.nth(0)).toBeChecked();
    await expect(checkboxes.nth(1)).toBeChecked();
  });

  test('옵션 선택에 따라 총 가격이 변경된다', async ({ page }) => {
    await page.goto('/shops/1/menus/1');

    // 필수 옵션 선택 (보통 - 0원)
    const radios = page.locator('input[type="radio"]');
    await expect(radios.first()).toBeVisible({ timeout: 5000 });
    await radios.first().check();

    const btnText1 = await page.locator('.add-to-cart-btn').textContent();

    // 곱배기 선택 (+1,500원)
    await radios.nth(1).check();
    const btnText2 = await page.locator('.add-to-cart-btn').textContent();

    // 가격이 달라야 함
    expect(btnText1).not.toEqual(btnText2);
  });
});
