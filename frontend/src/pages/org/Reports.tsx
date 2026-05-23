import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  AlertTriangle,
  BarChart3,
  CalendarRange,
  Download,
  FileSpreadsheet,
  FileText,
  Lock,
  RefreshCw,
  SlidersHorizontal,
  TrendingUp,
  type LucideIcon,
  Users,
  Wallet,
} from "lucide-react";
import { Link } from "react-router-dom";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import api from "@/lib/api";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

type ReportPeriod = "monthly" | "yearly";
type ReportRange = "current_month" | "last_month" | "current_year" | "custom";

type FinancialReport = {
  period: string;
  range: string;
  startDate: string;
  endDate: string;
  organizationName: string;
  logoUrl?: string | null;
  planName: string;
  planAccess?: {
    canReceiveMonthlyReports?: boolean;
    canExportReports?: boolean;
  };
  summary?: {
    totalRevenue?: number;
    admissionRevenue?: number;
    membershipRevenue?: number;
    duePayments?: number;
    expenses?: number;
    admissions?: number;
    activeMembers?: number;
    overdueMembers?: number;
  };
  months?: FinancialReportMonth[];
};

type FinancialReportMonth = {
  year: number;
  month: number;
  label: string;
  totalRevenue: number;
  admissionRevenue: number;
  membershipRevenue: number;
  duePayments: number;
  expenses: number;
  admissions: number;
  activeMembers: number;
  overdueMembers: number;
  planDetails: string;
};

const rangeOptions: Array<{ value: ReportRange; label: string }> = [
  { value: "current_month", label: "Current month" },
  { value: "last_month", label: "Last month" },
  { value: "current_year", label: "Current year" },
  { value: "custom", label: "Custom range" },
];

const money = (value?: number | string | null) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(Number(value ?? 0));

const compact = new Intl.NumberFormat("en-IN", { notation: "compact", maximumFractionDigits: 1 });

