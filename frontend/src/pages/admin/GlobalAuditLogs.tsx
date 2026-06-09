import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, Building2, CreditCard, RefreshCcw } from "lucide-react";
import api from "@/lib/api";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

type AuditTab = "LOGIN" | "SUBSCRIPTIONS";

type LoginAuditRow = {
  time: string;
  organizationId?: number | null;
  organizationName?: string | null;
  adminName?: string | null;
  email?: string | null;
  description: string;
};

type SubscriptionAuditRow = {
  time: string;
  organizationId?: number | null;
  organizationName?: string | null;
  plan?: string | null;
  amount?: number | null;
  status: "PENDING" | "PAID" | "FAILED";
  paymentDate?: string | null;
  subscriptionStart?: string | null;
  subscriptionEnd?: string | null;
  orderId?: string | null;
  paymentId?: string | null;
  failureReason?: string | null;
};

type AuditPage<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

type LoginAuditResponse = {
  page: AuditPage<LoginAuditRow>;
};

type SubscriptionAuditResponse = {
  page: AuditPage<SubscriptionAuditRow>;
};

const money = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

function formatDate(value?: string | null) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("en-IN", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(value));
}

function formatDateTime(value?: string | null) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function paymentStatusClass(status: SubscriptionAuditRow["status"]) {
  return {
    PAID: "border-emerald-200 bg-emerald-100 text-emerald-800",
    PENDING: "border-amber-200 bg-amber-100 text-amber-800",
    FAILED: "border-rose-200 bg-rose-100 text-rose-800",
  }[status];
}

