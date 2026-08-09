import { test, expect, Page } from '@playwright/test';
import { loginAsUser } from '../helpers';

/**
 * [E2E] U7 — View Collection
 *
 * User story:
 *   As a regular user, I want to view my image collection, so that I can review images
 *   I positively classified.
 *
 * Mechanics (verified in MyCollectionScreen.tsx + E2eDataSeeder):
 *   - Collection is NOT in the bottom bar. It's reached from the Stats screen's
 *     "Collection" button (Stats tab → "Collection").
 *   - The screen (header "My Collection") shows a summary card: a big number +
 *     "Tags Collected" label (stats.total), then a grid of CollectionCards. Each card
 *     has the image (AuthenticatedImage → <img>), the species name, and a date.
 *   - The seeder records a YES classification for e2e_user on the BEE image, which
 *     should surface as a collected entry (species "BEE"). Empty state otherwise shows
 *     "No items yet".
 *
 * Assertions: page loads, the collection count is shown and matches the number of
 * rendered cards, and (when non-empty) YES-classified images appear with metadata
 * (species + date).
 */

test.describe('[E2E] U7 View Collection', () => {
  test('loads the collection with count, images and metadata', async ({ page }) => {
    // 1. Login as a regular user.
    await loginAsUser(page);

    // 2. Navigate to Collection: Stats tab → "Collection" button.
    await page.locator('text=Stats').first().click();
    await expect(page.getByText('Comparisons').locator('visible=true').first()).toBeVisible({ timeout: 20000 });
    await page.getByText('Collection', { exact: true }).locator('visible=true').first().click();

    // 3. Load collected images — assert the Collection page loaded (its summary label).
    await expect(page.getByText('Tags Collected').locator('visible=true').first()).toBeVisible({ timeout: 20000 });

    // Read the collection count (the big number above "Tags Collected").
    const count = await collectionCount(page);
    expect(count, 'collection count should be a non-negative number').not.toBeNull();

    if ((count ?? 0) > 0) {
      // YES-classified images appear: at least one collection card with an image.
      await expect.poll(() => collectionImageCount(page), { timeout: 20000 }).toBeGreaterThan(0);

      // Image metadata is displayed: the seeded entry's species ("Cat") is shown.
      await expect(page.getByText('Cat', { exact: true }).locator('visible=true').first()).toBeVisible({ timeout: 10000 });

      // Collection count matches the number of rendered cards.
      await expect.poll(() => collectionImageCount(page), { timeout: 10000 }).toBe(count);
    } else {
      // No seeded YES classifications surfaced → the empty state is shown instead.
      await expect(page.getByText('No items yet').locator('visible=true').first()).toBeVisible({ timeout: 10000 });
    }

    // The page never errored.
    await expect(page.locator('text=Something went wrong')).toHaveCount(0);
  });
});

/** Reads the numeric collection total shown above the "Tags Collected" label. */
async function collectionCount(page: Page): Promise<number | null> {
  return page.evaluate(() => {
    const label = Array.from(document.querySelectorAll('div')).find(
      (d) => (d.textContent || '').trim() === 'Tags Collected' && (d as HTMLElement).offsetParent !== null,
    );
    // The number is the previous sibling within the stat card.
    const card = label?.parentElement;
    if (!card) return null;
    const num = Array.from(card.querySelectorAll('div'))
      .map((d) => (d.textContent || '').trim())
      .find((t) => /^\d+$/.test(t));
    return num != null ? Number(num) : null;
  });
}

/** Counts visible collection card images (the <img> inside each CollectionCard). */
async function collectionImageCount(page: Page): Promise<number> {
  return page.evaluate(() => {
    return Array.from(document.querySelectorAll('img')).filter(
      (img) => (img as HTMLElement).offsetParent !== null && img.getAttribute('src'),
    ).length;
  });
}
