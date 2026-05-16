# GymFlow Frontend Implementation Guide

This document is the frontend handoff for the current GymFlow repository.

The goal is simple:

- a frontend developer should be able to build the full product UI without reading backend code
- the UI should be implemented in React + TypeScript + Tailwind CSS
- the product should feel like a premium SaaS, not a generic admin panel

This guide documents:

- product structure
- backend API contract
- authentication and roles
- request and response shapes
- recommended frontend architecture
- screen-by-screen UX
- state handling
- validation and error handling
- visual design direction
- animations and motion behavior

---

## 1. Product Summary

GymFlow is a multi-tenant gym operations SaaS.

Hierarchy:

1. `SUPER_ADMIN`
2. `Organization`
3. `ORG_ADMIN`
4. `Members`

Current backend focus:

- authentication
- organization management
- member management
- membership lifecycle automation
- reminder execution
- notification logs
- organization settings
- analytics

Reminder delivery model:

- members are tracked entities only
- reminder notifications are sent only to organization admins
- reminders are grouped into organization-level renewal digests

Not in current frontend scope as working backend modules:

- payment system UX
- Razorpay flows
- advanced revenue analytics
- non-core SaaS modules

Build the frontend around what actually exists in backend today, while keeping the design system extensible for future modules.

---

## 2. Tech Expectations

Use:

- `React`
- `TypeScript`
- `Tailwind CSS`

Recommended supporting stack:

- `React Router`
- `TanStack Query`
- `Axios` or `fetch` wrapper
- `react-hook-form`
- `zod`
- `framer-motion`
- `clsx`
- `tailwind-merge`

Optional but useful:

- `date-fns`
- `recharts`
- `sonner` or `react-hot-toast`
- `lucide-react`

---

## 3. Backend Base Facts

Backend app:

- Spring Boot
- Base API prefix: `/api`
- Auth: JWT bearer token
- All successful responses are wrapped
- All errors are JSON
- Most routes require authentication

Important note:

- `POST /api/auth/register` is **not** public signup
- it is intended for `SUPER_ADMIN` only
- frontend must hide this route for normal org admin users

---

## 4. Environment / Frontend Assumptions

Frontend should support:

- local dev with backend on `http://localhost:8080`
- Vite dev server on `http://localhost:5173`

