# Backend

## Stack

- Java 21
- Spring Boot 3.3.5
- Spring Web, Security, Data JPA, Validation, Mail
- PostgreSQL
- Flyway
- JWT via `io.jsonwebtoken`
- BCrypt password hashing
- Google ID token verification
- Razorpay order/webhook integration
- WhatsApp Cloud API integration

## Entry Points

- Main app: `backend/src/main/java/com/gymflow/GymflowApplication.java`
- Config: `backend/src/main/resources/application.yml`
- Production overrides: `backend/src/main/resources/application-prod.yml`
- Migrations: `backend/src/main/resources/db/migration`
- Tests: `backend/src/test/java/com/gymflow`

## Security

`SecurityConfig` builds a stateless API security chain:

- CSRF is ignored for `/api/**` and `/actuator/**`.
- CORS allows local frontend and BillMyGym production domains.
- Public routes include login, signup, Google login, legal policy metadata, password reset, Razorpay webhook, and WhatsApp webhook.
- Super-admin routes include audit logs, platform analytics, organizations directory, organization subscription status/payment history, admin updates, deletion, referral settings, and bulk upload.
- Org-admin routes include members, memberships, payments, reports, reminders, settings, notifications, tracking, org analytics/revenue, subscription checkout/status, and referral dashboard.
- Filters run around JWT auth, legal consent guard, subscription guard, rate limiting, and API failure logging.

Auth uses:

- `AuthService` for login, signup, Google login, OTP/password flows, policy consent, and user context.
- `JwtService` for token creation/validation.
- `SecurityUtils.currentUser()` for role and organization context.

## Persistence

All schema changes should be Flyway migrations. Hibernate validates schema at startup. Do not rely on auto-DDL.

Important entities:

- `Organization`
- `User`
- `Member`
- `MemberMembership`
- `MemberPayment`
- `MemberCreditLedgerEntry`
- `OrganizationSubscriptionPayment`
- `OrganizationSetting`
- `PlatformSetting`
- `NotificationLog`
- `NotificationDeliveryLog`
- `WhatsappReminderLog`
- `UsageMetric`
- `AuditLog`
- `Referral`
- `ReferralReward`
- `ProcessedWebhookEvent`
- `MembershipOperationIdempotency`

Most entities extend `BaseEntity`, which supplies id, timestamps, and soft-delete style fields where applicable.

## Main Services

- `AuthService`: organization admin registration, public signup, login, Google auth, OTP/password reset, legal consent.
- `OrganizationService` / `OrganizationLifecycleService`: organization profile/admin changes, deletion, lifecycle normalization.
- `BillingLifecycleService`: trial, paid billing period, paused/deactivated transitions, due dates.
- `SubscriptionService`: checkout/status/payment history and Razorpay order creation.
- `OrganizationPaymentWebhookService`: Razorpay webhook/client payment validation and paid-period activation.
- `SubscriptionPricingService`: BillMyGym subscription pricing and active-member overage calculation.
- `MemberService`: member directory, create/update/delete, CSV bulk import.
- `MemberMembershipService`: membership creation/listing/status refresh.
- `MembershipLifecycleService`: membership end-date calculation, active/overdue status, payment coverage summaries.
- `PaymentLedgerService`: member payment creation, deletion, and idempotent payment responses.
- `MemberCreditLedgerService`: wallet/credit balance and applied credit.
- `ReportService`, `FinancialReportService`, `AnalyticsService`, `RevenueService`: dashboards and reports.
- `ReminderService`, `MembershipReminderScheduler`, `IndividualWhatsappReminderService`: reminder discovery and sending.
- `WhatsAppDeliveryService`, `WhatsAppWebhookService`, `WhatsAppRetryScheduler`, `WhatsAppUsageService`: WhatsApp send/delivery/webhook/retry/usage tracking.
- `ReferralService`: referral codes, signup rewards, dashboard, analytics, and settings.
- `AuditLogService`, `AuditQueryService`: event capture and query/export.
- `PlatformSettingService`: global platform settings such as referral enablement.

## Scheduled Jobs

The backend contains schedulers for:

- Membership reminders.
- Subscription billing/retry lifecycle.
- WhatsApp retry delivery.
- Monthly organization reports.

Check scheduler classes before changing lifecycle rules because scheduled transitions can update persisted state outside direct user requests.

## Tests

Existing unit tests focus on core services:

- Auth
- JWT
- OTP
- Billing lifecycle
- Membership lifecycle
- Payment ledger
- Reminder service
- Subscription pricing

Run backend tests from `backend` with:

```powershell
mvn test
```

