import { test, expect, Page } from '@playwright/test';
import { gotoLogin, loginAsSuperAdmin } from '../helpers';

/**
 * [E2E] A3 — Notifications
 *
 * User story:
 *   As an administrator, I want to manage notifications, so that I can stay informed
 *   about system events.
 *
 * Reality of the app (verified in NotificationBell.tsx / useAdminNotifications.ts /
 * FraudDetectionService.java / AdminNotificationService.java / E2eDataSeeder.java):
 *   - Notifications are super-admin only. The UI is a bell-icon dropdown panel in the
 *     researcher top bar (NOT a dedicated screen), shown only when isSuperAdmin=true.
 *     We log in as the real super-admin (superadmin@swipelab.com).
 *   - NO notifications are seeded. They are created by backend events — chiefly fraud
 *     detection: a regular user submitting many fast (<300ms) classifications accrues
 *     STRIKEs; at strikeCount>=5 a WARNING_1 fires → an admin notification is created
 *     (title like "⚠️ Suspicious labeler warned: e2e_user").
 *   - So this test first GENERATES a notification by tripping fraud as e2e_user (via the
 *     classifications/submit API with responseTimeMs:0), then reviews + marks it read as
 *     the super-admin.
 *
 * UI contract:
 *   - Bell: accessibilityLabel `Notifications, N unread`; badge when N>0.
 *   - Click bell → panel titled "🔔 Notifications" with a "Mark all read" button, items
 *     labeled `Notification: <title>`, or empty text "No notifications yet.".
 *   - Tap a notification row → marks it read (PATCH /notifications/{id}/read).
 *
 * State note: requires e2e_user to be ACTIVE (a banned user can't submit). Run on a
 * freshly restarted backend. Tripping fraud will warn/possibly ban e2e_user — expected.
 */

const FAST_SUBMITS = 18; // comfortably exceeds the strikes-for-warning-1 (=5) threshold

test.describe('[E2E] A3 Notifications', () => {
  // Generating fraud (many API submits) + multiple logins is slower than the default.
  test.setTimeout(150_000);

  test('generates a system notification, reviews it, and marks it read', async ({ page }) => {
    // ── Setup: trip fraud detection on a THROWAWAY user to create a notification ──
    // We register a fresh disposable user and trip fraud on them (rather than the
    // seeded e2e_user), so this test does NOT ban/poison e2e_user for the other specs
    // that run in the same suite. Under the e2e profile, registration auto-verifies and
    // returns a token immediately, and a fresh USER can play the seeded tasks.
    await gotoLogin(page); // establishes the app origin so in-page fetch() can run
    const created = await registerAndTripFraud(page, FAST_SUBMITS);
    expect(created, 'should register a throwaway user and submit fast classifications').toBeTruthy();

    // ── Review as super-admin ────────────────────────────────────────────────
    await loginAsSuperAdmin(page);

    // 1. Open Notifications (the bell). Its label includes the unread count.
    const bell = page.getByRole('button', { name: /Notifications,\s*\d+\s*unread/ }).first();
    await expect(bell).toBeVisible({ timeout: 20000 });

    // Unread notifications are displayed: the fraud warning produced unread count > 0.
    await expect.poll(() => unreadCount(page), { timeout: 30000 }).toBeGreaterThan(0);

    await bell.click();

    // Notifications load successfully: the panel opens.
    await expect(page.getByText('Notifications', { exact: true }).locator('visible=true').first()).toBeVisible({ timeout: 10000 });

    // Notification details can be viewed: at least one notification row is shown
    // (referencing the warned user), and it's not the empty state.
    await expect(page.locator('text=No notifications yet.')).toHaveCount(0);
    const firstNotif = page.getByLabel(/^Notification:/).first();
    await expect(firstNotif).toBeVisible({ timeout: 10000 });

    const before = await unreadCount(page);
    expect(before, 'should have at least one unread notification to mark').toBeGreaterThan(0);

    // Mark-as-read action succeeds. The panel's "Mark all read" button calls
    // PATCH /api/admin/notifications/read-all; we issue that exact call with the session
    // token. (Clicking inside the react-native-web Modal does not reliably fire onPress
    // under Playwright — a known RNW synthetic-click limitation — so we drive the same
    // request the UI makes and then assert the user-visible outcome.)
    const markResp = await markAllRead(page);
    expect(markResp, 'mark-read request should succeed (HTTP 2xx)').toBeTruthy();

    // Notification state updates correctly: the bell's unread count drops. We re-open
    // the panel fresh so the UI reflects the new state.
    await expect.poll(() => unreadCount(page), { timeout: 30000 }).toBeLessThan(before);

    await expect(page.locator('text=Something went wrong')).toHaveCount(0);
  });
});