Recommended env variables:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_NAME=GymFlow
```

Create one API client with:

- base URL from env
- automatic bearer token injection
- global 401 handling
- global 403 handling
- normalized error mapping

---

## 5. Roles and Access Model

### `SUPER_ADMIN`

Can:

- log in
- access platform analytics
- register organizations/admins
- list all organizations
- update organizations
- delete organizations

### `ORG_ADMIN`

Can:

- log in
- access only their own organization context
- manage members
- view organization analytics
- manage settings
- run reminders
- view reminder logs
- view notification logs
- view organization details returned by backend
- update only their own organization

### Frontend role rules

Do not rely only on hidden navigation. Also gate screens and actions in route-level checks.

Rules:

- `SUPER_ADMIN` sees platform-level navigation
- `ORG_ADMIN` never sees platform analytics or organization registration UI
- `ORG_ADMIN` should never see multi-organization management as an editable global list

---

## 6. Authentication Model

### Login methods

Available:

- email/password login
- Google login

Endpoints:

- `POST /api/auth/login`
- `POST /api/auth/google-login`

### JWT behavior

On successful login backend returns:

- `accessToken`
- `role`
- `organizationId`
- `user`

Store token in memory plus persistent storage fallback.

Recommended:

- primary: in-memory auth store
- persistence: `localStorage`

On app boot:

1. read stored token
2. decode lightweight client-side if useful for bootstrapping
3. set auth state
4. redirect to role-appropriate dashboard

There is no refresh token API currently. Treat login as a single-session access token workflow.

If any protected API returns `401`:

1. clear token
2. clear query cache
3. redirect to login
4. show a session-expired toast

---

## 7. Global Response Shapes

### Success envelope

Every successful API returns:

```json
{
  "success": true,
  "message": "Human readable success message",
  "data": {},
  "timestamp": "2026-05-16T12:00:00+05:30"
}
```

Sometimes `data` is `null`.

### Error envelope

Backend errors are returned as:

```json
{
  "success": false,
  "message": "Validation failed",
  "errorCode": "VALIDATION_ERROR",
  "errors": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    }
  ],
  "timestamp": "2026-05-16T12:00:00+05:30"
}
```

Possible common error codes:

- `BAD_REQUEST`
- `FORBIDDEN`
- `NOT_FOUND`
- `UNAUTHORIZED`
- `VALIDATION_ERROR`
- `MALFORMED_REQUEST`
- `RATE_LIMIT_EXCEEDED`
- `INTERNAL_SERVER_ERROR`

### Frontend handling rules

- show `message` in toast/banner
- for validation errors, map `errors[].field` into form field messages
- for `403`, show permission-state UI instead of generic crash screens
- for `429`, show retry messaging using `Retry-After` header when available

---

## 8. Rate Limiting

Current backend rate limits:

- auth routes: `12 requests / 60 seconds`
- notification routes: `60 requests / 60 seconds`
- general API routes: `180 requests / 60 seconds`

Frontend implications:

- disable repeated submit clicks
- use loading states
- debounce search/filter UIs if client later calls backend frequently
- for reminder run button, use optimistic disable and cooldown display

When rate limited:

- show toast: `Too many requests. Please try again later.`
- if `Retry-After` exists, show countdown helper text

---

## 9. Domain Enums

These values should be treated as API source of truth.

### Roles

```ts
type Role = "SUPER_ADMIN" | "ORG_ADMIN";
```

### Organization status

```ts
type OrganizationStatus = "ACTIVE" | "INACTIVE" | "SUSPENDED";
```

### Membership duration

```ts
type MembershipDuration =
  | "ONE_MONTH"
  | "THREE_MONTHS"
  | "SIX_MONTHS"
  | "TWELVE_MONTHS"
  | "CUSTOM";
```

### Membership status

```ts
type MembershipStatus =
  | "ACTIVE"
  | "EXPIRING_SOON"
  | "EXPIRED"
  | "OVERDUE"
  | "RENEWED";
```

### Notification channel

```ts
type NotificationChannel = "WHATSAPP" | "EMAIL";
```

### Notification status

```ts
type NotificationStatus = "PENDING" | "SENT" | "FAILED" | "RETRY";
```

### Notification type

```ts
type NotificationType =
  | "ORGANIZATION_RENEWAL_DIGEST";
```

---

## 10. Core TypeScript API Models

Use these exact frontend contracts.

```ts
export type ApiResponse<T> = {
  success: true;
  message: string;
  data: T;
  timestamp: string;
};

export type ApiError = {
  success: false;
  message: string;
  errorCode: string;
  errors: { field: string; message: string }[];
  timestamp: string;
};
```

### Auth

```ts
export type LoginRequest = {
  email: string;
  password: string;
};

export type GoogleLoginRequest = {
  idToken: string;
};

export type RegisterOrganizationAdminRequest = {
  organizationName: string;
  district: string;
  logoUrl?: string;
  mapLocationLink?: string;
  address?: string;
  adminFullName: string;
  adminPhoneNumber: string;
  adminEmail: string;
  organizationDescription?: string;
  password: string;
};

export type UserDto = {
  id: number;
  name?: string | null;
  email?: string | null;
  role?: string | null;
  googleId?: string | null;
  avatarUrl?: string | null;
  clientCrmId?: number | null;
  createdAt?: string | null;
  termsAccepted?: boolean | null;
  policyVersion?: string | null;
};

