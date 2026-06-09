# Development Runbook

## Prerequisites

- Java 21
- Maven
- Node.js compatible with frontend lockfile
- PostgreSQL

## Backend Environment

Required values from `application.yml`:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
GOOGLE_CLIENT_ID
MAIL_ID
APP_PASS
```

Optional values:

```text
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=local
APP_PUBLIC_URL=https://www.billmygym.tech
GYMFLOW_PAYMENTS_ENABLED=false
RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
RAZORPAY_WEBHOOK_SECRET=
GYMFLOW_WHATSAPP_ENABLED=false
GYMFLOW_WHATSAPP_API_URL=https://graph.facebook.com
GYMFLOW_WHATSAPP_ACCESS_TOKEN=
GYMFLOW_WHATSAPP_PHONE_NUMBER_ID=
GYMFLOW_WHATSAPP_BUSINESS_ACCOUNT_ID=
GYMFLOW_WHATSAPP_VERSION=v23.0
GYMFLOW_WHATSAPP_WEBHOOK_VERIFY_TOKEN=
GYMFLOW_WHATSAPP_APP_SECRET=
```

## Frontend Environment

Copy `frontend/.env.example` if needed.

```text
VITE_API_ORIGIN=
VITE_API_TARGET=
GEMINI_API_KEY=
```

API target selection:

- Localhost browser host uses local backend.
- Deployed domains use production backend.
- `VITE_API_ORIGIN` overrides origin.
- `VITE_API_TARGET=local|prod` forces target.
- Vite proxies `/api` to the active backend during dev.

## Common Commands

Backend:

```powershell
cd backend
mvn test
mvn spring-boot:run
```

Frontend:

```powershell
cd frontend
npm install
npm run lint
npm run build
npm run dev
```

## Safe Change Checklist

For backend changes:

- Check role and tenant scope.
- Check legal consent and subscription guard interactions.
- Add Flyway migration for schema changes.
- Evict `ReportCacheService` on writes that affect reports.
- Use idempotency for duplicate-prone writes.
- Add or update service tests for lifecycle, billing, payment, and security-sensitive changes.

For frontend changes:

- Check `App.tsx` route roles.
- Use the shared API client.
- Preserve envelope unwrapping assumptions.
- Handle loading, empty, error, and redirect states.
- Run `npm run lint` and preferably `npm run build`.

## Known Worktree Note

At the time this knowledge base was created, `git status --short` showed deleted root files:

```text
D Final-Version.md
D PRD.md
```

Those deletions were pre-existing and were not reverted.

