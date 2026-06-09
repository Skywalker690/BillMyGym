# API Surface

All API routes are under `/api` unless noted.

## Public Auth And Webhooks

- `POST /auth/signup`: public organization admin signup.
- `POST /auth/login`: password login.
- `POST /auth/google-login`: Google login.
- `GET /auth/legal-policy`: current legal policy metadata.
- `POST /auth/forgot-password`: request password reset OTP.
- `POST /auth/verify-otp`: verify reset OTP.
- `POST /auth/change-password`: reset password using OTP.
- `POST /subscription/webhook`: Razorpay webhook.
- `GET /whatsapp/webhook`: WhatsApp webhook verification.
- `POST /whatsapp/webhook`: WhatsApp webhook events.

## Authenticated Auth

- `POST /auth/register`: super-admin creates organization admin.
- `POST /auth/logout`: client logout response.
- `GET /auth/me`: current user.
- `POST /auth/consent`: accept terms/privacy policy.
- `POST /auth/password/request-otp`: authenticated password update OTP.
- `POST /auth/password/confirm`: authenticated password update confirmation.

## Organizations

- `GET /organizations`: list organizations or current organization depending role/service behavior.
- `GET /organizations/directory`: super-admin organization directory.
- `GET /organizations/{id}`: organization detail.
- `PATCH /organizations/{id}`: update organization.
- `PUT /organizations/{id}/admin`: super-admin update admin.
- `POST /organizations/{id}/members/bulk-upload`: super-admin CSV bulk member import.
- `DELETE /organizations/{id}`: super-admin soft delete organization.

## Members And Memberships

- `POST /members`: create org member.
- `GET /members`: paginated/searchable member directory.
- `GET /members/{id}`: member detail.
- `PATCH /members/{id}`: update member.
- `DELETE /members/{id}`: soft delete member.
- `GET /members/{id}/whatsapp-reminder/preview`: reminder preview.
- `POST /members/{id}/whatsapp-reminder/send`: send individual reminder.
- `POST /members/{memberId}/memberships`: create membership.
- `GET /members/{memberId}/memberships`: list memberships for member.

## Payments

- `POST /payments`: record member payment. Use `X-Idempotency-Key`.
- `GET /payments`: list member payments.

## Reports And Analytics

- `GET /reports/dashboard`
- `GET /reports/reminders`
- `GET /reports/financial`
- `GET /analytics/platform`
- `GET /analytics/platform/reports`
- `GET /analytics/organization`
- `GET /analytics/revenue`
- `GET /analytics/revenue/monthly`
- `GET /analytics/revenue/segmentation`
- `GET /tracking/export`

## Reminders And Notifications

- `POST /reminders/run`
- `GET /reminders/logs`
- `GET /notifications`

## Subscription

- `POST /subscription/create`: create checkout/order.
- `POST /subscription/verify`: verify client payment.
- `GET /subscription/status`: current organization status.
- `GET /subscription/payments`: current organization payment history.
- `GET /subscription/organizations/{organizationId}/status`: super-admin organization status.
- `GET /subscription/organizations/{organizationId}/payments`: super-admin organization payment history.

## Referrals

- `GET /referrals/dashboard`: org-admin referral dashboard.
- `GET /referrals/analytics`: super-admin referral analytics.
- `GET /referrals/settings`: referral platform settings.
- `POST /referrals/settings/toggle`: super-admin toggle referrals.

## Settings And Audit Logs

- `GET /settings`
- `PATCH /settings`
- `GET /audit-logs/login`
- `GET /audit-logs/login/export`
- `GET /audit-logs/subscriptions`
- `GET /organizations/{organizationId}/audit-logs/member-activity`
- `GET /organizations/{organizationId}/audit-logs/member-status`
- `GET /organizations/{organizationId}/audit-logs/admin-activity`
- `GET /organizations/{organizationId}/audit-logs/notification-activity`

