# Frontend PRD — GymFlow

This PRD defines the frontend product scope for the current GymFlow backend.

It is written for frontend implementation.

The goal is to make it clear:

- which pages must exist
- which user role can access each page
- what each page should do
- which backend APIs power each page
- what UX expectations the frontend should follow

This PRD is based on the backend that currently exists in this repository.

---

## 1. Objective

Build the GymFlow frontend in:

- React
- TypeScript
- Tailwind CSS

The frontend should support the current backend workflow completely for:

- `SUPER_ADMIN`
- `ORG_ADMIN`

The frontend should feel like a premium SaaS product and should provide:

- clean navigation
- role-based access
- membership operations
- reminder operations
- organization management
- analytics visibility
- notification tracking

---

## 2. Product Summary

GymFlow is a multi-tenant gym management application.

The hierarchy is:

1. `SUPER_ADMIN`
2. `Organization`
3. `ORG_ADMIN`
4. `Members`

The platform is currently centered around:

- organization administration
- member lifecycle management
- automated reminders
- notification visibility
- operational settings

Reminder delivery model:

- gym members do not receive system notifications
- reminder notifications are sent only to organization admins
- reminders are grouped into one organization-level digest per run/channel

---

## 3. Supported User Roles

## `SUPER_ADMIN`

This is the platform owner role.

Capabilities:

- log in
- view platform-level analytics
- register organizations and org admin accounts
- list all organizations
- edit organizations
- soft delete organizations

Restrictions:

- does not manage members directly in current backend workflow

## `ORG_ADMIN`

This is the gym owner or gym administrator role.

Capabilities:

- log in
- access only their own organization context
- view organization analytics
- manage members
- update own organization details
- update reminder and notification settings
- run reminders manually
- inspect reminder logs
- inspect notification logs
- receive operational digest notifications for pending renewals

Restrictions:

- cannot access platform analytics
- cannot register organizations
- cannot manage other organizations

---

## 4. Frontend Role Rules

The frontend must enforce role-aware navigation and route protection.

Rules:

- `SUPER_ADMIN` should see only platform-management pages
- `ORG_ADMIN` should see only organization-scoped operational pages
- unauthorized routes should redirect to a dedicated forbidden page or to the correct dashboard

Do not rely on hidden menu items alone.

Use:

- route guards
- layout guards
- conditional action rendering

---

## 5. Total Pages Required

The frontend should include these pages.

## Public Pages

1. `Login`
2. `Google Login Handling` optional but recommended if Google auth uses callback flow

## Shared Utility Pages

3. `Unauthorized / Forbidden`
4. `Not Found`
5. `Account / Profile` optional but recommended

## `SUPER_ADMIN` Pages

6. `Platform Overview / Dashboard`
7. `Organizations List`
8. `Create Organization`
9. `Organization Detail / Edit`

## `ORG_ADMIN` Pages

10. `Organization Dashboard`
11. `Members List`
12. `Create Member`
13. `Edit Member`
14. `Reminders`
15. `Notifications`
16. `Settings`
17. `Organization Profile / Edit`

Total:

- `17 pages` including shared utility pages
- `14 core product pages` excluding optional account page and Google callback specifics

---

## 6. Route Structure

Recommended routes:

```text
/login
/unauthorized
/app/account
/app/platform-overview
/app/organizations
/app/organizations/new
/app/organizations/:id
/app/dashboard
/app/members
/app/members/new
/app/members/:id/edit
/app/reminders
/app/notifications
/app/settings
/app/organization
/*
```

Recommended layout split:

- public layout
- authenticated app shell
- role-specific navigation inside app shell

---

## 7. Page PRD

## 7.1 Login Page

### Route

`/login`

### Roles

- public

### Purpose

Authenticate users using:

- email and password
- Google login

### Backend APIs

- `POST /api/auth/login`
- `POST /api/auth/google-login`

### Main UI Sections

- hero branding section
- login form card
- optional Google sign-in action
- support copy

### Required Fields

- email
- password

### UX Requirements

- form validation
- loading state on submit
- error toast/banner on failure
- redirect after success based on role

### Redirect Rules

- `SUPER_ADMIN` -> `/app/platform-overview`
- `ORG_ADMIN` -> `/app/dashboard`

---

## 7.2 Unauthorized / Forbidden Page

### Route

`/unauthorized`

### Roles

- authenticated users who hit forbidden areas

### Purpose

Explain that the user does not have access to this page or action.

### UX Requirements

- clear message
- button to go to correct dashboard
- no raw technical language

---

## 7.3 Not Found Page

### Route

fallback route

### Roles

- all

### Purpose

