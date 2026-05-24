import { useQuery } from "@tanstack/react-query";
import {
  AlertCircle,
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  Clock3,
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
  expiringSoon?: MembershipSnapshot[];
  overdue?: MembershipSnapshot[];
  recentReminderLogs?: ReminderLog[];
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
  const { data: report, isLoading } = useQuery<DashboardReport>({
    queryKey: ["dashboardReport"],
    queryFn: async () => {
      const res = await api.get("/reports/dashboard");
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
  const recentReminderLogs = report?.recentReminderLogs ?? [];
  const monthlyMemberJoins = report?.monthlyMemberJoins ?? [];
  const joinedThisMonth = monthlyMemberJoins.at(-1)?.joinedMembers ?? 0;
  const canViewAdvancedAnalytics = report?.planAccess?.canViewAdvancedAnalytics ?? true;
  const subscriptionValidity = report?.subscriptionValidity;

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

  if (!canViewAdvancedAnalytics) {
    const starterSignals = [
      { label: "Total Members", value: summary.totalMembers ?? 0 },
      { label: "Active Members", value: summary.activeMembers ?? 0 },
      { label: "New Members This Month", value: joinedThisMonth },
    ];

    return (
      <div className="mx-auto max-w-7xl space-y-8 p-8">
        <section className="billmygym-shell relative overflow-hidden rounded-[2rem] p-8 lg:p-10">
          <div className="relative z-10 grid gap-8 lg:grid-cols-[1.2fr_0.8fr] lg:items-center">
            <div>
              <div className="mb-4 flex flex-wrap items-center gap-2">
                <Badge className="border-emerald-200 bg-emerald-100 text-emerald-800">Starter Dashboard</Badge>
                <Badge className="border-slate-200 bg-white/80 text-slate-700">Live backend snapshot</Badge>
              </div>
              <h1 className="max-w-3xl text-4xl font-black tracking-tight text-slate-950 lg:text-5xl">
                Member activity, kept focused.
              </h1>
              <p className="mt-4 max-w-2xl text-base font-medium leading-7 text-slate-600">
                Your Starter plan dashboard shows the essential member signals for day-to-day operations.
              </p>

              <div className="mt-7 flex flex-wrap gap-3">
                <Link to="/app/members/new">
                  <Button className="h-12 rounded-full bg-slate-950 px-6 font-bold text-white hover:bg-slate-800">
                    <Plus className="mr-2 h-4 w-4" />
                    Add Member
                  </Button>
                </Link>
                <Link to="/app/members">
                  <Button variant="outline" className="h-12 rounded-full border-white/80 bg-white/75 px-6 font-bold text-slate-800 hover:bg-white">
                    Open Members
                    <ArrowRight className="ml-2 h-4 w-4" />
                  </Button>
                </Link>
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-3 lg:grid-cols-1">
              <HeroMetric label="Active Members" value={summary.activeMembers ?? 0} tone="emerald" detail="Currently active member profiles" />
              <HeroMetric label="New This Month" value={joinedThisMonth} tone="blue" detail="Members joined this month" />
              <HeroMetric label="Total Members" value={summary.totalMembers ?? 0} tone="slate" detail="All non-deleted members" />
            </div>
          </div>
        </section>

        <section className="grid gap-5 md:grid-cols-3">
          {starterSignals.map((signal) => (
            <div key={signal.label} className="glass-panel rounded-[1.4rem] border border-white/80 bg-white/72 p-6 shadow-sm">
              <div className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">{signal.label}</div>
              <div className="mt-2 text-4xl font-black tracking-tight text-slate-950">{signal.value}</div>
            </div>
          ))}
        </section>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl space-y-8 p-8">
      <section className="billmygym-shell relative overflow-hidden rounded-[2rem] p-8 lg:p-10">
        <div className="absolute inset-y-0 right-0 w-[38%] bg-[radial-gradient(circle_at_top,rgba(22,163,74,0.16),transparent_45%),radial-gradient(circle_at_bottom,rgba(245,158,11,0.12),transparent_35%)]" />
        <div className="relative z-10 grid gap-8 lg:grid-cols-[1.2fr_0.8fr] lg:items-center">
          <div>
            <div className="mb-4 flex flex-wrap items-center gap-2">
              <Badge className="border-emerald-200 bg-emerald-100 text-emerald-800">Revenue OS</Badge>
              <Badge className="border-slate-200 bg-white/80 text-slate-700">Live backend snapshot</Badge>
            </div>
            <h1 className="max-w-3xl text-4xl font-black tracking-tight text-slate-950 lg:text-5xl">
              Run your gym like a premium membership SaaS, not a spreadsheet.
            </h1>
            <p className="mt-4 max-w-2xl text-base font-medium leading-7 text-slate-600">
              Collections, upcoming dues, and reminder health are surfaced in one operator view so you can protect revenue
              before memberships slip into overdue territory.
            </p>

            <div className="mt-7 flex flex-wrap gap-3">
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

          <div className="grid gap-4 sm:grid-cols-2">
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
              detail="Due soon plus overdue"
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

      <section className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
        {statCards.map((card) => {
          const Icon = card.icon;
          return (
            <div key={card.title} className="glass-panel relative overflow-hidden rounded-[1.8rem] p-6">
              <div className={cn("absolute inset-0 bg-gradient-to-br", card.panel)} />
              <div className="relative z-10">
                <div className="flex items-start justify-between gap-3">
                  <div className="rounded-2xl border border-white/80 bg-white/75 p-3 shadow-sm">
                    <Icon className="h-5 w-5 text-slate-900" />
                  </div>
                  <Badge className={card.badge}>{card.eyebrow}</Badge>
                </div>
                <div className="mt-8">
                  <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">{card.title}</div>
                  <div className="mt-2 text-3xl font-black tracking-tight text-slate-950">{card.value}</div>
                </div>
              </div>
            </div>
          );
        })}
      </section>

      <section className="grid gap-6 xl:grid-cols-[1fr_0.95fr]">
        <div className="glass-panel overflow-hidden rounded-[1.9rem]">
          <div className="border-b border-white/70 p-7">
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

          <div className="grid gap-4 p-7 md:grid-cols-3">
            <RevenueBand
              label="Current Revenue"
              value={money(revenueSegmentation.totalRevenue)}
              accent="emerald"
              helper="Admission plus membership revenue collected this month"
              breakdown={{
                admission: money(revenueSegmentation.admissionRevenue),
                recurring: money(revenueSegmentation.recurringRevenue),
              }}
            />
            <RevenueBand
              label="Expected Revenue"
              value={money(revenue.expectedRevenue)}
              accent="slate"
              helper="What the cycle should close at"
            />
            <RevenueBand
              label="Overdue Revenue"
              value={money(revenue.overdueRevenue)}
              accent="rose"
              helper="Immediate leakage risk to recover"
            />
          </div>
        </div>

        <div className="space-y-6">
          <div className="glass-panel overflow-hidden rounded-[1.9rem]">
            <div className="border-b border-white/70 p-7">
              <div className="text-[10px] font-black uppercase tracking-[0.24em] text-slate-500">Member Health</div>
              <h2 className="mt-2 text-2xl font-black tracking-tight text-slate-950">Directory Performance</h2>
            </div>
            <div className="space-y-5 p-7">
              <div className="relative overflow-hidden rounded-[1.8rem] border border-emerald-100 bg-[linear-gradient(135deg,rgba(16,185,129,0.12),rgba(255,255,255,0.9)_42%,rgba(14,165,233,0.12))] p-6 shadow-sm">
                <div className="absolute -right-10 -top-10 h-32 w-32 rounded-full bg-emerald-200/30 blur-2xl" />
                <div className="absolute -bottom-12 left-10 h-28 w-28 rounded-full bg-sky-200/30 blur-2xl" />
                <div className="relative z-10 flex flex-col gap-5">
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                      <div className="text-[10px] font-black uppercase tracking-[0.22em] text-emerald-700">Collection Coverage</div>
                      <h3 className="mt-2 text-3xl font-black tracking-tight text-slate-950">
                        {activeMembersCollected} / {activeMembersTotal}
                      </h3>
                      <p className="mt-2 max-w-md text-sm font-medium leading-6 text-slate-600">
                        Members with fully covered active memberships, compared against your current active member base.
                      </p>
                    </div>
                    <div className="rounded-[1.3rem] border border-white/90 bg-white/80 px-4 py-3 text-right shadow-sm">
                      <div className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Coverage Rate</div>
                      <div className="mt-1 text-2xl font-black tracking-tight text-slate-950">{activeCollectionRate}%</div>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <div className="h-3 overflow-hidden rounded-full bg-white/80 ring-1 ring-inset ring-emerald-100">
                      <div
                        className="h-full rounded-full bg-[linear-gradient(90deg,#059669,#22c55e,#38bdf8)] shadow-[0_10px_30px_-12px_rgba(5,150,105,0.8)] transition-all"
                        style={{ width: `${Math.max(activeCollectionRate, activeMembersTotal > 0 ? 8 : 0)}%` }}
                      />
                    </div>
                    <div className="flex flex-wrap items-center justify-between gap-3 text-sm font-semibold text-slate-600">
                      <span>{activeMembersCollected} members covered</span>
                      <span>{activeMembersPendingCollection} still need collection or overdue follow-up</span>
                    </div>
                  </div>

                  <div className="grid gap-3 sm:grid-cols-2">
                    <div className="rounded-[1.4rem] border border-white/85 bg-white/78 p-4 shadow-sm">
                      <div className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Need Attention</div>
                      <div className="mt-2 text-3xl font-black tracking-tight text-slate-950">{activeMembersPendingCollection}</div>
                      <div className="mt-1 text-sm font-medium text-slate-500">Active members still waiting for collection or overdue follow-up</div>
                    </div>
                    <div className="rounded-[1.4rem] border border-white/85 bg-white/78 p-4 shadow-sm">
                      <div className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Active members</div>
                      <div className="mt-2 text-3xl font-black tracking-tight text-slate-950">{activeMembersTotal}</div>
                      <div className="mt-1 text-sm font-medium text-slate-500">Total active members currently in your directory</div>
                    </div>
                  </div>
                </div>
              </div>

            </div>
          </div>

          <div className="glass-panel overflow-hidden rounded-[1.9rem]">
            <div className="border-b border-white/70 p-7">
              <div className="text-[10px] font-black uppercase tracking-[0.24em] text-sky-700">Growth Track</div>
              <h2 className="mt-2 text-2xl font-black tracking-tight text-slate-950">Members Joined By Month</h2>
              <p className="mt-2 text-sm font-medium text-slate-500">
                A quick read on how many new members joined in each of the last 6 months.
              </p>
            </div>
            <div className="p-7">
              <MemberJoinTrendChart items={monthlyMemberJoins} />
            </div>
          </div>
        </div>
      </section>

      <section className="grid gap-6 xl:grid-cols-[0.92fr_1.08fr]">
        <div className="space-y-6">
          <MembershipListCard
            title="Upcoming Dues"
            subtitle="Active memberships nearing their end date and worth nudging early."
            items={expiringSoon}
            emptyMessage="No memberships are nearing expiry right now."
            tone="amber"
          />
          <MembershipListCard
            title="Overdue Memberships"
            subtitle="Overdue members that need immediate follow-up."
            items={overdue}
            emptyMessage="No overdue memberships in the current snapshot."
            tone="rose"
          />
        </div>

        <div className="glass-panel overflow-hidden rounded-[1.9rem]">
          <div className="flex items-center justify-between gap-4 border-b border-white/70 p-7">
            <div>
              <div className="text-[10px] font-black uppercase tracking-[0.24em] text-slate-500">Reminder Activity</div>
              <h2 className="mt-2 text-2xl font-black tracking-tight text-slate-950">Admin Delivery Feed</h2>
            </div>
            <div className="rounded-2xl border border-white/80 bg-white/80 p-3 shadow-sm">
              <MessageCircleMore className="h-5 w-5 text-emerald-700" />
            </div>
          </div>

          {recentReminderLogs.length === 0 ? (
            <div className="p-10 text-center text-sm font-semibold text-slate-500">
              No reminder delivery events have been recorded yet.
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {recentReminderLogs.map((log) => (
                <div key={log.id} className="grid gap-4 p-5 sm:grid-cols-[1.1fr_0.7fr_0.55fr_0.65fr] sm:items-center">
                  <div>
                    <div className="text-sm font-black text-slate-950">{shortDateTime(log.sentAt ?? log.createdAt)}</div>
                    <div className="mt-1 text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">
                      Reminder event #{log.id}
                    </div>
                  </div>
                  <div className="text-sm font-semibold text-slate-600">{log.channel}</div>
                  <div className="text-sm font-semibold text-slate-600">{log.memberCount ?? 0} members</div>
                  <div>
                    <StatusPill status={log.status ?? "PENDING"} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
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
    <div className={cn("rounded-[1.6rem] border p-5 shadow-sm", toneClass)}>
      <div className="flex items-center justify-between gap-3">
        <div className="text-[10px] font-black uppercase tracking-[0.2em] opacity-70">{label}</div>
        {infoTooltip && (
          <div className="group relative">
            <button
              type="button"
              className="inline-flex h-7 w-7 items-center justify-center rounded-full border border-white/80 bg-white/75 text-slate-600 shadow-sm transition-colors hover:bg-white hover:text-slate-900"
              aria-label="Subscription info"
            >
              <Info className="h-4 w-4" />
            </button>
            <div className="pointer-events-none absolute bottom-full right-0 z-20 mb-3 w-[240px] translate-y-1 rounded-2xl border border-slate-200 bg-white/95 p-4 text-left text-[11px] font-semibold leading-5 text-slate-700 opacity-0 shadow-xl backdrop-blur transition-all duration-200 group-hover:translate-y-0 group-hover:opacity-100">
              <div className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Subscription Date</div>
              <div className="mt-2 text-sm font-black text-slate-900">{infoTooltip}</div>
            </div>
          </div>
        )}
      </div>
      <div className="mt-2 text-3xl font-black tracking-tight">{value}</div>
      {detail ? <div className="mt-1 text-sm font-semibold opacity-70">{detail}</div> : null}
    </div>
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
}: {
  label: string;
  value: string;
  accent: "emerald" | "slate" | "amber" | "rose";
  helper: string;
  breakdown?: {
    admission: string;
    recurring: string;
  };
}) {
  const accentClass = {
    emerald: "from-emerald-500 to-emerald-300",
    slate: "from-slate-900 to-slate-500",
    amber: "from-amber-500 to-amber-300",
    rose: "from-rose-500 to-rose-300",
  }[accent];

  return (
    <div className="rounded-[1.5rem] border border-white/80 bg-white/75 p-5 shadow-sm">
      <div className={cn("h-1.5 w-16 rounded-full bg-gradient-to-r", accentClass)} />
      <div className="mt-4 flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">
        <span>{label}</span>
        {breakdown && (
          <div className="group relative">
            <span className="flex h-4 w-4 items-center justify-center rounded-full border border-slate-300 bg-white text-slate-500 transition-colors hover:bg-slate-50">
              <Info className="h-2.5 w-2.5" />
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
      <div className="mt-2 text-3xl font-black tracking-tight text-slate-950">{value}</div>
      <div className="mt-2 text-sm font-medium leading-6 text-slate-500">{helper}</div>
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
    <div className="grid gap-4 sm:grid-cols-6">
      {items.map((item) => {
        const height = `${Math.max((item.joinedMembers / maxValue) * 100, item.joinedMembers > 0 ? 16 : 6)}%`;
        return (
          <div key={item.month} className="rounded-[1.5rem] border border-white/80 bg-white/72 p-4 shadow-sm">
            <div className="flex h-32 items-end justify-center rounded-[1.2rem] bg-[linear-gradient(180deg,rgba(14,165,233,0.08),rgba(255,255,255,0.72))] p-3">
              <div
                className="w-full rounded-full bg-gradient-to-t from-sky-600 via-sky-500 to-cyan-300 shadow-[0_16px_28px_-18px_rgba(2,132,199,0.9)]"
                style={{ height }}
                title={`${item.label}: ${item.joinedMembers} joined`}
              />
            </div>
            <div className="mt-4 text-center text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">
              {item.label}
            </div>
            <div className="mt-1 text-center text-2xl font-black tracking-tight text-slate-950">{item.joinedMembers}</div>
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
    <div className="glass-panel overflow-hidden rounded-[1.9rem]">
      <div className="border-b border-white/70 p-7">
        <h2 className="text-2xl font-black tracking-tight text-slate-950">{title}</h2>
        <p className="mt-2 text-sm font-medium text-slate-500">{subtitle}</p>
      </div>

      <div className="p-5">
        {items.length === 0 ? (
          <div className="rounded-[1.4rem] border border-dashed border-slate-200 bg-white/55 p-6 text-sm font-semibold text-slate-500">
            {emptyMessage}
          </div>
        ) : (
          <div className="space-y-3">
            {items.map((member) => {
              return (
                <div key={member.id} className="rounded-[1.4rem] border border-white/80 bg-white/72 p-4 shadow-sm">
                  <div className="flex items-start justify-between gap-4">
                    <div className="min-w-0">
                      <div className="truncate text-base font-black text-slate-950">{member.fullName}</div>
                      <div className="mt-1 text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">
                        {member.currentMembership?.durationMonths ?? 0} month plan
                      </div>
                    </div>
                    <StatusPill status={member.currentMembership?.status ?? "OVERDUE"} />
                  </div>
                  <div className="mt-4 flex flex-wrap items-center gap-3 text-sm font-semibold text-slate-500">
                    <span>Ends {shortDate(member.currentMembership?.endDate)}</span>
                  </div>
                  <div className="mt-4">
                    <Link to={`/app/members/${member.id}/renew`}>
                      <Button variant="ghost" className="h-9 rounded-full px-0 font-bold text-slate-800 hover:bg-transparent hover:text-slate-950">
                        Renew membership
                        <ArrowRight className="ml-2 h-4 w-4" />
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
    <span className={cn("inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-[11px] font-black uppercase tracking-[0.16em]", style)}>
      <Icon className="h-3.5 w-3.5" />
      {status.replaceAll("_", " ")}
    </span>
  );
}
