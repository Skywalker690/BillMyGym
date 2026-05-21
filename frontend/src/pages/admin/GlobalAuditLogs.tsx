import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, Building2, RefreshCcw } from "lucide-react";
import api from "@/lib/api";
import { Button } from "@/components/ui/button";

type LoginAuditRow = {
  time: string;
  organizationId?: number | null;
  organizationName?: string | null;
  adminName?: string | null;
  email?: string | null;
  device?: string | null;
  description: string;
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

function formatDateTime(value?: string) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export default function GlobalAuditLogs() {
  const [page, setPage] = useState(0);
  const [hasRequested, setHasRequested] = useState(false);
  const [refreshToken, setRefreshToken] = useState(0);

  const auditQuery = useQuery<LoginAuditResponse>({
    queryKey: ["globalLoginAudit", page, refreshToken],
    queryFn: async () => {
      const res = await api.get("/audit-logs/login", {
        params: {
          page,
          size: 20,
        },
      });
      return res.data;
    },
    enabled: hasRequested,
    staleTime: Number.POSITIVE_INFINITY,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });

  const rows = auditQuery.data?.page.items ?? [];
  const meta = auditQuery.data?.page;
  const queryErrorMessage =
    auditQuery.error instanceof Error ? auditQuery.error.message : "Unable to load the login audit right now.";

  function requestAuditRefresh(nextPage = 0) {
    setPage(nextPage);
    if (!hasRequested) {
      setHasRequested(true);
      return;
    }
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
              Login Audit
            </div>
            <h1 className="mt-4 text-3xl font-black tracking-tight text-slate-950">Organization admin login activity</h1>
            <p className="mt-2 max-w-3xl text-sm font-semibold leading-6 text-slate-500">
              Latest org-admin login events across every organization.
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
            {hasRequested ? "Refresh" : "Load Audit Logs"}
          </Button>
        </div>
      </section>

      <section className="overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-sm">
        {auditQuery.isFetching && (
          <div className="border-b border-slate-100 bg-slate-50 px-5 py-3 text-xs font-bold uppercase tracking-[0.2em] text-slate-500">
            Refreshing login audit...
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
                    {hasRequested ? "No login audit rows found." : "Click load audit logs to fetch records."}
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
        <div className="flex items-center justify-between border-t border-slate-100 px-4 py-3 text-sm">
          <div className="font-semibold text-slate-500">
            Showing {rows.length} of {meta?.totalElements ?? 0}
          </div>
          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => requestAuditRefresh(Math.max((meta?.page ?? 0) - 1, 0))}
              disabled={!hasRequested || (meta?.page ?? 0) <= 0}
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
              disabled={!hasRequested || !meta?.hasNext}
            >
              Next
            </Button>
          </div>
        </div>
      </section>
    </div>
  );
}
