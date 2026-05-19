# BillMyGym — Main Product Requirements Document (PRD)

## Product Vision

BillMyGym is a production-ready, multi-tenant SaaS platform for gym owners to manage memberships, payments, renewals, reminders, and revenue operations.

The system is admin-focused.

Gym members do NOT:

* login
* use the app
* own accounts

They are tracked business entities.

---

# Platform Hierarchy

```text
Platform Owner (SUPER_ADMIN)
↓
Organizations (Gyms)
↓
Organization Admins
↓
Members (tracked entities only)
```

---

# Core Workflow

```text
Organization Created
↓
Admin Created
↓
Admin Creates Members
↓
Membership Purchased
↓
Membership History Created
↓
Payments Recorded
↓
Revenue Calculated
↓
Membership Expires
↓
Scheduler Runs
↓
Digest Generated
↓
Admin Receives WhatsApp/Email
```

---

# Core Architecture

```text
Member
↓
Membership History
↓
Payment Ledger
↓
Revenue Analytics
↓
Admin Reminder Digest
```

---

# Member System

Members store:

* full name
* phone
* email
* status
* notes

Rules:

* identity only
* no pricing
* no package duration
* no billing logic
* soft delete only

Member Status:

```text
ACTIVE
DEACTIVATED
```

Unique Constraint:

```text
organization_id + phone
```

---

# Membership System

Each purchase creates:

```text
NEW membership row
```

Supported:

* 1 month
* 3 month
* 6 month
* custom
* upgrade
* downgrade
* package switch
* early renewal
* rejoin

Rules:

* history never overwritten
* one ACTIVE membership only
* old memberships become COMPLETED

Membership Status:

```text
ACTIVE
EXPIRED
OVERDUE
COMPLETED
```

Dynamic only:

```text
EXPIRING_SOON
```

Formula:

```text
endDate =
startDate
+ duration
- 1 day
```

---

# Payment System

Revenue source:

```text
member_payments
```

Never:

```text
member_count × fee
```

Fields:

* amount
* paymentDate
* paymentType
* gatewayTransactionId

Rules:

* gatewayTransactionId optional
* globally unique when present
* supports manual and online payments

Payment Status:

```text
PAID
PARTIAL
PENDING
FAILED
```

---

# Revenue Dashboard

Organization admins see:

* Current Revenue
* Pending Revenue
* Expected Revenue
* Overdue Revenue
* Active Members
* Prepaid Members

Revenue calculations come from payment ledger only.

---

# Reminder Workflow

Notifications are NOT sent to members.

Workflow:

```text
Find EXPIRED + OVERDUE
↓
Group by organization
↓
Generate digest
↓
Send ONE WhatsApp/email
```

Example:

```text
BillMyGym Alert

John — Overdue 2 days
Rahul — Expired today

Total Pending: 2
```

---

# Reminder Dedup

Uses BOTH:

```text
membership.last_sent_at
+
notification_logs
```

---

# SaaS Features

* Multi-tenancy
* JWT
* RBAC
* Tenant isolation
* PostgreSQL
* Flyway
* Soft delete
* Usage metrics
* Organization subscriptions
* Audit logs
* Admin dashboards

---

# Product Goal

Build a simple operational SaaS for gyms that replaces notebook tracking with automated membership and revenue workflows.

---

# Organization Subscription & Platform Billing (MVP Addition)

## Objective

BillMyGym itself is a subscription SaaS.

Organizations (gyms) must pay recurring platform subscription fees to continue using the system.

Platform subscription billing is completely separate from gym-member billing.

---

# Organization Subscription States

```text
TRIAL
ACTIVE
PAUSED
DEACTIVATED
```

Definitions:

TRIAL

* initial onboarding period

ACTIVE

* subscription paid
* full access

PAUSED

* payment overdue
* services restricted

DEACTIVATED

* unpaid > 3 months
* organization disabled

---

# Subscription Workflow

```text
Organization Created
↓
Trial / Initial Subscription
↓
Subscription Due Date Reached
↓
Payment Missing
↓
Organization becomes PAUSED
↓
Paused > 3 months
↓
DEACTIVATED
```

---

# PAUSED Organization Rules

If organization becomes:

```text
PAUSED
```

Restrict:

* login access
* dashboard access
* member operations
* reminders
* analytics
* notifications
* API access

Behavior:

```text
organization login
↓
JWT validated
↓
subscription middleware
↓
PAUSED
↓
deny access
```

Only allowed:

* payment page
* subscription status APIs

---

# DEACTIVATED Organization Rules

Condition:

```text
PAUSED > 90 days
```

Effects:

* organization fully disabled
* no app access
* scheduler ignored
* reminders disabled
* analytics excluded

Historical data retained.

No hard delete.

---

# Razorpay Integration

Provider:

Razorpay

Features:

* create subscription order
* payment verification
* webhook processing
* recurring subscription handling
* invoice tracking

APIs:

```text
POST /api/platform/subscription/create
POST /api/platform/subscription/verify
POST /api/platform/subscription/webhook
GET /api/platform/subscription/status
```

---

# Organization Subscription Ledger

Create separate table:

```text
organization_subscription_payments
```

Do NOT mix with:

```text
member_payments
```

Reason:

Platform revenue and gym revenue are different domains.

---

Fields

```text
id
organization_id
amount
payment_date
subscription_start
subscription_end
payment_status
gateway_transaction_id
razorpay_order_id
razorpay_payment_id
created_at
```

Payment Status:

```text
PAID
PENDING
FAILED
REFUNDED
```

---

# Organization Subscription Middleware

Create:

```text
SubscriptionGuardFilter
```

Workflow:

```text
JWT
↓
Resolve organization
↓
Check subscription status
↓
ACTIVE → allow
PAUSED → deny
DEACTIVATED → deny
```

---

# Platform Owner Dashboard

SUPER_ADMIN can view:

* active organizations
* paused organizations
* deactivated organizations
* monthly SaaS revenue
* failed payments
* organization payment history
* subscription analytics