export default function Reports() {
  const [period, setPeriod] = useState<ReportPeriod>("monthly");
  const [range, setRange] = useState<ReportRange>("current_month");
  const [customFrom, setCustomFrom] = useState(() => new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().slice(0, 10));
  const [customTo, setCustomTo] = useState(() => new Date().toISOString().slice(0, 10));
  const [exportingType, setExportingType] = useState<"csv" | "pdf" | null>(null);

  const { data: subscription, isLoading: subscriptionLoading } = useQuery({
    queryKey: ["reportsSubscriptionStatus"],
    queryFn: async () => {
      const res = await api.get("/subscription/status");
      return res.data;
    },
    staleTime: 60_000,
  });

  const params = useMemo(() => {
    const next: Record<string, string> = { period, range };
    if (range === "custom") {
      next.from = customFrom;
      next.to = customTo;
    }
    return next;
  }, [period, range, customFrom, customTo]);

  const canViewReports = Boolean(subscription?.planAccess?.canReceiveMonthlyReports);
  const canExportReports = Boolean(subscription?.planAccess?.canExportReports);

  const { data: report, isLoading, refetch, isFetching } = useQuery<FinancialReport>({
    queryKey: ["financialReport", params],
    queryFn: async () => {
      const res = await api.get("/reports/financial", { params });
      return res.data;
    },
    enabled: canViewReports,
  });

  async function exportReport(type: "csv" | "pdf") {
    if (!canExportReports) {
      toast.error("Upgrade to Pro to unlock this feature");
      return;
    }
    setExportingType(type);
    try {
      const response = await api.get("/tracking/export", {
        params: { ...params, type },
        responseType: "blob",
      });
      const blob = new Blob([response.data], {
        type: type === "pdf" ? "application/pdf" : "text/csv;charset=utf-8",
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `financial-report-${report?.startDate ?? "range"}-${report?.endDate ?? "export"}.${type}`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      toast.success(type === "pdf" ? "PDF report exported" : "CSV report exported");
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Unable to export report");
    } finally {
      setExportingType(null);
    }
  }

  if (subscriptionLoading) {
    return <div className="p-8">Loading reports access...</div>;
  }

  if (!canViewReports) {
    return (
      <div className="mx-auto max-w-7xl space-y-8 p-8">
        <section className="billmygym-shell overflow-hidden rounded-[2rem] p-8 lg:p-10">
          <div className="grid gap-8 lg:grid-cols-[1fr_0.8fr] lg:items-center">
            <div>
              <Badge className="border-amber-200 bg-amber-100 text-amber-800">
                <Lock className="mr-1.5 h-3.5 w-3.5" />
                Reports Locked
              </Badge>
              <h1 className="mt-5 max-w-2xl text-4xl font-black tracking-tight text-slate-950 lg:text-5xl">
                Upgrade to Pro to unlock this feature
              </h1>
              <p className="mt-4 max-w-2xl text-base font-medium leading-7 text-slate-600">
                Financial reports, month-wise revenue views, and report exports are available on Growth, Pro, Pilot, and Demo workspaces.
              </p>
              <div className="mt-7 flex flex-wrap gap-3">
                <Link to="/app/subscription/plans">
                  <Button className="h-12 rounded-full bg-slate-950 px-6 font-bold text-white hover:bg-slate-800">
                    View Plans
                  </Button>
                </Link>
              </div>
            </div>
            <div className="rounded-[1.7rem] border border-white/80 bg-white/72 p-6 shadow-sm">
              <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">Current Plan</div>
              <div className="mt-2 text-4xl font-black tracking-tight text-slate-950">{subscription?.subscriptionPlan ?? "STARTER"}</div>
              <div className="mt-4 rounded-2xl border border-amber-100 bg-amber-50 px-4 py-3 text-sm font-bold text-amber-900">
                Upgrade to Pro to unlock this feature
              </div>
            </div>
          </div>
        </section>
      </div>
    );
  }

  const summary = report?.summary ?? {};
  const rows = report?.months ?? [];
  const maxRevenue = Math.max(...rows.map((row) => Number(row.totalRevenue ?? 0)), 1);

  return (
    <div className="mx-auto max-w-7xl space-y-8 p-8">
      <section className="billmygym-shell overflow-hidden rounded-[2rem] p-8 lg:p-10">
        <div className="grid gap-8 lg:grid-cols-[1.05fr_0.95fr] lg:items-center">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <Badge className="border-emerald-200 bg-emerald-100 text-emerald-800">
                <BarChart3 className="mr-1.5 h-3.5 w-3.5" />
                Financial Reports
              </Badge>
              <Badge className="border-slate-200 bg-white/80 text-slate-700">{report?.planName ?? subscription?.subscriptionPlan ?? "PLAN"}</Badge>
            </div>
            <h1 className="mt-5 max-w-3xl text-4xl font-black tracking-tight text-slate-950 lg:text-5xl">
              Month-wise revenue clarity for the gym.
            </h1>
            <p className="mt-4 max-w-2xl text-base font-medium leading-7 text-slate-600">
              Track admissions, membership collections, due payments, and member health in one export-ready workspace.
            </p>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <SummarySignal icon={Wallet} label="Total Revenue" value={money(summary.totalRevenue)} tone="emerald" />
            <SummarySignal icon={TrendingUp} label="Membership Revenue" value={money(summary.membershipRevenue)} tone="slate" />
            <SummarySignal icon={Users} label="Admissions" value={compact.format(Number(summary.admissions ?? 0))} tone="sky" />
            <SummarySignal icon={AlertTriangle} label="Due Payments" value={money(summary.duePayments)} tone="rose" />
          </div>
        </div>
      </section>

      <section className="glass-panel rounded-[1.8rem] p-5">
        <div className="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
          <div className="grid flex-1 gap-4 md:grid-cols-[0.8fr_1.2fr] xl:max-w-3xl">
            <div>
              <div className="mb-2 flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">
                <SlidersHorizontal className="h-3.5 w-3.5" />
                Period
              </div>
              <div className="grid grid-cols-2 gap-2 rounded-2xl border border-white/80 bg-white/60 p-1">
                {(["monthly", "yearly"] as ReportPeriod[]).map((item) => (
                  <button
                    key={item}
                    type="button"
                    onClick={() => {
                      setPeriod(item);
                      if (item === "yearly" && range !== "custom") {
                        setRange("current_year");
                      }
                    }}
                    className={cn(
                      "h-10 rounded-xl text-sm font-black capitalize transition-colors",
                      period === item ? "bg-slate-950 text-white shadow-sm" : "text-slate-600 hover:bg-white",
                    )}
                  >
                    {item}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <div className="mb-2 flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">
                <CalendarRange className="h-3.5 w-3.5" />
                Date Range
              </div>
              <div className="grid gap-2 sm:grid-cols-4">
                {rangeOptions.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => setRange(option.value)}
                    className={cn(
                      "h-10 rounded-xl border px-3 text-xs font-black transition-colors",
                      range === option.value
                        ? "border-emerald-200 bg-emerald-100 text-emerald-800"
                        : "border-white/80 bg-white/60 text-slate-600 hover:bg-white",
                    )}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
            {range === "custom" && (
              <div className="grid gap-3 sm:grid-cols-2">
                <Input type="date" value={customFrom} onChange={(event) => setCustomFrom(event.target.value)} className="h-11 rounded-xl bg-white/75" />
                <Input type="date" value={customTo} onChange={(event) => setCustomTo(event.target.value)} className="h-11 rounded-xl bg-white/75" />
              </div>
            )}
            <Button
              type="button"
              variant="outline"
              className="h-11 rounded-xl border-white/80 bg-white/75 px-4 font-bold"
              onClick={() => refetch()}
              disabled={isFetching}
            >
              <RefreshCw className={cn("mr-2 h-4 w-4", isFetching && "animate-spin")} />
              Refresh
            </Button>
            {canExportReports ? (
              <DropdownMenu>
                <DropdownMenuTrigger
                  render={
                    <Button className="h-11 rounded-xl bg-slate-950 px-5 font-bold text-white hover:bg-slate-800" disabled={Boolean(exportingType)}>
                      <Download className="mr-2 h-4 w-4" />
                      {exportingType ? "Exporting..." : "Export"}
                    </Button>
                  }
                />
                <DropdownMenuContent align="end" className="w-48 rounded-2xl border border-white/80 bg-white/95 p-2 shadow-[0_18px_40px_-24px_rgba(15,23,42,0.32)] backdrop-blur-xl">
                  <DropdownMenuItem className="rounded-xl px-3 py-2 font-semibold" onClick={() => exportReport("csv")}>
                    <FileSpreadsheet className="h-4 w-4" />
                    Export CSV
                  </DropdownMenuItem>
                  <DropdownMenuItem className="rounded-xl px-3 py-2 font-semibold" onClick={() => exportReport("pdf")}>
                    <FileText className="h-4 w-4" />
                    Export PDF
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            ) : (
              <Button
                type="button"
                className="h-11 rounded-xl bg-slate-200 px-5 font-bold text-slate-600 hover:bg-slate-200"
                onClick={() => toast.error("Upgrade to Pro to unlock this feature")}
              >
                <Lock className="mr-2 h-4 w-4" />
                Export Locked
              </Button>
            )}
          </div>
        </div>

        {!canExportReports && (
          <div className="mt-4 rounded-[1.25rem] border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-bold text-amber-900">
            Upgrade to Pro to unlock this feature
          </div>
        )}
      </section>

      {isLoading ? (
        <div className="glass-panel rounded-[1.8rem] p-10 text-center text-sm font-semibold text-slate-500">
          Loading financial report...
        </div>
      ) : (
        <>
          <section className="grid gap-5 md:grid-cols-4">
            <MetricTile label="Admission Revenue" value={money(summary.admissionRevenue)} />
            <MetricTile label="Active Members" value={compact.format(Number(summary.activeMembers ?? 0))} />
            <MetricTile label="Overdue Members" value={compact.format(Number(summary.overdueMembers ?? 0))} />
            <MetricTile label="Expenses" value={money(summary.expenses)} />
          </section>

          <section className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
            <div className="glass-panel overflow-hidden rounded-[1.9rem]">
              <div className="border-b border-white/70 p-6">
                <div className="text-[10px] font-black uppercase tracking-[0.24em] text-emerald-700">Revenue Trend</div>
                <h2 className="mt-2 text-2xl font-black tracking-tight text-slate-950">Month-wise performance</h2>
              </div>
              <div className="grid min-h-[310px] grid-cols-2 gap-3 p-6 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-3">
                {rows.map((row) => {
                  const height = `${Math.max((Number(row.totalRevenue ?? 0) / maxRevenue) * 100, Number(row.totalRevenue ?? 0) > 0 ? 12 : 4)}%`;
                  return (
                    <div key={`${row.year}-${row.month}`} className="rounded-[1.35rem] border border-white/80 bg-white/72 p-4 shadow-sm">
                      <div className="flex h-32 items-end rounded-[1.1rem] bg-slate-50 p-3">
                        <div className="w-full rounded-full bg-gradient-to-t from-emerald-700 via-emerald-500 to-sky-300" style={{ height }} />
                      </div>
                      <div className="mt-3 text-center text-[10px] font-black uppercase tracking-[0.18em] text-slate-500">{row.label}</div>
                      <div className="mt-1 text-center text-lg font-black text-slate-950">{money(row.totalRevenue)}</div>
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="glass-panel overflow-hidden rounded-[1.9rem]">
              <div className="border-b border-white/70 p-6">
                <div className="text-[10px] font-black uppercase tracking-[0.24em] text-slate-500">Report Contents</div>
                <h2 className="mt-2 text-2xl font-black tracking-tight text-slate-950">Financial details</h2>
              </div>
              <div className="overflow-x-auto">
                <table className="min-w-[920px] w-full text-left text-sm">
                  <thead className="border-b border-black/5 bg-white/35 text-slate-500">
                    <tr>
                      <th className="px-5 py-4 text-xs font-black uppercase tracking-[0.18em]">Month</th>
                      <th className="px-5 py-4 text-xs font-black uppercase tracking-[0.18em]">Total</th>
                      <th className="px-5 py-4 text-xs font-black uppercase tracking-[0.18em]">Admission</th>
                      <th className="px-5 py-4 text-xs font-black uppercase tracking-[0.18em]">Membership</th>
                      <th className="px-5 py-4 text-xs font-black uppercase tracking-[0.18em]">Due</th>
                      <th className="px-5 py-4 text-xs font-black uppercase tracking-[0.18em]">Admissions</th>
                      <th className="px-5 py-4 text-xs font-black uppercase tracking-[0.18em]">Active</th>
                      <th className="px-5 py-4 text-xs font-black uppercase tracking-[0.18em]">Overdue</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-black/5">
                    {rows.map((row) => (
                      <tr key={`${row.year}-${row.month}`} className="transition-colors hover:bg-white/50">
                        <td className="px-5 py-4 font-black text-slate-950">{row.label}</td>
                        <td className="px-5 py-4 font-bold text-slate-700">{money(row.totalRevenue)}</td>
                        <td className="px-5 py-4 font-semibold text-slate-600">{money(row.admissionRevenue)}</td>
                        <td className="px-5 py-4 font-semibold text-slate-600">{money(row.membershipRevenue)}</td>
                        <td className="px-5 py-4 font-semibold text-rose-700">{money(row.duePayments)}</td>
                        <td className="px-5 py-4 font-semibold text-slate-600">{row.admissions}</td>
                        <td className="px-5 py-4 font-semibold text-slate-600">{row.activeMembers}</td>
                        <td className="px-5 py-4 font-semibold text-slate-600">{row.overdueMembers}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        </>
      )}
    </div>
  );
}

function SummarySignal({
  icon: Icon,
  label,
  value,
  tone,
}: {
  icon: LucideIcon;
  label: string;
  value: string;
  tone: "emerald" | "slate" | "sky" | "rose";
}) {
  const toneClass = {
    emerald: "border-emerald-100 bg-emerald-50/72 text-emerald-900",
    slate: "border-slate-200 bg-white/82 text-slate-900",
    sky: "border-sky-100 bg-sky-50/72 text-sky-900",
    rose: "border-rose-100 bg-rose-50/72 text-rose-900",
  }[tone];

  return (
    <div className={cn("rounded-[1.5rem] border p-5 shadow-sm", toneClass)}>
      <Icon className="h-5 w-5" />
      <div className="mt-4 text-[10px] font-black uppercase tracking-[0.2em] opacity-70">{label}</div>
      <div className="mt-2 text-2xl font-black tracking-tight">{value}</div>
    </div>
  );
}

function MetricTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="glass-panel rounded-[1.5rem] p-5">
      <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">{label}</div>
      <div className="mt-2 text-3xl font-black tracking-tight text-slate-950">{value}</div>
    </div>
  );
}
