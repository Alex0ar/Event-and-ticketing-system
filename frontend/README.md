# TicketFlow — Frontend

React + TypeScript single-page app for the TicketFlow API.

## Stack

React 19 · TypeScript · Vite · React Router · TanStack Query · axios · react-hook-form ·
Tailwind CSS · recharts · ZXing (camera QR scanning) · Vitest · Playwright

## Run locally

Needs the API running (see the root README) — with the `demo` profile if you want the demo accounts.

```bash
cp .env.example .env.local   # VITE_API_URL=http://localhost:8080
npm install
npm run dev                  # http://localhost:5173
```

The API must allow this origin: `APP_CORS_ALLOWED_ORIGINS=http://localhost:5173` (the default).

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Dev server with hot reload |
| `npm run build` | Typecheck + production build into `dist/` |
| `npm run typecheck` | TypeScript only |
| `npm run lint` | oxlint |
| `npm test` | Unit/component tests (Vitest) |
| `npm run e2e` | Playwright golden path — needs the full stack running with the `demo` profile |

## Structure

```
src/
  api/          axios client (bearer token, single-flight refresh on 401), typed endpoint modules
  auth/         AuthProvider, token store, role-guarded routes
  components/   UI primitives + domain components (EventCard, CountdownTimer, QrImage, …)
  pages/        public · auth · customer · organizer · gate · admin
  lib/          formatting, idempotency keys, helpers
e2e/            Playwright specs
```

## Notes on behaviour

- **Tokens**: the access token is kept in memory; the refresh token in `localStorage` so a reload can
  restore the session. Concurrent 401s share one refresh call, because the API rotates refresh tokens.
- **Payments**: each attempt gets an `Idempotency-Key` stored in `sessionStorage`. A network failure keeps
  the key (a retry can't double-charge); a definitive answer such as a decline discards it.
- **Free-tier hosting**: `ServerWakeBanner` pings `/actuator/health` on load and explains the delay if the
  API is waking from sleep.
- Organizer, gate and admin pages are lazy-loaded so visitors don't download the charting and QR libraries.

## Deploy

Vercel: set the project root to `frontend/`, add `VITE_API_URL` (and optionally `VITE_GITHUB_URL`).
`vercel.json` rewrites all paths to `index.html` for client-side routing. See `../DEPLOY.md`.
