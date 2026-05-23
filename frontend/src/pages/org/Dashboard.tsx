import { useQuery } from "@tanstack/react-query";
import {
  AlertCircle,
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  Clock3,
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
    paymentStatus?: string;
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
  expiringSoon?: MembershipSnapshot[];
  overdue?: MembershipSnapshot[];
  recentReminderLogs?: ReminderLog[];
};

const money = (value: number | string | undefined) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(Number(value ?? 0));

const shortDate = (value?: string) =>
  value
    ? new Intl.DateTimeFormat("en-IN", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(value))
    : "-";

const shortDateTime = (value?: string) =>
  value
    ? new Intl.DateTimeFormat("en-IN", {
        day: "2-digit",
        month: "short",
        hour: "2-digit",
        minute: "2-digit",
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
  const summary = report?.memberSummary ?? {};
  const expiringSoon = report?.expiringSoon ?? [];
  const overdue = report?.overdue ?? [];
  const recentReminderLogs = report?.recentReminderLogs ?? [];
  const monthlyMemberJoins = report?.monthlyMemberJoins ?? [];
  const joinedThisMonth = monthlyMemberJoins.at(-1)?.joinedMembers ?? 0;
  const canViewAdvancedAnalytics = report?.planAccess?.canViewAdvancedAnalytics ?? true;

  const collectionRate =
    Number(revenue.expectedRevenue ?? 0) > 0
      ? Math.round((Number(revenue.currentRevenue ?? 0) / Number(revenue.expectedRevenue ?? 0)) * 100)
      : 0;
  const atRiskMembers = Number(summary.expiringSoonMemberships ?? 0) + Number(summary.overdueMemberships ?? 0);
  const operationsSignal = recentReminderLogs[0]?.status ?? "QUIET";

  const statCards = [
    {
      title: "Collected Revenue",
      value: money(revenue.currentRevenue),
      eyebrow: `${collectionRate}% collection pace`,
      icon: Wallet,
      panel: "from-emerald-500/18 via-emerald-400/8 to-transparent",
      badge: "bg-emerald-100 text-emerald-800 border-emerald-200",
    },
    {
      title: "Expected This Cycle",
      value: money(revenue.expectedRevenue),
      eyebrow: "Pipeline target",
      icon: TrendingUp,
      panel: "from-slate-900/10 via-slate-500/5 to-transparent",
      badge: "bg-slate-100 text-slate-700 border-slate-200",
    },
    {
      title: "Pending Revenue",
      value: money(revenue.pendingRevenue),
      eyebrow: `${summary.expiringSoonMemberships ?? 0} expiring soon`,
      icon: Clock3,
      panel: "from-amber-500/20 via-amber-300/8 to-transparent",
      badge: "bg-amber-100 text-amber-800 border-amber-200",
    },
    {
      title: "Overdue Exposure",
      value: money(revenue.overdueRevenue),
      eyebrow: `${summary.overdueMemberships ?? 0} overdue memberships`,
      icon: AlertTriangle,
      panel: "from-rose-500/18 via-rose-300/8 to-transparent",
      badge: "bg-rose-100 text-rose-800 border-rose-200",
    },
  ];

  const memberSignals = [
    { label: "Total Members", value: summary.totalMembers ?? 0 },
    { label: "Active Members", value: summary.activeMembers ?? 0 },
    { label: "Active Memberships", value: summary.activeMemberships ?? 0 },
    { label: "Joined This Month", value: joinedThisMonth },
    { label: "Prepaid Members", value: revenue.prepaidMembers ?? 0 },
    { label: "Expiring Soon", value: summary.expiringSoonMemberships ?? 0 },
    { label: "Overdue", value: summary.overdueMemberships ?? 0 },
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
              Collections, renewals, and reminder health are surfaced in one operator view so you can protect revenue
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
              detail="Expiring soon plus overdue"
            />
            <HeroMetric
              label="Active Members"
              value={revenue.activeMembers ?? 0}
              tone="slate"
              detail="Members currently generating activity"
            />
            <HeroMetric
              label="Ops Signal"
              value={operationsSignal}
              tone="blue"
              detail="Latest reminder workflow status"
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
                {money(revenue.currentRevenue)} realized
              </div>
            </div>
          </div>

          <div className="grid gap-4 p-7 md:grid-cols-2">
            <RevenueBand
              label="Current Revenue"
              value={money(revenue.currentRevenue)}
              accent="emerald"
              helper="Already collected and safely recognized"
            />
            <RevenueBand
              label="Expected Revenue"
              value={money(revenue.expectedRevenue)}
              accent="slate"
              helper="What the cycle should close at"
            />
            <RevenueBand
              label="Pending Revenue"
              value={money(revenue.pendingRevenue)}
              accent="amber"
              helper="Awaiting collection from live memberships"
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
            <div className="grid gap-3 p-7 sm:grid-cols-2">
              {memberSignals.map((signal) => (
                <div key={signal.label} className="rounded-[1.4rem] border border-white/80 bg-white/72 p-4 shadow-sm">
                  <div className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">{signal.label}</div>
                  <div className="mt-2 text-3xl font-black tracking-tight text-slate-950">{signal.value}</div>
                </div>
              ))}
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
            title="Expiring Soon"
            subtitle="Members approaching renewal and worth nudging early."
            items={expiringSoon}
            emptyMessage="No memberships are nearing expiry right now."
            tone="amber"
          />
          <MembershipListCard
            title="Pending Renewals"
            subtitle="Revenue-risk memberships that need immediate follow-up."
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
}: {
  label: string;
  value: string | number;
  tone: "emerald" | "amber" | "slate" | "blue";
  detail: string;
}) {
  const toneClass = {
    emerald: "border-emerald-100 bg-emerald-50/72 text-emerald-900",
    amber: "border-amber-100 bg-amber-50/72 text-amber-900",
    slate: "border-slate-200 bg-white/82 text-slate-900",
    blue: "border-sky-100 bg-sky-50/72 text-sky-900",
  }[tone];

  return (
    <div className={cn("rounded-[1.6rem] border p-5 shadow-sm", toneClass)}>
      <div className="text-[10px] font-black uppercase tracking-[0.2em] opacity-70">{label}</div>
      <div className="mt-2 text-3xl font-black tracking-tight">{value}</div>
      <div className="mt-1 text-sm font-semibold opacity-70">{detail}</div>
    </div>
  );
}

function RevenueBand({
  label,
  value,
  accent,
  helper,
}: {
  label: string;
  value: string;
  accent: "emerald" | "slate" | "amber" | "rose";
  helper: string;
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
      <div className="mt-4 text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">{label}</div>
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
                    <StatusPill status={member.currentMembership?.paymentStatus ?? member.currentMembership?.status ?? "PENDING"} />
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
