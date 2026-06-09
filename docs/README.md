# BillMyGym Repo Knowledge Base

BillMyGym is a multi-tenant gym management SaaS. The repository has two main applications:

- `backend`: Spring Boot 3.3 / Java 21 REST API for authentication, tenant data, memberships, payments, reminders, reporting, referrals, and subscription billing.
- `frontend`: Vite / React 19 / TypeScript single-page app for organization admins and super admins.

The app is role-based:

- `SUPER_ADMIN`: manages organizations, platform reports, audit logs, referral settings, subscription status, and bulk member import for organizations.
- `ORG_ADMIN`: manages one gym organization, its members, memberships, payments, reminders, reports, referrals, settings, profile, legal consent, and subscription recovery.

Core production assumptions:

- Backend API prefix is `/api`.
- Backend local port defaults to `8080`.
- Frontend local Vite server defaults to `5173`.
- Production frontend is `https://www.billmygym.tech`.
- Production API origin is `https://api.billmygym.tech`.
- App timezone is `Asia/Kolkata`.
- Data is PostgreSQL with Flyway migrations and Hibernate `ddl-auto: validate`.

Important docs in this folder:

- [architecture.md](architecture.md): system shape and request flow.
- [backend.md](backend.md): Spring Boot modules, services, persistence, security, jobs, and tests.
- [frontend.md](frontend.md): frontend routes, auth state, API client, and UI organization.
- [domain-model.md](domain-model.md): business entities and lifecycle rules.
- [api-surface.md](api-surface.md): controller endpoints and role access.
- [development-runbook.md](development-runbook.md): local setup, commands, env vars, and verification.
- [email-deliverability.md](email-deliverability.md): production sender identity, DNS records, and inbox validation.
- [agent-stored.md](agent-stored.md): operational memory for future agents.
