import api from "@/lib/api";
import { persistMemberPricing, toMemberPricing } from "@/lib/memberPricingCache";

export type CachedSubscriptionStatus = {
  organizationId?: number;
  organizationName?: string;
  organizationType?: "REGULAR" | "DEMO";
  organizationStatus?: "TRIAL" | "ACTIVE" | "PAUSED" | "DEACTIVATED";
  subscriptionPlan?: string;
  monthlyMembershipFee?: number | string | null;
  admissionFee?: number | string | null;
  [key: string]: unknown;
};

const SUBSCRIPTION_STATUS_STORAGE_KEY = "billmygym_subscription_status";

export function readCachedSubscriptionStatus() {
  if (!isBrowser()) {
    return null;
  }

  try {
    const serializedStatus = window.localStorage.getItem(SUBSCRIPTION_STATUS_STORAGE_KEY);
    return serializedStatus ? JSON.parse(serializedStatus) as CachedSubscriptionStatus : null;
  } catch {
    window.localStorage.removeItem(SUBSCRIPTION_STATUS_STORAGE_KEY);
    return null;
  }
}

export function persistSubscriptionStatus(status: CachedSubscriptionStatus) {
  if (!isBrowser()) {
    return;
  }

  try {
    window.localStorage.setItem(SUBSCRIPTION_STATUS_STORAGE_KEY, JSON.stringify(status));
  } catch {
    // Subscription status cache is an optimization. API guards still protect access.
  }
}

export function clearCachedSubscriptionStatus() {
  if (!isBrowser()) {
    return;
  }

  try {
    window.localStorage.removeItem(SUBSCRIPTION_STATUS_STORAGE_KEY);
  } catch {
    // Best-effort cleanup.
  }
}

export async function fetchAndCacheSubscriptionStatus() {
  const response = await api.get("/subscription/status");
  persistSubscriptionStatus(response.data);
  const memberPricing = toMemberPricing(response.data, response.data?.organizationId);
  if (memberPricing) {
    persistMemberPricing(memberPricing.organizationId, memberPricing);
  }
  return response.data as CachedSubscriptionStatus;
}

function isBrowser() {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}
