import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Activity,
  AlertTriangle,
  ArrowUpRight,
  Building2,
  CalendarClock,
  CheckCircle2,
  CreditCard,
  Eye,
  EyeOff,
  Mail,
  MessageCircle,
  PauseCircle,
  Plus,
  Rocket,
  ShieldOff,
  Sparkles,
  TrendingUp,
  WalletCards,
  type LucideIcon,
} from "lucide-react";
import { Link } from "react-router-dom";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import api from "@/lib/api";

type PlatformBreakdown = {
  key: string;
  label: string;
  count: number;
  amount: number;
  percentage: number;
};

type PlatformSubscriptionMonth = {
  month: string;
  label: string;
  paidSubscriptions: number;
  pendingSubscriptions: number;
  paidAmount: number;
  pendingAmount: number;
};

type PlatformAnalytics = {
  totalOrganizations: number;
  activeOrganizations: number;
  trialOrganizations: number;
  pilotOrganizations: number;
  pausedOrganizations: number;
  overdueOrganizations: number;
  deactivatedOrganizations: number;
  expiredOrganizations: number;
  activeSubscriptionRecords: number;
  monthlySaasRevenue: number;
  previousMonthlySaasRevenue: number;
  revenueGrowthPercent: number;
  lifetimeSaasRevenue: number;
  projectedMonthlyRevenue: number;
  paymentFailures: number;
  pendingPayments: number;
  whatsappUsage: number;
  emailUsage: number;
  totalCommunicationUsage: number;
  renewalsDueInSevenDays: number;
  pilotsEndingInThirtyDays: number;
  currentMonthNewOrganizations: number;
  previousMonthNewOrganizations: number;
  organizationGrowthPercent: number;
  monthlySubscriptionActivity: PlatformSubscriptionMonth[];
  organizationsByStatus: PlatformBreakdown[];
  organizationsBySubscriptionPlan: PlatformBreakdown[];
  paymentsByStatus: PlatformBreakdown[];
  lifecycleAlerts: PlatformBreakdown[];
};

type MetricCard = {
  label: string;
  value: string | number;
  detail: string;
  icon: LucideIcon;
  tone: string;
  borderTone: string;
};

const money = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

const compact = new Intl.NumberFormat("en-IN", {
  maximumFractionDigits: 0,
});

const statusTone: Record<string, string> = {
  ACTIVE: "bg-emerald-100 text-emerald-800 border-emerald-200",
  TRIAL: "bg-amber-100 text-amber-800 border-amber-200",
  PAUSED: "bg-orange-100 text-orange-800 border-orange-200",
  DEACTIVATED: "bg-rose-100 text-rose-800 border-rose-200",
  BILLMYGYM: "bg-emerald-100 text-emerald-800 border-emerald-200",
  DEMO: "bg-cyan-100 text-cyan-800 border-cyan-200",
  RENEWAL_REQUIRED: "bg-orange-100 text-orange-800 border-orange-200",
};

function percentText(value: number) {
  if (!Number.isFinite(value)) return "0%";
  return `${value > 0 ? "+" : ""}${Math.round(value)}%`;
}

function BreakdownBar({ item, amount = false }: { key?: string; item: PlatformBreakdown; amount?: boolean }) {
  return (
    <div className="rounded-2xl border border-white/75 bg-white/70 p-3 shadow-sm sm:p-4">
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0">
          <div className="truncate text-xs font-black text-slate-950 sm:text-sm">{item.label}</div>
          <div className="mt-1 text-[10px] font-bold text-slate-500 sm:text-xs">
            {item.count} orgs
            {amount ? ` • ${item.amount > 0 ? money.format(item.amount) : "No billed revenue"}` : ""}
          </div>
        </div>
        <Badge className={statusTone[item.key] ?? "border-slate-200 bg-slate-100 text-slate-700"}>{item.percentage}%</Badge>
      </div>
      <div className="mt-2 h-2 overflow-hidden rounded-full bg-slate-100 sm:mt-3 sm:h-2.5">
        <div
          className="h-full rounded-full bg-gradient-to-r from-emerald-500 via-teal-500 to-sky-500"
          style={{ width: `${Math.min(Math.max(item.percentage, 0), 100)}%` }}
        />
      </div>
    </div>
  );
}

