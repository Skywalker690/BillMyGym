import { useQuery } from "@tanstack/react-query";
import {
  AlertCircle,
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  Clock3,
  Crown,
  Info,
  MessageCircleMore,
  Plus,
  TrendingUp,
  Users,
  Wallet,
} from "lucide-react";
import { Link } from "react-router-dom";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import api from "@/lib/api";
import { cn } from "@/lib/utils";
import { useDemoPlanPreview } from "@/context/DemoPlanPreviewContext";

type ReminderLog = {
  id: number;
  channel: string;
  memberCount?: number;
  status?: string;
  sentAt?: string;
  createdAt?: string;
};

type MembershipSnapshot = {
  id: number;
  fullName: string;
  phone?: string;
  currentMembership?: {
    durationMonths?: number;
    endDate?: string;
    status?: string;
  } | null;
};

type MonthlyMemberJoin = {
  month: string;
  label: string;
  joinedMembers: number;
};

type DashboardReport = {
  planName?: string;
  revenue?: {
    currentRevenue?: number;
    pendingRevenue?: number;
    expectedRevenue?: number;
    overdueRevenue?: number;
    activeMembers?: number;
    prepaidMembers?: number;
    activeMembersCollected?: number;
    activeMembersPendingCollection?: number;
  };
  revenueSegmentation?: {
    admissionsThisMonth?: number;
    admissionRevenue?: number;
    recurringRevenue?: number;
    totalRevenue?: number;
  };
  memberSummary?: {
    totalMembers?: number;
    activeMembers?: number;
    deactivatedMembers?: number;
    activeMemberships?: number;
    expiringSoonMemberships?: number;
    expiredMemberships?: number;
    overdueMemberships?: number;
  };
  monthlyMemberJoins?: MonthlyMemberJoin[];
  planAccess?: {
    canCreateMembers?: boolean;
    canViewAdvancedAnalytics?: boolean;
    canReceiveMonthlyReports?: boolean;
    canExportReports?: boolean;
    canUsePremiumFeatures?: boolean;
  };
  subscriptionValidity?: {
    label?: string;
    daysRemaining?: number;
    tooltipLabel?: string;
    tooltipDate?: string;
  };
  subscriptionPricing?: {
    baseAmount?: number;
    totalAmount?: number;
    activeMembers?: number;
    includedMembers?: number | null;
    overageMembers?: number;
    overageRate?: number;
    overageAmount?: number;
  };
  expiringSoon?: MembershipSnapshot[];
  overdue?: MembershipSnapshot[];
};

type SubscriptionStatusSnapshot = {
  organizationType?: "REGULAR" | "DEMO";
  organizationName?: string;
};

const INDIAN_TIME_ZONE = "Asia/Kolkata";

const money = (value: number | string | undefined) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(Number(value ?? 0));

const shortDate = (value?: string) =>
  value
    ? new Intl.DateTimeFormat("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        timeZone: INDIAN_TIME_ZONE,
      }).format(new Date(`${value}T00:00:00+05:30`))
    : "-";

const shortDateTime = (value?: string) =>
  value
    ? new Intl.DateTimeFormat("en-IN", {
        day: "2-digit",
        month: "short",
        hour: "2-digit",
        minute: "2-digit",
        timeZone: INDIAN_TIME_ZONE,
      }).format(new Date(value))
    : "-";