Handle missing routes cleanly.

### UX Requirements

- polished empty/error visual
- CTA back to app home

---

## 7.4 Account / Profile Page

### Route

`/app/account`

### Roles

- `SUPER_ADMIN`
- `ORG_ADMIN`

### Purpose

Display current authenticated user information returned during login.

### Backend APIs

No dedicated profile endpoint currently exists.

Use login response/session state for now.

### UI Sections

- user identity
- role
- email
- avatar if present
- account metadata if available

### Notes

This page is optional from backend perspective but recommended for complete UX.

---

## 7.5 Platform Overview / Dashboard

### Route

`/app/platform-overview`

### Roles

- `SUPER_ADMIN`

### Purpose

Provide platform-level overview for the platform owner.

### Backend APIs

- `GET /api/analytics/platform`

### Data

- total organizations
- active subscriptions

### UI Sections

- KPI cards
- platform summary hero
- CTA to create organization
- shortcut to organization list

### UX Requirements

- premium executive dashboard feel
- strong visual hierarchy
- should scale for future analytics additions

---

## 7.6 Organizations List Page

### Route

`/app/organizations`

### Roles

- `SUPER_ADMIN`

### Purpose

List all organizations in the system.

### Backend APIs

- `GET /api/organizations`

### Main UI Sections

- page header
- search and filters
- organization table or card list
- row actions

### Required Data Display

- organization name
- district
- admin name
- admin email
- phone number
- status
- short description

### User Actions

- open organization detail
- edit organization
- delete organization
- create new organization

### UX Requirements

- searchable
- filterable by status
- premium table layout

---

## 7.7 Create Organization Page

### Route

`/app/organizations/new`

### Roles

- `SUPER_ADMIN`

### Purpose

Create a new organization and its `ORG_ADMIN`.

### Backend APIs

- `POST /api/auth/register`

### Form Sections

1. Organization information
2. Contact and location
3. Admin account details
4. Review and submit

### Required Fields

- organization name
- district
- admin full name
- admin phone number
- admin email
- password

### Optional Fields

- logo URL
- map location link
- address
- description

### UX Requirements

- onboarding-style experience
- inline validation
- success confirmation

---

## 7.8 Organization Detail / Edit Page

### Route

`/app/organizations/:id`

### Roles

- `SUPER_ADMIN`

### Purpose

View and edit a specific organization.

### Backend APIs

- `GET /api/organizations`
- `PATCH /api/organizations/{id}`
- `DELETE /api/organizations/{id}`

### UI Sections

- organization summary
- editable details form
- status control
- admin contact block
- destructive delete action

### Required Editable Fields

- name
- district
- logo URL
- map location link
- address
- admin full name
- admin phone number
- admin email
- description
- status

### UX Requirements

- use a detail page or split detail layout
- emphasize status clearly
- delete action must require confirmation

---

## 7.9 Organization Dashboard

### Route

`/app/dashboard`

### Roles

- `ORG_ADMIN`

### Purpose

Provide the org admin with a daily operational overview.

### Backend APIs

- `GET /api/analytics/organization`
- `GET /api/members`
- `GET /api/reminders/logs`

### Main UI Sections

- KPI cards
- membership health summary
- expiring soon list
- overdue members list
- recent reminder activity
- quick actions

### Primary Metrics

- active members
- reminders sent

### Secondary Derived Metrics

These can be derived from members list:

- expiring soon count
- expired count
- overdue count

### Quick Actions

- add member
- run reminders
- view notifications
- open settings

---

## 7.10 Members List Page

### Route

`/app/members`

### Roles

- `ORG_ADMIN`

### Purpose

List and manage all members in the current organization.

### Backend APIs

- `GET /api/members`
- `DELETE /api/members/{id}`

### Main UI Sections

- summary strip
- search/filter controls
- members table
- bulk overview by status
- create member CTA

### Required Data Display

- full name
- joining date
- membership duration
- membership start date
- membership end date
- membership status
- notes preview

### User Actions

- create member
- edit member
- delete member

### UX Requirements

- filter by membership status
- sort by expiry date
- search by name
- empty state for no members

---

## 7.11 Create Member Page

### Route

`/app/members/new`

### Roles

- `ORG_ADMIN`

### Purpose

Create a new member record.

### Backend APIs

- `POST /api/members`

### Required Fields

- full name
- joining date
- membership duration

### Optional Fields

- custom duration days
- renewed
- notes

### Important Business Rules

- `membershipEndDate` is not entered by the frontend
- `membershipStatus` is not entered by the frontend
- if duration is `CUSTOM`, `customDurationDays` must be shown

### UX Requirements

