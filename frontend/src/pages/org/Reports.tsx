import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  AlertTriangle,
  BarChart3,
  CalendarRange,
  Download,
  FileSpreadsheet,
  FileText,
  Info,
  RefreshCw,
  TrendingUp,
  type LucideIcon,
  Users,
  Wallet,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import api from "@/lib/api";
import { cn } from "@/lib/utils";
import { toast } from "sonner";
import { useDemoPlanPreview } from "@/context/DemoPlanPreviewContext";

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
    deactivatedMembers?: number;
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
  deactivatedMembers: number;
};

const rangeOptions: Array<{ value: ReportRange; label: string }> = [
  { value: "current_month", label: "Current month" },
  { value: "last_month", label: "Last month" },
  { value: "current_year", label: "Current year" },
  { value: "custom", label: "Custom range" },
];

const INDIAN_TIME_ZONE = "Asia/Kolkata";

const money = (value?: number | string | null) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(Number(value ?? 0));

const compact = new Intl.NumberFormat("en-IN", { notation: "compact", maximumFractionDigits: 1 });

function todayInIndiaIso() {
  return isoDateInIndia(new Date());
}

function currentYearStartInIndiaIso() {
  const today = todayInIndiaIso();
  return `${today.slice(0, 4)}-01-01`;
}

function currentYearMonthInIndia() {
  return todayInIndiaIso().slice(0, 7);
}

function isoDateInIndia(date: Date) {
  const parts = new Intl.DateTimeFormat("en-GB", {
    timeZone: INDIAN_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(date);
  const get = (type: string) => parts.find((part) => part.type === type)?.value ?? "";
  return `${get("year")}-${get("month")}-${get("day")}`;
}

function monthYearLabel(value?: string | null) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("en-IN", {
    month: "long",
    year: "numeric",
    timeZone: INDIAN_TIME_ZONE,
  }).format(new Date(`${value}T00:00:00+05:30`));
}

function fullDateLabel(value?: string | null) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("en-IN", {
    day: "numeric",
    month: "long",
    year: "numeric",
    timeZone: INDIAN_TIME_ZONE,
  }).format(new Date(`${value}T00:00:00+05:30`));
}

function reportWindowLabel(startDate?: string | null, endDate?: string | null) {
  if (!startDate || !endDate) return "-";
  const sameMonth = startDate.slice(0, 7) === endDate.slice(0, 7);
  if (sameMonth) {
    return monthYearLabel(startDate);
  }
  return `${monthYearLabel(startDate)} to ${monthYearLabel(endDate)}`;
}

