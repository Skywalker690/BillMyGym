# Architecture

## High-Level Shape

BillMyGym is a split frontend/backend SaaS:

- React SPA handles routing, auth context, layouts, forms, dashboards, and admin views.
- Spring Boot API owns all business rules, security enforcement, persistence, integrations, scheduled jobs, and response envelopes.
- PostgreSQL is the source of truth. Flyway migrations define and evolve schema.

The frontend never talks directly to third-party billing or WhatsApp APIs. It calls the backend, which then handles Razorpay, WhatsApp Cloud API, email, audit logging, and domain state transitions.

## Runtime Flow

1. User authenticates with `/api/auth/login` or `/api/auth/google-login`.
2. Backend issues a JWT with role and organization context.
3. Frontend stores token under `billmygym_token` and user under `billmygym_user`.
4. Axios adds `Authorization: Bearer <token>` on API calls.
5. Backend filters validate JWT, legal policy consent, subscription access, rate limits, and API failures.
6. Controllers delegate to services. Services enforce tenant scope and business rules.
7. Responses are usually wrapped as `{ success, message, data, timestamp }`.
8. Frontend API client unwraps envelope data and handles auth/legal/subscription redirects centrally.

## Backend Layers

- `api`: REST controllers, exception handler, DTOs, mappers, response envelope.
- `config`: security, async, cache, rate limiting, WhatsApp properties.
- `domain`: JPA entities and enums.
- `repository`: Spring Data JPA repositories.
- `security`: JWT, authentication principal, filters, access-denied/entry-point writers.
- `service`: business logic, billing lifecycle, reports, notifications, integrations, scheduled jobs.
- `shared`: small cross-cutting utilities for money, idempotency, and events.

## Frontend Layers

- `src/App.tsx`: all routes.
- `src/context`: auth and demo plan preview state.
- `src/lib`: API client, config helpers, local caches, labels.
- `src/components/layout`: protected routes, sidebar, topbar, app shell.
- `src/pages/org`: organization admin workflows.
- `src/pages/admin`: super admin workflows.
- `src/components/ui` and top-level `components/ui`: shadcn-style primitives.

## Cross-Cutting Concerns

- Tenant isolation: org-admin operations use `SecurityUtils.currentUser().organizationId()`.
- Soft deletion: most core entities have `deleted` and sometimes `deletedAt`.
- Audit logging: auth, organization, member, notification, subscription, and admin activity are logged.
- Idempotency: payment and membership operations use idempotency keys where duplicate requests are risky.
- Caching: report and frontend subscription/member pricing caches exist and must be invalidated on relevant writes.
- Subscription gating: legal consent and organization subscription state affect access to protected routes.

