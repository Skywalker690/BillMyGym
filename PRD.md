# GymFlow — Main Product Requirements Document (PRD)

## Product Vision

GymFlow is a production-ready, multi-tenant SaaS platform for gym owners to manage memberships, payments, renewals, reminders, and revenue operations.

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
GymFlow Alert

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
