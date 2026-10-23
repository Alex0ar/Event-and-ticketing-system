import { expect, test, type Page } from '@playwright/test'

async function demoLogin(page: Page, role: 'Customer' | 'Gate staff' | 'Organizer') {
  await page.goto('/login')
  await page.getByRole('button', { name: new RegExp(`^${role}`) }).click()
  await expect(page.getByRole('button', { name: 'Sign out' })).toBeVisible()
}

/** The demo customer can hold only one reservation at a time; drop any left over from earlier runs. */
async function releaseActiveHold(page: Page) {
  await page.goto('/checkout')
  const release = page.getByRole('button', { name: 'Release seats' })
  const empty = page.getByText(/No active reservation|Your hold expired/)
  await expect(release.or(empty)).toBeVisible()
  if (await release.isVisible()) {
    await release.click()
    await expect(page).toHaveURL(/\/events\/\d+$/)
  }
}

async function signOut(page: Page) {
  await page.getByRole('button', { name: 'Sign out' }).click()
  await expect(page.getByRole('link', { name: 'Sign in' })).toBeVisible()
}

test('customer buys a ticket and gate staff admits it exactly once', async ({ page }) => {
  await demoLogin(page, 'Customer')
  await releaseActiveHold(page)

  // Browse and hold one seat.
  await page.goto('/events?query=neon')
  await page.getByRole('link', { name: /Neon Nights/ }).click()
  await expect(page).toHaveURL(/\/events\/\d+$/)
  await page.getByRole('button', { name: 'More' }).first().click()
  await page.getByRole('button', { name: /Hold 1 seat/ }).click()

  // Checkout: hold is visible with its countdown; pay.
  await expect(page).toHaveURL(/\/checkout$/)
  await expect(page.getByText('Seats held for', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: /^Pay/ }).click()

  // Tickets: newest first, with a QR image.
  await expect(page).toHaveURL(/\/my\/tickets$/)
  const code = (await page.getByTitle('Copy ticket code').first().innerText()).trim()
  expect(code).toMatch(/^[0-9a-f-]{36}$/)
  await expect(page.getByRole('img', { name: `QR code for ticket ${code}` })).toBeVisible()
  await signOut(page)

  // Gate: first scan admits, second is rejected.
  await demoLogin(page, 'Gate staff')
  await page.getByRole('textbox', { name: 'Ticket code' }).fill(code)
  await page.getByRole('button', { name: 'Check in' }).click()
  await expect(page.getByRole('status')).toContainText('Welcome in')

  await page.getByRole('button', { name: 'Check in' }).click()
  await expect(page.getByRole('status')).toContainText('Already checked in')
})

test('a declined card keeps the hold so the customer can retry', async ({ page }) => {
  await demoLogin(page, 'Customer')
  await releaseActiveHold(page)
  await page.goto('/events?query=derby')
  await page.getByRole('link', { name: /Bucharest Derby/ }).click()
  await expect(page).toHaveURL(/\/events\/\d+$/)
  await page.getByRole('button', { name: 'More' }).first().click()
  await page.getByRole('button', { name: /Hold 1 seat/ }).click()

  await page.getByLabel(/Simulate a declined card/).check()
  await page.getByRole('button', { name: /^Pay/ }).click()
  await expect(page.getByRole('alert')).toContainText('Card declined')

  await page.getByLabel(/Simulate a declined card/).uncheck()
  await page.getByRole('button', { name: 'Try again' }).click()
  await expect(page).toHaveURL(/\/my\/tickets$/)
})

test('anonymous visitors are sent to login for protected pages', async ({ page }) => {
  await page.goto('/my/tickets')
  await expect(page).toHaveURL(/\/login$/)
})