function SubscriptionActivityChart({ items }: { items: PlatformSubscriptionMonth[] }) {
  const maxCount = Math.max(...items.map((item) => item.paidSubscriptions + item.pendingSubscriptions), 1);

  if (items.length === 0) {
    return <div className="mt-5 rounded-2xl border border-dashed border-slate-200 p-6 text-sm font-semibold text-slate-500">No subscription activity yet.</div>;
  }

  return (
    <div className="mt-4 grid grid-cols-3 gap-2 sm:mt-6 sm:grid-cols-6 sm:gap-3">
      {items.map((item) => {
        const paidHeight = Math.max((item.paidSubscriptions / maxCount) * 100, item.paidSubscriptions > 0 ? 10 : 3);
        const pendingHeight = Math.max((item.pendingSubscriptions / maxCount) * 100, item.pendingSubscriptions > 0 ? 10 : 3);
        return (
          <div key={item.month} className="rounded-xl border border-white/75 bg-white/70 p-2 shadow-sm sm:rounded-2xl sm:p-4">
            <div className="flex h-20 items-end justify-center gap-1.5 rounded-lg bg-slate-50 p-2 sm:h-32 sm:gap-2 sm:rounded-xl sm:p-3">
              <div className="w-3 rounded-full bg-emerald-600 sm:w-5" style={{ height: `${paidHeight}%` }} title={`${item.paidSubscriptions} paid`} />
              <div className="w-3 rounded-full bg-amber-400 sm:w-5" style={{ height: `${pendingHeight}%` }} title={`${item.pendingSubscriptions} pending`} />
            </div>
            <div className="mt-2 text-center text-[9px] font-black uppercase text-slate-500 sm:mt-3 sm:text-[10px] sm:tracking-[0.2em]">{item.label}</div>
            <div className="mt-1 text-center text-xs font-black text-slate-950 sm:text-sm">{item.paidSubscriptions} / {item.pendingSubscriptions}</div>
          </div>
        );
      })}
    </div>
  );
}

function SnapshotCard({
  label,
  value,
  detail,
  tone,
}: {
  label: string;
  value: number;
  detail: string;
  tone: "emerald" | "amber";
}) {
  const toneClass = tone === "emerald"
    ? "border-emerald-100 bg-emerald-50/90 text-emerald-950"
    : "border-amber-100 bg-amber-50/90 text-amber-950";

  return (
    <div className={`rounded-[1.25rem] border p-3 shadow-sm sm:rounded-[1.5rem] sm:p-4 ${toneClass}`}>
      <div className="text-[9px] font-black uppercase tracking-[0.12em] opacity-70 sm:text-[10px] sm:tracking-[0.2em]">{label}</div>
      <div className="mt-1 text-2xl font-black tracking-tight sm:mt-2 sm:text-3xl">{value}</div>
      <div className="mt-1 text-xs font-bold opacity-80 sm:mt-2 sm:text-sm">{detail}</div>
    </div>
  );
}

