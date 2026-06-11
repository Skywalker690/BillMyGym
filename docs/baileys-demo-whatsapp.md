# Demo WhatsApp Gateway

BillMyGym uses the normal Java reminder API for real organizations. Demo organization `1000` can optionally use a local Node gateway powered by Baileys so sales demos can send WhatsApp messages from a logged-in WhatsApp Web session.

Baileys is a WebSocket-based TypeScript/JavaScript library for interacting with WhatsApp Web. It authenticates by QR code or pairing code, stores WhatsApp Web auth state locally, and sends messages with `sock.sendMessage(...)`. It is not affiliated with WhatsApp, and the maintainers warn against spam or Terms of Service misuse.

## Run Locally

Requires Node 20 or newer.

```bash
cd demo-whatsapp-gateway
npm install
npm run dev
```

The gateway starts at:

```text
http://localhost:3217
```

The frontend defaults to that URL for demo workspaces. Override it with:

```text
VITE_DEMO_BAILEYS_URL=http://localhost:3217
```

## Demo Flow

1. Log in as the demo organization admin.
2. Open `Reminders`.
3. In the demo WhatsApp panel, use QR login or enter a phone number for a pairing code.
4. Set the demo workflow recipient phone number. The field is saved in local storage.
5. Choose a demo trigger mode: expired-members workflow or monthly report.
6. Click `Run Now`.

For demo workspaces only, `Run Now` calls the Node gateway instead of `/api/reminders/run`. It sends one selected workflow-style WhatsApp digest to the configured recipient phone number, mirroring either the production expired-members digest or the monthly report digest. Real organizations continue to use the existing backend workflow.

## Safety Boundary

This gateway is for local/internal demo use only. Do not use it as the production WhatsApp provider. The regular backend reminder workflow remains the production path.
