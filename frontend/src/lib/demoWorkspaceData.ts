const INDIAN_TIME_ZONE = "Asia/Kolkata";
const DAY_MS = 86_400_000;
const DEMO_MEMBERSHIP_END_OFFSETS = [
  ...Array.from({ length: 20 }, (_, index) => index + 10),
  ...Array.from({ length: 5 }, () => 0),
  -1,
  -2,
  -3,
  -10,
  -15,
];

export const DEMO_ORGANIZATION_ID = 1000;
export const DEMO_ORGANIZATION = {
  id: DEMO_ORGANIZATION_ID,
  name: "Demo Gym Workspace",
  place: "Kochi",
  district: "Ernakulam",
  monthlyMembershipFee: 800,
  admissionFee: 300,
};

export function isDemoOrganizationContext({
  isDemoWorkspace,
  organizationId,
  organizationType,
}: {
  isDemoWorkspace?: boolean | null;
  organizationId?: number | string | null;
  organizationType?: string | null;
}) {
  return Boolean(isDemoWorkspace || organizationType === "DEMO" || Number(organizationId) === DEMO_ORGANIZATION_ID);
}

export function isoDateInIndia(date = new Date()) {
  const parts = new Intl.DateTimeFormat("en-GB", {
    timeZone: INDIAN_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(date);
  const get = (type: string) => parts.find((part) => part.type === type)?.value ?? "";
  return `${get("year")}-${get("month")}-${get("day")}`;
}

export function isoDateOffset(days: number, date = new Date()) {
  const base = new Date(`${isoDateInIndia(date)}T00:00:00+05:30`);
  base.setDate(base.getDate() + days);
  return isoDateInIndia(base);
}

function daysBetweenIso(startIso: string, endIso: string) {
  const start = new Date(`${startIso}T00:00:00+05:30`).getTime();
  const end = new Date(`${endIso}T00:00:00+05:30`).getTime();
  return Math.round((end - start) / DAY_MS);
}

function demoMembershipState(endDate: string, date = new Date()) {
  const today = isoDateInIndia(date);
  const daysUntilEnd = daysBetweenIso(today, endDate);
  const overdue = daysUntilEnd < 0;
  const expired = daysUntilEnd <= -7;
  const expiringSoon = daysUntilEnd === 0;

  return {
    daysUntilEnd,
    status: overdue ? "OVERDUE" : "ACTIVE",
    displayStatus: expired ? "EXPIRED" : expiringSoon ? "EXPIRING_SOON" : overdue ? "OVERDUE" : "ACTIVE",
    expiringSoon,
    expired,
  };
}

export function demoMembers(date = new Date()) {
  return Array.from({ length: 30 }, (_, index) => {
    const memberNumber = index + 1;
    const endOffset = DEMO_MEMBERSHIP_END_OFFSETS[index] ?? 10;
    const startOffset = endOffset - 30;
    const endDate = isoDateOffset(endOffset, date);
    const membershipState = demoMembershipState(endDate, date);
    const pendingAmount = membershipState.status === "OVERDUE" ? DEMO_ORGANIZATION.monthlyMembershipFee : 0;

    return {
      id: 10000 + memberNumber,
      fullName: `Demo Member ${String(memberNumber).padStart(2, "0")}`,
      phone: `+91987654${String(1000 + memberNumber).slice(-4)}`,
      status: "ACTIVE",
      currentMembership: {
        id: 20000 + memberNumber,
        durationMonths: 1,
        planAmount: DEMO_ORGANIZATION.monthlyMembershipFee,
        startDate: isoDateOffset(startOffset, date),
        endDate,
        status: membershipState.status,
        displayStatus: membershipState.displayStatus,
        expiringSoon: membershipState.expiringSoon,
        expired: membershipState.expired,
        daysUntilEnd: membershipState.daysUntilEnd,
        pendingAmount,
        paymentCoverage: pendingAmount > 0 ? "DUE" : "PAID",
      },
    };
  });
}

export function demoMembersPage({
  page = 0,
  size = 40,
  search = "",
  memberStatus = "ACTIVE",
  date = new Date(),
}: {
  page?: number;
  size?: number;
  search?: string;
  memberStatus?: "ALL" | "ACTIVE" | "DEACTIVATED";
  date?: Date;
}) {
  const query = search.trim().toLowerCase();
  const allMembers = demoMembers(date);
  const filtered = allMembers.filter((member) => {
    const statusMatches = memberStatus === "ALL" || member.status === memberStatus;
    const searchMatches = !query || member.fullName.toLowerCase().includes(query) || member.phone.toLowerCase().includes(query);
    return statusMatches && searchMatches;
  });
  const start = page * size;
  const members = filtered.slice(start, start + size);
  const totalPages = Math.max(Math.ceil(filtered.length / size), 1);

  return {
    members,
    summary: demoMemberSummary(date),
    page,
    size,
    totalElements: filtered.length,
    totalPages,
    first: page <= 0,
    last: page >= totalPages - 1,
  };
}

export function demoMemberSummary(date = new Date()) {
  const members = demoMembers(date);
  const memberships = members.map((member) => member.currentMembership);
  const expiringSoonMemberships = memberships.filter((membership) => membership.expiringSoon).length;
  const expiredMemberships = memberships.filter((membership) => membership.expired).length;
  const overdueMemberships = memberships.filter((membership) => membership.status === "OVERDUE" && !membership.expired).length;
  const activeMemberships = memberships.filter((membership) => membership.status === "ACTIVE").length;

  return {
    totalMembers: members.length,
    activeMembers: members.filter((member) => member.status === "ACTIVE").length,
    deactivatedMembers: 0,
    activeMemberships,
    expiringSoonMemberships,
    expiredMemberships,
    overdueMemberships,
    memberLimit: {
      usedMembers: 30,
      memberLimit: null,
      unlimited: true,
      remainingSlots: null,
      usagePercent: 0,
      plan: "DEMO",
    },
  };
}

// Month-profile seeds (non-current months): percentages relative to a base
// so scaling is realistic and the current month is always dynamically computed.
const PAST_MONTH_SEEDS = [
  { activeMembersFraction: 0.60, admissions: 12, overdueMembers: 1, deactivatedMembers: 0 },
  { activeMembersFraction: 0.67, admissions: 15, overdueMembers: 2, deactivatedMembers: 0 },
  { activeMembersFraction: 0.73, admissions: 18, overdueMembers: 2, deactivatedMembers: 1 },
  { activeMembersFraction: 0.77, admissions: 20, overdueMembers: 3, deactivatedMembers: 0 },
  { activeMembersFraction: 0.80, admissions: 24, overdueMembers: 4, deactivatedMembers: 0 },
];

export function demoFinancialReport(date = new Date()) {
  const current = new Date(`${isoDateInIndia(date)}T00:00:00+05:30`);
  const summary = demoMemberSummary(date);

  // Current month — derived from real live demo state
  const currentActiveMembers = summary.activeMemberships;
  const currentOverdueMembers = summary.overdueMemberships + summary.expiredMemberships;
  const currentAdmissions = 30;
  const currentMembershipRevenue = (currentActiveMembers - currentOverdueMembers) * DEMO_ORGANIZATION.monthlyMembershipFee;
  const currentAdmissionRevenue = currentAdmissions * DEMO_ORGANIZATION.admissionFee;
  const currentDuePayments = currentOverdueMembers * DEMO_ORGANIZATION.monthlyMembershipFee;

  const months = PAST_MONTH_SEEDS.map((seed, index) => {
    const monthDate = new Date(current.getFullYear(), current.getMonth() - (PAST_MONTH_SEEDS.length - index), 1);
    const activeMembers = Math.round(30 * seed.activeMembersFraction);
    const membershipRevenue = (activeMembers - seed.overdueMembers) * DEMO_ORGANIZATION.monthlyMembershipFee;
    const admissionRevenue = seed.admissions * DEMO_ORGANIZATION.admissionFee;
    return {
      year: monthDate.getFullYear(),
      month: monthDate.getMonth() + 1,
      label: new Intl.DateTimeFormat("en-IN", { month: "short", year: "numeric", timeZone: INDIAN_TIME_ZONE }).format(monthDate),
      totalRevenue: admissionRevenue + membershipRevenue,
      admissionRevenue,
      membershipRevenue,
      duePayments: 0,
      expenses: 0,
      admissions: seed.admissions,
      activeMembers,
      overdueMembers: seed.overdueMembers,
      deactivatedMembers: seed.deactivatedMembers,
    };
  });

  // Add current month
  months.push({
    year: current.getFullYear(),
    month: current.getMonth() + 1,
    label: new Intl.DateTimeFormat("en-IN", { month: "short", year: "numeric", timeZone: INDIAN_TIME_ZONE }).format(current),
    totalRevenue: currentAdmissionRevenue + currentMembershipRevenue,
    admissionRevenue: currentAdmissionRevenue,
    membershipRevenue: currentMembershipRevenue,
    duePayments: currentDuePayments,
    expenses: 0,
    admissions: currentAdmissions,
    activeMembers: currentActiveMembers,
    overdueMembers: currentOverdueMembers,
    deactivatedMembers: 0,
  });

  const firstMonth = months[0];
  const latestMonth = months[months.length - 1];

  return {
    period: "monthly",
    range: "demo_six_months",
    startDate: `${firstMonth.year}-${String(firstMonth.month).padStart(2, "0")}-01`,
    endDate: isoDateInIndia(date),
    organizationName: DEMO_ORGANIZATION.name,
    logoUrl: null,
    planName: "DEMO",
    planAccess: {
      canReceiveMonthlyReports: true,
      canExportReports: true,
    },
    summary: {
      totalRevenue: months.reduce((total, month) => total + month.totalRevenue, 0),
      admissionRevenue: months.reduce((total, month) => total + month.admissionRevenue, 0),
      membershipRevenue: months.reduce((total, month) => total + month.membershipRevenue, 0),
      duePayments: months.reduce((total, month) => total + month.duePayments, 0),
      expenses: months.reduce((total, month) => total + month.expenses, 0),
      admissions: months.reduce((total, month) => total + month.admissions, 0),
      activeMembers: latestMonth.activeMembers,
      overdueMembers: latestMonth.overdueMembers,
      deactivatedMembers: months.reduce((total, month) => total + month.deactivatedMembers, 0),
    },
    months,
  };
}

export function demoReminderReport(date = new Date()) {
  const reminderCount = demoReminderCandidates(date).length;

  return {
    settings: {
      whatsappEnabled: true,
      emailEnabled: false,
      expiryReminderDays: 2,
    },
    manualQuota: {
      usedThisMonth: 2,
      monthlyLimit: null,
      unlimited: true,
      canRun: true,
    },
    recentReminderLogs: [
      {
        id: 30001,
        channel: "WHATSAPP",
        status: "SENT",
        memberCount: reminderCount,
        sentAt: `${isoDateOffset(-2, date)}T10:00:00+05:30`,
      },
      {
        id: 30002,
        channel: "WHATSAPP",
        status: "SENT",
        memberCount: 3,
        sentAt: `${isoDateOffset(-4, date)}T10:00:00+05:30`,
      },
    ],
  };
}

export function demoReminderPreview(member: any) {
  const membership = member?.currentMembership ?? {};
  const pendingAmount = Number(membership.pendingAmount ?? DEMO_ORGANIZATION.monthlyMembershipFee);
  return {
    canSend: Boolean(member?.phone),
    disabledReason: member?.phone ? null : "This demo member does not have a phone number.",
    memberName: member?.fullName ?? "Demo Member",
    phone: member?.phone ?? "",
    gymName: DEMO_ORGANIZATION.name,
    dueDate: membership.endDate ?? isoDateOffset(-1),
    pendingAmount,
    monthlyCustomReminderUsed: 2,
    monthlyCustomReminderLimit: 50,
    monthlyCustomReminderRemaining: 48,
    message: `Hi ${member?.fullName ?? "Demo Member"}, your ${DEMO_ORGANIZATION.name} membership is overdue. Pending amount: INR ${pendingAmount.toLocaleString("en-IN")}. Please renew to continue your training.`,
  };
}

export function demoReminderCandidates(date = new Date()) {
  return demoMembers(date).filter((member) => {
    const membership = member.currentMembership;
    return member.status === "ACTIVE" && membership.status === "OVERDUE" && membership.daysUntilEnd < 0;
  });
}

export function demoReminderMessages(date = new Date(), overridePhone?: string) {
  return demoReminderCandidates(date).map((member) => {
    const preview = demoReminderPreview(member);
    return {
      memberId: member.id,
      memberName: member.fullName,
      phone: overridePhone?.trim() || member.phone,
      dueDate: preview.dueDate,
      pendingAmount: preview.pendingAmount,
      message: preview.message,
    };
  });
}

export function demoWorkflowDigestMessage(date = new Date(), recipientPhone = "") {
  const candidates = demoReminderCandidates(date);
  const memberList = candidates
    .map((member, index) => {
      const overdueDays = Math.max(1, Math.abs(Number(member.currentMembership.daysUntilEnd ?? 0)));
      return `${index + 1}. ${member.fullName} | overdue by ${overdueDays} day${overdueDays === 1 ? "" : "s"}`;
    })
    .join("\n");

  return [{
    phone: recipientPhone.trim(),
    memberName: DEMO_ORGANIZATION.name,
    memberCount: candidates.length,
    message: `BillMyGym Alert\n\nExpired memberships needing follow-up:\n\n${memberList}\n\nTotal expired memberships: ${candidates.length}`,
  }];
}

export function demoMonthlyReportDigestMessage(date = new Date(), recipientPhone = "") {
  const report = demoFinancialReport(date);
  const latestMonth = report.months.at(-1) ?? {
    admissions: 0,
    activeMembers: 0,
    overdueMembers: 0,
    totalRevenue: 0,
    duePayments: 0,
  };
  const money = new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  });
  const reportMonth = new Intl.DateTimeFormat("en-IN", {
    month: "long",
    year: "numeric",
    timeZone: INDIAN_TIME_ZONE,
  }).format(new Date(`${isoDateInIndia(date)}T00:00:00+05:30`));

  return [{
    phone: recipientPhone.trim(),
    memberName: DEMO_ORGANIZATION.name,
    memberCount: latestMonth.activeMembers,
    message: [
      `BillMyGym Monthly Report | ${reportMonth}`,
      "",
      `New members: ${latestMonth.admissions}`,
      `Active members: ${latestMonth.activeMembers}`,
      `Collected revenue: ${money.format(latestMonth.totalRevenue)}`
    ].join("\n"),
  }];
}