export default function Dashboard() {
  const { effectivePlan, isDemoWorkspace } = useDemoPlanPreview();
  const { data: report, isLoading } = useQuery<DashboardReport>({
    queryKey: ["dashboardReport"],
    queryFn: async () => {
      const res = await api.get("/reports/dashboard");
      return res.data;
    },
  });

  const { data: subscriptionStatus } = useQuery<SubscriptionStatusSnapshot>({
    queryKey: ["subscriptionStatus", "dashboard-demo-flag"],
    queryFn: async () => {
      const res = await api.get("/subscription/status");
      return res.data;
    },
  });

  if (isLoading) {
    return <div className="p-8">Loading dashboard...</div>;
  }

  const revenue = report?.revenue ?? {};
  const revenueSegmentation = report?.revenueSegmentation ?? {};
  const summary = report?.memberSummary ?? {};
  const expiringSoon = report?.expiringSoon ?? [];
  const overdue = report?.overdue ?? [];
  const monthlyMemberJoins = report?.monthlyMemberJoins ?? [];
  const joinedThisMonth = monthlyMemberJoins.at(-1)?.joinedMembers ?? 0;
  const isDemoGym = subscriptionStatus?.organizationType === "DEMO";
  const subscriptionValidity = report?.subscriptionValidity;
  const subscriptionPricing = report?.subscriptionPricing;

  const activeMembersTotal = Number(revenue.activeMembers ?? summary.activeMembers ?? 0);
  const activeMembersPendingCollection = Number(revenue.activeMembersPendingCollection ?? 0);
  const activeMembersCollected = Math.max(
    0,
    Number(revenue.activeMembersCollected ?? activeMembersTotal - activeMembersPendingCollection)
  );
  const activeCollectionRate =
    activeMembersTotal > 0 ? Math.round((activeMembersCollected / activeMembersTotal) * 100) : 0;
  const collectionRate =
    Number(revenue.expectedRevenue ?? 0) > 0
      ? Math.round((Number(revenue.currentRevenue ?? 0) / Number(revenue.expectedRevenue ?? 0)) * 100)
      : 0;
  const atRiskMembers = Number(summary.expiringSoonMemberships ?? 0) + Number(summary.overdueMemberships ?? 0);
  const subscriptionDaysRemaining = Number(subscriptionValidity?.daysRemaining ?? 0);
  const subscriptionDaysText =
    subscriptionDaysRemaining === 1
      ? "1 day"
      : `${subscriptionDaysRemaining} days`;
  const subscriptionTooltipLabel = subscriptionValidity?.tooltipLabel;
  const subscriptionTooltipDate = subscriptionValidity?.tooltipDate;
  const subscriptionTooltipText = buildSubscriptionTooltip(subscriptionTooltipLabel, subscriptionTooltipDate);

  const statCards = [
    {
      title: "Admissions This Month",
      value: `${revenueSegmentation.admissionsThisMonth ?? 0} Members`,
      eyebrow: money(revenueSegmentation.admissionRevenue),
      icon: Users,
      panel: "from-emerald-500/18 via-emerald-400/8 to-transparent",
      badge: "bg-emerald-100 text-emerald-800 border-emerald-200",
    },
    {
      title: "Monthly Revenue",
      value: money(revenueSegmentation.recurringRevenue),
      eyebrow: "Membership",
      icon: TrendingUp,
      panel: "from-slate-900/10 via-slate-500/5 to-transparent",
      badge: "bg-slate-100 text-slate-700 border-slate-200",
    },
    {
      title: "Total Revenue",
      value: money(revenueSegmentation.totalRevenue),
      eyebrow: "Admission + monthly",
      icon: Wallet,
      panel: "from-amber-500/20 via-amber-300/8 to-transparent",
      badge: "bg-amber-100 text-amber-800 border-amber-200",
    },
    {
      title: "Overdue Exposure",
      value: money(revenue.overdueRevenue),
      eyebrow: `${summary.overdueMemberships ?? 0} overdue`,
      icon: AlertTriangle,
      panel: "from-rose-500/18 via-rose-300/8 to-transparent",
      badge: "bg-rose-100 text-rose-800 border-rose-200",
    },
  ];

  return (
    <div className="mx-auto max-w-7xl space-y-5 px-3 pb-8 pt-3 sm:space-y-6 sm:p-6 lg:space-y-8 lg:p-8">
      {isDemoGym && <DemoWorkspaceBanner organizationName={subscriptionStatus?.organizationName} previewPlan={effectivePlan} />}
      <section className="relative overflow-hidden rounded-[1.8rem] bg-white border border-slate-200 shadow-sm p-5 sm:rounded-[2rem] sm:p-7 lg:p-10">
        <div className="absolute inset-y-0 right-0 w-[50%] bg-[radial-gradient(ellipse_at_top_right,rgba(22,163,74,0.08),transparent_50%),radial-gradient(ellipse_at_bottom_right,rgba(245,158,11,0.06),transparent_50%)]" />
        <div className="relative z-10 grid gap-8 lg:grid-cols-[1.2fr_0.8fr] lg:items-center">
          <div>
            <div className="mb-4 flex flex-wrap items-center gap-2">
              <Badge className="border-emerald-200 bg-emerald-100 text-emerald-800">Revenue OS</Badge>
              <Badge className="hidden border-slate-200 bg-white/80 text-slate-700 sm:inline-flex">Live backend snapshot</Badge>
              <Badge className="border-emerald-200 bg-emerald-100 text-emerald-800">BillMyGym: All features included</Badge>
            </div>
            <h1 className="max-w-3xl text-3xl font-black tracking-tight text-slate-950 sm:text-4xl lg:text-5xl">
              Run your gym like a premium membership SaaS, not a spreadsheet.
            </h1>
            <p className="mt-3 max-w-2xl text-sm font-medium leading-6 text-slate-600 sm:mt-4 sm:text-base sm:leading-7">
              Collections, upcoming dues, and reminder health are surfaced in one operator view so you can protect revenue before memberships slip into overdue territory.
            </p>

            <div className="mt-5 flex flex-wrap gap-3 sm:mt-7">
              <Link to="/app/members/new">
                <Button className="h-12 rounded-full bg-slate-950 px-6 font-bold text-white hover:bg-slate-800">
                  <Plus className="mr-2 h-4 w-4" />
                  Add Member
                </Button>
              </Link>
              <Link to="/app/reminders">
                <Button variant="outline" className="h-12 rounded-full border-white/80 bg-white/75 px-6 font-bold text-slate-800 hover:bg-white">
                  Open Reminders
                  <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
              </Link>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-2 sm:gap-4">
            <HeroMetric
              label="Collection Pace"
              value={`${collectionRate}%`}
              tone="emerald"
              detail="Current revenue vs expected revenue"
            />
            <HeroMetric
              label="At-Risk Members"
              value={atRiskMembers}
              tone="amber"
              detail="Due today plus overdue"
            />
            <HeroMetric
              label="Active Members"
              value={revenue.activeMembers ?? 0}
              tone="slate"
              detail="Members currently generating activity"
            />
            <HeroMetric
              label={subscriptionValidity?.label ?? "Subscription valid"}
              value={subscriptionDaysText}
              tone="blue"
              infoTooltip={subscriptionTooltipText ?? undefined}
            />
          </div>
        </div>
      </section>

      {subscriptionPricing && (
        <PlanBillingPanel planName={(isDemoWorkspace ? effectivePlan : report?.planName) ?? "BILLMYGYM"} pricing={subscriptionPricing} />
      )}

      <section className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
        {statCards.map((card) => {
          const Icon = card.icon;
          return (
            <div key={card.title} className="glass-panel relative overflow-hidden rounded-[1.4rem] p-4 sm:rounded-[1.8rem] sm:p-6">
              <div className={cn("absolute inset-0 bg-gradient-to-br", card.panel)} />
              <div className="relative z-10">
                <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-2 sm:gap-3">
                  <div className="w-fit rounded-xl border border-white/80 bg-white/75 p-2 shadow-sm sm:rounded-2xl sm:p-3">
                    <Icon className="h-4 w-4 text-slate-900 sm:h-5 sm:w-5" />
                  </div>
                  <Badge className={cn("text-[8px] leading-tight px-1.5 py-0 sm:text-xs sm:px-2.5 sm:py-0.5 w-fit whitespace-normal text-center sm:text-left sm:whitespace-nowrap", card.badge)}>{card.eyebrow}</Badge>
                </div>
                <div className="mt-4 sm:mt-8">
                  <div className="text-[9px] font-black uppercase leading-tight tracking-[0.1em] text-slate-500 sm:text-[10px] sm:tracking-[0.22em]">{card.title}</div>
                  <div className="mt-1 truncate text-lg font-black tracking-tight text-slate-950 sm:mt-2 sm:text-3xl">{card.value}</div>
                </div>
              </div>
            </div>
          );
        })}
      </section>

      <section className="grid gap-4 sm:gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="glass-panel overflow-hidden rounded-[1.9rem]">
          <div className="border-b border-white/70 p-5 sm:p-7">
            <div className="flex items-center justify-between gap-4">
              <div>
                <div className="text-[10px] font-black uppercase tracking-[0.24em] text-emerald-700">Revenue Focus</div>
                <h2 className="mt-2 text-2xl font-black tracking-tight text-slate-950">Collections Command Center</h2>
              </div>
              <div className="rounded-full border border-emerald-100 bg-emerald-50 px-4 py-2 text-sm font-bold text-emerald-700">
                {money(revenueSegmentation.totalRevenue)} collected this month
              </div>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-1.5 p-2 sm:grid-cols-3 sm:gap-3 sm:p-4 md:gap-4 lg:p-7">
            <RevenueBand
              label="Current Revenue"
              value={money(revenueSegmentation.totalRevenue)}
              accent="emerald"
              helper="Admission plus membership revenue collected this month"
              breakdown={{
                admission: money(revenueSegmentation.admissionRevenue),
                recurring: money(revenueSegmentation.recurringRevenue),
              }}
              compact
            />
            <RevenueBand
              label="Expected Revenue"
              value={money(revenue.expectedRevenue)}
              accent="slate"
              helper="What the cycle should close at"
              compact
            />
            <RevenueBand
              label="Overdue Revenue"
              value={money(revenue.overdueRevenue)}
              accent="rose"
              helper="Immediate leakage risk to recover"
              compact
            />
          </div>
        </div>

        <div className="space-y-6">
          <div className="glass-panel overflow-hidden rounded-[1.9rem]">
            <div className="border-b border-white/70 p-5 sm:p-7">
              <div className="text-[10px] font-black uppercase tracking-[0.24em] text-slate-500">Member Health</div>
              <h2 className="mt-2 text-2xl font-black tracking-tight text-slate-950">Directory Performance</h2>
            </div>
            <div className="space-y-2 p-3 sm:space-y-4 sm:p-6">
              <div className="relative overflow-hidden rounded-[1.4rem] sm:rounded-[1.8rem] border border-emerald-100 bg-[linear-gradient(135deg,rgba(16,185,129,0.12),rgba(255,255,255,0.9)_42%,rgba(14,165,233,0.12))] p-3 shadow-sm sm:p-6">
                <div className="absolute -right-10 -top-10 h-24 sm:h-32 w-24 sm:w-32 rounded-full bg-emerald-200/30 blur-2xl" />
                <div className="absolute -bottom-8 sm:-bottom-12 left-6 sm:left-10 h-20 sm:h-28 w-20 sm:w-28 rounded-full bg-sky-200/30 blur-2xl" />
                <div className="relative z-10 flex flex-col gap-2.5 sm:gap-5">
                  <div className="flex flex-col gap-2 sm:gap-4">
                    <div>
                      <div className="text-[9px] sm:text-[10px] font-black uppercase tracking-[0.2em] sm:tracking-[0.22em] text-emerald-700">Collection Coverage</div>
                      <div className="mt-1.5 sm:mt-2 flex items-center justify-between gap-2">
                        <h3 className="text-2xl sm:text-3xl font-black tracking-tight text-slate-950">
                          {activeMembersCollected} / {activeMembersTotal}
                        </h3>
                        <div className="rounded-[1rem] sm:rounded-[1.3rem] border border-white/90 bg-white/80 px-2.5 sm:px-4 py-1.5 sm:py-3 text-center shadow-sm">
                          <div className="text-[8px] sm:text-[9px] font-black uppercase tracking-[0.18em] text-slate-500">Coverage</div>
                          <div className="mt-0.5 sm:mt-1 text-lg sm:text-2xl font-black tracking-tight text-slate-950">{activeCollectionRate}%</div>
                        </div>
                      </div>
                      <p className="mt-1 hidden sm:block text-xs font-medium leading-5 text-slate-600">
                        Active members fully covered
                      </p>
                    </div>
                  </div>

                  <div className="space-y-1.5 sm:space-y-2">
                    <div className="h-1.5 sm:h-3 overflow-hidden rounded-full bg-white/80 ring-1 ring-inset ring-emerald-100">
                      <div
                        className="h-full rounded-full bg-[linear-gradient(90deg,#059669,#22c55e,#38bdf8)] shadow-[0_10px_30px_-12px_rgba(5,150,105,0.8)] transition-all"
                        style={{ width: `${Math.max(activeCollectionRate, activeMembersTotal > 0 ? 8 : 0)}%` }}
                      />
                    </div>
                    <div className="flex items-center justify-between gap-1 text-[11px] sm:text-sm font-semibold text-slate-600">
                      <span>{activeMembersCollected} covered</span>
                      <span className="hidden sm:inline">•</span>
                      <span>{activeMembersPendingCollection} pending</span>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-2 sm:gap-3">
                    <div className="rounded-[1rem] sm:rounded-[1.4rem] border border-white/85 bg-white/78 p-2.5 sm:p-4 shadow-sm">
                      <div className="text-[8px] sm:text-[10px] font-black uppercase tracking-[0.18em] text-slate-500">Attention</div>
                      <div className="mt-1 text-xl sm:text-3xl font-black tracking-tight text-slate-950">{activeMembersPendingCollection}</div>
                      <div className="mt-0.5 hidden sm:block text-xs font-medium text-slate-500">Pending</div>
                    </div>
                    <div className="rounded-[1rem] sm:rounded-[1.4rem] border border-white/85 bg-white/78 p-2.5 sm:p-4 shadow-sm">
                      <div className="text-[8px] sm:text-[10px] font-black uppercase tracking-[0.18em] text-slate-500">Active</div>
                      <div className="mt-1 text-xl sm:text-3xl font-black tracking-tight text-slate-950">{activeMembersTotal}</div>
                      <div className="mt-0.5 hidden sm:block text-xs font-medium text-slate-500">Members</div>
                    </div>
                  </div>
                </div>
              </div>

            </div>
          </div>

          <div className="glass-panel overflow-hidden rounded-[1.9rem]">
            <div className="border-b border-white/70 p-5 sm:p-7">
              <div className="text-[10px] font-black uppercase tracking-[0.24em] text-sky-700">Member Growth</div>
              <h2 className="mt-2 text-2xl font-black tracking-tight text-slate-950">Members Joined By Month</h2>
              <p className="mt-2 hidden text-sm font-medium text-slate-500 sm:block">
                A quick read on how many new members joined in each of the last 6 months.
              </p>
            </div>
            <div className="p-5 sm:p-7">
              <MemberJoinTrendChart items={monthlyMemberJoins} />
            </div>
          </div>
        </div>
      </section>

      <section className="grid gap-4 sm:gap-6">
        <div className="space-y-6">
          <MembershipListCard
            title="Overdue Memberships"
            subtitle="Overdue members that need immediate follow-up."
            items={overdue}
            emptyMessage="No overdue memberships in the current snapshot."
            tone="rose"
          />
        </div>
      </section>
    </div>
  );
}

