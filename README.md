# NicheFlow

This program is a niche marketplace and booking platform for short-term rentals, services, and local resource sharing — built for AUCSC 325.

## Overview

NicheFlow is a marketplace for short-term rentals, booking services and booking places within the user's local area. Streamlining the process of renting and booking for both sides by having everything needed in order to book and/or rent happen on the same site.  Customer users can search for what they need in their local area and when they find a listing that matches what they are looking for they can book/rent directly on the site picking their day/s and/or times, complete the payment and listing provided instruction on how to pick up or access their rental/booking. While listing users can create new listings, block certain days or times when their listing is unavailable, upload photos to show off the listing, provide a description and even provide instructions that are only shown to booked customers. After the rental/booking both types of users can rate each other in order to allow for other users to gauge reputation and to give feedback to each other. The site also allows for administrators to review listings and reviews, and remove any that may violate any terms and conditions and/or laws. 

The project is organized into four modules:
- **AUTH** — Authentication & Accounts
- **LST** — Listings & Search
- **BK** — Booking & Payment
- **ADM** — Admin & Reviews

## Specification

Full feature breakdown, prioritized with MoSCoW and mapped to requirement IDs for test traceability. This section is the living specification and will be kept current as the project evolves.

| Status | Meaning |
|---|---|
| **Implemented** | Present in the current application and covered by automated tests |
| **Partial** | The core behavior exists, but part of the requirement or UI is still missing |
| **Planned** | Approved for a future version and not implemented in the current tree |
| **Out of scope** | Explicitly excluded from the project |

### Must Have (October MVP)

| ID | Feature | Status | Capability and testing focus |
|---|---|---|---|
| AUTH-01 | Registration and login | **Implemented** | Customer and supplier self-registration, secure password hashing, JWT login, duplicate-email and credential tests. Admin accounts are not self-registered. |
| AUTH-02 | Role-based access control | **Implemented** | Customer, supplier, and admin roles are enforced on protected endpoints; account and ownership tests cover authorization. |
| AUTH-03 | Password reset | **Planned** | The UI route exists, but it does not send reset email or change a password because no reset API or email provider is connected. |
| LST-01 | Supplier listing management | **Partial** | Suppliers create, edit, and deactivate listings and view account activity. Listing fields and ownership are implemented; availability slots, photos, and private access instructions are not yet implemented. |
| LST-02 | Browse and basic search | **Partial** | The backend provides keyword, category, and price search with pagination. The frontend provides additional client-side location filtering, category/price controls, and sorting. |
| BK-01 | Booking with availability check | **Implemented** | Customers book open time ranges and overlapping requests are blocked by application checks, a database constraint, and concurrency tests. |
| BK-02 | Booking state machine | **Partial** | Backend transitions cover pending → accepted → confirmed and cancellation; completed transition logic exists but there is no public completion endpoint yet. |
| BK-03 | Payment through a mocked Stripe layer | **Implemented** | Confirmation charges through an idempotent mock payment client. Live Stripe processing and webhook handling remain planned/excluded. |
| BK-04 | Cancellation and refund calculation | **Implemented** | Cancellation stores a timing-based refund amount; unit tests cover refund and boundary behavior. |
| BK-05 | Timezone-safe scheduling | **Implemented** | Booking timestamps use UTC and timezone and DST behavior is covered by backend tests. |
| ADM-01 | Basic admin console | **Implemented** | Admin APIs paginate users and bookings and allow booking cancellation through the booking authorization rules. |
| QA-01 | Test and CI baseline | **Implemented** | Backend and frontend builds and test suites run in GitHub Actions on pushes and pull requests. |

### Should Have (final version)

| ID | Feature | Status | Capability and testing focus |
|---|---|---|---|
| BK-06 | Dispute workflow | **Planned** | Adds the disputed state, with an admin resolution flow and a full audit trail. |
| BK-07 | Webhook handling and retries | **Planned** | Handles a mock or real Stripe charge whose webhook times out; retries remain idempotent. |
| BK-08 | Concurrent cancellation safety | **Partial** | Booking cancellation is persisted safely, but refund and concurrent-admin/supplier behavior needs further testing. |
| ADM-02 | Reviews and ratings | **Planned** | Reviews are allowed only after a completed booking, one per booking, with timing validation. |
| ADM-03 | Moderation tools | **Planned** | Admins can hide listings and reviews. |
| LST-03 | Advanced search | **Partial** | The frontend has sorting and basic filters; backend date-availability filtering and stale-index handling are not implemented. |
| BK-09 | Discount rules and promo codes | **Planned** | Unit-testable pricing logic, including stacking rules. |
| NOT-01 | Email notification queue | **Planned** | Booking confirmations and cancellations sent asynchronously, with retry and queue-failure tests. |
| QA-02 | Performance baseline and reporting | **Partial** | Automated test suites run in CI, but the BDD and Allure reporting baseline is not yet complete. |

