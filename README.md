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