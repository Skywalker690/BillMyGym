import { useDeferredValue, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  ArrowUpRight,
  Building2,
  CalendarDays,
  CheckCircle2,
  Crown,
  MapPin,
  PauseCircle,
  Plus,
  Search,
  Sparkles,
  Trash2,
  Users,
} from "lucide-react";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import api from "@/lib/api";
import { organizationStatusLabel, organizationTypeLabel } from "@/lib/subscriptionLabels";

type Organization = {
  id: number;
  name: string;
  place?: string | null;
  district?: string | null;
  adminFullName?: string | null;
  adminEmail?: string | null;
  adminPhoneNumber?: string | null;
  status: "TRIAL" | "ACTIVE" | "PAUSED" | "DEACTIVATED";
  organizationType?: "REGULAR" | "DEMO";
  subscriptionPlan?: "BILLMYGYM";
  subscriptionDueDate?: string | null;
  trialEndDate?: string | null;
  activeMembers?: number | null;
  includedActiveMembers?: number | null;
  overageMembers?: number | null;
  overageRate?: number | null;
  overageAmount?: number | null;
  estimatedMonthlyBill?: number | null;
};

type DirectoryScope = "ALL" | "REGULAR" | "DEMO" | "REGULAR_TRIAL";

type DistributionItem = {
  key: string;
  label: string;
  count: number;
  percentage: number;
};

type DirectorySummary = {
  totalOrganizations: number;
  totalActiveOrganizations: number;
  totalRenewalPendingOrganizations: number;
  totalPilotOrganizations: number;
  totalTrialOrganizations: number;
  subscriptionDistribution: DistributionItem[];
};

