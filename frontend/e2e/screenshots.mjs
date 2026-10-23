// Captures the README screenshots from a running stack (API with the demo profile + frontend).
// Usage: node e2e/screenshots.mjs [baseUrl]   — writes PNGs to ../docs/screenshots
import { chromium } from '@playwright/test'

const base = process.argv[2] ?? 'http://localhost:5173'
const out = new URL('../../docs/screenshots/', import.meta.url).pathname
const browser = await chromium.launch()
const page = await browser.newPage({ viewport: { width: 1280, height: 800 }, deviceScaleFactor: 1 })
const shot = async (name) => {
  await page.locator('[data-sonner-toast]').first().waitFor({ state: 'detached', timeout: 8000 }).catch(() => {})
  await page.waitForTimeout(600); await page.screenshot({ path: `${out}${name}.png` }) }
const login = async (role) => {
  await page.goto(`${base}/login`)
  await page.getByRole('button', { name: new RegExp(`^${role}`) }).click()
  await page.getByRole('button', { name: 'Sign out' }).waitFor()
}
/** Full reload while signed in: wait until the refresh-token rotation has finished before moving on. */
const go = async (path) => {
  await page.goto(`${base}${path}`)
  await page.getByRole('button', { name: 'Sign out' }).waitFor()
}
const logout = async () => {
  await page.getByRole('button', { name: 'Sign out' }).click()
  await page.getByRole('link', { name: 'Sign in' }).waitFor()
}

await page.goto(base)
await page.getByText('Coming up').waitFor()
await shot('home')

await page.goto(`${base}/events`)
await page.getByText(/\d+ events/).waitFor()
await shot('events')

await login('Customer')
await go('/checkout')
await page.getByText(/Held seats|No active reservation|Your hold expired/).first().waitFor()
const release = page.getByRole('button', { name: 'Release seats' })
if (await release.isVisible()) { await release.click(); await page.waitForURL(/\/events\/\d+$/) }
await go('/events?query=neon')
await page.getByRole('link', { name: /Neon Nights/ }).click()
await page.waitForURL(/\/events\/\d+$/)
await page.getByRole('button', { name: 'More' }).nth(1).click()
await page.getByRole('button', { name: 'More' }).nth(1).click()
await shot('event-detail')
await page.getByRole('button', { name: /Hold 2 seats/ }).click()
await page.getByText('Seats held for', { exact: true }).waitFor()
await page.waitForTimeout(3000)
await shot('checkout')
await page.getByRole('button', { name: /^Pay/ }).click()
await page.waitForURL(/my\/tickets/)
await page.getByRole('img', { name: /QR code/ }).first().waitFor()
await shot('tickets')
const code = (await page.getByTitle('Copy ticket code').first().innerText()).trim()
await logout()

await login('Gate staff')
await page.getByRole('textbox', { name: 'Ticket code' }).fill(code)
await page.getByRole('button', { name: 'Check in' }).click()
await page.getByRole('status').waitFor()
await shot('gate')
await logout()

await login('Organizer')
await go('/organizer/events/1/stats')
await page.getByText('Inventory by tier').waitFor()
await page.waitForTimeout(1200)
await shot('organizer-stats')

await browser.close()
console.log('screenshots written to', out)