export default function PlatformOverview() {
  const [showLifetimeRevenue, setShowLifetimeRevenue] = useState(false);
  const { data: analytics, isLoading } = useQuery<PlatformAnalytics>({
    queryKey: ["platformAnalytics"],
    queryFn: async () => {
      const res = await api.get("/analytics/platform");
      return res.data;
    },
    staleTime: 60_000,
  });

  if (isLoading) {
    return <div className="p-8 text-sm font-semibold text-slate-500">Loading platform intelligence...</div>;
  }

  const data = analytics ?? {
    totalOrganizations: 0,
    activeOrganizations: 0,
    trialOrganizations: 0,
    pilotOrganizations: 0,
    pausedOrganizations: 0,
    overdueOrganizations: 0,
    deactivatedOrganizations: 0,
    expiredOrganizations: 0,
    activeSubscriptionRecords: 0,
    monthlySaasRevenue: 0,
    previousMonthlySaasRevenue: 0,
    revenueGrowthPercent: 0,
    lifetimeSaasRevenue: 0,
    projectedMonthlyRevenue: 0,
    paymentFailures: 0,
    pendingPayments: 0,
    whatsappUsage: 0,
    emailUsage: 0,
    totalCommunicationUsage: 0,
    renewalsDueInSevenDays: 0,
    pilotsEndingInThirtyDays: 0,
    currentMonthNewOrganizations: 0,
    previousMonthNewOrganizations: 0,
    organizationGrowthPercent: 0,
    monthlySubscriptionActivity: [],
    organizationsByStatus: [],
    organizationsBySubscriptionPlan: [],
    paymentsByStatus: [],
    lifecycleAlerts: [],
  };

  const metricCards: MetricCard[] = [
    {
      label: "Total gyms",
      value: data.totalOrganizations,
      detail: `${data.currentMonthNewOrganizations} new this month`,
      icon: Building2,
      tone: "from-slate-950 to-slate-800 text-white",
      borderTone: "border-slate-800/70",
    },
    {
      label: "Active organizations",
      value: data.activeOrganizations,
      detail: `${data.activeSubscriptionRecords} active billing periods`,
      icon: CheckCircle2,
      tone: "from-emerald-600 to-teal-500 text-white",
      borderTone: "border-emerald-500/60",
    },
    {
      label: "Overdue organizations",
      value: data.overdueOrganizations,
      detail: `${data.renewalsDueInSevenDays} renewals due in 7 days`,
      icon: AlertTriangle,
      tone: "from-orange-500 to-rose-500 text-white",
      borderTone: "border-orange-400/60",
    },
    {
      label: "Inactive accounts",
      value: data.deactivatedOrganizations,
      detail: `${data.pausedOrganizations} renewal-required accounts`,
      icon: PauseCircle,
      tone: "from-rose-700 to-slate-700 text-white",
      borderTone: "border-rose-500/60",
    },
    {
      label: "Projected MRR",
      value: money.format(data.projectedMonthlyRevenue),
      detail: `${percentText(data.revenueGrowthPercent)} vs previous month`,
      icon: WalletCards,
      tone: "from-indigo-700 to-sky-500 text-white",
      borderTone: "border-sky-500/60",
    },
  ];
  const latestMonth = data.monthlySubscriptionActivity.at(-1);
  return (
    <div className="mx-auto w-full max-w-7xl space-y-4 p-3 sm:space-y-6 sm:p-6 lg:p-8">
      <div className="relative overflow-hidden rounded-[1.5rem] border border-white/75 bg-slate-950 p-4 text-white shadow-[0_30px_90px_rgba(15,23,42,0.28)] sm:rounded-[2.25rem] sm:p-8 lg:p-10">
        <div className="absolute -right-24 -top-24 h-72 w-72 rounded-full bg-emerald-400/20 blur-3xl" />
        <div className="absolute bottom-0 left-1/2 h-56 w-56 rounded-full bg-amber-300/10 blur-3xl" />
        <div className="relative z-10 grid gap-4 sm:gap-8 lg:grid-cols-[1.1fr_0.9fr] lg:items-end">
          <div>
            <div className="inline-flex items-center rounded-full border border-emerald-300/20 bg-emerald-300/10 px-3 py-1.5 text-[9px] font-black uppercase tracking-[0.18em] text-emerald-200 sm:px-4 sm:py-2 sm:text-[11px] sm:tracking-[0.28em]">
              Super Admin Command Center
            </div>
            <h1 className="mt-3 max-w-3xl text-2xl font-black tracking-tight sm:mt-5 sm:text-5xl lg:text-6xl">
              Platform visibility without digging through tables.
            </h1>
            <p className="mt-3 max-w-2xl text-xs font-semibold leading-5 text-slate-300 sm:mt-5 sm:text-base sm:leading-7">
              Revenue, lifecycle risk, BillMyGym billing shape, growth momentum, communication load, and payment health are all computed server-side for the whole SaaS fleet.
            </p>
            <div className="mt-4 grid grid-cols-2 gap-2 sm:mt-7 sm:flex sm:flex-row sm:gap-3">
              <Link to="/app/organizations/new">
                <Button size="lg" className="h-10 w-full rounded-full border-0 bg-white px-3 text-xs font-black text-slate-950 hover:bg-emerald-50 sm:h-12 sm:px-7 sm:text-sm">
                  <Plus className="mr-2 h-5 w-5" /> Provision Organization
                </Button>
              </Link>
              <Link to="/app/organizations">
                <Button size="lg" variant="outline" className="h-10 w-full rounded-full border-white/15 bg-white/5 px-3 text-xs font-black text-white hover:bg-white/10 hover:text-white sm:h-12 sm:px-7 sm:text-sm">
                  Open Directory
                  <ArrowUpRight className="ml-2 h-4 w-4" />
                </Button>
              </Link>
            </div>
          </div>

          <div className="rounded-[1.4rem] border border-white/10 bg-white/[0.07] p-4 backdrop-blur sm:rounded-[2rem] sm:p-5">
            <div className="flex items-center justify-between gap-3">
              <div>
                <div className="text-[9px] font-black uppercase tracking-[0.16em] text-emerald-200 sm:text-[11px] sm:tracking-[0.24em]">This Month SaaS Revenue</div>
                <div className="mt-1 text-3xl font-black tracking-tight sm:mt-2 sm:text-4xl">{money.format(data.monthlySaasRevenue)}</div>
              </div>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-300/15 text-emerald-200">
                <CreditCard className="h-7 w-7" />
              </div>
            </div>
            <div className="mt-5 grid grid-cols-2 gap-3">
              <div className="rounded-2xl bg-white/10 p-4">
                <div className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400">Previous</div>
                <div className="mt-2 text-xl font-black">{money.format(data.previousMonthlySaasRevenue)}</div>
              </div>
              <div className="rounded-2xl bg-white/10 p-4">
                <div className="flex items-center justify-between gap-3 text-[10px] font-black uppercase tracking-[0.2em] text-slate-400">
                  <span>Lifetime</span>
                  <button
                    type="button"
                    onClick={() => setShowLifetimeRevenue((current) => !current)}
                    className="rounded-full p-1 text-slate-300 transition hover:bg-white/10 hover:text-white"
                    aria-label={showLifetimeRevenue ? "Hide lifetime revenue" : "Show lifetime revenue"}
                  >
                    {showLifetimeRevenue ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                <div className="mt-2 text-xl font-black">{showLifetimeRevenue ? money.format(data.lifetimeSaasRevenue) : "XXXXXXXX"}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-2 sm:gap-4 xl:grid-cols-5">
        {metricCards.map((card) => {
          const Icon = card.icon;
          return (
            <div key={card.label} className={`relative overflow-hidden rounded-[1.15rem] border bg-gradient-to-br p-3 shadow-sm sm:rounded-[1.75rem] sm:p-5 ${card.tone} ${card.borderTone}`}>
              <div className="absolute -right-8 -top-8 h-28 w-28 rounded-full bg-white/10 blur-2xl" />
              <div className="relative z-10">
                <Icon className="h-5 w-5 opacity-90 sm:h-7 sm:w-7" />
                <div className="mt-3 text-[9px] font-black uppercase tracking-[0.12em] opacity-70 sm:mt-5 sm:text-[10px] sm:tracking-[0.24em]">{card.label}</div>
                <div className="mt-1 text-2xl font-black tracking-tight sm:mt-2 sm:text-3xl">{card.value}</div>
                <div className="mt-1 line-clamp-2 text-[11px] font-bold opacity-75 sm:mt-2 sm:text-sm">{card.detail}</div>
              </div>
            </div>
          );
        })}
      </div>

      <div className="grid gap-5 xl:grid-cols-[1.08fr_0.92fr]">
        <div className="glass-panel rounded-[1.5rem] p-4 sm:rounded-[2rem] sm:p-6">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div className="text-[9px] font-black uppercase tracking-[0.16em] text-emerald-700 sm:text-[11px] sm:tracking-[0.24em]">Subscription Rate</div>
              <h2 className="mt-1 text-lg font-black tracking-tight text-slate-950 sm:mt-2 sm:text-2xl">Paid vs pending subscriptions by month</h2>
            </div>
            <Badge className="w-fit border-slate-200 bg-white text-slate-800">
              {data.pendingPayments} pending now
            </Badge>
          </div>
          <SubscriptionActivityChart items={data.monthlySubscriptionActivity} />
        </div>

        <div className="glass-panel rounded-[1.5rem] p-4 sm:rounded-[2rem] sm:p-6">
          <div className="flex items-center justify-between gap-3">
            <div>
              <div className="text-[11px] font-black uppercase tracking-[0.24em] text-slate-500">Current Month</div>
              <h2 className="mt-2 text-2xl font-black tracking-tight text-slate-950">Subscription snapshot</h2>
            </div>
            <CreditCard className="h-7 w-7 text-emerald-700" />
          </div>
          <div className="mt-4 grid grid-cols-2 gap-2 sm:mt-5 sm:gap-3">
            <SnapshotCard
              label="Paid"
              value={latestMonth?.paidSubscriptions ?? 0}
              detail={money.format(latestMonth?.paidAmount ?? 0)}
              tone="emerald"
            />
            <SnapshotCard
              label="Pending"
              value={latestMonth?.pendingSubscriptions ?? 0}
              detail={money.format(latestMonth?.pendingAmount ?? 0)}
              tone="amber"
            />
          </div>
          <div className="mt-5 rounded-[1.6rem] border border-slate-100 bg-slate-50/90 p-4">
            <div className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">What this means</div>
            <p className="mt-2 text-sm font-bold leading-6 text-slate-600">
              Super admins can read current collection momentum at a glance: completed subscriptions, pending subscriptions, and the rupee value attached to both.
            </p>
          </div>
        </div>
      </div>

      <div className="grid gap-5 xl:grid-cols-[1fr_0.9fr]">
        <div className="glass-panel rounded-[2rem] p-5 sm:p-6">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div className="text-[11px] font-black uppercase tracking-[0.24em] text-emerald-700">Subscription Distribution</div>
              <h2 className="mt-2 text-2xl font-black tracking-tight text-slate-950">Billing cohort mix</h2>
            </div>
            <Badge className="w-fit border-amber-200 bg-amber-100 text-amber-800">{data.trialOrganizations} free trial gyms</Badge>
          </div>
          <div className="mt-5 grid gap-4 md:grid-cols-2">
            {data.organizationsBySubscriptionPlan.map((item) => (
              <BreakdownBar key={item.key} item={item} amount />
            ))}
          </div>
        </div>

        <div className="glass-panel rounded-[2rem] p-5 sm:p-6">
          <div className="flex items-center justify-between gap-3">
            <div>
              <div className="text-[11px] font-black uppercase tracking-[0.24em] text-rose-700">Lifecycle Watchlist</div>
              <h2 className="mt-2 text-2xl font-black tracking-tight text-slate-950">Where attention is needed</h2>
            </div>
            <Sparkles className="h-7 w-7 text-amber-400" />
          </div>
          <div className="mt-5 space-y-3">
            {data.lifecycleAlerts.map((item) => (
              <div key={item.key} className="flex items-center justify-between gap-4 rounded-2xl border border-white/75 bg-white/70 p-4 shadow-sm">
                <div>
                  <div className="text-sm font-black text-slate-950">{item.label}</div>
                  <div className="mt-1 text-xs font-bold text-slate-500">{item.percentage}% of visible platform load</div>
                </div>
                <div className="text-2xl font-black text-slate-950">{compact.format(item.count)}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="grid gap-5 lg:grid-cols-3">
        <div className="glass-panel rounded-[2rem] p-5 sm:p-6">
          <TrendingUp className="h-7 w-7 text-emerald-700" />
          <div className="mt-5 text-[11px] font-black uppercase tracking-[0.24em] text-slate-500">Growth</div>
          <div className="mt-2 text-3xl font-black text-slate-950">{percentText(data.organizationGrowthPercent)}</div>
          <p className="mt-2 text-sm font-bold text-slate-500">
            {data.currentMonthNewOrganizations} orgs this month vs {data.previousMonthNewOrganizations} last month.
          </p>
        </div>
        <div className="glass-panel rounded-[2rem] p-5 sm:p-6">
          <MessageCircle className="h-7 w-7 text-emerald-700" />
          <div className="mt-5 text-[11px] font-black uppercase tracking-[0.24em] text-slate-500">Communication Load</div>
          <div className="mt-2 text-3xl font-black text-slate-950">{compact.format(data.totalCommunicationUsage)}</div>
          <p className="mt-2 text-sm font-bold text-slate-500">
            {compact.format(data.whatsappUsage)} WhatsApp and {compact.format(data.emailUsage)} email this month.
          </p>
        </div>
        <div className="glass-panel rounded-[2rem] p-5 sm:p-6">
          <PauseCircle className="h-7 w-7 text-orange-600" />
          <div className="mt-5 text-[11px] font-black uppercase tracking-[0.24em] text-slate-500">Billing Risk</div>
          <div className="mt-2 text-3xl font-black text-slate-950">{data.paymentFailures + data.pendingPayments}</div>
          <p className="mt-2 text-sm font-bold text-slate-500">
            {data.paymentFailures} failed payments and {data.pendingPayments} pending payments.
          </p>
        </div>
      </div>

      <div className="grid gap-5 xl:grid-cols-[0.85fr_1.15fr]">
        <div className="glass-panel rounded-[2rem] p-5 sm:p-6">
          <div className="flex items-center justify-between gap-3">
            <div>
              <div className="text-[11px] font-black uppercase tracking-[0.24em] text-slate-500">Organization States</div>
              <h2 className="mt-2 text-2xl font-black text-slate-950">Status split</h2>
            </div>
            <ShieldOff className="h-7 w-7 text-slate-400" />
          </div>
          <div className="mt-5 space-y-3">
            {data.organizationsByStatus.map((item) => (
              <BreakdownBar key={item.key} item={item} />
            ))}
          </div>
        </div>

        <Link to="/app/organizations" className="group block">
          <div className="relative h-full overflow-hidden rounded-[2rem] border border-emerald-100 bg-[linear-gradient(135deg,rgba(236,253,245,0.95),rgba(255,255,255,0.88)),radial-gradient(circle_at_top_right,rgba(52,211,153,0.32),transparent_24rem)] p-6 shadow-sm transition duration-300 group-hover:-translate-y-1 group-hover:shadow-xl sm:p-8">
            <div className="absolute -right-12 -bottom-16 h-56 w-56 rounded-full bg-emerald-300/25 blur-3xl" />
            <div className="relative z-10 flex h-full flex-col justify-between gap-8">
              <div>
                <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-950 text-white">
                  <Building2 className="h-7 w-7" />
                </div>
                <h2 className="mt-6 max-w-xl text-3xl font-black tracking-tight text-slate-950">Manage every organization from the redesigned command directory.</h2>
                <p className="mt-3 max-w-xl text-sm font-bold leading-6 text-slate-600">
                  Filter by lifecycle, active-member usage, owner, district, and risk. The list is optimized for quick scanning instead of heavy card browsing.
                </p>
              </div>
              <div className="flex flex-wrap gap-3">
                <Badge className="border-sky-200 bg-sky-100 text-sky-800">
                  <Rocket className="mr-1 h-3.5 w-3.5" />
                  {data.pilotsEndingInThirtyDays} founding partner access windows ending soon
                </Badge>
                <Badge className="border-amber-200 bg-amber-100 text-amber-800">
                  <CalendarClock className="mr-1 h-3.5 w-3.5" />
                  {data.renewalsDueInSevenDays} renewals due
                </Badge>
                <Badge className="border-slate-200 bg-white text-slate-800">
                  <Mail className="mr-1 h-3.5 w-3.5" />
                  {compact.format(data.emailUsage)} emails
                </Badge>
              </div>
            </div>
          </div>
        </Link>
      </div>
    </div>
  );
}
