# Agent Stored Knowledge

This file is for future coding agents working in this repository.

## Current Understanding

BillMyGym is a production-oriented gym SaaS with:

- tenant-scoped org admin workflows.
- super-admin platform workflows.
- member lifecycle and payment ledger.
- organization subscription billing through Razorpay.
- WhatsApp reminders and delivery tracking.
- legal consent gating.
- referral and GymCoin features.
- audit logs and reports.

## Do Not Break These Assumptions

- Org-admin data access must be scoped to `SecurityUtils.currentUser().organizationId()`.
- Super-admin routes are the only cross-organization routes.
- Backend response envelopes are unwrapped by the frontend API client.
- Subscription/legal guard redirects are centralized in `frontend/src/lib/api.ts` and `ProtectedRoute`.
- Flyway migrations are required for schema changes.
- Hibernate validates schema; it does not create/update schema.
- Soft-delete fields matter; repository methods usually filter `deleted=false`.
- App date logic uses `Asia/Kolkata`.
- Membership end date is inclusive.
- Organization subscription due date is exclusive: access exists while due date is after today.
- Demo orgs bypass billing but have a 50-member sandbox limit.
- Payments-disabled local checkout auto-marks subscription orders paid.

## Files To Inspect First For Common Tasks

Auth or roles:

- `backend/src/main/java/com/gymflow/config/SecurityConfig.java`
- `backend/src/main/java/com/gymflow/service/AuthService.java`
- `backend/src/main/java/com/gymflow/security`
- `frontend/src/context/AuthContext.tsx`
- `frontend/src/components/layout/ProtectedRoute.tsx`

Members and memberships:

- `backend/src/main/java/com/gymflow/service/MemberService.java`
- `backend/src/main/java/com/gymflow/service/MemberMembershipService.java`
- `backend/src/main/java/com/gymflow/service/MembershipLifecycleService.java`
- `frontend/src/pages/org/MembersList.tsx`
- `frontend/src/pages/org/MemberForm.tsx`

Payments and subscription:

- `backend/src/main/java/com/gymflow/service/PaymentLedgerService.java`
- `backend/src/main/java/com/gymflow/service/SubscriptionService.java`
- `backend/src/main/java/com/gymflow/service/OrganizationPaymentWebhookService.java`
- `backend/src/main/java/com/gymflow/service/BillingLifecycleService.java`
- `backend/src/main/java/com/gymflow/service/SubscriptionPricingService.java`
- `frontend/src/pages/org/Subscription.tsx`

Reports and analytics:

- `backend/src/main/java/com/gymflow/service/ReportService.java`
- `backend/src/main/java/com/gymflow/service/FinancialReportService.java`
- `backend/src/main/java/com/gymflow/service/AnalyticsService.java`
- `backend/src/main/java/com/gymflow/service/RevenueService.java`
- `backend/src/main/java/com/gymflow/service/ReportCacheService.java`
- `frontend/src/pages/org/Reports.tsx`
- `frontend/src/pages/admin/PlatformReports.tsx`
- `frontend/src/pages/admin/PlatformOverview.tsx`

Notifications and WhatsApp:

- `backend/src/main/java/com/gymflow/service/ReminderService.java`
- `backend/src/main/java/com/gymflow/service/IndividualWhatsappReminderService.java`
- `backend/src/main/java/com/gymflow/service/WhatsAppDeliveryService.java`
- `backend/src/main/java/com/gymflow/service/WhatsAppWebhookService.java`
- `frontend/src/pages/org/Reminders.tsx`

Referrals:

- `backend/src/main/java/com/gymflow/service/ReferralService.java`
- `backend/src/main/java/com/gymflow/api/ReferralController.java`
- `frontend/src/pages/org/ReferralDashboard.tsx`
- `frontend/src/pages/admin/AdminReferralAnalytics.tsx`

## Verification Defaults

- Backend-only logic: run `mvn test` in `backend`.
- Frontend-only changes: run `npm run lint` and `npm run build` in `frontend`.
- Cross-stack route/API changes: run backend tests, frontend lint/build, and manually verify route/API shape.

## Implementation Style

- Follow existing service/controller/repository patterns.
- Keep role and tenant checks in services unless controller/security config already guarantees them.
- Prefer DTO records/classes already grouped under `api/dto`.
- Use mapper classes when one exists.
- Preserve report cache eviction on domain writes.
- Avoid broad refactors while fixing a focused behavior.