### Could Have (desired, implementation uncertain)

| ID | Feature | Status | Why it is uncertain |
|---|---|---|---|
| NOT-02 | Real-time WebSocket notifications | **Planned** | Adds infrastructure complexity and is hard to test deterministically. |
| ADM-04 | Review fraud detection | **Planned** | Needs a defensible detection heuristic (e.g., burst patterns or account-age signals). |
| BK-10 | Multi-currency support with conversion | **Planned** | Requires exchange-rate sourcing and rounding rules that never lose money. |
| LST-04 | Eventually consistent search index | **Planned** | A separate search index means handling sync lag deliberately. |
| BK-11 | Supplier payouts and calendar sync | **Planned** | Real payout flows and iCal or Google Calendar integration go beyond a sandbox. |

**Out of scope:** native mobile apps and live payment processing (Stripe stays in test mode).

## Documentation

This README is the source of truth for project requirements. When behavior changes, update the matching requirement ID, status, and test evidence in the specification tables.

New requirements should receive a stable ID under the appropriate module, follow the existing MoSCoW priority, and include enough acceptance detail for a test to verify it.

### Current API surface

| Area | Implemented routes |
|---|---|
| Health | `GET /health` |
| Authentication | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| Account | `GET /api/account` |
| Listings | `GET /api/listings`, `GET /api/listings/{id}`, `POST /api/listings`, `PUT /api/listings/{id}`, `POST /api/listings/{id}/deactivate` |
| Bookings | `GET /api/bookings/availability/{listingId}`, `POST /api/bookings`, `PUT /api/bookings/{id}/accept`, `PUT /api/bookings/{id}/confirm`, `PUT /api/bookings/{id}/cancel` |
| Administration | `GET /api/admin/users`, `GET /api/admin/bookings` |

Protected routes require `Authorization: Bearer <token>`. Listing search and detail routes are public.

## Postgres

Before starting the backend and frontend servers, ensure the postgres database server is on.

Docker command to run postgres from a docker image:

```
docker run --name nicheflow-postgres -p 5432:5432 `
  -e POSTGRES_DB=nicheflow `
  -e POSTGRES_USER=nicheflow `
  -e POSTGRES_PASSWORD=nicheflow `
  postgres:latest
```

## Running the Backend

The springboot backend lives in `backend/`. From the repository root run:

```bash
cd backend
.\mvnw.cmd spring-boot:run
```

The backend reads database settings from `NICHEFLOW_DB_URL`, `NICHEFLOW_DB_USERNAME`, and `NICHEFLOW_DB_PASSWORD`. It also reads the JWT signing key from `NICHEFLOW_JWT_SECRET`; set it to a random value of at least 32 bytes outside local development.

## Running the Frontend

The React frontend lives in `frontend/`. From the repository root, run:

```bash
cd frontend
npm ci
npm run dev
```

This starts the frontend development server. Run `npm run build` from `frontend/` to create a production build. The frontend and Spring Boot backend run separately during development.

## Running Tests

```bash
# Backend
cd backend
.\mvnw.cmd test

# Frontend
cd frontend
npm test
npm run lint
npm run build
```


## Tech Stack

Frontend --> React (JavaScript, HTML, CSS) 
Backend --> Java, Spring Boot 
Database --> PostgreSQL, via Spring Data JPA / Hibernate 
Payments --> Stripe (test/mock mode) 
Real-time --> WebSockets (native to Spring Boot) 

## Testing & Quality Tooling

| Purpose | Backend (Java) | Frontend (JavaScript/React) |
|---|---|---|
| Unit testing | JUnit | Vitest |
| Coverage | JaCoCo | Vitest coverage |
| Static analysis | SpotBugs, Checkstyle | Oxlint |
| Mutation testing | PIT | StrykerJS |
| Component testing | — | React Testing Library |
| E2E testing | Playwright (shared) | Playwright (shared) |
| BDD | Cucumber | Cucumber |
| Test isolation | Testcontainers | — |
| CI/CD | GitHub Actions | GitHub Actions |
| Reporting | Allure Reports | Allure Reports |


## Team — Group 6

| Role | Member | Responsibilities |
|---|---|---|
| Backend Lead | Isaac Alawiye | API architecture, auth, bookings, payments, DB schema, unit/integration tests |
| Frontend Lead | Madison Allen | React UI, listings, booking flow, component tests, Playwright E2E |
| QA/Test Automation Lead | Oluwafunto Odunaike | Test strategy, BDD, unit/integration/E2E coverage, performance/security tests, Allure |
| DevOps Lead | Roddy McFarlane | CI/CD pipeline, PR workflow, regression suite, code quality tools |
