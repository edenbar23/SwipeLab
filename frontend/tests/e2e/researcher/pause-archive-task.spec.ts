import { test, expect, Page } from '@playwright/test';

const BASE_URL = 'http://localhost:8081';
const STARDBI_USER = 'swipe_lab_test_user';
const STARDBI_PASS = 'password';

// R8 archives the task it operates on, and archiving is a ONE-WAY transition in the
// backend (no ARCHIVED -> ACTIVE — see Task.activate()). So this spec must NOT touch
// "E2E Identification Task" (id 1), which the user specs (U4 swipe, U9 references) rely
// on staying Active in the same shared-backend run. It targets a dedicated, disposable
// seeded task instead (see E2eDataSeeder#seedTasksAndImages).
const ARCHIVE_TASK = 'E2E Archive Target';

/** Log in as the StarDBI researcher and wait for the dashboard. */
async function loginAsResearcher(page: Page): Promise<void> {
    await test.step('Login as researcher', async () => {
        await page.goto(BASE_URL);
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(1500);

        await expect(page.locator('text=Welcome to SwipeLab')).toBeVisible({ timeout: 15000 });
        await page.locator('input[placeholder="Enter your username"]').fill(STARDBI_USER);
        await page.locator('input[placeholder="Enter your password"]').fill(STARDBI_PASS);
        await page.keyboard.press('Escape');
        await page.waitForTimeout(500);
        await page.locator('text=Login as Researcher').click();
        await expect(page.locator('text=Welcome to SwipeLab')).not.toBeVisible({ timeout: 15000 });
    });
}

test.describe('[E2E] R8 Pause / Archive Task', () => {
    test.beforeEach(async ({ page }) => {
        test.setTimeout(120000);
        page.on('console', msg => console.log(`[Browser] ${msg.type()}: ${msg.text()}`));
        page.on('pageerror', error => console.log(`[Browser Error]: ${error.message}`));
        
        await loginAsResearcher(page);
    });

    test('should successfully pause and archive an active task', async ({ page }) => {
        test.setTimeout(90000);

        await test.step('Open Task Management', async () => {
            // From ResearcherDashboard (Home), navigate to Tasks
            await expect(page.locator('text=Tasks').first()).toBeVisible({ timeout: 15000 });
            await page.locator('text=Tasks').first().click();

            // Wait for task list to load
            await expect(page.getByPlaceholder('Search tasks…')).toBeVisible({ timeout: 15000 });
            await expect(page.locator('text=Progress:').first()).toBeVisible({ timeout: 15000 });
        });

        await test.step('Ensure task is active', async () => {
            // Search for the pre-seeded, disposable archive-target task
            await page.getByPlaceholder('Search tasks…').fill(ARCHIVE_TASK);
            await expect(page.locator('text=Progress:').first()).toBeVisible({ timeout: 15000 });

            // In the list view, the TaskCard has a Pause/Resume button. 
            // If the task is currently PAUSED or ARCHIVED, it will say "Resume".
            try {
                const resumeBtn = page.getByText('Resume', { exact: true }).first();
                await expect(resumeBtn).toBeVisible({ timeout: 3000 });
                console.log('[Debug] Found Resume button on TaskCard, clicking it to activate task');
                await resumeBtn.click();
                // Wait for it to turn into a Pause button
                await expect(page.getByText('Pause', { exact: true }).first()).toBeVisible({ timeout: 10000 });
            } catch (e) {
                console.log('[Debug] Resume button not found (task is likely already Active)');
            }
        });

        await test.step('Select a task', async () => {
            // Click the task card to open details
            await page.getByText(ARCHIVE_TASK).first().click();

            // Wait for task details to load
            await expect(page.locator('text=Back to Tasks')).toBeVisible({ timeout: 15000 });
            await expect(page.locator('text=Classification Progress')).toBeVisible();
        });

        await test.step('Pause task', async () => {
            // Click the Pause button in details view (use .last() because previous screen is still in DOM)
            await expect(page.getByText('Pause', { exact: true }).last()).toBeVisible();
            await page.getByText('Pause', { exact: true }).last().click();
        });

        await test.step('Verify status update', async () => {
            // Status badge in Task Details changes to "Paused"
            await expect(page.getByText('Paused', { exact: true }).last()).toBeVisible({ timeout: 10000 });
            // The action button should now say "Resume"
            await expect(page.getByText('Resume', { exact: true }).last()).toBeVisible();
        });

        await test.step('Archive paused task', async () => {
            // Click the Archive button
            await expect(page.getByText('Archive', { exact: true }).last()).toBeVisible();
            await page.getByText('Archive', { exact: true }).last().click();
            
            // Confirm the archive action in the modal
            await expect(page.getByText('Are you sure you want to archive', { exact: false }).last()).toBeVisible();
            await page.getByText('Archive', { exact: true }).last().click();
        });

        await test.step('Verify archive status', async () => {
            // Status badge changes to "Archived"
            await expect(page.getByText('Archived', { exact: true }).last()).toBeVisible({ timeout: 10000 });
            
            // Go back to tasks list
            await page.locator('text=Back to Tasks').click();
            await expect(page.getByPlaceholder('Search tasks…')).toBeVisible({ timeout: 15000 });
        });
    });
});
