# Frontend

## Stack

- Vite 6
- React 19
- TypeScript
- React Router 7
- TanStack React Query
- Axios
- Tailwind CSS 4
- shadcn-style UI primitives
- lucide-react icons
- Recharts
- Sonner toasts
- Framer Motion / motion

## App Entry Points

- `frontend/src/main.tsx`: React mount.
- `frontend/src/App.tsx`: route tree.
- `frontend/src/index.css`: global styling.
- `frontend/app.config.ts`: API target selection.
- `frontend/vite.config.ts`: Vite, Tailwind, alias, and `/api` proxy.

## API Client

`frontend/src/lib/api.ts` creates the shared Axios instance.

Key behavior:

- Base URL is resolved from `app.config.ts`.
- Dev server calls use `/api`, which Vite proxies to the selected backend.
- Production calls go to `https://api.billmygym.tech/api` unless overridden.
- JWT is read from `billmygym_token`, with legacy fallback from `gymflow_token`.
- Backend response envelopes are unwrapped to `response.data`.
- Errors are normalized with `api.status`, `api.errorCode`, `api.message`, and field errors.
- 401 clears auth and redirects to `/login`.
- `LEGAL_CONSENT_REQUIRED` redirects to `/consent`.
- inactive subscription or 402 redirects to `/app/subscription/recovery`.
- `withIdempotencyKey()` creates `X-Idempotency-Key` headers for risky writes.

## Auth Context

`frontend/src/context/AuthContext.tsx` owns:

- token and user bootstrap from localStorage.
- `/auth/me` refresh on startup.
- legal policy metadata cache.
- consent-required derivation.
- login, logout, update user, and refresh user functions.

`ProtectedRoute` enforces:

- authenticated user presence.
- legal consent unless `allowPendingConsent` is true.
- allowed role membership.

## Routes

Public:

- `/login`
- `/signup`
- `/forgot-password`
- `/reset-password`
- `/terms`
- `/privacy`
- `/unauthorized`

Legal consent:

- `/consent`, available to authenticated org admins and super admins even while consent is pending.

Org admin:

- `/app/dashboard`
- `/app/members`
- `/app/members/new`
- `/app/members/:id/edit`
- `/app/members/:id/renew`
- `/app/reminders`
- `/app/reports`
- `/app/referrals`
- `/app/subscription`
- `/app/subscription/plans`
- `/app/subscription/recovery`
- `/app/subscription/recovery/plans`
- `/app/settings`
- `/app/organization`

Super admin:

- `/app/platform-overview`
- `/app/platform-reports`
- `/app/organizations`
- `/app/organizations/new`
- `/app/organizations/:id`
- `/app/organizations/:id/audit-logs`
- `/app/audit-logs`
- `/app/referrals/analytics`

## UI Organization

- App shell is `AppLayout`, with `Sidebar`, `Topbar`, and scroll utilities.
- Org pages live under `src/pages/org`.
- Admin pages live under `src/pages/admin`.
- Legal documents use `components/legal/LegalDocumentLayout`.
- UI primitives are duplicated in both `src/components/ui` and top-level `components/ui`; check imports before editing to avoid changing the wrong copy.

## Frontend Verification

From `frontend`:

```powershell
npm run lint
npm run build
npm run dev
```

`npm run lint` runs `tsc --noEmit`.

