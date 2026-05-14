PRD — Multi-Tenant Gym Membership Management SaaS

Product Name

GymFlow (working name)

---

1. Product Vision

Build a production-ready multi-tenant SaaS platform for gyms and fitness centers to manage:

- memberships
- renewals
- reminders
- subscriptions
- organization operations
- admin workflows

The platform focuses on:

- operational simplicity
- automated renewal reminders
- organization-level management
- centralized SaaS control

---

2. Product Goals

Primary Goals

- eliminate manual notebook tracking
- automate membership renewals
- automate reminders
- centralize gym operations
- provide organization-level analytics
- provide SaaS subscription control

Secondary Goals

- WhatsApp communication
- beautiful branded emails
- operational visibility
- scalable SaaS architecture

---

3. Platform Hierarchy

SUPER_ADMIN (Platform Owner)
↓
Organizations (Gyms)
↓
Organization Admins
↓
Gym Members

---

4. User Roles

4.1 SUPER_ADMIN

Platform owner role.

Capabilities:

- manage all organizations
- view all organization analytics
- suspend organizations
- manage subscriptions
- monitor WhatsApp/email usage
- categorize organizations by district
- manage platform-wide settings
- access operational analytics

---

4.2 ORG_ADMIN

Gym owner/admin role.

Capabilities:

- manage gym members
- manage subscriptions
- configure notifications
- manage organization settings
- view organization reports
- send reminders
- manage membership statuses

---

5. Core Features

5.1 Authentication & Authorization

Features

- JWT authentication
- role-based access control (RBAC)
- secure password hashing
- session management
- organization-scoped access

---

5.2 Multi-Tenant Organization System

Features

- multiple gyms under one platform
- tenant isolation
- organization-scoped data
- organization-specific settings

Organization Fields

- organization name
- district
- logo/image URL
- map location link
- address
- admin full name
- admin phone number
- admin email
- organization description
- active/inactive status

---

5.3 Organization Subscription System

Features

- subscription-based organizations
- monthly billing
- Razorpay integration
- active/inactive subscription states

Rules

If organization subscription expires:

- login restricted
- reminders disabled
- operations disabled
- dashboard locked

---

5.4 Membership Management

Features

- add/edit/delete members
- membership duration
- joining date
- expiry date
- renewal tracking
- payment tracking

Member Fields

- full name
- phone number
- email
- joining date
- membership plan
- expiry date
- status
- notes

---

5.5 Automated Reminder Engine

Features

- renewal reminders
- scheduled notifications
- pre-expiry reminders
- overdue reminders

Channels

- WhatsApp
- email

Trigger Example

1 day before expiry
↓
send reminder

---

5.6 Notification Preference Settings

Each organization can:

- enable/disable WhatsApp reminders
- enable/disable email reminders

Settings Examples

whatsapp_enabled = true
email_enabled = false

---

5.7 WhatsApp Messaging System

Features

- WhatsApp renewal reminders
- organization-specific templates
- automated delivery
- usage tracking

Message Types

- membership expiry
- overdue payment
- welcome message

---

5.8 Email Notification System

Features

- beautiful HTML templates
- onboarding emails
- renewal emails
- organization branding

Mail Provider

- Google Mail SMTP

---

5.9 Usage Tracking System

Track monthly usage per organization.

Metrics

- WhatsApp messages sent
- emails sent
- active members
- active admins
- subscription status

---

5.10 Analytics Dashboard

SUPER_ADMIN Dashboard

Features

- total organizations
- active subscriptions
- district-wise organizations
- monthly revenue
- message usage analytics
- active organizations

---

ORG_ADMIN Dashboard

Features

- expiring memberships
- active members
- overdue renewals
- reminder counts
- monthly statistics

---

5.11 Soft Delete System

Features

All delete operations use:

soft delete

Records remain in database.

Fields

deleted = true
deleted_at

Applied to:

- organizations
- members
- admins
- subscriptions

---

5.12 Role-Based Access Control

Roles

SUPER_ADMIN
ORG_ADMIN

Security

- tenant isolation
- permission guards
- protected APIs

---

5.13 Dedicated Organization Pages

Each organization has:

- organization profile page
- analytics page
- settings page
- member management page

---

5.14 Dedicated User Pages

Each member has:

- profile page
- membership history
- renewal history
- status tracking

---

5.15 Organization Settings Module

Organization admins can configure:

- WhatsApp toggle
- email toggle
- branding
- reminder timings
- organization details

---

5.16 Audit Logging

Features

Track:

- admin actions
- membership updates
- reminder activity
- subscription changes

---

6. Payment System

Provider

- Razorpay

Features

- recurring subscriptions
- monthly billing
- payment verification
- invoice records

---

7. Tech Stack

Frontend

- React
- TypeScript
- TailwindCSS

---

Backend

- Java
- Spring Boot

Spring Modules

- Spring Security
- Spring Data JPA
- Spring Validation
- Spring Mail

---

Database

- PostgreSQL

---

Database Migration

- Flyway

---

Authentication

- JWT

---

Queue / Async Processing

- RabbitMQ (optional initially)

---

Deployment

- Docker
- Nginx
- AWS EC2

---

Email Service

- Google SMTP

---

WhatsApp Integration

- WhatsApp Business API

---

8. Database Architecture

Core Tables

organizations

organization_settings

organization_subscriptions

admins

members

notifications

usage_metrics

audit_logs

payments

---

9. Core API Examples

Auth

POST /api/auth/login
POST /api/auth/register

---

Organizations

GET /api/organizations
POST /api/organizations
PATCH /api/organizations/{id}

