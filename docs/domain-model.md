# Domain Model And Business Rules

## Organization

`Organization` is the tenant root.

Key fields:

- name, district, place, logo/map/address.
- admin full name, phone, and email.
- monthly membership fee and admission fee.
- `organizationType`: currently `REGULAR` or `DEMO`.
- `subscriptionPlan`: currently simplified to `BILLMYGYM`.
- `status`: trial/active/paused/deactivated style lifecycle.
- trial dates and subscription due date.
- referral code and GymCoin balances.

Lifecycle rules:

- Public signup always creates a `REGULAR` organization.
- Super-admin registration can create `DEMO` or regular organizations.
- Regular organizations start with a 60-day trial.
- `subscriptionDueDate` is the first day without access; access is valid while it is after today.
- Paid periods run one month: start date through `start.plusMonths(1).minusDays(1)`.
- Demo organizations bypass billing and have active access.

## User

Users authenticate into the app.

Roles:

- `SUPER_ADMIN`: no organization required.
- `ORG_ADMIN`: belongs to one organization.

Important fields:

- email, password hash, role, status.
- Google ID/avatar for Google login.
- terms/privacy acceptance, policy version, accepted time, IP, and device info.

Legal policy consent is mandatory for protected app routes.

## Member

`Member` belongs to an organization.

Current simplified shape:

- full name.
- optional phone.
- status, usually `ACTIVE` or `DEACTIVATED`.
- deactivation reason/timestamp.

Deleting a member is soft delete. Deactivating a member deactivates latest membership and prevents payment recording.

## Member Membership

`MemberMembership` represents a dated membership period for one member.

Key fields:

- duration months.
- plan amount.
- start date and end date.
- status.
- last reminder sent timestamp.

Rules:

- End date is `startDate.plusMonths(durationMonths).minusDays(1)`.
- A membership is `ACTIVE` through its end date.
- It becomes `OVERDUE` after the end date.
- "Expiring soon" is computed dynamically and currently behaves as "due today"; it is not a persisted status.

## Member Payment

`MemberPayment` records payments against a membership.

Key fields:

- member, membership, organization.
- amount, payment date, billing month.
- payment status.
- payment type.
- notes and optional gateway transaction id.

Rules:

- Current payment recording supports paid records only.
- Membership coverage includes `MEMBERSHIP`, `NEW_MEMBERSHIP`, and `RENEWAL` payment types.
- Admission payments do not count toward membership coverage.
- Duplicate payment requests should use idempotency keys.

## Member Credit Ledger

The member credit ledger tracks wallet-like balances and applied credits. Directory and membership coverage views include:

- wallet balance.
- applied credit amount.
- paid amount.
- pending amount.

## Subscription Billing

Organization subscription billing is separate from member payments.

Important services:

- `SubscriptionPricingService`: base monthly amount, included members, and overage members.
- `SubscriptionService`: order creation, status, history, and verification entry points.
- `OrganizationPaymentWebhookService`: Razorpay payment/webhook handling.
- `BillingLifecycleService`: dates and status transitions.

When payments are disabled (`GYMFLOW_PAYMENTS_ENABLED=false`), local subscription orders are marked paid immediately for development.

## Referrals And GymCoins

Organizations get referral codes. Referral signup can award GymCoins/rewards depending on platform settings. GymCoins can be applied during subscription checkout, reducing the payable amount up to the subscription amount.

## Notifications And Reminders

Notification records track messages and delivery attempts. WhatsApp reminders use:

- organization settings.
- membership status/due dates.
- delivery logs.
- webhook delivery updates.
- monthly custom reminder limits where applicable.

## Reports And Analytics

Reports pull from members, memberships, payments, notifications, usage metrics, and subscription data. Write operations that affect reports should evict `ReportCacheService` for the organization.

