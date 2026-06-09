# Email Deliverability

BillMyGym sends transactional email from the backend `EmailService`.

## Required Production Setup

The app only requires one mailbox and one app password:

- `MAIL_ID=your-sender-email`
- `APP_PASS=your-sender-app-password`
- `MAIL_FROM_NAME=BillMyGym`

The same email address is used for From, Reply-To, support contact in the footer, and SMTP envelope sender. If someone replies, the reply goes to the same mailbox.

For best inbox placement, use a sender address on the product domain, for example `no-reply@billmygym.tech` or `support@billmygym.tech`. Sending production mail from a generic Gmail address while links and branding point to `billmygym.tech` commonly causes spam placement.

## DNS Records

Configure these DNS records with the mail provider that sends BillMyGym email:

- SPF: authorize the actual SMTP provider for `billmygym.tech`.
- DKIM: enable provider DKIM signing and publish the provider-supplied DKIM TXT or CNAME records.
- DMARC: publish a DMARC policy for `_dmarc.billmygym.tech`.
- MX: required if the sender mailbox receives replies.

Example DMARC ramp:

```txt
v=DMARC1; p=none; rua=mailto:dmarc@billmygym.tech; adkim=s; aspf=s
```

After SPF and DKIM are passing consistently, move from `p=none` to `p=quarantine`, then `p=reject`.

## Validation

Before production use, send test emails to Gmail and Outlook and verify:

- SPF passes.
- DKIM passes.
- DMARC passes with aligned From domain.
- The visible From address is the mailbox configured in `MAIL_ID`.
- The app links use `https://www.billmygym.tech`.

Inbox placement is not guaranteed by application code. It depends on domain authentication, sending provider reputation, message volume, bounce rate, user complaints, and recipient mailbox rules.