---

Members

POST /api/members
GET /api/members
PATCH /api/members/{id}

---

Analytics

GET /api/analytics/platform
GET /api/analytics/organization

---

10. Production Requirements

Security

- RBAC
- tenant isolation
- encrypted secrets
- secure JWT

---

Reliability

- scheduled retries
- failure logging
- audit trails

---

Scalability

- multi-tenant architecture
- async processing support
- organization isolation

---

11. MVP Scope

Included

- multi-tenancy
- organization management
- membership tracking
- reminders
- WhatsApp/email notifications
- analytics
- subscriptions
- soft deletes

Excluded Initially

- AI features
- workout plans
- trainer systems
- advanced fitness analytics

---

12. Product Philosophy

This product is NOT:

- a bloated ERP
- a fitness social app

This product IS:

- operational membership management SaaS
- renewal automation system
- multi-tenant gym operations platform

focused on:

simplicity
automation
operational reliability




PRD ADDENDUM — Domain Model & Database Architecture Update

1. User Model Clarification

The system distinguishes between:

A. Application Users

Authenticated platform users with login access.

These users can:

- access dashboard
- manage organizations
- manage members
- configure settings

Roles

SUPER_ADMIN
ORG_ADMIN

Stored In

users table

Fields

id
organization_id
email
password_hash
role
status
created_at
updated_at
deleted

---

B. Gym Members

Business entities tracked by organizations.

Gym members:

- DO NOT have login access
- DO NOT authenticate
- DO NOT receive JWT tokens
- are operational records only

Their purpose is:

- membership tracking
- renewal reminders
- operational management

Stored In

members table

Fields

id
organization_id
full_name
phone_number
email
joining_date
membership_start_date
membership_end_date
membership_status
notes
deleted
created_at
updated_at

---

2. Database Design Decision

Approved Architecture

Use:

normalized relational tables

NOT:

JSON-based member storage

---

3. Reasoning

Using relational tables provides:

- proper indexing
- scalable querying
- efficient reminder processing
- analytics support
- filtering/sorting
- reporting support
- production-grade architecture

---

4. Example Queries

Expiring Membership Query

SELECT *
FROM members
WHERE membership_end_date <= CURRENT_DATE + INTERVAL '1 day'
AND deleted = false;

---

5. Reminder Processing Workflow

Scheduler
↓
Find expiring members
↓
Check org settings
↓
Send WhatsApp/email
↓
Log notification
↓
Update usage metrics

---

6. Database System Update

Database Engine

Use:

- PostgreSQL for development
- PostgreSQL for production
- PostgreSQL for testing

---

7. Testing Database Policy

The system will NOT use:

H2 database

Reason:

- avoid environment inconsistency
- ensure production parity
- avoid SQL dialect differences
- realistic migration validation

---

8. Flyway Migration Policy

All schema creation and updates must use:

Flyway migrations

No automatic schema generation in production.

---

9. Spring Boot Configuration Policy

application.yml / application.properties

Environment-based configuration:

- local
- dev
- prod

Configured for:

- PostgreSQL
- Flyway
- JWT
- mail service
- Razorpay
- WhatsApp APIs

---

10. Soft Delete Enforcement

All domain entities use:

soft delete

No hard delete operations allowed.

Applied to:

- organizations
- users
- members
- subscriptions
- notifications

---

11. Final Architecture Summary

Platform Owner
↓
Organizations
↓
Application Users (Admins)
↓
Members (Tracked Entities)
↓
Membership Workflows
↓
Notifications & Renewals

This structure becomes the official production architecture for the platform.

---

## Implementation Status (Current)

The repository now includes an initial backend implementation at:

- `/home/runner/work/GymMax/GymMax/backend`

Implemented in this phase:

- Spring Boot 3 backend scaffold (Java 21, Maven)
- PostgreSQL + Flyway baseline schema migration
- JWT-based authentication with role claims
- RBAC-aware API surface for:
  - auth (`/api/auth/register`, `/api/auth/login`)
  - organizations (`/api/organizations`)
  - members (`/api/members`)
  - analytics (`/api/analytics/platform`, `/api/analytics/organization`)
- Soft delete support in core entities and repositories
- Scheduled reminder foundation for expiry notifications with usage metric logging
- Default SUPER_ADMIN bootstrap user (configurable by env vars)

## Local Run (Backend)

### 1) Start PostgreSQL

Use any local PostgreSQL instance and create a database/user, for example:

- database: `gymflow`
- user: `gymflow`
- password: `gymflow`

### 2) Configure environment

Set environment variables (optional if using defaults):

- `DB_URL` (default `jdbc:postgresql://localhost:5432/gymflow`)
- `DB_USERNAME` (default `gymflow`)
- `DB_PASSWORD` (default `gymflow`)
- `JWT_SECRET` (**required**, base64-encoded secret key)
- `SUPER_ADMIN_EMAIL` (default `admin@gymflow.local`)
- `SUPER_ADMIN_PASSWORD` (**required**)

### 3) Run the application

From `/home/runner/work/GymMax/GymMax/backend`:

```bash
mvn spring-boot:run
```

Flyway migrations run automatically on startup.

### 4) Java version

- Java 17 is required.

## Local Run (Frontend)

The frontend is available at:

- `/home/runner/work/GymMax/GymMax/frontend`

### 1) Install dependencies

```bash
cd /home/runner/work/GymMax/GymMax/frontend
npm install
```

### 2) Start development server

```bash
npm run dev
```

### 3) Production build

```bash
npm run build
```