function DemoWorkspaceBanner({
  organizationName,
  previewPlan,
}: {
  organizationName?: string;
  previewPlan: "BILLMYGYM";
}) {
  return (
    <section className="relative overflow-hidden rounded-[1.9rem] border border-sky-200 bg-[linear-gradient(135deg,rgba(14,165,233,0.12),rgba(255,255,255,0.92)_45%,rgba(34,197,94,0.12))] p-6 shadow-sm">
      <div className="absolute -right-10 -top-10 h-32 w-32 rounded-full bg-sky-200/40 blur-2xl" />
      <div className="relative z-10 flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <div className="inline-flex items-center rounded-full border border-sky-200 bg-sky-100 px-3 py-1 text-[10px] font-black uppercase tracking-[0.22em] text-sky-800">
            Demo Workspace
          </div>
          <h2 className="mt-3 text-2xl font-black tracking-tight text-slate-950">
            {organizationName || "This gym"} is running in demo mode.
          </h2>
          <p className="mt-2 max-w-3xl text-sm font-semibold leading-6 text-slate-600">
            This workspace is intended for showcasing workflows and product demonstrations. Billing is disabled, and data here can be used safely for walkthroughs.
          </p>
        </div>
        <div className="flex flex-col gap-2">
          <div className="rounded-[1.3rem] border border-white/80 bg-white/80 px-4 py-3 text-sm font-bold text-sky-800 shadow-sm">
            Demo gym clearly marked
          </div>
          <div className="rounded-[1.3rem] border border-slate-900/5 bg-slate-950 px-4 py-3 text-sm font-black uppercase tracking-[0.18em] text-white shadow-sm">
            {previewPlan}
          </div>
        </div>
      </div>
    </section>
  );
}