export type LoginResponse = {
  accessToken: string;
  role: Role;
  organizationId: number | null;
  user: UserDto;
};
```

### Organization

```ts
export type OrganizationResponse = {
  id: number;
  name: string;
  district: string;
  logoUrl?: string | null;
  mapLocationLink?: string | null;
  address?: string | null;
  adminFullName: string;
  adminPhoneNumber: string;
  adminEmail: string;
  description?: string | null;
  status: OrganizationStatus;
};

export type UpdateOrganizationRequest = {
  name: string;
  district: string;
  logoUrl?: string;
  mapLocationLink?: string;
  address?: string;
  adminFullName: string;
  adminPhoneNumber: string;
  adminEmail: string;
  description?: string;
  status: OrganizationStatus;
};
```

### Member

```ts
export type MemberRequest = {
  fullName: string;
  joiningDate: string;
  membershipDuration: MembershipDuration;
  customDurationDays?: number | null;
  renewed?: boolean | null;
  notes?: string;
};

export type MemberResponse = {
  id: number;
  organizationId: number;
  fullName: string;
  joiningDate: string;
  membershipDuration: MembershipDuration;
  customDurationDays?: number | null;
  membershipStartDate: string;
  membershipEndDate: string;
  membershipStatus: MembershipStatus;
  renewed: boolean;
  lastReminderSent?: string | null;
  reminderCount: number;
  notes?: string | null;
};
```

### Settings

```ts
export type SettingsResponse = {
  whatsappEnabled: boolean;
  emailEnabled: boolean;
  reminderDaysBeforeExpiry: number;
  brandingName?: string | null;
};

export type UpdateSettingsRequest = {
  whatsappEnabled?: boolean;
  emailEnabled?: boolean;
  reminderDaysBeforeExpiry?: number;
  brandingName?: string;
};
```

### Notifications

```ts
export type NotificationResponse = {
  id: number;
  memberId?: number | null;
  memberCount: number;
  type: NotificationType;
  channel: NotificationChannel;
  recipient: string;
  status: NotificationStatus;
  attemptCount: number;
  failureReason?: string | null;
  sentAt?: string | null;
  message: string;
  createdAt: string;
};
```

### Reminders

```ts
export type ReminderRunResponse = {
  processedMembers: number;
  organizationsGrouped: number;
  digestsSent: number;
};
```

### Analytics

```ts
export type PlatformAnalyticsResponse = {
  totalOrganizations: number;
  activeSubscriptions: number;
};

export type OrganizationAnalyticsResponse = {
  activeMembers: number;
  remindersSent: number;
};
```

---

## 11. API Endpoints

## Auth

### `POST /api/auth/login`

Public route.

Request:

```json
{
  "email": "admin1@gymflow.local",
  "password": "secret123"
}
```

Success `200`:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "jwt-token",
    "role": "ORG_ADMIN",
    "organizationId": 1,
    "user": {
      "id": 3,
      "email": "admin1@gymflow.local",
      "role": "ORG_ADMIN",
      "googleId": null,
      "avatarUrl": null,
      "createdAt": "2026-01-03T10:00:00"
    }
  },
  "timestamp": "2026-05-16T12:00:00+05:30"
}
```

UI:

- email field
- password field
- primary submit button
- optional Google sign-in button
- animated gradient/auth card

### `POST /api/auth/google-login`

Public route.

Request:

```json
{
  "idToken": "google-id-token"
}
```

Use when Google auth is configured. If not configured, backend can return `BAD_REQUEST`.

### `POST /api/auth/register`

Protected route.

Access:

- `SUPER_ADMIN` only

Purpose:

- create new organization
- create its org admin

Request:

```json
{
  "organizationName": "Gym Nova",
  "district": "Central",
  "logoUrl": "https://...",
  "mapLocationLink": "https://maps.google.com/...",
  "address": "101 Main Street",
  "adminFullName": "Aarav Mehta",
  "adminPhoneNumber": "+919999999999",
  "adminEmail": "admin@gymnova.com",
  "organizationDescription": "Premium strength and conditioning center",
  "password": "password123"
}
```