export default function GlobalAuditLogs() {
  const [activeTab, setActiveTab] = useState<AuditTab>("LOGIN");
  const [page, setPage] = useState(0);
  const [refreshToken, setRefreshToken] = useState(0);

  const auditQuery = useQuery<LoginAuditResponse | SubscriptionAuditResponse>({
    queryKey: ["globalAudit", activeTab, page, refreshToken],
    queryFn: async () => {
      const res = await api.get(activeTab === "LOGIN" ? "/audit-logs/login" : "/audit-logs/subscriptions", {
        params: {
          page,
          size: 20,
        },
      });
      return res.data;
    },
    enabled: true,
    staleTime: Number.POSITIVE_INFINITY,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });

  const rows = auditQuery.data?.page.items ?? [];
  const meta = auditQuery.data?.page;
  const subscriptionRows = activeTab === "SUBSCRIPTIONS" ? (rows as SubscriptionAuditRow[]) : [];
  const paidOnPage = subscriptionRows.filter((row) => row.status === "PAID").length;
  const pendingOnPage = subscriptionRows.filter((row) => row.status === "PENDING").length;
  const failedOnPage = subscriptionRows.filter((row) => row.status === "FAILED").length;
  const amountOnPage = subscriptionRows.reduce((total, row) => total + Number(row.amount ?? 0), 0);
  const queryErrorMessage =
    auditQuery.error instanceof Error ? auditQuery.error.message : "Unable to load the audit log right now.";

  function switchTab(tab: AuditTab) {
    setActiveTab(tab);
    setPage(0);
    setRefreshToken((current) => current + 1);
  }

  function requestAuditRefresh(nextPage = 0) {
    setPage(nextPage);
    if (nextPage === page) {
      setRefreshToken((current) => current + 1);
    }
  }

  return (
    <div className="mx-auto max-w-7xl space-y-3 p-2.5 sm:space-y-5 sm:p-6 lg:p-8">
      <section className="rounded-[1.35rem] border border-slate-200 bg-white p-3.5 shadow-sm sm:rounded-[2rem] sm:p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <div className="inline-flex items-center rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-[9px] font-black uppercase tracking-[0.18em] text-emerald-700 sm:px-4 sm:py-2 sm:text-[11px] sm:tracking-[0.24em]">
              Global Audit
            </div>
            <h1 className="mt-3 text-xl font-black tracking-tight text-slate-950 sm:mt-4 sm:text-3xl">
              {activeTab === "LOGIN" ? "Organization admin login activity" : "Organization subscription ledger"}
            </h1>
            <p className="mt-1.5 max-w-3xl text-xs font-semibold leading-5 text-slate-500 sm:mt-2 sm:text-sm sm:leading-6">
              {activeTab === "LOGIN"
                ? "Latest org-admin login events across every organization."
                : "Subscription payments across all organizations, including pending and failed attempts."}
            </p>
          </div>
          <Button
            type="button"
            variant="outline"
            onClick={() => requestAuditRefresh(0)}
            disabled={auditQuery.isFetching}
            className="h-9 rounded-full border-white/80 bg-white px-4 text-xs font-black sm:h-10 sm:px-5 sm:text-sm"
          >
            <RefreshCcw className={`mr-2 h-4 w-4 ${auditQuery.isFetching ? "animate-spin" : ""}`} />
            Refresh
          </Button>
        </div>

        <div className="mt-4 flex flex-wrap gap-2 sm:mt-5">
          <button
            type="button"
            onClick={() => switchTab("LOGIN")}
            className={cn(
              "inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-black sm:gap-2 sm:px-4 sm:py-2 sm:text-sm",
              activeTab === "LOGIN" ? "border-emerald-200 bg-emerald-100 text-emerald-800" : "border-slate-200 bg-white text-slate-600"
            )}
          >
            <Building2 className="h-4 w-4" />
            Login Audit
          </button>
          <button
            type="button"
            onClick={() => switchTab("SUBSCRIPTIONS")}
            className={cn(
              "inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-black sm:gap-2 sm:px-4 sm:py-2 sm:text-sm",
              activeTab === "SUBSCRIPTIONS" ? "border-emerald-200 bg-emerald-100 text-emerald-800" : "border-slate-200 bg-white text-slate-600"
            )}
          >
            <CreditCard className="h-4 w-4" />
            Subscriptions
          </button>
        </div>
      </section>

      <section className="overflow-hidden rounded-[1.35rem] border border-slate-200 bg-white shadow-sm sm:rounded-[2rem]">
        {activeTab === "SUBSCRIPTIONS" && (
          <div className="grid grid-cols-2 gap-2 border-b border-slate-100 bg-slate-50/80 p-2.5 sm:grid-cols-4 sm:gap-3 sm:p-4">
            <AuditMetric label="Paid on page" value={paidOnPage} tone="emerald" />
            <AuditMetric label="Pending on page" value={pendingOnPage} tone="amber" />
            <AuditMetric label="Failed on page" value={failedOnPage} tone="rose" />
            <AuditMetric label="Amount on page" value={money.format(amountOnPage)} tone="slate" />
          </div>
        )}
        {auditQuery.isFetching && (
          <div className="border-b border-slate-100 bg-slate-50 px-3 py-2 text-[10px] font-bold uppercase tracking-[0.16em] text-slate-500 sm:px-5 sm:py-3 sm:text-xs sm:tracking-[0.2em]">
            Refreshing audit...
          </div>
        )}
        {auditQuery.isError && (
          <div className="border-b border-rose-100 bg-rose-50 px-3 py-3 text-xs font-semibold text-rose-700 sm:px-5 sm:py-4 sm:text-sm">
            <div className="flex items-center gap-2">
              <AlertTriangle className="h-4 w-4" />
              {queryErrorMessage}
            </div>
          </div>
        )}

        {activeTab === "LOGIN" ? (
          <LoginAuditTable rows={rows as LoginAuditRow[]} />
        ) : (
          <SubscriptionAuditTable rows={rows as SubscriptionAuditRow[]} />
        )}

        <div className="flex flex-col gap-3 border-t border-slate-100 px-3 py-3 text-xs sm:flex-row sm:items-center sm:justify-between sm:px-4 sm:text-sm">
          <div className="font-semibold text-slate-500">
            Showing {rows.length} of {meta?.totalElements ?? 0}
          </div>
          <div className="flex items-center justify-between gap-2 sm:justify-end">
            <Button
              type="button"
              variant="outline"
              onClick={() => requestAuditRefresh(Math.max((meta?.page ?? 0) - 1, 0))}
              disabled={(meta?.page ?? 0) <= 0}
            >
              Previous
            </Button>
            <div className="text-center font-semibold text-slate-600">
              Page {(meta?.page ?? 0) + 1} / {Math.max(meta?.totalPages ?? 1, 1)}
            </div>
            <Button
              type="button"
              variant="outline"
              onClick={() => requestAuditRefresh((meta?.page ?? 0) + 1)}
              disabled={!meta?.hasNext}
            >
              Next
            </Button>
          </div>
        </div>
      </section>
    </div>
  );
}

