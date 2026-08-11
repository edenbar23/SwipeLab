import { expect, Page } from '@playwright/test';

/**
 * Shared helpers for the E2E user-story suite (tests/e2e).
 *
 * Constraint: the app ships no testIDs and we do not modify app source, so every
 * selector here targets visible text or input placeholders only — matching the
 * real LoginScreen / RegisterForm / UserNavigator / UserTopBar markup.
 */

export const BASE_URL = 'http://localhost:8081';

/** Seeded e2e users (see backend E2eDataSeeder / E2E_GUIDE.md). */
export const SEEDED = {
  user: { username: 'e2e_user', password: 'password' },
  admin: { username: 'admin_e2e', password: 'superpassword123' },
  reviewer: { username: 'reviewer_e2e', password: 'password' },
  // Under the e2e profile, the super-admin IS admin_e2e (application-e2e.yml sets
  // app.security.super-admin.username=admin_e2e). So admin_e2e is a RESEARCHER *and*
  // isSuperAdmin=true — it can reach the Users Management screen, ban/unban, and see
  // admin notifications.
  superAdmin: { username: 'admin_e2e', password: 'superpassword123' },
} as const;

/**
 * Navigate to the app and wait for the login screen to be ready.
 *
 * The app persists auth in web localStorage, so a prior run/test can leave it logged in
 * (it would then skip the login screen). We clear storage on first load so every test
 * starts logged out, then reload to land on the login screen deterministically.
 */
export async function gotoLogin(page: Page): Promise<void> {
  await page.goto(BASE_URL);
  await page.evaluate(() => {
    try { window.localStorage.clear(); } catch {}
  });
  await page.reload();
  await page.waitForLoadState('domcontentloaded');
  await expect(page.locator('text=Welcome to SwipeLab')).toBeVisible({ timeout: 20000 });
}

/** Fill the login form and submit. Username is matched case-insensitively by the
 *  backend but stored lowercase, so pass the exact handle you registered with. */
export async function login(page: Page, username: string, password: string): Promise<void> {
  await page.locator('input[placeholder="Enter your username"]').fill(username);
  await page.locator('input[placeholder="Enter your password"]').fill(password);
  await page.locator('text=Login').first().click();
}

/** Assert the regular-user home is shown: login title gone + a USER bottom-bar marker. */
export async function assertOnUserHome(page: Page): Promise<void> {
  await expect(page.locator('text=Welcome to SwipeLab')).not.toBeVisible({ timeout: 20000 });
  // Bottom-bar labels rendered only inside UserNavigator.
  await expect(page.locator('text=My Tasks').first()).toBeVisible({ timeout: 20000 });
  await expect(page.locator('text=Leaderboard').first()).toBeVisible({ timeout: 10000 });
}

/** Check if the app stored the auth session (on web, this is the isAuthenticated flag). */
export async function getStoredToken(page: Page): Promise<string | null> {
  return page.evaluate(() => window.localStorage.getItem('isAuthenticated') || window.localStorage.getItem('token'));
}

/** Log in as the seeded regular user and wait for the home screen. */
export async function loginAsUser(page: Page): Promise<void> {
  await gotoLogin(page);
  await login(page, SEEDED.user.username, SEEDED.user.password);
  await assertOnUserHome(page);
}

/**
 * Log in as the super-admin (RESEARCHER role, isSuperAdmin=true) via the normal
 * username/password Login button, and wait for the researcher dashboard. The
 * super-admin-only "Users" tab is then available in the bottom bar.
 */
export async function loginAsSuperAdmin(page: Page): Promise<void> {
  await gotoLogin(page);
  await login(page, SEEDED.superAdmin.username, SEEDED.superAdmin.password);
  // Researcher dashboard tiles confirm we left the login screen into researcher mode.
  await expect(page.locator('text=Welcome to SwipeLab')).not.toBeVisible({ timeout: 20000 });
  await expect(page.getByText('Tasks').locator('visible=true').first()).toBeVisible({ timeout: 20000 });
}

/** From anywhere in UserNavigator, open the "My Tasks" screen via the bottom bar. */
export async function gotoMyTasks(page: Page): Promise<void> {
  await page.locator('text=My Tasks').first().click();
  // "Explore Tasks" may match more than one node (section header + a nav/label), so
  // scope to the first visible one rather than requiring strict uniqueness.
  await expect(page.getByText('Explore Tasks').locator('visible=true').first()).toBeVisible({ timeout: 20000 });
}

/**
 * Open the Challenges screen. It is not in the bottom bar — the UserTopBar's stats
 * block (Score / Rank / streak) navigates there on press. We click the "Score:" chip.
 */
export async function gotoChallenges(page: Page): Promise<void> {
  await page.getByText(/^Score:/).first().click();
  await expect(page.getByText('In Progress').locator('visible=true').first()).toBeVisible({ timeout: 20000 });
}

/** Click the Logout button in UserTopBar and wait for the login screen to return. */
export async function logout(page: Page): Promise<void> {
  await page.locator('text=Logout').first().click();
  await expect(page.locator('text=Welcome to SwipeLab')).toBeVisible({ timeout: 20000 });
}
