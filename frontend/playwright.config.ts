import { defineConfig, devices } from '@playwright/test'

/**
 * End-to-end tests run against a full running stack (API with the `demo` profile + this frontend).
 * Start them yourself, then: E2E_BASE_URL=http://localhost:5173 npm run e2e
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  retries: 0,
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