function LoginAuditTable({ rows }: { rows: LoginAuditRow[] }) {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-[760px] text-left sm:min-w-full">
        <thead className="bg-slate-50">
          <tr className="text-[10px] uppercase tracking-wide text-slate-500 sm:text-xs">
            <th className="px-3 py-2.5 sm:px-4 sm:py-3">Time</th>
            <th className="px-3 py-2.5 sm:px-4 sm:py-3">Organization</th>
            <th className="px-3 py-2.5 sm:px-4 sm:py-3">Admin Name</th>
            <th className="px-3 py-2.5 sm:px-4 sm:py-3">Email</th>
            <th className="px-3 py-2.5 sm:px-4 sm:py-3">Description</th>
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 && (
            <tr>
              <td className="px-3 py-6 text-center text-xs font-semibold text-slate-500 sm:px-4 sm:py-8 sm:text-sm" colSpan={5}>
                No login audit rows found.
              </td>
            </tr>
          )}
          {rows.map((row, index) => (
            <tr key={`${row.time}-${index}`} className="border-t border-slate-100 text-xs text-slate-700 sm:text-sm">
              <td className="px-3 py-2.5 sm:px-4 sm:py-3">{formatDateTime(row.time)}</td>
              <td className="px-3 py-2.5 sm:px-4 sm:py-3">
                <div className="inline-flex items-center gap-2 font-semibold text-slate-700">
                  <Building2 className="h-4 w-4 text-slate-300" />
                  {row.organizationName || "-"}
                </div>
              </td>
              <td className="px-3 py-2.5 font-semibold text-slate-900 sm:px-4 sm:py-3">{row.adminName || "-"}</td>
              <td className="px-3 py-2.5 sm:px-4 sm:py-3">{row.email || "-"}</td>
              <td className="px-3 py-2.5 sm:px-4 sm:py-3">{row.description}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function SubscriptionAuditTable({ rows }: { rows: SubscriptionAuditRow[] }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[1060px] text-left sm:min-w-[1240px]">
        <thead className="bg-slate-50">
          <tr className="text-[10px] uppercase tracking-wide text-slate-500 sm:text-xs">
            <th className="px-3 py-2.5 sm:px-4 sm:py-3">Logged At</th>
            <th className="px-3 py-2.5 sm:px-4 sm:py-3">Organization</th>
            <th className="px-3 py-2.5 sm:px-4 sm:py-3">Plan</th>
            <th className="px-3 py-2.5 sm:px-4 sm:py-3">Amount</th>
            <th className="px-3 py-2.5 sm:px-4 sm:py-3">Status</th>
            <th className="px-3 py-2.5 sm:px-4 sm:py-3">Payment Date</th>
            <th className="px-3 py-2.5 sm:px-4 sm:py-3">Period</th>
            <th className="px-3 py-2.5 sm:px-4 sm:py-3">Gateway</th>
            <th className="px-3 py-2.5 sm:px-4 sm:py-3">Details</th>
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 && (
            <tr>
              <td className="px-3 py-6 text-center text-xs font-semibold text-slate-500 sm:px-4 sm:py-8 sm:text-sm" colSpan={9}>
                No subscription rows found.
              </td>
            </tr>
          )}
          {rows.map((row, index) => (
            <tr key={`${row.time}-${row.orderId}-${index}`} className="border-t border-slate-100 text-xs text-slate-700 sm:text-sm">
              <td className="px-3 py-2.5 font-semibold text-slate-900 sm:px-4 sm:py-3">{formatDateTime(row.time)}</td>
              <td className="px-3 py-2.5 sm:px-4 sm:py-3">
                <div className="font-semibold text-slate-900">{row.organizationName || "-"}</div>
                <div className="mt-1 text-[10px] font-semibold text-slate-500 sm:text-xs">Org ID: {row.organizationId ?? "-"}</div>
              </td>
              <td className="px-3 py-2.5 font-semibold sm:px-4 sm:py-3">{row.plan || "-"}</td>
              <td className="px-3 py-2.5 font-black text-slate-950 sm:px-4 sm:py-3">{money.format(Number(row.amount ?? 0))}</td>
              <td className="px-3 py-2.5 sm:px-4 sm:py-3">
                <Badge className={paymentStatusClass(row.status)}>{row.status}</Badge>
              </td>
              <td className="px-3 py-2.5 sm:px-4 sm:py-3">{formatDate(row.paymentDate)}</td>
              <td className="px-3 py-2.5 sm:px-4 sm:py-3">{formatDate(row.subscriptionStart)} - {formatDate(row.subscriptionEnd)}</td>
              <td className="px-3 py-2.5 text-[10px] font-semibold text-slate-500 sm:px-4 sm:py-3 sm:text-xs">
                <div>Order: {row.orderId || "-"}</div>
                <div className="mt-1">Payment: {row.paymentId || "-"}</div>
              </td>
              <td className="px-3 py-2.5 sm:px-4 sm:py-3">
                <div className="font-semibold text-slate-700">
                  {row.failureReason || "Subscription payment record"}
                </div>
                <div className="mt-1 text-[10px] text-slate-500 sm:text-xs">
                  {row.status === "PAID" ? "Successful charge captured for this organization." : row.status === "PENDING" ? "Payment is still awaiting completion or retry." : "Payment attempt failed and needs attention."}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function AuditMetric({
  label,
  value,
  tone,
}: {
  label: string;
  value: string | number;
  tone: "emerald" | "amber" | "rose" | "slate";
}) {
  const toneClass = {
    emerald: "border-emerald-100 bg-emerald-50 text-emerald-900",
    amber: "border-amber-100 bg-amber-50 text-amber-900",
    rose: "border-rose-100 bg-rose-50 text-rose-900",
    slate: "border-slate-200 bg-white text-slate-900",
  }[tone];

  return (
    <div className={`rounded-[1rem] border p-2.5 shadow-sm sm:rounded-[1.2rem] sm:p-4 ${toneClass}`}>
      <div className="text-[9px] font-black uppercase tracking-[0.14em] opacity-70 sm:text-[10px] sm:tracking-[0.2em]">{label}</div>
      <div className="mt-1.5 text-lg font-black tracking-tight sm:mt-2 sm:text-2xl">{value}</div>
    </div>
  );
}