Success:

- `201`
- `data` is `null`

UI:

- multi-section organization onboarding form
- sticky summary sidebar on desktop
- animated success state after creation

---

## Organizations

All organization routes are protected.

### `GET /api/organizations`

Behavior:

- `SUPER_ADMIN`: returns all organizations
- `ORG_ADMIN`: returns only the current organization as a single-item array

Response:

```json
[
  {
    "id": 1,
    "name": "Gym Alpha",
    "district": "Central",
    "logoUrl": "https://img.example.com/alpha.png",
    "mapLocationLink": "https://maps.example.com/alpha",
    "address": "101 Alpha Street",
    "adminFullName": "Aarav Mehta",
    "adminPhoneNumber": "+919100000001",
    "adminEmail": "alpha.admin@gymflow.local",
    "description": "Premium strength and conditioning center",
    "status": "ACTIVE"
  }
]
```

Frontend implication:

- use same API for both roles
- derive single-organization profile for `ORG_ADMIN` from first item

### `PATCH /api/organizations/{id}`

Access:

- `SUPER_ADMIN` can update any organization
- `ORG_ADMIN` can update only their own organization

Request matches `UpdateOrganizationRequest`.

UI:

- organization profile form
- logo preview
- status select for `SUPER_ADMIN`
- status read-only or controlled carefully for `ORG_ADMIN` depending on product decision

### `DELETE /api/organizations/{id}`

Access:

- `SUPER_ADMIN` only

Behavior:

- soft delete
- backend marks deleted
- organization status becomes `INACTIVE`

UI:

- destructive confirmation modal
- require typed confirmation for safety
- after delete, refetch list and show toast

---

## Members

All member routes are org-scoped.

### `GET /api/members`

Returns all non-deleted members for current org.

Important:

- backend recalculates membership status on fetch
- frontend should treat API response as truth
- do not calculate membership state independently for business logic

### `POST /api/members`

Request:

```json
{
  "fullName": "John Doe",
  "joiningDate": "2026-05-16",
  "membershipDuration": "THREE_MONTHS",
  "customDurationDays": null,
  "notes": "Prefers morning batch"
}
```

Backend behavior:

- `membershipStartDate` is derived from `joiningDate`
- `membershipEndDate` is auto-calculated
- `membershipStatus` is auto-derived
- reminder digests are sent only to organization admins

Frontend rules:

- do not ask user for membership end date
- do not ask user for membership status
- if duration = `CUSTOM`, show `customDurationDays` input
- otherwise hide `customDurationDays`

### `PATCH /api/members/{id}`

Same payload as create.

Frontend edit form should be the same component as create form.

### `DELETE /api/members/{id}`

Behavior:

- soft delete only

UI:

- no hard delete language in copy
- text should say `Archive member` or `Remove from active records`
- but if product wants explicitness, `Delete member` is acceptable with explanatory note

### Member table requirements

Columns:

- member name
- joining date
- duration
- membership start
- membership end
- status
- notes preview
- actions

Filters:

- search by name on client side initially
- filter by status
- sort by expiry date
- quick segments:
  - all
  - active
  - expiring soon
  - expired
  - overdue

Status badge styling:

- `ACTIVE`: emerald
- `EXPIRING_SOON`: amber
- `EXPIRED`: slate/red neutral danger
- `OVERDUE`: stronger red

---

## Settings

### `GET /api/settings`

Returns org notification settings.

Response:

```json
{
  "whatsappEnabled": true,
  "emailEnabled": true,
  "reminderDaysBeforeExpiry": 1,
  "brandingName": "Gym Alpha Pro"
}
```

### `PATCH /api/settings`

Partial update.

Request example:

```json
{
  "whatsappEnabled": true,
  "emailEnabled": false,
  "reminderDaysBeforeExpiry": 2,
  "brandingName": "Gym Alpha Elite"
}
```

UI:

- notification channel toggles
- reminder day slider or segmented control
- branding name input
- instant preview card for outgoing messages

Recommended form sections:

1. Reminder timing
2. Delivery channels
3. Brand identity

---

## Reminders

### `POST /api/reminders/run`

Manual trigger for current organization.

Response:

```json
{
  "processedMembers": 24,
  "organizationsGrouped": 3,
  "digestsSent": 5
}
```

UI use:

- show in reminder control panel
- include a primary CTA: `Run reminders now`
- disable while pending
- after success show a mini results panel with counts

Important:

- backend also has a scheduled job
- this manual route is an admin utility trigger

### `GET /api/reminders/logs`

Returns notification logs filtered to organization renewal digest notifications.

Use as a reminder activity feed.

Columns:

- created at
- member count
- type
- channel
- recipient
- status
- attempts
- sent at
- failure reason

UX:

- tabs: `All`, `Sent`, `Pending`, `Failed`
- filter chips for `Email` and `WhatsApp`
- expandable row to inspect full message

---

## Notifications

### `GET /api/notifications`

Returns all notification logs for current organization. These are organization-admin digest notifications, not member-facing notifications.

Use this screen as the canonical delivery audit table.

Recommended features:

- status filtering
- type filtering
- channel filtering
- date sorting
- row detail drawer

Status UX:

- `PENDING`: muted amber with spinner dot
- `SENT`: success green
- `FAILED`: red with failure tooltip
- `RETRY`: blue/amber retry state

Failure UX:

- if `failureReason` exists, surface it in tooltip or inline detail

---

## Analytics

### `GET /api/analytics/platform`

Access:

- `SUPER_ADMIN` only

Response:

```json
{
  "totalOrganizations": 10,
  "activeSubscriptions": 7
}
```

### `GET /api/analytics/organization`

Access:

- authenticated users with org context

Response:

```json
{
  "activeMembers": 95,
  "remindersSent": 8
}
```

Current analytics are intentionally minimal.

Frontend should still design cards in a way that can scale later to:

- trends
- charts
- segmented metrics
- comparisons

---

## 12. Suggested App Information Architecture

### Route map

Public:

- `/login`

Shared authenticated shell:

- `/app`

`SUPER_ADMIN` routes:

- `/app/platform-overview`
- `/app/organizations`
- `/app/organizations/new`
- `/app/organizations/:id`

`ORG_ADMIN` routes:

- `/app/dashboard`
- `/app/members`
- `/app/members/new`
- `/app/members/:id/edit`
- `/app/reminders`
- `/app/notifications`
- `/app/settings`
- `/app/organization`

Optional shared profile route:

- `/app/account`

### Navigation model

For `SUPER_ADMIN`:

- Overview
- Organizations
- Register Organization

For `ORG_ADMIN`:

- Dashboard
- Members
- Reminders
- Notifications
- Settings
- Organization

---

## 13. Recommended React Project Structure

```text
src/
  app/
    providers/
    router/
    store/
  api/
    client.ts
    auth.ts
    organizations.ts
    members.ts
    settings.ts
    reminders.ts
    notifications.ts
    analytics.ts
    types.ts
  features/
    auth/
    dashboard/
    organizations/
    members/
    reminders/
    notifications/
    settings/
  components/
    ui/
    layout/
    charts/
    feedback/
    data-display/
  hooks/
  lib/
  styles/
```

Use feature-based organization, not one giant components folder.

---

## 14. Data Fetching Strategy

Use `TanStack Query`.

Suggested query keys:

```ts
["auth", "me"]
["organizations"]
["organization", organizationId]
["members"]
["member", memberId]
["settings"]
["notifications"]
["reminder-logs"]
["analytics", "platform"]
["analytics", "organization"]
```

Suggested mutations:

- `login`
- `googleLogin`
- `registerOrganizationAdmin`
- `updateOrganization`
- `createMember`
- `updateMember`
- `deleteMember`
- `updateSettings`
- `runReminders`
- `deleteOrganization`

On successful mutations, invalidate only relevant queries.

Examples:

- create/update/delete member -> invalidate `["members"]` and `["analytics", "organization"]`
- update settings -> invalidate `["settings"]`
- run reminders -> invalidate `["notifications"]`, `["reminder-logs"]`, `["analytics", "organization"]`

---

## 15. Form UX Rules

Use:

- `react-hook-form`
- `zod`

### Login form

Validation:

- email required and valid
- password required

### Register organization form

Validation:

- all backend required fields
- password minimum 8 chars
- clean phone formatting before submit if desired

### Member form

Validation:

- full name required
- joining date required
- membership duration required
- `customDurationDays` required and positive only when duration is `CUSTOM`

### Settings form

Validation:

- reminder days between `0` and `30`

### Form interaction patterns

- inline field errors
- floating save bar on long forms
- dirty state protection
- unsaved-changes confirmation on navigation
- success toast after mutation

---

## 16. Screen-by-Screen UX

## Login Screen

Goals:

- premium first impression
- dark-on-warm-neutral or light editorial feel
- not default corporate blue

Content:

- product headline
- short value proposition
- login form card
- optional Google button
- subtle animated background

Motion:

- form card fade-up on mount
- staggered text reveal
- soft animated grain/mesh in background

---

## SUPER_ADMIN Overview

Purpose:

- fast platform summary

Sections:

- headline KPI cards
- organization count
- active subscriptions
- CTA strip for registering new organization
- organization status summary list

Even though backend metrics are small now, UI should feel executive and scalable.

---

## Organizations List

Use a premium data table plus side drawer pattern.

Features:

- card/table hybrid layout
- filters by status and district
- search by organization name or admin email
- row click opens detail drawer
- action menu for edit/delete

Desktop:

- main table
- animated right-side details panel

Mobile:

- stacked organization cards

---

## Register Organization Screen

Feel like onboarding, not raw CRUD.

Sections:

1. Organization identity
2. Contact details
3. Admin account
4. Review

Add:

- live branding preview
- progress indicator
- completion success state with confetti-lite motion, not cheesy full-screen explosion

---

## ORG_ADMIN Dashboard

This is the primary operating screen.

Top cards:

- active members
- reminders sent
- expiring soon count
- overdue count

Since backend currently returns only `activeMembers` and `remindersSent`, calculate `expiring soon` and `overdue` locally from `GET /api/members`.

Sections:

- membership health overview
- upcoming expiries
- recent reminder activity
- quick actions

Quick actions:

- Add member
- Run reminders now
- Open settings
- View notifications

---

## Members Screen

This should be a strong operational workspace.

Layout:

- top summary strip
- search + filters row
- data table
- create member CTA

Enhancements:

- sticky toolbar
- animated status chips
- row hover reveal for actions
- slide-over edit/create form

Recommended slide-over sections:

1. Basic information
2. Membership setup
3. Notes
4. Summary preview

Preview block should show:

- joining date
- auto-calculated duration label
- expected expiry date preview on client side

Important:

- preview is just UX convenience
- backend result remains the source of truth

---

## Reminders Screen

This should feel like a mission control panel.

Sections:

- reminder execution hero panel
- last run result card
- reminder settings snapshot
- reminder logs table

Main CTA:

- `Run reminders now`

After run:

- animate number counters for processed, triggered, sent
- refresh logs automatically

---

## Notifications Screen

This should feel like a delivery audit console.

Sections:

- filters
- status summary pills
- logs table
- detail drawer

Detail drawer content:

- type
- channel
- recipient
- status
- attempt count
- sent at
- created at
- failure reason
- full message body

---

## Settings Screen

