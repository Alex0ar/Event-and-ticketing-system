# TicketFlow

A REST API for event ticketing: organizers publish events with seat tiers, customers reserve and pay
for tickets, and gate staff scan them at the door.

Built with Spring Boot as a learning project, with a deliberate focus on one hard problem:
**never overselling a ticket tier, no matter how many people click "buy" at the same moment.**

## Tech stack

Java 21 · Spring Boot 3.5 · PostgreSQL 16 · Spring Data JPA · Flyway · Spring Security (JWT) ·
Testcontainers · Docker · Maven

## Features

- **Auth** — registration, login, JWT access tokens, refresh tokens with rotation and revocation, role-based access (`CUSTOMER`, `ORGANIZER`, `GATE_STAFF`, `ADMIN`)
- **Catalog** — venues, events, ticket tiers; draft → publish workflow with validation
- **Search** — public event listing with dynamic filters (city, category, date range, price, free text), pagination, sorting and caching
- **Reservations** — 10-minute inventory holds that expire automatically, safe under concurrent load
- **Checkout** — orders, mock payment gateway, idempotent payment, ticket issuance with QR codes
- **Check-in** — one-time ticket scanning at the gate
- **Refunds** — order cancellation and full event cancellation, with an audit trail
- **Reporting** — per-event sales stats and attendee CSV export

## Getting started

**Prerequisites:** Java 21, Docker.

```bash
git clone https://github.com/Alex0ar/Event-and-ticketing-system.git
cd Event-and-ticketing-system
cp .env.example .env          # adjust if you like; defaults work for local dev
docker compose up -d          # starts PostgreSQL
./mvnw spring-boot:run        # starts the API on :8080
```

Flyway creates the schema automatically on first start.

- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

## Running tests

```bash
./mvnw test
```

Tests run against a real PostgreSQL started by Testcontainers, so Docker must be running.

> **macOS note:** if Testcontainers can't find Docker, point it at Docker Desktop's engine socket by
> creating `~/.testcontainers.properties`:
> ```
> docker.host=unix:///Users/<you>/Library/Containers/com.docker.docker/Data/docker.raw.sock
> ```

## API overview

| Area | Base path |
|---|---|
| Auth | `/api/v1/auth` |
| Users | `/api/v1/users` |
| Public catalog | `/api/v1/events` |
| Organizer events | `/api/v1/organizer/events` |
| Venues | `/api/v1/venues` |
| Reservations | `/api/v1/reservations` |
| Orders & payment | `/api/v1/orders` |
| Tickets | `/api/v1/tickets` |
| Check-in | `/api/v1/checkIn` |

Full request/response details are in Swagger UI.

## How overselling is prevented

The core problem: two customers reserving the last seat at the same instant.

A naive `read → check availability → update` sequence has a race — both requests can read the same
state before either writes, and both succeed. The fix used here is an **atomic conditional update**:

```sql
UPDATE ticket_tiers
   SET reserved_quantity = reserved_quantity + :qty
 WHERE id = :id
   AND (total_quantity - reserved_quantity - sold_quantity) >= :qty
```

The check and the increment happen in a single indivisible statement, so there is no gap to race
through. The number of rows updated tells you the outcome: `1` means the hold succeeded, `0` means
there wasn't enough left.

This was chosen over the alternatives because it is correct by construction — there is no
read-then-write window at all:

- **Pessimistic locking** (`SELECT ... FOR UPDATE`) is also correct, but serializes every request for
  a tier and holds locks for the duration of the transaction.
- **Optimistic locking** (`@Version`) alone requires retry logic and wastes work under high contention.

Two further layers back it up:

1. `@Version` on the tier, protecting any entity-based update path.
2. A database `CHECK (reserved_quantity + sold_quantity <= total_quantity)` constraint, which makes an
   oversold row physically impossible to store.

Reservations that are never paid for are released by a scheduled job, returning the held quantity to
the pool.

## Design notes

- **Layering** — `Controller → Service → Repository`. Controllers handle HTTP only; entities are never
  returned from controllers, always mapped to DTOs.
- **Flyway owns the schema.** Hibernate runs with `ddl-auto: validate` and never modifies tables.
- **Money** is `BigDecimal` / SQL `NUMERIC`, never floating point.
- **Caching** — the public event listing is cached (60s TTL, evicted on publish). Availability shown
  there may lag slightly; that is acceptable for browsing. The reservation path always reads fresh
  from the database and never uses the cache.
- **Idempotent payments** — the pay endpoint accepts an `Idempotency-Key` so a retried request never
  charges twice or issues duplicate tickets.

## Configuration

All configuration is externalized; nothing sensitive is committed. See `.env.example` for the
variables in use. Profiles: `dev` (default, local), `test` (Testcontainers), `prod` (all values
required from the environment).

## Project status

Learning project, built in phases: foundation → auth → catalog → reservations → checkout →
check-in/refunds/reporting → hardening.