- live expiry preview is recommended
- backend remains source of truth
- success toast and redirect to members list or detail flow

---

## 7.12 Edit Member Page

### Route

`/app/members/:id/edit`

### Roles

- `ORG_ADMIN`

### Purpose

Edit an existing member.

### Backend APIs

- `GET /api/members`
- `PATCH /api/members/{id}`

### Form Behavior

Same form structure as create member.

### Editable Fields

- full name
- joining date
- membership duration
- custom duration days
- notes

### Read Context to Show

- current membership status
- current membership start date
- current membership end date

---

## 7.13 Reminders Page

### Route

`/app/reminders`

### Roles

- `ORG_ADMIN`

### Purpose

Allow admin to:

- manually run reminders
- inspect reminder workflow logs

### Backend APIs

- `POST /api/reminders/run`
- `GET /api/reminders/logs`
- `GET /api/settings`

### Main UI Sections

- reminder control panel
- current reminder settings snapshot
- reminder run action
- reminder results summary
- reminder logs table

### Run Result Display

- processed members
- organizations grouped
- digests sent

### UX Requirements

- manual run button with loading state
- refetch logs after run
- show last action results prominently

---

## 7.14 Notifications Page

### Route

`/app/notifications`

### Roles

- `ORG_ADMIN`

### Purpose

Display all notification delivery logs for the current organization.

### Backend APIs

- `GET /api/notifications`

### Main UI Sections

- page header
- status and channel filters
- notifications table
- detail drawer or row expansion

### Required Data Display

- member count
- type
- channel
- recipient
- status
- attempt count
- failure reason
- sent at
- created at
- message

### UX Requirements

- filter by status
- filter by channel
- filter by type
- clear failed-state explanation

---

## 7.15 Settings Page

### Route

`/app/settings`

### Roles

- `ORG_ADMIN`

### Purpose

Configure organization reminder and notification preferences.

### Backend APIs

- `GET /api/settings`
- `PATCH /api/settings`

### Main UI Sections

1. Branding
2. Reminder timing
3. Delivery channels
4. Notification preview

### Editable Fields

- whatsapp enabled
- email enabled
- reminder days before expiry
- branding name

### UX Requirements

- toggles should be clear and premium
- live preview is recommended
- save feedback must be immediate and clear

Preview should reflect admin-facing digest content, not member-facing reminder content.

---

## 7.16 Organization Profile / Edit Page

### Route

`/app/organization`

### Roles

- `ORG_ADMIN`

### Purpose

Allow org admin to view and edit their own organization profile.

### Backend APIs

- `GET /api/organizations`
- `PATCH /api/organizations/{id}`

### Main UI Sections

- organization hero/profile section
- details form
- contact form section
- map/external link area

### Editable Fields

- name
- district
- logo URL
- map location link
- address
- admin full name
- admin phone number
- admin email
- description

### Notes

Backend returns org admin organization through the organizations list API as a single-item array.

Frontend must read the first record from that array.

---

## 8. Navigation by Role

## `SUPER_ADMIN` Navigation

- Platform Overview
- Organizations
- Register Organization
- Account

## `ORG_ADMIN` Navigation

- Dashboard
- Members
- Reminders
- Notifications
- Settings
- Organization
- Account

---

## 9. Shared Frontend Requirements

These apply to all pages.

### Authentication

- JWT bearer token
- redirect to login on `401`
- clear session on invalid token

### Error Handling

- use backend `message`
- support validation field mapping
- support forbidden screen
- support rate limit responses

### Loading States

- skeletons for pages
- button loading states
- smooth transitions between data states

### Empty States

- purpose-driven empty states
- action CTA on every empty screen

### Motion

- page transitions
- hover states
- drawer and modal animation
- metric count animation

### Accessibility

- keyboard navigation
- proper focus states
- semantic markup
- reduced motion support

---

## 10. Recommended Build Order

Build the frontend in this order:

1. Login
2. App shell and route guards
3. Organization dashboard
4. Members list
5. Create and edit member flows
6. Settings
7. Reminders
8. Notifications
9. Organization profile
10. SUPER_ADMIN platform overview
11. Organizations list
12. Create organization
13. Organization detail/edit
14. Utility pages and final polish

---

## 11. Final Product Goal

The frontend should result in a complete role-based GymFlow interface where:

- `SUPER_ADMIN` can operate the platform
- `ORG_ADMIN` can operate a gym
- members can be managed fully
- reminders can be monitored and triggered
- notifications can be audited
- settings can be configured cleanly

The UI should feel:

- premium
- operational
- calm
- modern
- animation-rich but controlled

This is the frontend product scope for the current backend.