This should feel like control center settings, not plain toggles.

Sections:

1. Brand identity
2. Reminder timing
3. Delivery channels
4. Notification preview

Notification preview cards:

- WhatsApp digest preview
- email digest preview

Live-updating branding preview is strongly recommended.

---

## Organization Profile Screen

For ORG_ADMIN, this is their gym profile.

Sections:

- profile hero
- gym details
- admin contact
- operational status

If map link exists:

- show external link card

If logo exists:

- show image/avatar in hero banner

---

## 17. Suggested Derived Client Utilities

Create utilities for:

- enum label formatting
- phone display formatting
- date formatting
- relative expiry messaging
- status color mapping
- analytics card formatting

Examples:

- `ONE_MONTH` -> `1 Month`
- `EXPIRING_SOON` -> `Expiring Soon`
- `ORGANIZATION_RENEWAL_DIGEST` -> `Renewal Digest`

---

## 18. Membership UX Logic

Even though backend calculates lifecycle state, frontend should still present intuitive explanations.

### Duration selector labels

- `ONE_MONTH` -> `1 Month`
- `THREE_MONTHS` -> `3 Months`
- `SIX_MONTHS` -> `6 Months`
- `TWELVE_MONTHS` -> `12 Months`
- `CUSTOM` -> `Custom`

### Status explanations

- `ACTIVE`: Membership is healthy and outside reminder window
- `EXPIRING_SOON`: Membership is inside reminder window
- `EXPIRED`: Membership expires today
- `OVERDUE`: Membership expired before today and still needs renewal
- `RENEWED`: Membership has been renewed and is excluded from digest reminders

### Helpful UI copy

- `Expires on 24 Jun 2026`
- `Reminder window starts tomorrow`
- `Expired 3 days ago`
- `Overdue renewal`

---

## 19. Search, Filtering, and Table UX

Since current backend does not expose server-side search/filtering endpoints, implement client-side filtering for now.

Rules:

- fetch full collection per screen
- filter in memory
- debounce input for smoothness
- preserve filters in URL query params

Recommended URL state examples:

- `/app/members?status=EXPIRING_SOON`
- `/app/notifications?channel=EMAIL&status=FAILED`

---

## 20. Empty States

Do not use generic empty states.

Use product-aware empty states.

### No members

Message:

- `No members added yet`
- `Start by creating your first member record and GymFlow will track renewal timing automatically.`

CTA:

- `Add first member`

### No notifications

Message:

- `No notifications yet`
- `Renewal digest deliveries will appear here once activity starts.`

### No reminder logs

Message:

- `No reminder activity yet`
- `Run reminders manually or wait for the daily scheduler.`

---

## 21. Loading States

Avoid full-page blank spinners.

Use:

- skeleton cards
- skeleton table rows
- blurred placeholder panels
- shimmer animations

Patterns:

- dashboard cards skeleton
- members table skeleton
- side panel loading skeleton
- button inline spinner on submit

---

## 22. Error States

Patterns:

- inline form error
- page-level fetch retry block
- toast for mutation errors
- permission screen for `403`
- auth-expired redirect for `401`

Recommended copy:

- `We couldn't load this section right now.`
- `Try again`
- `Your session has expired. Please sign in again.`

---

## 23. Accessibility Requirements

Required:

- keyboard-navigable sidebar
- visible focus rings
- semantic buttons and forms
- high contrast for status badges
- reduced motion support
- modals with focus trap
- screen-reader labels for icon buttons

Animations must respect `prefers-reduced-motion`.

---

## 24. Motion and Animation System

The app should feel premium and alive, but controlled.

Do not use random bouncy motion everywhere.

### Motion principles

- smooth
- confident
- editorial
- high-end SaaS
- minimal but noticeable

### Recommended motion toolkit

- page transition fade + translate
- staggered children reveal
- number count-up for metrics
- side drawer slide with opacity
- hover elevation and glow
- status pulse only for actionable items