function HeroMetric({
  label,
  value,
  tone,
  detail,
  infoTooltip,
}: {
  label: string;
  value: string | number;
  tone: "emerald" | "amber" | "slate" | "blue";
  detail?: string;
  infoTooltip?: string;
}) {
  const toneClass = {
    emerald: "border-emerald-100 bg-emerald-50/72 text-emerald-900",
    amber: "border-amber-100 bg-amber-50/72 text-amber-900",
    slate: "border-slate-200 bg-white/82 text-slate-900",
    blue: "border-sky-100 bg-sky-50/72 text-sky-900",
  }[tone];

  return (
    <div className={cn("rounded-[1.2rem] sm:rounded-[1.5rem] border p-3 sm:p-4 sm:rounded-[1.6rem] sm:p-5 shadow-sm", toneClass)}>
      <div className="flex items-center justify-between gap-2 sm:gap-3">
        <div className="text-[9px] sm:text-[10px] font-black uppercase tracking-[0.18em] sm:tracking-[0.2em] opacity-70">{label}</div>
        {infoTooltip && (
          <div className="group relative">
            <button
              type="button"
              className="inline-flex h-6 sm:h-7 w-6 sm:w-7 items-center justify-center rounded-full border border-white/80 bg-white/75 text-slate-600 shadow-sm transition-colors hover:bg-white hover:text-slate-900"
              aria-label="Subscription info"
            >
              <Info className="h-3.5 sm:h-4 w-3.5 sm:w-4" />
            </button>
            <div className="pointer-events-none absolute bottom-full right-0 z-20 mb-2 sm:mb-3 w-[220px] sm:w-[240px] translate-y-1 rounded-2xl border border-slate-200 bg-white/95 p-3 sm:p-4 text-left text-[10px] sm:text-[11px] font-semibold leading-5 text-slate-700 opacity-0 shadow-xl backdrop-blur transition-all duration-200 group-hover:translate-y-0 group-hover:opacity-100">
              <div className="text-[9px] sm:text-[10px] font-black uppercase tracking-[0.18em] text-slate-500">Subscription Date</div>
              <div className="mt-2 text-xs sm:text-sm font-black text-slate-900">{infoTooltip}</div>
            </div>
          </div>
        )}
      </div>
      <div className="mt-1.5 sm:mt-2 text-xl sm:text-2xl font-black tracking-tight sm:text-3xl">{value}</div>
      {detail ? <div className="mt-0.5 sm:mt-1 hidden text-xs sm:text-sm font-semibold opacity-70 sm:block">{detail}</div> : null}
    </div>
  );
}