type DirectoryResponse = {
  items: Organization[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  summary: DirectorySummary;
};

const lifecycleStatuses = ["ALL", "TRIAL", "ACTIVE", "PAUSED", "DEACTIVATED"] as const;
const pageSize = 20;

const badgeClasses: Record<string, string> = {
  ACTIVE: "border-emerald-200 bg-emerald-100 text-emerald-800",
  TRIAL: "border-amber-200 bg-amber-100 text-amber-800",
  PAUSED: "border-rose-200 bg-rose-100 text-rose-800",
  DEACTIVATED: "border-slate-300 bg-slate-200 text-slate-800",
  DEMO: "border-cyan-200 bg-cyan-100 text-cyan-800",
  BILLMYGYM: "border-emerald-200 bg-emerald-100 text-emerald-800",
};

const money = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

function formatDate(value?: string | null) {
  if (!value) return "Not scheduled";
  return new Intl.DateTimeFormat("en-IN", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(value));
}

function organizationPlanLabel(plan?: Organization["subscriptionPlan"]) {
  return plan === "BILLMYGYM" ? "BillMyGym" : "BillMyGym";
}

function isRenewalAttention(org: Organization) {
  const today = new Date();
  const nextWeek = new Date();
  nextWeek.setDate(today.getDate() + 7);

  const parse = (value?: string | null) => (value ? new Date(value) : null);
  const dueDate =
    org.status === "TRIAL"
        ? parse(org.trialEndDate ?? org.subscriptionDueDate)
        : parse(org.subscriptionDueDate);

  if (!dueDate) return org.status === "PAUSED" || org.status === "DEACTIVATED";
  return org.status === "PAUSED" || org.status === "DEACTIVATED" || dueDate <= nextWeek;
}

function lifecycleMeta(org: Organization) {
  if (org.organizationType === "DEMO") {
    return { label: "Demo Workspace", value: "Billing disabled for this showcase workspace" };
  }
  if (org.status === "TRIAL") {
    return { label: "Free Trial Access", value: `Access Ends: ${formatDate(org.trialEndDate ?? org.subscriptionDueDate)}` };
  }
  if (org.status === "ACTIVE") {
    return { label: "Active Subscription", value: `Next Renewal: ${formatDate(org.subscriptionDueDate)}` };
  }
  return { label: organizationStatusLabel(org.status), value: `Renewal Date: ${formatDate(org.subscriptionDueDate)}` };
}

function formatMoney(value?: number | null) {
  return money.format(Number(value ?? 0));
}

function usageLabel(org: Organization) {
  const activeMembers = Number(org.activeMembers ?? 0);
  if (org.includedActiveMembers == null) {
    return `${activeMembers} active members`;
  }
  return `${activeMembers}/${org.includedActiveMembers} active members`;
}

export default function OrganizationsList() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [searchTerm, setSearchTerm] = useState("");
  const [scope, setScope] = useState<DirectoryScope>("ALL");
  const [statusFilter, setStatusFilter] = useState<(typeof lifecycleStatuses)[number]>("ALL");
  const deferredSearch = useDeferredValue(searchTerm);

  useEffect(() => {
    setPage(0);
  }, [deferredSearch, scope, statusFilter]);

  const directoryQuery = useQuery<DirectoryResponse>({
    queryKey: ["organizationsDirectory", page, deferredSearch, scope, statusFilter],
    queryFn: async () => {
      const res = await api.get("/organizations/directory", {
        params: {
          page,
          size: pageSize,
          search: deferredSearch || undefined,
          includeAllTypes: scope === "ALL",
          pilotOnly: scope === "DEMO",
          organizationType:
            scope === "ALL"
              ? undefined
              : scope === "REGULAR" || scope === "REGULAR_TRIAL"
              ? "REGULAR"
              : "DEMO",
          status: scope === "REGULAR_TRIAL" ? "TRIAL" : statusFilter !== "ALL" ? statusFilter : undefined,
        },
      });
      return res.data;
    },
    placeholderData: (previousData) => previousData,
    staleTime: 60_000,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.delete(`/organizations/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["organizationsDirectory"] });
      queryClient.invalidateQueries({ queryKey: ["platformAnalytics"] });
      queryClient.invalidateQueries({ queryKey: ["platformYearlyReport"] });
      toast.success("Organization successfully soft deleted");
    },
    onError: () => toast.error("Failed to delete organization"),
  });

  const directory = directoryQuery.data;
  const organizations = directory?.items ?? [];
  const summary = directory?.summary;

  const paginationNumbers = useMemo(() => {
    const totalPages = directory?.totalPages ?? 0;
    const currentPage = directory?.page ?? 0;
    const start = Math.max(0, currentPage - 2);
    const end = Math.min(totalPages, start + 5);
    return Array.from({ length: Math.max(end - start, 0) }, (_, index) => start + index);
  }, [directory?.page, directory?.totalPages]);

  return (
    <div className="mx-auto w-full max-w-7xl space-y-6 p-4 sm:p-6 lg:p-8">
      <section className="relative overflow-hidden rounded-[2.4rem] border border-white/80 bg-slate-950 p-6 text-white shadow-[0_30px_90px_rgba(15,23,42,0.28)] sm:p-8">
        <div className="absolute -right-24 -top-24 h-72 w-72 rounded-full bg-emerald-400/20 blur-3xl" />
        <div className="absolute bottom-0 left-12 h-64 w-64 rounded-full bg-rose-400/10 blur-3xl" />
        <div className="relative z-10 flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <div className="inline-flex items-center rounded-full border border-emerald-300/20 bg-emerald-300/10 px-4 py-2 text-[11px] font-black uppercase tracking-[0.28em] text-emerald-200">
              Super Admin Directory
            </div>
            <h1 className="mt-5 max-w-4xl text-4xl font-black tracking-tight sm:text-5xl">
              Review gyms with renewal pressure first, then scan the rest with clean pagination.
            </h1>
            <p className="mt-4 max-w-3xl text-sm font-semibold leading-6 text-slate-300">
              Organizations with subscription renewal required are prioritized at the top. Every tab and filter loads a fresh page of 20 so the directory stays fast and predictable.
            </p>
          </div>
          <Link to="/app/organizations/new">
            <Button className="h-12 rounded-full bg-white px-7 font-black text-slate-950 hover:bg-emerald-50">
              <Plus className="mr-2 h-5 w-5" />
              Create Organization
            </Button>
          </Link>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          icon={Building2}
          label="Total Organizations"
          value={summary?.totalOrganizations ?? 0}
          detail="Across every workspace"
          tone="slate"
        />
        <MetricCard
          icon={CheckCircle2}
          label="Active Organizations"
          value={summary?.totalActiveOrganizations ?? 0}
          detail="Healthy paid subscriptions"
          tone="emerald"
        />
        <MetricCard
          icon={PauseCircle}
          label="Renewal Pending"
          value={summary?.totalRenewalPendingOrganizations ?? 0}
          detail="Needs billing attention"
          tone="rose"
        />
        <MetricCard
          icon={Crown}
          label="Trial Organizations"
          value={summary?.totalTrialOrganizations ?? 0}
          detail="Currently in free trial"
          tone="sky"
        />
      </section>

      <section className="grid gap-5 xl:grid-cols-[1.2fr_0.8fr]">
        <div className="glass-panel rounded-[2rem] p-5 sm:p-6">
          <div className="flex items-center justify-between gap-3">
            <div>
              <div className="text-[11px] font-black uppercase tracking-[0.24em] text-emerald-700">Subscription Distribution</div>
              <h2 className="mt-2 text-2xl font-black tracking-tight text-slate-950">BillMyGym adoption across the platform</h2>
            </div>
            <Sparkles className="h-6 w-6 text-amber-400" />
          </div>
          <div className="mt-5 grid gap-4 md:grid-cols-2">
            {(summary?.subscriptionDistribution ?? []).map((item) => (
              <div key={item.key} className="rounded-[1.5rem] border border-white/80 bg-white/80 p-4 shadow-sm">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="text-sm font-black text-slate-950">{item.label}</div>
                    <div className="mt-1 text-xs font-bold text-slate-500">
                      {item.count} gyms
                    </div>
                  </div>
                  <Badge className={badgeClasses[item.key] ?? badgeClasses.BILLMYGYM}>{item.percentage}%</Badge>
                </div>
                <div className="mt-4 h-2.5 overflow-hidden rounded-full bg-slate-100">
                  <div
                    className={`h-full rounded-full ${
                      item.key === "BILLMYGYM"
                        ? "bg-emerald-500"
                        : item.key === "TRIAL"
                          ? "bg-sky-500"
                          : "bg-slate-500"
                    }`}
                    style={{ width: `${Math.min(Math.max(item.percentage, 0), 100)}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="glass-panel rounded-[2rem] p-5 sm:p-6">
          <div className="text-[11px] font-black uppercase tracking-[0.24em] text-rose-700">Directory Controls</div>
          <h2 className="mt-2 text-2xl font-black tracking-tight text-slate-950">Tabs, filters, and search</h2>
          <div className="mt-5 flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => setScope("ALL")}
              className={`rounded-full px-5 py-2 text-sm font-black transition ${
                scope === "ALL" ? "bg-slate-950 text-white" : "bg-white text-slate-700 shadow-sm"
              }`}
            >
              All Gyms
            </button>
            <button
              type="button"
              onClick={() => setScope("DEMO")}
              className={`rounded-full px-5 py-2 text-sm font-black transition ${
                scope === "DEMO" ? "bg-slate-950 text-white" : "bg-white text-slate-700 shadow-sm"
              }`}
            >
              Demo Gyms
            </button>
            <button
              type="button"
              onClick={() => setScope("REGULAR_TRIAL")}
              className={`rounded-full px-5 py-2 text-sm font-black transition ${
                scope === "REGULAR_TRIAL" ? "bg-slate-950 text-white" : "bg-white text-slate-700 shadow-sm"
              }`}
            >
              Free Trial Gyms
            </button>
          </div>

          <div className="mt-5 space-y-3">
            <div className="relative">
              <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <Input
                placeholder="Search by gym, place, district, owner, or email..."
                className="h-12 rounded-2xl border-white/80 bg-white/75 pl-11 font-semibold shadow-sm focus-visible:ring-emerald-500/50"
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
              />
            </div>
            {scope !== "REGULAR_TRIAL" && (
              <select
                className="h-12 w-full rounded-2xl border border-white/80 bg-white/75 px-4 py-2 text-sm font-black text-slate-700 shadow-sm outline-none focus:ring-2 focus:ring-emerald-500/40"
                value={statusFilter}
                onChange={(event) => setStatusFilter(event.target.value as (typeof lifecycleStatuses)[number])}
              >
                {lifecycleStatuses.map((status) => (
                  <option key={status} value={status}>
                    {status === "ALL" ? "All subscription states" : organizationStatusLabel(status)}
                  </option>
                ))}
              </select>
            )}
          </div>
        </div>
      </section>

      <section className="overflow-hidden rounded-[2rem] border border-white/80 bg-white/80 shadow-sm backdrop-blur">
        {directoryQuery.isFetching && (
          <div className="border-b border-slate-100 bg-slate-50 px-5 py-3 text-xs font-bold uppercase tracking-[0.2em] text-slate-500">
            Refreshing organizations...
          </div>
        )}
        <div className="hidden grid-cols-[1.2fr_0.9fr_1.15fr_1fr_auto] gap-4 border-b border-slate-100 px-5 py-4 text-[10px] font-black uppercase tracking-[0.22em] text-slate-500 lg:grid">
          <div>Organization</div>
          <div>Admin</div>
          <div>Members & Overage</div>
          <div>Renewal & Lifecycle</div>
          <div className="text-right">Actions</div>
        </div>

        {organizations.length === 0 ? (
          <div className="p-14 text-center">
            <AlertTriangle className="mx-auto h-8 w-8 text-slate-300" />
            <p className="mt-4 text-sm font-bold text-slate-500">No organizations match the current view.</p>
            <p className="mt-2 text-xs font-semibold text-slate-400">Try changing the tab, lifecycle filter, or search term.</p>
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {organizations.map((org) => {
              const lifecycle = lifecycleMeta(org);
              const attention = isRenewalAttention(org);
              const plan = org.subscriptionPlan ?? "BILLMYGYM";

              return (
                <div key={org.id} className="grid gap-4 p-5 transition hover:bg-white/90 lg:grid-cols-[1.2fr_0.9fr_1.15fr_1fr_auto] lg:items-center">
                  <div className="min-w-0">
                    <div className="flex items-start gap-3">
                      <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-slate-950 text-white shadow-sm">
                        <Building2 className="h-5 w-5" />
                      </div>
                      <div className="min-w-0">
                        <Link to={`/app/organizations/${org.id}`} className="group inline-flex max-w-full items-center gap-2">
                          <span className="truncate text-lg font-black text-slate-950 group-hover:text-emerald-700">{org.name}</span>
                          <ArrowUpRight className="h-4 w-4 shrink-0 text-slate-300 group-hover:text-emerald-700" />
                        </Link>
                        <div className="mt-1 flex min-w-0 items-center gap-1.5 text-sm font-bold text-slate-500">
                          <MapPin className="h-3.5 w-3.5 shrink-0" />
                          <span className="truncate">
                            {org.place ? `${org.place}, ` : ""}{org.district || "No district"}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="rounded-2xl border border-slate-100 bg-slate-50/80 p-3 lg:border-0 lg:bg-transparent lg:p-0">
                    <div className="truncate text-sm font-black text-slate-900">{org.adminFullName || "Unassigned"}</div>
                    <div className="mt-1 truncate text-xs font-semibold text-slate-500">{org.adminEmail || "No email"}</div>
                    <div className="mt-1 truncate text-xs font-semibold text-slate-400">{org.adminPhoneNumber || "No phone"}</div>
                  </div>

                  <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-3">
                    <div className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Usage</div>
                    <div className="mt-1 text-sm font-black text-slate-950">{usageLabel(org)}</div>
                    <div className="mt-2 text-xs font-semibold text-slate-600">
                      {Number(org.overageMembers ?? 0) > 0
                        ? `${org.overageMembers} extra members at ${formatMoney(org.overageRate)} each`
                        : "No extra member charges"}
                    </div>
                    <div className="mt-2 flex flex-wrap items-center gap-2">
                      <Badge className={Number(org.overageMembers ?? 0) > 0 ? "border-rose-200 bg-rose-100 text-rose-800" : "border-emerald-200 bg-emerald-100 text-emerald-800"}>
                        {Number(org.overageMembers ?? 0) > 0 ? `+${org.overageMembers} extra` : "Within included limit"}
                      </Badge>
                      <span className="text-xs font-black text-slate-700">Extra: {formatMoney(org.overageAmount)}</span>
                    </div>
                    <div className="mt-2 text-xs font-bold text-slate-600">
                      Estimated bill: <span className="text-slate-950">{formatMoney(org.estimatedMonthlyBill)}</span>
                    </div>
                    <div className="mt-2 flex flex-wrap gap-2">
                      <Badge className={badgeClasses[plan]}>{organizationPlanLabel(plan)}</Badge>
                      <Badge className={badgeClasses[org.status]}>{organizationStatusLabel(org.status)}</Badge>
                      {org.organizationType === "DEMO" && (
                        <Badge className={badgeClasses[org.organizationType]}>{organizationTypeLabel(org.organizationType)}</Badge>
                      )}
                    </div>
                  </div>

                  <div className={`rounded-2xl border p-3 ${attention ? "border-rose-200 bg-rose-50/80" : "border-emerald-100 bg-emerald-50/70"}`}>
                    <div className={`flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.2em] ${attention ? "text-rose-700" : "text-emerald-700"}`}>
                      <CalendarDays className="h-4 w-4" />
                      {lifecycle.label}
                    </div>
                    <div className={`mt-1 text-sm font-black ${attention ? "text-rose-700" : "text-slate-950"}`}>{lifecycle.value}</div>
                  </div>

                  <div className="flex items-center justify-end gap-2">
                    <Link to={`/app/organizations/${org.id}`}>
                      <Button variant="outline" size="sm" className="rounded-full border-white/80 bg-white px-5 font-black hover:bg-emerald-50">
                        Manage
                      </Button>
                    </Link>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="rounded-full text-red-500 hover:bg-red-50 hover:text-red-600"
                      onClick={() => {
                        if (confirm("Soft delete this organization? History, members, and payments will be preserved.")) {
                          deleteMutation.mutate(org.id);
                        }
                      }}
                      disabled={deleteMutation.isPending}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      <section className="flex flex-col gap-4 rounded-[1.6rem] border border-emerald-100 bg-emerald-50/80 p-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-2 text-sm font-bold text-emerald-950">
          <Users className="h-4 w-4" />
          Showing {organizations.length} of {directory?.totalElements ?? 0} organizations in this view
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Button
            type="button"
            variant="outline"
            className="rounded-full border-white/80 bg-white px-4 font-black"
            onClick={() => setPage((current) => Math.max(current - 1, 0))}
            disabled={(directory?.page ?? 0) === 0}
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Previous
          </Button>
          {paginationNumbers.map((pageNumber) => (
            <button
              key={pageNumber}
              type="button"
              onClick={() => setPage(pageNumber)}
              className={`h-10 min-w-10 rounded-full px-3 text-sm font-black transition ${
                pageNumber === (directory?.page ?? 0) ? "bg-slate-950 text-white" : "bg-white text-slate-700 shadow-sm"
              }`}
            >
              {pageNumber + 1}
            </button>
          ))}
          <Button
            type="button"
            variant="outline"
            className="rounded-full border-white/80 bg-white px-4 font-black"
            onClick={() => setPage((current) => current + 1)}
            disabled={!directory?.hasNext}
          >
            Next
            <ArrowRight className="ml-2 h-4 w-4" />
          </Button>
        </div>
      </section>
    </div>
  );
}

function MetricCard({
  icon: Icon,
  label,
  value,
  detail,
  tone,
}: {
  icon: typeof Building2;
  label: string;
  value: number;
  detail: string;
  tone: "slate" | "emerald" | "rose" | "sky";
}) {
  const tones = {
    slate: "from-slate-950 to-slate-800 text-white",
    emerald: "from-emerald-600 to-teal-500 text-white",
    rose: "from-rose-600 to-orange-500 text-white",
    sky: "from-sky-600 to-indigo-500 text-white",
  }[tone];

  return (
    <div className={`relative overflow-hidden rounded-[1.75rem] bg-gradient-to-br p-5 shadow-sm ${tones}`}>
      <div className="absolute -right-8 -top-8 h-28 w-28 rounded-full bg-white/10 blur-2xl" />
      <div className="relative z-10">
        <Icon className="h-7 w-7 opacity-90" />
        <div className="mt-5 text-[10px] font-black uppercase tracking-[0.24em] opacity-70">{label}</div>
        <div className="mt-2 text-3xl font-black tracking-tight">{value}</div>
        <div className="mt-2 text-sm font-bold opacity-75">{detail}</div>
      </div>
    </div>
  );
}
