# Deploying the live demo (free tier)

```
Browser ──► Vercel (React app, static, always on)
               │ VITE_API_URL
               ▼
            Render free web service (this repo's Dockerfile, sleeps when idle)
               │ JDBC + SSL
               ▼
            Neon free Postgres (persistent)
```

The frontend is hosted separately so the page loads instantly even while the API wakes up; it shows a
"waking up the demo server" banner during the cold start (typically 30–60 s, once).

> Free-tier limits and UIs change. If a step below doesn't match what you see, check the provider's
> current docs — the configuration values stay the same.

You need accounts on **GitHub** (have it), **Neon**, **Render** and **Vercel**. All three can sign in
with GitHub.

---

## 1. Push the code

Merge the work into `main` and push — Render and Vercel deploy from GitHub.

```bash
git push origin main
```

## 2. Database — Neon

1. [neon.tech](https://neon.tech) → **New project** (any name, region close to your Render region).
2. Open **Connection details** and pick the **Java / JDBC** format if offered. You need three values:
   - `DB_URL` = `jdbc:postgresql://<host>/<database>?sslmode=require`
     (from a URL like `postgresql://user:pass@ep-xyz.eu-central-1.aws.neon.tech/neondb`, the host is
     `ep-xyz.eu-central-1.aws.neon.tech` and the database is `neondb`)
   - `DB_USER` — the role name
   - `DB_PASSWORD` — the password

Flyway creates all tables on the API's first start; you don't run any SQL yourself.

## 3. API — Render

1. [render.com](https://render.com) → **New → Blueprint** → select the GitHub repo.
   Render reads `render.yaml` and proposes a free Docker web service named `ticketflow-api`.
2. Fill in the values it asks for:
   - `DB_URL`, `DB_USER`, `DB_PASSWORD` from Neon
   - `APP_CORS_ALLOWED_ORIGINS` — put `http://localhost:5173` for now; you'll replace it in step 5
   (`JWT_SECRET` is generated automatically; `SPRING_PROFILES_ACTIVE=prod,demo` and
   `APP_DEMO_RESET_ON_STARTUP=true` come from the blueprint.)
3. **Apply**. The first build takes a few minutes.
4. Check it: `https://ticketflow-api.onrender.com/actuator/health` → `{"status":"UP"}`
   (your exact URL is shown at the top of the service page).

## 4. Frontend — Vercel

1. [vercel.com](https://vercel.com) → **Add New → Project** → import the repo.
2. **Root Directory**: `frontend`. Framework preset: **Vite** (auto-detected).
3. **Environment Variables**:
   - `VITE_API_URL` = your Render URL, e.g. `https://ticketflow-api.onrender.com` (no trailing slash)
   - `VITE_GITHUB_URL` = `https://github.com/Alex0ar/Event-and-ticketing-system`
4. **Deploy**. Note the URL, e.g. `https://ticketflow.vercel.app`.

## 5. Connect them (CORS)

Back on Render → `ticketflow-api` → **Environment** → set
`APP_CORS_ALLOWED_ORIGINS` to your Vercel URL (comma-separate several, e.g. also a custom domain).
Save; Render redeploys.

## 6. Smoke test

Open the Vercel URL in a private window:

- [ ] Home loads instantly; if the API was asleep, the amber "waking up" banner appears, then disappears
- [ ] **Customer** demo login → reserve → pay → ticket with QR
- [ ] **Gate staff** demo login → paste that ticket code → "Welcome in"; again → "Already checked in"
- [ ] **Organizer** demo login → publish "Winter Lights Gala" → stats page shows charts

Then put the Vercel URL at the top of `README.md`.

---

## Notes

- **Demo reset**: with `APP_DEMO_RESET_ON_STARTUP=true` the API wipes and reseeds on every boot. On the
  free tier it sleeps after inactivity, so each cold start serves fresh data with event dates in the
  future. Set it to `false` if you want data to persist across restarts.
- **Demo accounts**: `customer@`, `organizer@`, `gate@`, `admin@demo.ticketflow`, password `demo1234`.
- **Cold starts**: you could ping `/actuator/health` on a schedule to keep the service awake, but check
  the host's free-tier terms first; the wake banner already handles the delay gracefully.
- **Troubleshooting**
  - Browser console shows CORS errors → `APP_CORS_ALLOWED_ORIGINS` doesn't exactly match the Vercel
    origin (scheme + host, no trailing slash).
  - Render logs show a Postgres connection error → `DB_URL` must start with `jdbc:postgresql://` and
    end with `?sslmode=require`; user and password go in their own variables.
  - Refreshing a page on Vercel gives 404 → `frontend/vercel.json` (SPA rewrite) must be present and the
    root directory must be `frontend`.
