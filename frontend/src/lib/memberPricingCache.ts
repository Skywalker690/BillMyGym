export type MemberPricing = {
  organizationId?: number | null;
  monthlyMembershipFee: number;
  admissionFee: number;
};

const MEMBER_PRICING_CACHE_PREFIX = "billmygym_member_pricing:";

export function toMemberPricing(value: unknown, organizationId?: number | null): MemberPricing | null {
  if (!value || typeof value !== "object") {
    return null;
  }

  const source = value as {
    organizationId?: number | string | null;
    monthlyMembershipFee?: number | string | null;
    admissionFee?: number | string | null;
  };
  const sourceOrganizationId = source.organizationId == null ? organizationId : Number(source.organizationId);
  if (organizationId && Number.isFinite(sourceOrganizationId) && sourceOrganizationId !== organizationId) {
    return null;
  }

  const monthlyMembershipFee = Number(source.monthlyMembershipFee);
  const admissionFee = Number(source.admissionFee ?? 0);
  if (!Number.isFinite(monthlyMembershipFee) || monthlyMembershipFee <= 0 || !Number.isFinite(admissionFee) || admissionFee < 0) {
    return null;
  }

  return {
    organizationId: organizationId ?? (Number.isFinite(sourceOrganizationId) ? sourceOrganizationId : null),
    monthlyMembershipFee,
    admissionFee,
  };
}

export function readCachedMemberPricing(organizationId?: number | null): MemberPricing | null {
  if (!isBrowser()) {
    return null;
  }

  try {
    return toMemberPricing(
      JSON.parse(window.localStorage.getItem(memberPricingCacheKey(organizationId)) ?? "null"),
      organizationId
    );
  } catch {
    window.localStorage.removeItem(memberPricingCacheKey(organizationId));
    return null;
  }
}

export function persistMemberPricing(organizationId: number | null | undefined, pricing: MemberPricing) {
  if (!isBrowser()) {
    return;
  }

  try {
    window.localStorage.setItem(memberPricingCacheKey(organizationId), JSON.stringify(pricing));
  } catch {
    // Last-known-good pricing is only a resilience cache.
  }
}

export function clearMemberPricingCache() {
  if (!isBrowser()) {
    return;
  }

  try {
    for (let index = window.localStorage.length - 1; index >= 0; index -= 1) {
      const key = window.localStorage.key(index);
      if (key?.startsWith(MEMBER_PRICING_CACHE_PREFIX)) {
        window.localStorage.removeItem(key);
      }
    }
  } catch {
    // Best-effort cleanup.
  }
}

function memberPricingCacheKey(organizationId?: number | null) {
  return `${MEMBER_PRICING_CACHE_PREFIX}${organizationId ?? "current"}`;
}

function isBrowser() {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}
