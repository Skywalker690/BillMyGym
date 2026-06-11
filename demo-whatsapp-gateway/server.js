import express from "express";
import cors from "cors";
import Pino from "pino";
import QRCode from "qrcode";
import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import makeWASocket, {
  Browsers,
  DisconnectReason,
  useMultiFileAuthState,
} from "@whiskeysockets/baileys";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PORT = Number(process.env.DEMO_BAILEYS_PORT ?? 3217);
const AUTH_DIR = process.env.DEMO_BAILEYS_AUTH_DIR || path.join(__dirname, ".demo-baileys-auth");
const DEFAULT_ORIGINS = [
  "http://localhost:5173",
  "http://127.0.0.1:5173",
  "http://localhost:4173",
  "http://127.0.0.1:4173",
];
const ALLOWED_ORIGINS = (process.env.DEMO_BAILEYS_CORS_ORIGIN || DEFAULT_ORIGINS.join(","))
  .split(",")
  .map((origin) => origin.trim())
  .filter(Boolean);

const app = express();
const logger = Pino({ level: process.env.DEMO_BAILEYS_LOG_LEVEL || "silent" });

let sock = null;
let status = "idle";
let latestQr = null;
let latestQrDataUrl = null;
let latestPairingCode = null;
let connectedPhoneNumber = null;
let lastError = null;
let startPromise = null;

app.use(cors({
  origin(origin, callback) {
    if (!origin || ALLOWED_ORIGINS.includes(origin)) {
      callback(null, true);
      return;
    }
    callback(new Error(`Origin ${origin} is not allowed by DEMO_BAILEYS_CORS_ORIGIN`));
  },
}));
app.use(express.json({ limit: "256kb" }));

app.get("/health", (_req, res) => {
  res.json({ ok: true, status });
});

app.get("/session/status", (_req, res) => {
  res.json(sessionStatus());
});

app.post("/session/start", async (req, res) => {
  try {
    const result = await startSession(req.body?.phoneNumber);
    res.json(result);
  } catch (error) {
    status = "error";
    lastError = error instanceof Error ? error.message : "Unable to start demo WhatsApp session";
    res.status(500).json({ message: lastError, ...sessionStatus() });
  }
});

app.post("/session/logout", async (_req, res) => {
  try {
    if (sock) {
      await sock.logout().catch(() => undefined);
      sock.end?.(undefined);
    }
    sock = null;
    await fs.rm(AUTH_DIR, { recursive: true, force: true });
    status = "logged_out";
    latestQr = null;
    latestQrDataUrl = null;
    latestPairingCode = null;
    connectedPhoneNumber = null;
    lastError = null;
    res.json(sessionStatus());
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unable to logout demo WhatsApp session";
    res.status(500).json({ message, ...sessionStatus() });
  }
});

app.post("/demo/reminders/run", async (req, res) => {
  if (!sock || status !== "open") {
    res.status(409).json({ message: "Demo WhatsApp is not connected. Start the gateway session and scan the QR first." });
    return;
  }

  const messages = Array.isArray(req.body?.messages) ? req.body.messages : [];
  const results = [];
  const processedMemberCount = messages.reduce((total, item) => {
    const memberCount = Number(item?.memberCount ?? 1);
    return total + (Number.isFinite(memberCount) && memberCount > 0 ? memberCount : 1);
  }, 0);

  for (const item of messages) {
    const phone = normalizePhone(item?.phone);
    if (!phone) {
      results.push({
        memberId: item?.memberId,
        memberName: item?.memberName,
        phone: item?.phone ?? "",
        sent: false,
        error: "Invalid phone number",
      });
      continue;
    }

    try {
      await sock.sendMessage(`${phone}@s.whatsapp.net`, { text: String(item?.message ?? "") });
      results.push({
        memberId: item?.memberId,
        memberName: item?.memberName,
        phone,
        sent: true,
      });
      await delay(350);
    } catch (error) {
      results.push({
        memberId: item?.memberId,
        memberName: item?.memberName,
        phone,
        sent: false,
        error: error instanceof Error ? error.message : "Send failed",
      });
    }
  }

  const messagesSent = results.filter((result) => result.sent).length;
  res.json({
    processedMembers: processedMemberCount,
    organizationsGrouped: messages.length > 0 ? 1 : 0,
    digestsSent: messagesSent > 0 ? 1 : 0,
    messagesSent,
    failedMessages: results.length - messagesSent,
    results,
  });
});

app.listen(PORT, () => {
  console.log(`BillMyGym demo WhatsApp gateway running on http://localhost:${PORT}`);
});

async function startSession(phoneNumber) {
  if (sock && status === "open") {
    return sessionStatus();
  }
  if (startPromise) {
    await startPromise;
    return sessionStatus();
  }

  startPromise = connect(phoneNumber);
  try {
    await startPromise;
    return sessionStatus();
  } finally {
    startPromise = null;
  }
}

async function connect(phoneNumber) {
  status = "connecting";
  lastError = null;
  latestPairingCode = null;

  const { state, saveCreds } = await useMultiFileAuthState(AUTH_DIR);
  sock = makeWASocket({
    auth: state,
    browser: Browsers.ubuntu("BillMyGym Demo"),
    logger,
    markOnlineOnConnect: false,
    printQRInTerminal: false,
  });

  sock.ev.on("creds.update", saveCreds);
  sock.ev.on("connection.update", async (update) => {
    if (update.qr) {
      latestQr = update.qr;
      latestQrDataUrl = await QRCode.toDataURL(update.qr);
      status = "connecting";
    }

    if (update.connection === "open") {
      status = "open";
      latestQr = null;
      latestQrDataUrl = null;
      latestPairingCode = null;
      connectedPhoneNumber = sock?.user?.id ?? null;
    }

    if (update.connection === "close") {
      const code = update.lastDisconnect?.error?.output?.statusCode;
      const loggedOut = code === DisconnectReason.loggedOut;
      status = loggedOut ? "logged_out" : "close";
      lastError = update.lastDisconnect?.error?.message ?? null;
      sock = null;
      if (!loggedOut) {
        setTimeout(() => {
          connect().catch((error) => {
            status = "error";
            lastError = error instanceof Error ? error.message : "Reconnect failed";
          });
        }, 1500);
      }
    }
  });

  const normalizedPhone = normalizePhone(phoneNumber);
  if (normalizedPhone && !state.creds.registered) {
    latestPairingCode = await sock.requestPairingCode(normalizedPhone);
  }
}

function sessionStatus() {
  return {
    status,
    qr: latestQr,
    qrDataUrl: latestQrDataUrl,
    pairingCode: latestPairingCode,
    phoneNumber: connectedPhoneNumber,
    lastError,
  };
}

function normalizePhone(value) {
  const digits = String(value ?? "").replace(/\D/g, "");
  if (!digits || digits.length < 8) {
    return "";
  }
  return digits;
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