function PlanBillingPanel({
  planName,
  pricing,
}: {
  planName: string;
  pricing: {
    baseAmount?: number;
    totalAmount?: number;
    activeMembers?: number;
    includedMembers?: number | null;
    overageMembers?: number;
    overageRate?: number;
    overageAmount?: number;
  };
}) {
  const includedMembers = pricing.includedMembers ?? 100;
  const activeMembers = pricing.activeMembers ?? 0;
  const overageMembers = pricing.overageMembers ?? Math.max(activeMembers - includedMembers, 0);
  const overageRate = pricing.overageRate ?? 4;
  const overageAmount = pricing.overageAmount ?? overageMembers * overageRate;
  const totalAmount = pricing.totalAmount ?? (pricing.baseAmount ?? 0) + overageAmount;
  const includedPercent = activeMembers > 0
    ? Math.min(Math.round((Math.min(activeMembers, includedMembers) / includedMembers) * 100), 100)
    : 0;

  return (
    <section className="relative overflow-hidden rounded-[1.4rem] sm:rounded-[1.9rem] border border-slate-900 bg-[linear-gradient(135deg,#020617,#0f172a_48%,#075985)] p-4 sm:p-6 text-white shadow-[0_28px_80px_rgba(2,6,23,0.22)]">
      <div className="absolute inset-x-0 top-0 h-1 bg-[linear-gradient(90deg,#38bdf8,#22c55e,#f59e0b)]" />
      <div className="relative z-10 grid gap-4 sm:gap-6 lg:grid-cols-[1.05fr_0.95fr] lg:items-center">
        <div>
          <div className="hidden sm:flex flex-wrap items-center gap-2">
            <Badge className="border-sky-300/30 bg-sky-300/10 text-sky-100">{planName} Billing</Badge>
            <Badge className="border-white/10 bg-white/10 text-slate-100">Live member responsive</Badge>
          </div>
          <div className="mt-2 sm:mt-5 flex items-center gap-3 sm:gap-4">
            <div className="flex h-10 w-10 sm:h-14 sm:w-14 items-center justify-center rounded-xl sm:rounded-2xl bg-white/10 text-sky-100 ring-1 ring-white/10">
              <Crown className="h-5 w-5 sm:h-7 sm:w-7" />
            </div>
            <div>
              <div className="text-[9px] sm:text-[10px] font-black uppercase tracking-[0.2em] sm:tracking-[0.24em] text-sky-200">Monthly subscription total</div>
              <div className="mt-0.5 sm:mt-1 text-2xl sm:text-4xl font-black tracking-tight">{money(totalAmount)}</div>
            </div>
          </div>
          <p className="hidden sm:block mt-4 max-w-2xl text-sm font-semibold leading-6 text-slate-300">
            BillMyGym includes {includedMembers} active members and keeps total members unlimited. Every active member above that adds {money(overageRate)} to the next monthly subscription payment.
          </p>
        </div>

        <div className="grid grid-cols-3 gap-2 sm:gap-3">
          <div className="rounded-xl sm:rounded-[1.3rem] border border-white/10 bg-white/[0.06] p-2 sm:p-4">
            <div className="text-[8px] sm:text-[10px] font-black uppercase tracking-[0.15em] sm:tracking-[0.2em] text-slate-300">Active</div>
            <div className="mt-1 sm:mt-2 text-lg sm:text-3xl font-black">{activeMembers}</div>
          </div>
          <div className="rounded-xl sm:rounded-[1.3rem] border border-white/10 bg-white/[0.06] p-2 sm:p-4">
            <div className="text-[8px] sm:text-[10px] font-black uppercase tracking-[0.15em] sm:tracking-[0.2em] text-slate-300">Included</div>
            <div className="mt-1 sm:mt-2 text-lg sm:text-3xl font-black">{includedMembers}</div>
          </div>
          <div className="rounded-xl sm:rounded-[1.3rem] border border-sky-300/20 bg-sky-300/10 p-2 sm:p-4">
            <div className="text-[8px] sm:text-[10px] font-black uppercase tracking-[0.15em] sm:tracking-[0.2em] text-sky-100">Overage</div>
            <div className="mt-1 sm:mt-2 text-lg sm:text-3xl font-black">{money(overageAmount)}</div>
          </div>
          <div className="col-span-3">
            <div className="h-2 sm:h-3 overflow-hidden rounded-full bg-white/10 ring-1 ring-inset ring-white/10">
              <div
                className="h-full rounded-full bg-[linear-gradient(90deg,#38bdf8,#22c55e)] shadow-[0_10px_30px_-12px_rgba(56,189,248,0.9)]"
                style={{ width: `${Math.max(includedPercent, activeMembers > 0 ? 6 : 0)}%` }}
              />
            </div>
            <div className="mt-2 sm:mt-3 flex flex-wrap items-center justify-between gap-2 sm:gap-3 text-[9px] sm:text-xs font-bold uppercase tracking-[0.1em] sm:tracking-[0.16em] text-slate-300">
              <span>{Math.min(activeMembers, includedMembers)} used</span>
              <span>{overageMembers} extra billed</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function buildSubscriptionTooltip(label?: string, isoDate?: string) {
  if (!label || !isoDate) return null;
  const date = new Date(`${isoDate}T00:00:00+05:30`);
  if (Number.isNaN(date.getTime())) return null;
  return `${label} ${formatMonthDayOrdinal(date)}`;
}

function formatMonthDayOrdinal(date: Date) {
  const month = new Intl.DateTimeFormat("en-IN", { month: "short", timeZone: INDIAN_TIME_ZONE }).format(date);
  const day = Number(new Intl.DateTimeFormat("en-IN", { day: "numeric", timeZone: INDIAN_TIME_ZONE }).format(date));
  const suffix = ordinalSuffix(day);
  return `${month} ${day}${suffix}`;
}

function ordinalSuffix(day: number) {
  const modulo100 = day % 100;
  if (modulo100 >= 11 && modulo100 <= 13) return "th";
  return { 1: "st", 2: "nd", 3: "rd" }[day % 10] ?? "th";
}

function RevenueBand({
  label,
  value,
  accent,
  helper,
  breakdown,
  compact = false,
}: {
  label: string;
  value: string;
  accent: "emerald" | "slate" | "amber" | "rose";
  helper: string;
  breakdown?: {
    admission: string;
    recurring: string;
  };
  compact?: boolean;
}) {
  const accentClass = {
    emerald: "from-emerald-500 to-emerald-300",
    slate: "from-slate-900 to-slate-500",
    amber: "from-amber-500 to-amber-300",
    rose: "from-rose-500 to-rose-300",
  }[accent];

  return (
    <div className={cn("border border-white/80 bg-white/75 shadow-sm rounded-[1.2rem] sm:rounded-[1.4rem] lg:rounded-[1.5rem]", compact ? "p-2 sm:p-3 lg:p-4" : "p-4 sm:p-5")}>
      <div className={cn("rounded-full bg-gradient-to-r", accentClass, compact ? "h-1 w-12 sm:h-1.5 sm:w-16" : "h-1.5 w-16")} />
      <div className={cn("flex items-center gap-1.5 sm:gap-2 opacity-70", compact ? "mt-2 sm:mt-3 text-[8px] sm:text-[10px] font-black uppercase tracking-[0.18em]" : "mt-4 text-[10px] font-black uppercase tracking-[0.22em]")}>
        <span>{label}</span>
        {breakdown && (
          <div className="group relative">
            <span className={cn("flex items-center justify-center rounded-full border border-slate-300 bg-white text-slate-500 transition-colors hover:bg-slate-50", compact ? "h-3 w-3" : "h-4 w-4")}>
              <Info className={compact ? "h-2 w-2" : "h-2.5 w-2.5"} />
            </span>
            <div className="pointer-events-none absolute bottom-full left-1/2 z-20 mb-2 w-[260px] -translate-x-1/2 rounded-2xl border border-slate-200 bg-white p-3 text-left opacity-0 shadow-xl transition-all duration-200 group-hover:translate-y-0 group-hover:opacity-100">
              <div className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Revenue Split</div>
              <div className="mt-2 flex items-center justify-between text-xs font-semibold text-slate-700">
                <span>Admission</span>
                <span>{breakdown.admission}</span>
              </div>
              <div className="mt-1 flex items-center justify-between text-xs font-semibold text-slate-700">
                <span>Monthly</span>
                <span>{breakdown.recurring}</span>
              </div>
            </div>
          </div>
        )}
      </div>
      <div className={cn("font-black tracking-tight text-slate-950", compact ? "mt-1.5 sm:mt-2 text-lg sm:text-2xl lg:text-3xl" : "mt-2 text-2xl sm:text-3xl")}>{value}</div>
      <div className={cn("hidden sm:block font-medium leading-5 text-slate-500", compact ? "mt-1 sm:mt-1.5 text-[10px] sm:text-xs" : "mt-2 text-sm")}>{helper}</div>
    </div>
  );
}

function MemberJoinTrendChart({ items }: { items: MonthlyMemberJoin[] }) {
  const maxValue = Math.max(...items.map((item) => item.joinedMembers), 1);

  if (items.length === 0) {
    return (
      <div className="rounded-[1.4rem] border border-dashed border-slate-200 bg-white/55 p-6 text-sm font-semibold text-slate-500">
        No join trend data is available yet.
      </div>
    );
  }

  return (
    <div className="grid grid-cols-3 gap-2 sm:grid-cols-6 sm:gap-4">
      {items.map((item) => {
        const height = `${Math.max((item.joinedMembers / maxValue) * 100, item.joinedMembers > 0 ? 16 : 6)}%`;
        return (
          <div key={item.month} className="rounded-xl border border-white/80 bg-white/72 p-2 shadow-sm sm:rounded-[1.5rem] sm:p-4">
            <div className="hidden sm:flex h-24 items-end justify-center rounded-[1.1rem] bg-[linear-gradient(180deg,rgba(14,165,233,0.08),rgba(255,255,255,0.72))] p-3 sm:h-32 sm:rounded-[1.2rem]">
              <div
                className="w-full rounded-full bg-gradient-to-t from-sky-600 via-sky-500 to-cyan-300 shadow-[0_16px_28px_-18px_rgba(2,132,199,0.9)]"
                style={{ height }}
                title={`${item.label}: ${item.joinedMembers} joined`}
              />
            </div>
            <div className="mt-1 text-center text-[9px] font-black uppercase tracking-[0.1em] text-slate-500 sm:mt-4 sm:text-[10px] sm:tracking-[0.22em]">
              {item.label}
            </div>
            <div className="mt-0.5 text-center text-lg font-black tracking-tight text-slate-950 sm:mt-1 sm:text-2xl">{item.joinedMembers}</div>
          </div>
        );
      })}
    </div>
  );
}

function MembershipListCard({
  title,
  subtitle,
  items,
  emptyMessage,
  tone,
}: {
  title: string;
  subtitle: string;
  items: MembershipSnapshot[];
  emptyMessage: string;
  tone: "amber" | "rose";
}) {
  return (
    <div className="glass-panel overflow-hidden rounded-[1.4rem] sm:rounded-[1.9rem]">
      <div className="border-b border-white/70 p-4 sm:p-7">
        <h2 className="text-xl sm:text-2xl font-black tracking-tight text-slate-950">{title}</h2>
        <p className="mt-1 sm:mt-2 hidden text-xs sm:text-sm font-medium text-slate-500 sm:block">{subtitle}</p>
      </div>

      <div className="p-3 sm:p-5">
        {items.length === 0 ? (
          <div className="rounded-xl sm:rounded-[1.4rem] border border-dashed border-slate-200 bg-white/55 p-4 sm:p-6 text-xs sm:text-sm font-semibold text-slate-500">
            {emptyMessage}
          </div>
        ) : (
          <div className="space-y-2 sm:space-y-3">
            {items.map((member) => {
              return (
                <div key={member.id} className="rounded-xl sm:rounded-[1.4rem] border border-white/80 bg-white/72 p-3 sm:p-4 shadow-sm">
                  <div className="flex items-start justify-between gap-3 sm:gap-4">
                    <div className="min-w-0">
                      <div className="truncate text-sm sm:text-base font-black text-slate-950">{member.fullName}</div>
                      <div className="mt-0.5 sm:mt-1 text-[10px] sm:text-xs font-semibold uppercase tracking-[0.15em] sm:tracking-[0.18em] text-slate-400">
                        {member.currentMembership?.durationMonths ?? 0} month plan
                      </div>
                    </div>
                    <StatusPill status={member.currentMembership?.status ?? "OVERDUE"} />
                  </div>
                  <div className="mt-2 sm:mt-4 flex flex-wrap items-center gap-2 sm:gap-3 text-xs sm:text-sm font-semibold text-slate-500">
                    <span>Ends {shortDate(member.currentMembership?.endDate)}</span>
                  </div>
                  <div className="mt-2 sm:mt-4">
                    <Link to={`/app/members/${member.id}/renew`}>
                      <Button variant="ghost" className="h-8 sm:h-9 rounded-full px-0 text-xs sm:text-sm font-bold text-slate-800 hover:bg-transparent hover:text-slate-950">
                        Renew membership
                        <ArrowRight className="ml-1.5 sm:ml-2 h-3.5 w-3.5 sm:h-4 sm:w-4" />
                      </Button>
                    </Link>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

function StatusPill({ status }: { status: string }) {
  const normalized = status.toUpperCase();
  const style =
    normalized === "PAID" || normalized === "ACTIVE" || normalized === "SENT"
      ? "bg-emerald-100 text-emerald-800 border-emerald-200"
      : normalized === "PENDING" || normalized === "EXPIRING_SOON" || normalized === "RETRYING"
        ? "bg-amber-100 text-amber-800 border-amber-200"
        : "bg-rose-100 text-rose-800 border-rose-200";

  const Icon =
    normalized === "PAID" || normalized === "ACTIVE" || normalized === "SENT"
      ? CheckCircle2
      : normalized === "PENDING" || normalized === "EXPIRING_SOON" || normalized === "RETRYING"
        ? Clock3
        : AlertCircle;

  return (
    <span className={cn("inline-flex items-center gap-1 sm:gap-1.5 rounded-full border px-2 py-0.5 sm:px-3 sm:py-1 text-[9px] sm:text-[11px] font-black uppercase tracking-[0.12em] sm:tracking-[0.16em]", style)}>
      <Icon className="h-2.5 w-2.5 sm:h-3.5 sm:w-3.5" />
      {status.replaceAll("_", " ")}
    </span>
  );
}