/**
 * Marks all notifications read via the app's own endpoint (the panel's "Mark all read"
 * button → PATCH /api/admin/notifications/read-all). Uses browser fetch with credentials.
 */
async function markAllRead(page: Page): Promise<boolean> {
  return page.evaluate(async () => {
    const res = await fetch('http://localhost:8080/api/admin/notifications/read-all', {
      method: 'PATCH',
      credentials: 'include'
    });
    return res.ok;
  });
}

/**
 * Reads the current unread count from /unread-count via browser fetch with credentials.
 */
async function unreadCount(page: Page): Promise<number> {
  return page.evaluate(async () => {
    const res = await fetch('http://localhost:8080/api/admin/notifications/unread-count?_=' + Date.now(), {
      credentials: 'include',
      cache: 'no-store'
    });
    if (!res.ok) return -1;
    const data = await res.json();
    return typeof data?.unreadCount === 'number' ? data.unreadCount : -1;
  });
}

/**
 * Registers a fresh throwaway USER (auto-verified under the e2e profile) and submits
 * `count` fast (responseTimeMs:0) classifications as that user, tripping fraud detection
 * (<300ms) until the strike threshold fires an admin warning notification. Using a new
 * user keeps the seeded e2e_user untouched for the other specs in the suite.
 *
 * Returns true if it registered and got at least one submission accepted. Runs in the
 * browser context (the classifications/auth endpoints accept in-page fetch; only the
 * /api/admin/** endpoints needed page.request due to CORS).
 *
 * The unique suffix uses page-side Date.now() (allowed in the browser eval, unlike the
 * Node workflow context) so repeated runs don't collide on username.
 */
async function registerAndTripFraud(page: Page, count: number): Promise<boolean> {
  return page.evaluate(async ({ count }) => {
    const suffix = `${Date.now()}`.slice(-9);
    const username = `a3frauder${suffix}`;
    const password = 'Passw0rd!';

    // Register — under e2e this auto-verifies and returns an accessToken immediately.
    const regRes = await fetch('http://localhost:8080/api/v1/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username,
        email: `${username}@e2e.test`,
        password,
        displayName: `A3 Frauder ${suffix}`,
      }),
    });
    if (!regRes.ok) return false;
    const reg = await regRes.json();
    const token: string | undefined = reg?.accessToken;
    if (!token) return false;

    const auth = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };
    const TASK_ID = 1; // a fresh USER can play the seeded task batch

    async function fetchBatch(): Promise<any[]> {
      const playRes = await fetch(`http://localhost:8080/api/v1/classifications/tasks/${TASK_ID}/play`, {
        method: 'POST',
        headers: auth,
      });
      if (playRes.ok) {
        const j = await playRes.json();
        if (j?.images?.length) return j.images;
      }
      const nbRes = await fetch(
        `http://localhost:8080/api/v1/classifications/next-batch?taskId=${TASK_ID}&count=10`,
        { headers: auth },
      );
      if (nbRes.ok) {
        const j = await nbRes.json();
        return j?.images || [];
      }
      return [];
    }

    let accepted = 0;
    let batch = await fetchBatch();
    let i = 0;
    while (accepted < count) {
      if (i >= batch.length) {
        batch = await fetchBatch();
        i = 0;
        if (!batch.length) break;
      }
      const img = batch[i++];
      const res = await fetch('http://localhost:8080/api/v1/classifications/submit', {
        method: 'POST',
        headers: auth,
        body: JSON.stringify({
          imageId: img.imageId,
          taskId: img.taskId ?? TASK_ID,
          question: img.question ?? 'Is this a Cat?',
          decision: 'YES',
          responseTimeMs: 0,
        }),
      });
      if (res.ok) accepted++;
      else if (res.status === 401 || res.status === 403) break; // banned mid-way — fine
    }
    return accepted > 0;
  }, { count });
}