export function demoDashboardReport(date = new Date()) {
  const monthOffsets = [-5, -4, -3, -2, -1, 0];
  // Joined members per month — scaled to match PAST_MONTH_SEEDS admissions
  const joinedMembers = [...PAST_MONTH_SEEDS.map((s) => s.admissions), 30];
  const summary = demoMemberSummary(date);
  const members = demoMembers(date);
  const expiringSoon = members.filter((member) => member.currentMembership.expiringSoon).map(toDashboardMember);
  const overdue = members
    .filter((member) => member.currentMembership.status === "OVERDUE")
    .map(toDashboardMember);

  // Derive current month revenue from live demo state
  const activeMembers = summary.activeMemberships;
  const overdueMembers = summary.overdueMemberships + summary.expiredMemberships;
  const paidMembers = activeMembers - overdueMembers;
  const admissionsThisMonth = 30;

  const currentRevenue =
    paidMembers * DEMO_ORGANIZATION.monthlyMembershipFee +
    admissionsThisMonth * DEMO_ORGANIZATION.admissionFee;
  const overdueRevenue = overdueMembers * DEMO_ORGANIZATION.monthlyMembershipFee;
  const expectedRevenue = currentRevenue + overdueRevenue;

  const activeMembersPendingCollection = overdueMembers;
  const activeMembersCollected = Math.max(activeMembers - activeMembersPendingCollection, 0);

  return {
    planName: "BILLMYGYM",
    revenue: {
      currentRevenue,
      pendingRevenue: 0,
      expectedRevenue,
      overdueRevenue,
      activeMembers,
      prepaidMembers: 0,
      activeMembersCollected,
      activeMembersPendingCollection,
    },
    revenueSegmentation: {
      admissionsThisMonth,
      admissionRevenue: admissionsThisMonth * DEMO_ORGANIZATION.admissionFee,
      recurringRevenue: paidMembers * DEMO_ORGANIZATION.monthlyMembershipFee,
      totalRevenue: currentRevenue,
    },
    memberSummary: summary,
    monthlyMemberJoins: monthOffsets.map((offset, index) => {
      const monthDate = new Date(date);
      monthDate.setMonth(monthDate.getMonth() + offset);
      const year = new Intl.DateTimeFormat("en-CA", { year: "numeric", timeZone: INDIAN_TIME_ZONE }).format(monthDate);
      const month = new Intl.DateTimeFormat("en-CA", { month: "2-digit", timeZone: INDIAN_TIME_ZONE }).format(monthDate);
      const label = new Intl.DateTimeFormat("en-IN", { month: "short", timeZone: INDIAN_TIME_ZONE }).format(monthDate);
      return {
        month: `${year}-${month}`,
        label,
        joinedMembers: joinedMembers[index] ?? 0,
      };
    }),
    planAccess: {
      canCreateMembers: true,
      canViewAdvancedAnalytics: true,
      canReceiveMonthlyReports: true,
      canExportReports: true,
      canUsePremiumFeatures: true,
    },
    subscriptionValidity: {
      label: "Demo access",
      daysRemaining: 30,
      tooltipLabel: "Demo valid through",
      tooltipDate: isoDateOffset(30, date),
    },
    subscriptionPricing: {
      baseAmount: 0,
      totalAmount: 0,
      activeMembers: summary.activeMemberships,
      includedMembers: null,
      overageMembers: 0,
      overageRate: 0,
      overageAmount: 0,
    },
    expiringSoon,
    overdue,
  };
}

function toDashboardMember(member: ReturnType<typeof demoMembers>[number]) {
  return {
    id: member.id,
    fullName: member.fullName,
    phone: member.phone,
    currentMembership: {
      durationMonths: member.currentMembership.durationMonths,
      endDate: member.currentMembership.endDate,
      status: member.currentMembership.displayStatus,
    },
  };
}