export default function Reports() {
  const { effectivePlan, isDemoWorkspace } = useDemoPlanPreview();
  const [period, setPeriod] = useState<ReportPeriod>("monthly");
  const [range, setRange] = useState<ReportRange>("current_year");
  const [customFrom, setCustomFrom] = useState(() => currentYearStartInIndiaIso());
  const [customTo, setCustomTo] = useState(() => todayInIndiaIso());
  const [exportingType, setExportingType] = useState<"csv" | "pdf" | null>(null);

  const { data: subscription, isLoading: subscriptionLoading } = useQuery({
    queryKey: ["reportsSubscriptionStatus"],
    queryFn: async () => {
      const res = await api.get("/subscription/status");
      return res.data;
    },
    staleTime: 60_000,
  });

  const previewPlan = isDemoWorkspace ? effectivePlan : subscription?.subscriptionPlan ?? "BILLMYGYM";
  const params = useMemo(() => {
    const next: Record<string, string> = { period, range };
    if (range === "custom") {
      next.from = customFrom;
      next.to = customTo;
    }
    return next;
  }, [period, range, customFrom, customTo]);

  const todayIso = todayInIndiaIso();
  const currentMonthKey = currentYearMonthInIndia();
  const customRangeInvalid = range === "custom" && (!customFrom || !customTo || customFrom > customTo || customFrom > todayIso);
  const customRangeMessage = customFrom > customTo
    ? "Start date must be on or before end date."
    : customFrom > todayIso
      ? "Start date cannot be in the future."
      : "Choose both dates to generate a custom report.";

  const { data: report, isLoading, refetch, isFetching } = useQuery<FinancialReport>({
    queryKey: ["financialReport", params],
    queryFn: async () => {
      const res = await api.get("/reports/financial", { params });
      return res.data;
    },
    enabled: !customRangeInvalid,
  });
  const visibleWindowLabel = reportWindowLabel(report?.startDate ?? customFrom, report?.endDate ?? customTo);
  const visibleWindowDetail = report?.startDate && report?.endDate
    ? `${fullDateLabel(report.startDate)} to ${fullDateLabel(report.endDate)}`
    : `${fullDateLabel(customFrom)} to ${fullDateLabel(customTo)}`;

  function updateCustomFrom(value: string) {
    const next = value > todayIso ? todayIso : value;
    setCustomFrom(next);
    if (next > customTo) {
      setCustomTo(next);
    }
  }

  function updateCustomTo(value: string) {
    const next = value > todayIso ? todayIso : value;
    setCustomTo(next);
    if (customFrom > next) {
      setCustomFrom(next);
    }
  }

  async function exportReport(type: "csv" | "pdf") {
    if (customRangeInvalid) {
      toast.error(customRangeMessage);
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
              <Badge className="border-slate-200 bg-white/80 text-slate-700">{previewPlan ?? report?.planName ?? "PLAN"}</Badge>
            </div>
            <h1 className="mt-5 max-w-3xl text-4xl font-black tracking-tight text-slate-950 lg:text-5xl">
              Month-wise revenue clarity for the gym.
            </h1>
            <p className="mt-4 max-w-2xl text-base font-medium leading-7 text-slate-600">
              Track admissions, membership collections, due payments, and member health in one export-ready workspace.
            </p>
          </div>

          <div className="grid grid-cols-2 gap-2 sm:gap-3 md:grid-cols-4">
            <SummarySignal
              icon={Wallet}
              label="Total Revenue"
              value={money(summary.totalRevenue)}
              tone="emerald"
              breakdown={[
                { label: "Admission", value: money(summary.admissionRevenue) },
                { label: "Membership", value: money(summary.membershipRevenue) },
              ]}
              compact
            />
            <SummarySignal icon={TrendingUp} label="Membership" value={money(summary.membershipRevenue)} tone="slate" compact />
            <SummarySignal icon={Users} label="Admissions" value={compact.format(Number(summary.admissions ?? 0))} tone="sky" compact />
            <SummarySignal icon={AlertTriangle} label="Due Amount" value={money(summary.duePayments)} tone="rose" compact />
          </div>
        </div>
      </section>

      <section className="glass-panel overflow-hidden rounded-[1.8rem]">
        <div className="flex flex-col gap-4 border-b border-white/70 p-5 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.22em] text-emerald-700">
              <CalendarRange className="h-3.5 w-3.5" />
              Report Filters
            </div>
            <div className="mt-1 text-base font-black text-slate-900">
              {visibleWindowLabel}
            </div>
            <div className="mt-1 text-sm font-semibold text-slate-500">
              {visibleWindowDetail} in Indian Standard Time
            </div>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Button
              type="button"
              variant="outline"
              className="h-11 rounded-xl border-white/80 bg-white/80 px-4 font-bold"
              onClick={() => refetch()}
              disabled={isFetching || customRangeInvalid}
            >
              <RefreshCw className={cn("mr-2 h-4 w-4", isFetching && "animate-spin")} />
              Refresh
            </Button>
            <DropdownMenu>
              <DropdownMenuTrigger
                render={
                  <Button className="h-11 rounded-xl bg-slate-950 px-5 font-bold text-white hover:bg-slate-800" disabled={Boolean(exportingType) || customRangeInvalid}>
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
          </div>
        </div>

        <div className="grid gap-5 p-5 xl:grid-cols-[0.72fr_1.28fr]">
            <div>
              <div className="mb-2 text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">Period</div>
              <div className="grid grid-cols-2 gap-2 rounded-[1.2rem] border border-white/80 bg-white/80 p-1.5">
                {(["monthly", "yearly"] as ReportPeriod[]).map((item) => (
                  <button
                    key={item}
                    type="button"
                    onClick={() => {
                      setPeriod(item);
                      if (range !== "custom") {
                        setRange("current_year");
                      }
                    }}
                    className={cn(
                      "h-11 rounded-[1rem] text-sm font-black capitalize transition-all",
                      period === item
                        ? "bg-slate-950 text-white shadow-[0_12px_26px_-16px_rgba(15,23,42,0.8)]"
                        : "text-slate-600 hover:bg-slate-50",
                    )}
                  >
                    {item}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <div className="mb-2 text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">Date Range</div>
              <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
                {rangeOptions.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => setRange(option.value)}
                    className={cn(
                      "h-11 rounded-[1rem] border px-3 text-xs font-black transition-all",
                      range === option.value
                        ? "border-slate-950 bg-slate-950 text-white shadow-[0_12px_26px_-16px_rgba(15,23,42,0.8)]"
                        : "border-white/80 bg-white/80 text-slate-600 hover:bg-slate-50",
                    )}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>
          </div>

        {range === "custom" && (
          <div className="border-t border-white/70 p-5">
            <div className="grid gap-4 md:grid-cols-[1fr_1fr_auto] md:items-end">
              <label className="space-y-2">
                <span className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">From</span>
                <Input
                  type="date"
                  value={customFrom}
                  max={customTo < todayIso ? customTo : todayIso}
                  onChange={(event) => updateCustomFrom(event.target.value)}
                  className="h-11 rounded-xl bg-white/85"
                />
              </label>
              <label className="space-y-2">
                <span className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">To</span>
                <Input
                  type="date"
                  value={customTo}
                  min={customFrom}
                  max={todayIso}
                  onChange={(event) => updateCustomTo(event.target.value)}
                  className="h-11 rounded-xl bg-white/85"
                />
              </label>
              <div className="rounded-xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm font-bold text-emerald-800">
                Max 36 months
              </div>
            </div>
            {customRangeInvalid && (
              <div className="mt-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-bold text-amber-900">
                {customRangeMessage}
              </div>
            )}
          </div>
        )}

      </section>

      {isLoading ? (
        <div className="glass-panel rounded-[1.8rem] p-10 text-center text-sm font-semibold text-slate-500">
          Loading financial report...
        </div>
      ) : (
        <>
          <section className="grid gap-3 sm:gap-4 grid-cols-2 md:grid-cols-4">
            <MetricTile label="Admission" value={money(summary.admissionRevenue)} />
            <MetricTile label="Active" value={compact.format(Number(summary.activeMembers ?? 0))} />
            <MetricTile label="Overdue" value={compact.format(Number(summary.overdueMembers ?? 0))} />
            <MetricTile label="Deactivated" value={compact.format(Number(summary.deactivatedMembers ?? 0))} />
          </section>

          <section className="glass-panel overflow-hidden rounded-[1.9rem]">
            <div className="border-b border-white/70 p-4 sm:p-6">
              <div className="text-[10px] font-black uppercase tracking-[0.24em] text-emerald-700">Revenue Trend</div>
              <h2 className="mt-2 text-xl sm:text-2xl font-black tracking-tight text-slate-950">Month-wise performance</h2>
            </div>
            <div className="grid min-h-[280px] grid-cols-3 gap-2 p-4 sm:gap-3 sm:p-6 md:grid-cols-4 lg:grid-cols-6 xl:grid-cols-12">
              {rows.map((row) => {
                const height = `${Math.max((Number(row.totalRevenue ?? 0) / maxRevenue) * 100, Number(row.totalRevenue ?? 0) > 0 ? 12 : 4)}%`;
                const isCurrentMonth = `${row.year}-${String(row.month).padStart(2, "0")}` === currentMonthKey;
                return (
                  <div
                    key={`${row.year}-${row.month}`}
                    className={cn(
                      "rounded-[1.1rem] sm:rounded-[1.35rem] border bg-white/72 p-2 sm:p-3 shadow-sm xl:col-span-1",
                      isCurrentMonth
                        ? "border-emerald-300 ring-4 ring-emerald-100 shadow-[0_18px_38px_-26px_rgba(5,150,105,0.9)]"
                        : "border-white/80",
                    )}
                  >
                    <div className="flex h-32 sm:h-40 items-end rounded-[0.8rem] sm:rounded-[1.1rem] bg-[linear-gradient(180deg,rgba(16,185,129,0.08),rgba(255,255,255,0.95))] p-2 sm:p-3">
                      <div
                        className={cn(
                          "w-full rounded-full",
                          isCurrentMonth
                            ? "bg-gradient-to-t from-emerald-800 via-emerald-500 to-lime-300"
                            : "bg-gradient-to-t from-emerald-700 via-emerald-500 to-sky-300",
                        )}
                        style={{ height }}
                      />
                    </div>
                    <div className={cn("mt-2 text-center text-[8px] sm:text-[10px] font-black uppercase tracking-[0.16em]", isCurrentMonth ? "text-emerald-700" : "text-slate-500")}>
                      {row.label}
                    </div>
                    {isCurrentMonth && (
                      <div className="mt-0.5 text-center text-[7px] sm:text-[10px] font-black uppercase tracking-[0.16em] text-emerald-700">
                        Current
                      </div>
                    )}
                    <div className="mt-1 text-center text-xs sm:text-sm font-black text-slate-950">{money(row.totalRevenue)}</div>
                  </div>
                );
              })}
            </div>
          </section>

          <section className="space-y-3 sm:space-y-4">
            <div className="glass-panel overflow-hidden rounded-[1.9rem]">
              <div className="border-b border-white/70 p-4 sm:p-6">
                <div className="text-[10px] font-black uppercase tracking-[0.24em] text-slate-500">Report Contents</div>
                <h2 className="mt-2 text-xl sm:text-2xl font-black tracking-tight text-slate-950">Monthly details</h2>
              </div>
              <div className="space-y-2 p-4 sm:space-y-3 sm:p-6">
                {rows.map((row) => {
                  const isCurrentMonth = `${row.year}-${String(row.month).padStart(2, "0")}` === currentMonthKey;
                  return (
                    <div
                      key={`${row.year}-${row.month}`}
                      className={cn(
                        "rounded-[1.2rem] sm:rounded-[1.55rem] border bg-white/72 p-3 sm:p-5 shadow-sm",
                        isCurrentMonth
                          ? "border-emerald-300 ring-4 ring-emerald-100 shadow-[0_18px_42px_-28px_rgba(5,150,105,0.85)]"
                          : "border-white/80",
                      )}
                    >
                      <div className="grid gap-3 sm:gap-4 lg:grid-cols-[auto_1fr_auto]">
                        {/* Left: Month & Value */}
                        <div className="flex flex-col justify-center">
                          <div className="flex flex-wrap items-center gap-2">
                            <div className="text-[9px] sm:text-[10px] font-black uppercase tracking-[0.2em] text-emerald-700">{row.label}</div>
                            {isCurrentMonth && (
                              <Badge className="text-[7px] sm:text-xs border-emerald-200 bg-emerald-100 text-emerald-800 px-1.5 sm:px-2 py-0 sm:py-0.5">Current</Badge>
                            )}
                          </div>
                          <div className="mt-1.5 sm:mt-2 flex items-center gap-1.5 sm:gap-2 text-2xl sm:text-3xl font-black tracking-tight text-slate-950">
                            <span>{money(row.totalRevenue)}</span>
                            <RevenueInfoButton
                              admissionRevenue={money(row.admissionRevenue)}
                              membershipRevenue={money(row.membershipRevenue)}
                            />
                          </div>
                          <div className="mt-0.5 sm:hidden text-[8px] font-medium text-slate-500">Admission + Membership</div>
                        </div>

                        {/* Middle: Pills */}
                        <div className="grid grid-cols-2 gap-1.5 sm:grid-cols-5 sm:gap-2">
                          <MonthDetailPill label="Admission" value={money(row.admissionRevenue)} tone="sky" compact />
                          <MonthDetailPill label="Membership" value={money(row.membershipRevenue)} tone="emerald" compact />
                          <MonthDetailPill label="Overdue" value={money(row.duePayments)} tone="rose" compact />
                          <MonthDetailPill label="Active" value={String(row.activeMembers)} tone="slate" compact />
                          <MonthDetailPill label="Deactivated" value={String(row.deactivatedMembers)} tone="amber" compact />
                        </div>

                        {/* Right: Stats (hidden on mobile) */}
                        <div className="hidden lg:flex flex-wrap gap-1.5 items-center text-xs font-bold text-slate-600">
                          <span className="rounded-full border border-slate-200 bg-slate-50 px-2 py-1">{row.admissions} admissions</span>
                          <span className="rounded-full border border-rose-100 bg-rose-50 px-2 py-1 text-rose-700 whitespace-nowrap">{row.overdueMembers} overdue</span>
                        </div>
                      </div>
                    </div>
                  );
                })}
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
  breakdown,
  compact = false,
}: {
  icon: LucideIcon;
  label: string;
  value: string;
  tone: "emerald" | "slate" | "sky" | "rose";
  breakdown?: Array<{ label: string; value: string }>;
  compact?: boolean;
}) {
  const toneClass = {
    emerald: "border-emerald-100 bg-emerald-50/72 text-emerald-900",
    slate: "border-slate-200 bg-white/82 text-slate-900",
    sky: "border-sky-100 bg-sky-50/72 text-sky-900",
    rose: "border-rose-100 bg-rose-50/72 text-rose-900",
  }[tone];

  return (
    <div className={cn("rounded-[1.2rem] sm:rounded-[1.5rem] border shadow-sm", compact ? "p-2.5 sm:p-4" : "p-5", toneClass)}>
      <Icon className={cn(compact ? "h-4 w-4 sm:h-5 sm:w-5" : "h-5 w-5")} />
      <div className={cn("flex items-center gap-1.5 sm:gap-2 opacity-70", compact ? "mt-2 sm:mt-3 text-[8px] sm:text-[10px] font-black uppercase tracking-[0.18em]" : "mt-4 text-[10px] font-black uppercase tracking-[0.2em]")}>
        <span>{label}</span>
        {breakdown && <RevenueInfoButton items={breakdown} compact={compact} />}
      </div>
      <div className={cn("font-black tracking-tight", compact ? "mt-1.5 sm:mt-2 text-base sm:text-2xl" : "mt-2 text-2xl")}>{value}</div>
    </div>
  );
}

function RevenueInfoButton({
  admissionRevenue,
  membershipRevenue,
  items,
  compact = false,
}: {
  admissionRevenue?: string;
  membershipRevenue?: string;
  items?: Array<{ label: string; value: string }>;
  compact?: boolean;
}) {
  const breakdown = items ?? [
    { label: "Admission", value: admissionRevenue ?? money(0) },
    { label: "Membership", value: membershipRevenue ?? money(0) },
  ];

  return (
    <div className="group relative inline-flex">
      <button
        type="button"
        className={cn(
          "inline-flex items-center justify-center rounded-full border border-slate-200 bg-white/85 text-slate-600 shadow-sm transition-colors hover:bg-white hover:text-slate-950",
          compact ? "h-5 w-5" : "h-7 w-7",
        )}
        aria-label="Revenue split"
      >
        <Info className={compact ? "h-3 w-3" : "h-4 w-4"} />
      </button>
      <div className="pointer-events-none absolute bottom-full left-1/2 z-20 mb-3 w-56 -translate-x-1/2 translate-y-1 rounded-2xl border border-slate-200 bg-white/95 p-4 text-left text-xs font-semibold text-slate-700 opacity-0 shadow-xl backdrop-blur transition-all duration-200 group-hover:translate-y-0 group-hover:opacity-100">
        <div className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Revenue Split</div>
        {breakdown.map((item) => (
          <div key={item.label} className="mt-2 flex items-center justify-between gap-4">
            <span>{item.label}</span>
            <span className="font-black text-slate-950">{item.value}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function MetricTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="glass-panel rounded-[1.2rem] sm:rounded-[1.5rem] p-3 sm:p-4">
      <div className="text-[8px] sm:text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">{label}</div>
      <div className="mt-1.5 sm:mt-2 text-xl sm:text-3xl font-black tracking-tight text-slate-950">{value}</div>
    </div>
  );
}

function MonthDetailPill({
  label,
  value,
  tone,
  compact = false,
}: {
  label: string;
  value: string;
  tone: "slate" | "emerald" | "sky" | "rose" | "amber";
  compact?: boolean;
}) {
  const toneClass = {
    slate: "border-slate-200 bg-slate-50 text-slate-900",
    emerald: "border-emerald-100 bg-emerald-50 text-emerald-900",
    sky: "border-sky-100 bg-sky-50 text-sky-900",
    rose: "border-rose-100 bg-rose-50 text-rose-900",
    amber: "border-amber-100 bg-amber-50 text-amber-900",
  }[tone];

  return (
    <div className={cn("rounded-[1.1rem] border shadow-sm", compact ? "px-2 py-1.5 sm:px-4 sm:py-3" : "px-4 py-3", toneClass)}>
      <div className={cn("font-black uppercase tracking-[0.2em] opacity-70", compact ? "text-[7px] sm:text-[10px]" : "text-[10px]")}>{label}</div>
      <div className={cn("font-black", compact ? "mt-0.5 sm:mt-1 text-xs sm:text-base" : "mt-1 text-base")}>{value}</div>
    </div>
  );
}
