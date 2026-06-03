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
    <div className="mx-auto max-w-7xl space-y-5 p-4 sm:p-6 lg:p-8">
      <section className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <div className="inline-flex items-center rounded-full border border-emerald-200 bg-emerald-50 px-4 py-2 text-[11px] font-black uppercase tracking-[0.24em] text-emerald-700">
              Global Audit
            </div>
            <h1 className="mt-4 text-3xl font-black tracking-tight text-slate-950">
              {activeTab === "LOGIN" ? "Organization admin login activity" : "Organization subscription ledger"}
            </h1>
            <p className="mt-2 max-w-3xl text-sm font-semibold leading-6 text-slate-500">
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
            className="rounded-full border-white/80 bg-white px-5 font-black"
          >
            <RefreshCcw className={`mr-2 h-4 w-4 ${auditQuery.isFetching ? "animate-spin" : ""}`} />
            Refresh
          </Button>
        </div>

        <div className="mt-5 flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => switchTab("LOGIN")}
            className={cn(
              "inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm font-black",
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
              "inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm font-black",
              activeTab === "SUBSCRIPTIONS" ? "border-emerald-200 bg-emerald-100 text-emerald-800" : "border-slate-200 bg-white text-slate-600"
            )}
          >
            <CreditCard className="h-4 w-4" />
            Subscriptions
          </button>
        </div>
      </section>

      <section className="overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-sm">
        {activeTab === "SUBSCRIPTIONS" && (
          <div className="grid gap-3 border-b border-slate-100 bg-slate-50/80 p-4 sm:grid-cols-4">
            <AuditMetric label="Paid on page" value={paidOnPage} tone="emerald" />
            <AuditMetric label="Pending on page" value={pendingOnPage} tone="amber" />
            <AuditMetric label="Failed on page" value={failedOnPage} tone="rose" />
            <AuditMetric label="Amount on page" value={money.format(amountOnPage)} tone="slate" />
          </div>
        )}
        {auditQuery.isFetching && (
          <div className="border-b border-slate-100 bg-slate-50 px-5 py-3 text-xs font-bold uppercase tracking-[0.2em] text-slate-500">
            Refreshing audit...
          </div>
        )}
        {auditQuery.isError && (
          <div className="border-b border-rose-100 bg-rose-50 px-5 py-4 text-sm font-semibold text-rose-700">
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

        <div className="flex items-center justify-between border-t border-slate-100 px-4 py-3 text-sm">
          <div className="font-semibold text-slate-500">
            Showing {rows.length} of {meta?.totalElements ?? 0}
          </div>
          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => requestAuditRefresh(Math.max((meta?.page ?? 0) - 1, 0))}
              disabled={(meta?.page ?? 0) <= 0}
            >
              Previous
            </Button>
            <div className="font-semibold text-slate-600">
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
      <table className="min-w-full text-left">
        <thead className="bg-slate-50">
          <tr className="text-xs uppercase tracking-wide text-slate-500">
            <th className="px-4 py-3">Time</th>
            <th className="px-4 py-3">Organization</th>
            <th className="px-4 py-3">Admin Name</th>
            <th className="px-4 py-3">Email</th>
            <th className="px-4 py-3">Description</th>
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 && (
            <tr>
              <td className="px-4 py-8 text-center text-sm font-semibold text-slate-500" colSpan={5}>
                No login audit rows found.
              </td>
            </tr>
          )}
          {rows.map((row, index) => (
            <tr key={`${row.time}-${index}`} className="border-t border-slate-100 text-sm text-slate-700">
              <td className="px-4 py-3">{formatDateTime(row.time)}</td>
              <td className="px-4 py-3">
                <div className="inline-flex items-center gap-2 font-semibold text-slate-700">
                  <Building2 className="h-4 w-4 text-slate-300" />
                  {row.organizationName || "-"}
                </div>
              </td>
              <td className="px-4 py-3 font-semibold text-slate-900">{row.adminName || "-"}</td>
              <td className="px-4 py-3">{row.email || "-"}</td>
              <td className="px-4 py-3">{row.description}</td>
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
      <table className="min-w-[1240px] w-full text-left">
        <thead className="bg-slate-50">
          <tr className="text-xs uppercase tracking-wide text-slate-500">
            <th className="px-4 py-3">Logged At</th>
            <th className="px-4 py-3">Organization</th>
            <th className="px-4 py-3">Plan</th>
            <th className="px-4 py-3">Amount</th>
            <th className="px-4 py-3">Status</th>
            <th className="px-4 py-3">Payment Date</th>
            <th className="px-4 py-3">Period</th>
            <th className="px-4 py-3">Gateway</th>
            <th className="px-4 py-3">Details</th>
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 && (
            <tr>
              <td className="px-4 py-8 text-center text-sm font-semibold text-slate-500" colSpan={9}>
                No subscription rows found.
              </td>
            </tr>
          )}
          {rows.map((row, index) => (
            <tr key={`${row.time}-${row.orderId}-${index}`} className="border-t border-slate-100 text-sm text-slate-700">
              <td className="px-4 py-3 font-semibold text-slate-900">{formatDateTime(row.time)}</td>
              <td className="px-4 py-3">
                <div className="font-semibold text-slate-900">{row.organizationName || "-"}</div>
                <div className="mt-1 text-xs font-semibold text-slate-500">Org ID: {row.organizationId ?? "-"}</div>
              </td>
              <td className="px-4 py-3 font-semibold">{row.plan || "-"}</td>
              <td className="px-4 py-3 font-black text-slate-950">{money.format(Number(row.amount ?? 0))}</td>
              <td className="px-4 py-3">
                <Badge className={paymentStatusClass(row.status)}>{row.status}</Badge>
              </td>
              <td className="px-4 py-3">{formatDate(row.paymentDate)}</td>
              <td className="px-4 py-3">{formatDate(row.subscriptionStart)} - {formatDate(row.subscriptionEnd)}</td>
              <td className="px-4 py-3 text-xs font-semibold text-slate-500">
                <div>Order: {row.orderId || "-"}</div>
                <div className="mt-1">Payment: {row.paymentId || "-"}</div>
              </td>
              <td className="px-4 py-3">
                <div className="font-semibold text-slate-700">
                  {row.failureReason || "Subscription payment record"}
                </div>
                <div className="mt-1 text-xs text-slate-500">
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
    <div className={`rounded-[1.2rem] border p-4 shadow-sm ${toneClass}`}>
      <div className="text-[10px] font-black uppercase tracking-[0.2em] opacity-70">{label}</div>
      <div className="mt-2 text-2xl font-black tracking-tight">{value}</div>
    </div>
  );
}
