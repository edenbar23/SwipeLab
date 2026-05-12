import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:8081';

const ADMIN_USER = 'admin_mock';
const PASSWORD   = 'password';

// ── Login Helper ───────────────────────────────────────────────────────────────
async function loginAsAdmin(page: any) {
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    await expect(page.locator('text=Welcome to SwipeLab')).toBeVisible({ timeout: 15000 });

    await page.locator('input[placeholder="Username"]').fill(ADMIN_USER);
    await page.locator('input[placeholder="Password"]').fill(PASSWORD);
    await page.locator('text=Login').first().click();

    // Wait for admin dashboard
    await expect(page.locator('text=Tasks').first()).toBeVisible({ timeout: 15000 });
}

test.describe('Admin Users Management', () => {
    test.beforeEach(async ({ page }) => {
        await loginAsAdmin(page);
        
        // Navigate to Users screen via bottom nav
        await page.locator('text=Users').first().click();
        await page.waitForTimeout(2000);
        await expect(page.locator('input[placeholder="Search users..."]')).toBeVisible();
    });

    test('search filters users list (happy flow)', async ({ page }) => {
        // Wait for users to load
        await page.waitForTimeout(2000);
        
        // Ensure the input exists and we can type in it
        await page.locator('input[placeholder="Search users..."]').fill('admin_mock');
        await page.waitForTimeout(1000);
        
        // Ensure admin_mock is visible (filtered list)
        await expect(page.locator('text=admin_mock').first()).toBeVisible();
    });

    test('search shows no users found for invalid query (edge case)', async ({ page }) => {
        // Wait for users to load
        await page.waitForTimeout(2000);
        
        // Search for non-existent user
        await page.locator('input[placeholder="Search users..."]').fill('nonexistent_user_12345');
        await page.waitForTimeout(1000);
        
        // Should show 'No users found.' text
        await expect(page.locator('text=No users found.')).toBeVisible();
    });
});