### Specific animation patterns

#### Page enter

- initial: `opacity: 0`, `y: 16`
- animate: `opacity: 1`, `y: 0`
- duration: `0.45s`

#### Table row hover

- subtle background lift
- action buttons fade in

#### Cards

- mount stagger of `40ms` to `70ms`
- hover translate `-2px`
- shadow deepens slightly

#### Buttons

- press shrink to `0.98`
- glow or tint shift on hover

#### Drawers

- backdrop fade
- panel slide from right with slight blur

#### KPI cards

- count-up on first reveal
- tiny gradient sweep on mount

### Avoid

- overdone elastic transitions
- cheap glass everywhere
- noisy particle effects
- excessive parallax

---

## 25. Tailwind Design Tokens Recommendation

Use CSS variables under Tailwind theme extension.

Suggested design language:

- warm neutral canvas
- deep ink text
- moss/emerald success
- amber warning
- coral/red danger
- steel/slate surfaces

Suggested token categories:

- `--bg`
- `--bg-elevated`
- `--bg-muted`
- `--text`
- `--text-muted`
- `--border`
- `--brand`
- `--brand-strong`
- `--success`
- `--warning`
- `--danger`

Border radius:

- cards `20px`
- inputs `14px`
- buttons `14px`
- pills `999px`

Shadows:

- soft but layered
- avoid harsh default Tailwind box-shadows everywhere

---

## 26. Recommended Layout System

### App shell

- left sidebar on desktop
- top header with contextual actions
- content width with generous padding
- sticky page header for operational screens

### Mobile

- bottom sheet or slide menu
- compact summary cards
- stacked data cards instead of dense tables

---

## 27. Suggested Component Inventory

Build reusable components early.

Core UI:

- `AppShell`
- `Sidebar`
- `Topbar`
- `PageHeader`
- `MetricCard`
- `StatusBadge`
- `SectionCard`
- `DataTable`
- `EmptyState`
- `ErrorState`
- `FilterBar`
- `DrawerForm`
- `ConfirmDialog`
- `ToastProvider`

Feature components:

- `LoginForm`
- `GoogleLoginButton`
- `OrganizationForm`
- `MemberForm`
- `ReminderRunCard`
- `ReminderLogTable`
- `NotificationTable`
- `SettingsForm`
- `BrandPreviewCard`

---

## 28. Frontend Delivery Priorities

Build in this order:

1. Auth foundation
2. Protected routing and role shell
3. API client + error handling
4. ORG_ADMIN member workflow
5. Settings
6. Reminders and notifications
7. Organization profile
8. SUPER_ADMIN organization management
9. Analytics polish
10. Motion polish

This order aligns with current backend maturity.

---

## 29. Known Backend Realities the Frontend Must Respect

1. There is no public self-signup flow.
2. There is no refresh token route.
3. Member status and end date are backend-derived.
4. Search/filter/sort are client-side for now.
5. Notification delivery is tracked, but external providers may still be simulated/non-final.
6. Platform analytics are intentionally minimal today.
7. Org admins get their organization through `GET /api/organizations` as a single-item list.

---

## 30. Suggested UI Copy Tone

Use a tone that feels:

- premium
- clear
- operational
- not robotic

Examples:

- `Membership expires soon`
- `Reminder workflow completed`
- `Changes saved`
- `This organization is currently suspended`
- `No failed deliveries in the selected period`

Avoid:

- overly casual slang
- developer jargon in UI
- alarmist error copy

---

## 31. Final Build Standard

The frontend should feel like:

- a premium vertical SaaS
- highly usable for gym operators
- calm and polished
- efficient under daily repeated use

The frontend developer should optimize for:

- fast scanning
- low training burden
- operational confidence
- elegant motion
- strong data readability
- future feature scalability

If built well, the user should feel:

- in control
- informed
- never lost
- supported by automation
