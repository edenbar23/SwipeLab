import { test, expect } from '@playwright/test';
import { loginAsUser, assertOnUserHome, gotoLogin } from '../helpers';

test.describe('[E2E] U10 Settings', () => {
  test('navigates to settings and interacts with options', async ({ page }) => {
    // 1. Login as user and reach home
    await loginAsUser(page);

    // 2. Navigate to Settings via bottom tab
    await page.getByText('Settings').first().click();

    // 3. Verify Settings page elements
    // The Settings screen header should be visible
    await expect(page.getByText('Settings', { exact: true }).locator('visible=true').first()).toBeVisible({ timeout: 10000 });
    
    await expect(page.getByText('Profile').locator('visible=true').first()).toBeVisible();
    await expect(page.getByText('Notifications').locator('visible=true').first()).toBeVisible();
    await expect(page.getByText('Dark Mode').locator('visible=true').first()).toBeVisible();

    // 4. Verify Log Out button exists and works
    const logOutBtn = page.getByText('Log Out').locator('visible=true').first();
    await expect(logOutBtn).toBeVisible();

    await logOutBtn.click();

    // 5. Assert we are returned to the login screen
    await expect(page.getByText('Welcome to SwipeLab').locator('visible=true').first()).toBeVisible({ timeout: 10000 });
  });

  test('top bar logout works', async ({ page }) => {
    await loginAsUser(page);
    
    // Find top bar logout icon (usually an icon, but we can look for "Logout" text if present)
    const topBarLogout = page.getByText('Logout', { exact: true }).locator('visible=true').first();
    await expect(topBarLogout).toBeVisible({ timeout: 10000 });
    await topBarLogout.click();

    await expect(page.getByText('Welcome to SwipeLab').locator('visible=true').first()).toBeVisible({ timeout: 10000 });
  });
});
