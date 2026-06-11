export type DemoBaileysStatus = {
  status: "idle" | "connecting" | "open" | "close" | "logged_out" | "error";
  qr?: string | null;
  qrDataUrl?: string | null;
  pairingCode?: string | null;
  phoneNumber?: string | null;
  lastError?: string | null;
};

export type DemoBaileysMessage = {
  memberId?: number;
  memberName?: string;
  memberCount?: number;
  phone: string;
  dueDate?: string;
  pendingAmount?: number;
  message: string;
};

export type DemoBaileysRunResponse = {
  processedMembers: number;
  organizationsGrouped: number;
  digestsSent: number;
  messagesSent: number;
  failedMessages: number;
  results: Array<{
    memberId?: number;
    memberName?: string;
    phone: string;
    sent: boolean;
    error?: string;
  }>;
};

const DEMO_BAILEYS_URL_KEY = "billmygym.demoBaileysUrl";
const DEMO_BAILEYS_RECIPIENT_KEY = "billmygym.demoBaileysRecipient";
const DEFAULT_DEMO_BAILEYS_URL = import.meta.env.VITE_DEMO_BAILEYS_URL || "http://localhost:3217";
export const DEFAULT_DEMO_BAILEYS_RECIPIENT = "9633918604";

export function readDemoBaileysUrl() {
  return readStorage(DEMO_BAILEYS_URL_KEY) || DEFAULT_DEMO_BAILEYS_URL;
}

export function persistDemoBaileysUrl(url: string) {
  writeStorage(DEMO_BAILEYS_URL_KEY, normalizeBaseUrl(url || DEFAULT_DEMO_BAILEYS_URL));
}

export function readDemoBaileysRecipient() {
  return readStorage(DEMO_BAILEYS_RECIPIENT_KEY)?.trim() || DEFAULT_DEMO_BAILEYS_RECIPIENT;
}

export function persistDemoBaileysRecipient(phone: string) {
  writeStorage(DEMO_BAILEYS_RECIPIENT_KEY, phone.trim());
}

export function getDemoBaileysStatus(baseUrl = readDemoBaileysUrl()) {
  return request<DemoBaileysStatus>(baseUrl, "/session/status");
}

export function startDemoBaileysSession(baseUrl = readDemoBaileysUrl(), phoneNumber?: string) {
  return request<DemoBaileysStatus>(baseUrl, "/session/start", {
    method: "POST",
    body: JSON.stringify({ phoneNumber: phoneNumber?.trim() || undefined }),
  });
}

export function logoutDemoBaileysSession(baseUrl = readDemoBaileysUrl()) {
  return request<DemoBaileysStatus>(baseUrl, "/session/logout", { method: "POST" });
}

export function runDemoBaileysReminders(messages: DemoBaileysMessage[], baseUrl = readDemoBaileysUrl()) {
  return request<DemoBaileysRunResponse>(baseUrl, "/demo/reminders/run", {
    method: "POST",
    body: JSON.stringify({ messages }),
  });
}

async function request<T>(baseUrl: string, path: string, init: RequestInit = {}) {
  const response = await fetch(`${normalizeBaseUrl(baseUrl)}${path}`, {
    ...init,
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      ...(init.headers ?? {}),
    },
  });
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(body?.message || body?.error || `Demo WhatsApp gateway request failed (${response.status})`);
  }
  return body as T;
}

function normalizeBaseUrl(url: string) {
  return url.trim().replace(/\/+$/, "");
}

function readStorage(key: string) {
  if (typeof window === "undefined") return null;
  try {
    return window.localStorage.getItem(key);
  } catch {
    return null;
  }
}

function writeStorage(key: string, value: string) {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(key, value);
  } catch {
    // Demo configuration should never block the main application.
  }
}
