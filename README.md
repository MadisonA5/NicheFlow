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

### Must Have (October MVP)

| ID | Feature | Capability and testing focus |
|---|---|---|
| AUTH-01 | Registration and login | Email and password sign-up as a customer or supplier, with secure password hashing and session or token management. Admin accounts are not self-registered. Tests cover invalid credentials, expired sessions, and duplicate accounts. |
| AUTH-02 | Role-based access control | Customer, supplier, and admin roles. The account page shows each customer’s bookings and each supplier’s listings and bookings. Tests cover privilege escalation attempts (e.g., a customer calling supplier or admin endpoints). |
| LST-01 | Supplier listing management | Suppliers create, edit, and deactivate listings (title, category, price, availability slots) and review their listings and booking requests from the account page. Tests cover input validation and ownership checks. |
| LST-02 | Browse and basic search | Keyword and category search with price filters. Tests cover empty results, special characters, and pagination boundaries. |
| BK-01 | Booking with availability check | Customers book an open slot. The system blocks double-booking, so if two customers book the last slot at once, exactly one succeeds. Tests include concurrent requests. |
| BK-02 | Booking state machine | Lifecycle of pending → accepted → confirmed → completed, plus cancelled. Suppliers accept requests; customers confirm and pay. Tests cover every valid transition and reject every invalid one. |
| BK-03 | Payment through a mocked Stripe layer | Charge when the customer confirms an accepted booking, with idempotency keys so retries never double-charge. The mock layer will be WireMock or an equivalent. |
| BK-04 | Cancellation and refund calculation | Refund amount depends on cancellation timing. Unit tests cover the calculation rules, including boundary cases. |
| BK-05 | Timezone-safe scheduling | Times are stored in UTC and displayed in the viewer's timezone. Tests cover DST changes and mismatched customer and supplier timezones. |
| ADM-01 | Basic admin console | Admins view users and bookings and can cancel a booking. |
| QA-01 | Test and CI baseline | Unit and integration tests run automatically on every pull request through GitHub Actions. |

### Should Have (final version)

| ID | Feature | Capability and testing focus |
|---|---|---|
| BK-06 | Dispute workflow | Adds the disputed state, with an admin resolution flow and a full audit trail. |
| BK-07 | Webhook handling and retries | Handles the case where a Stripe charge succeeds but the webhook times out. Retries are idempotent. |
| BK-08 | Concurrent cancellation safety | If an admin and a supplier cancel the same booking, the refund is issued exactly once. |
| ADM-02 | Reviews and ratings | Reviews are allowed only after a completed booking, one per booking, with timing validation. |
| ADM-03 | Moderation tools | Admins can hide listings and reviews. |
| LST-03 | Advanced search | Sorting, date-availability filtering, and search that copes with stale data. |
| BK-09 | Discount rules and promo codes | Unit-testable pricing logic, including stacking rules. |
| NOT-01 | Email notification queue | Booking confirmations and cancellations sent asynchronously, with tests for queue failure and retry. |
| QA-02 | Performance baseline and reporting | Benchmarks for concurrent booking and search response times, with Allure reports and a BDD (Cucumber) suite. |

### Could Have (desired, implementation uncertain)

| ID | Feature | Why it is uncertain |
|---|---|---|
| NOT-02 | Real-time WebSocket notifications | Adds infrastructure complexity and is hard to test deterministically. |
| ADM-04 | Review fraud detection | Needs a defensible detection heuristic (e.g., burst patterns or account-age signals). |
| BK-10 | Multi-currency support with conversion | Requires exchange-rate sourcing and rounding rules that never lose money. |
| LST-04 | Eventually consistent search index | A separate search index means handling sync lag deliberately. |
| BK-11 | Supplier payouts and calendar sync | Real payout flows and iCal or Google Calendar integration go beyond a sandbox. |

**Out of scope:** native mobile apps and live payment processing (Stripe stays in test mode).

## Documentation

This README is maintained as the living specification for the project and will be kept current through the term as requirements evolve.

## Running the Frontend

The React frontend lives in `frontend/`. From the repository root, run:

```bash
cd frontend
npm install
npm run dev
```

This starts the frontend development server. Run `npm run build` from `frontend/` to create a production build. The frontend and Spring Boot backend run separately during development.

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
