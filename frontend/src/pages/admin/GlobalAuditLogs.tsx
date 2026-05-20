import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Download, Search } from "lucide-react";
import api from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

type OrganizationOption = {
  id: number;
  name: string;
};

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
  successfulCount: number;
  failedCount: number;
  suspiciousCount: number;
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
  const [search, setSearch] = useState("");
  const [organizationId, setOrganizationId] = useState<string>("ALL");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [page, setPage] = useState(0);

  const organizationsQuery = useQuery<OrganizationOption[]>({
    queryKey: ["auditOrganizationOptions"],
    queryFn: async () => {
      const res = await api.get("/organizations");
      return (res.data ?? []).map((item: any) => ({ id: item.id, name: item.name }));
    },
    staleTime: 60_000,
  });

  const auditQuery = useQuery<LoginAuditResponse>({
    queryKey: ["globalLoginAudit", search, organizationId, fromDate, toDate, page],
    queryFn: async () => {
      const res = await api.get("/audit-logs/login", {
        params: {
          tab: "ALL",
          search: search.trim() || undefined,
          organizationId: organizationId === "ALL" ? undefined : Number(organizationId),
          fromDate: fromDate || undefined,
          toDate: toDate || undefined,
          page,
          size: 20,
        },
      });
      return res.data;
    },
  });

  const rows = auditQuery.data?.page.items ?? [];
  const meta = auditQuery.data?.page;

  const exportCsv = async () => {
    const response = await api.get("/audit-logs/login/export", {
      params: {
        tab: "ALL",
        search: search.trim() || undefined,
        organizationId: organizationId === "ALL" ? undefined : Number(organizationId),
        fromDate: fromDate || undefined,
        toDate: toDate || undefined,
      },
      responseType: "blob",
    });
    const blob = new Blob([response.data], { type: "text/csv;charset=utf-8;" });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "global-login-audit.csv";
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  };

  return (
    <div className="mx-auto max-w-7xl space-y-5 p-4 sm:p-6 lg:p-8">
      <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
        <h1 className="text-2xl font-black text-slate-950">Global Audit Log</h1>
        <p className="mt-1 text-sm font-semibold text-slate-500">Latest login activity from organization admins across all organizations.</p>
      </section>

      <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="grid gap-3 md:grid-cols-5">
          <div className="relative md:col-span-2">
            <Search className="pointer-events-none absolute left-3 top-3.5 h-4 w-4 text-slate-400" />
            <Input
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                setPage(0);
              }}
              className="pl-9"
              placeholder="Search admin, email, description"
            />
          </div>
          <select
            value={organizationId}
            onChange={(event) => {
              setOrganizationId(event.target.value);
              setPage(0);
            }}
            className="h-10 rounded-md border border-slate-200 px-3 text-sm font-semibold"
          >
            <option value="ALL">All organizations</option>
            {(organizationsQuery.data ?? []).map((org) => (
              <option key={org.id} value={org.id}>
                {org.name}
              </option>
            ))}
          </select>
          <Input
            type="date"
            value={fromDate}
            onChange={(event) => {
              setFromDate(event.target.value);
              setPage(0);
            }}
          />
          <Input
            type="date"
            value={toDate}
            onChange={(event) => {
              setToDate(event.target.value);
              setPage(0);
            }}
          />
        </div>
        <div className="mt-3 flex justify-end">
          <Button type="button" variant="outline" onClick={exportCsv}>
            <Download className="mr-2 h-4 w-4" />
            Export CSV
          </Button>
        </div>
      </section>

      <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="min-w-full text-left">
            <thead className="bg-slate-50">
              <tr className="text-xs uppercase tracking-wide text-slate-500">
                <th className="px-4 py-3">Time</th>
                <th className="px-4 py-3">Organization</th>
                <th className="px-4 py-3">Admin Name</th>
                <th className="px-4 py-3">Email</th>
                <th className="px-4 py-3">Device</th>
                <th className="px-4 py-3">Description</th>
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 && (
                <tr>
                  <td className="px-4 py-8 text-center text-sm font-semibold text-slate-500" colSpan={6}>
                    No login audit rows found.
                  </td>
                </tr>
              )}
              {rows.map((row, index) => (
                <tr key={`${row.time}-${index}`} className="border-t border-slate-100 text-sm text-slate-700">
                  <td className="px-4 py-3">{formatDateTime(row.time)}</td>
                  <td className="px-4 py-3">{row.organizationName || "-"}</td>
                  <td className="px-4 py-3">{row.adminName || "-"}</td>
                  <td className="px-4 py-3">{row.email || "-"}</td>
                  <td className="px-4 py-3">{row.device || "-"}</td>
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
            <Button type="button" variant="outline" onClick={() => setPage((current) => Math.max(current - 1, 0))} disabled={(meta?.page ?? 0) <= 0}>
              Previous
            </Button>
            <div className="font-semibold text-slate-600">
              Page {(meta?.page ?? 0) + 1} / {Math.max(meta?.totalPages ?? 1, 1)}
            </div>
            <Button type="button" variant="outline" onClick={() => setPage((current) => current + 1)} disabled={!meta?.hasNext}>
              Next
            </Button>
          </div>
        </div>
      </section>
    </div>
  );
}
