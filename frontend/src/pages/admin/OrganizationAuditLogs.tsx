import { useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Search } from "lucide-react";
import api from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

type TabKey = "MEMBER_ACTIVITY" | "MEMBER_STATUS_ACTIVITY" | "NOTIFICATION_ACTIVITY" | "ADMIN_ACTIVITY";

type AuditPage<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};

type MemberActivityRow = {
  time: string;
  admin: string;
  action: string;
  member: string;
  description: string;
};

type MemberStatusRow = {
  time: string;
  admin: string;
  member: string;
  action: string;
  description: string;
};

type AdminActivityRow = {
  time: string;
  admin: string;
  action: string;
  description: string;
};

type NotificationRow = {
  time: string;
  channel: "WHATSAPP" | "EMAIL";
  receiver: string;
  status: "PENDING" | "SENT" | "FAILED" | "RETRYING";
  description: string;
};

type NotificationAuditResponse = {
  page: AuditPage<NotificationRow>;
  summary: {
    whatsappSent: number;
    emailSent: number;
    failedCount: number;
    todaySentCount: number;
    monthlySentCount: number;
  };
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

export default function OrganizationAuditLogs() {
  const { id } = useParams();
  const [tab, setTab] = useState<TabKey>("MEMBER_ACTIVITY");
  const [search, setSearch] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [page, setPage] = useState(0);

  const organizationQuery = useQuery({
    queryKey: ["organizationBrief", id],
    queryFn: async () => {
      const res = await api.get(`/organizations/${id}`);
      return res.data;
    },
    enabled: Boolean(id),
  });

  const memberActivityQuery = useQuery<AuditPage<MemberActivityRow>>({
    queryKey: ["organizationAuditMember", id, tab, search, fromDate, toDate, page],
    queryFn: async () => {
      const endpoint =
        tab === "MEMBER_ACTIVITY"
          ? `/organizations/${id}/audit-logs/member-activity`
          : tab === "MEMBER_STATUS_ACTIVITY"
            ? `/organizations/${id}/audit-logs/member-status`
            : `/organizations/${id}/audit-logs/admin-activity`;
      const res = await api.get(endpoint, {
        params: {
          search: search.trim() || undefined,
          fromDate: fromDate || undefined,
          toDate: toDate || undefined,
          page,
          size: 20,
        },
      });
      return res.data;
    },
    enabled: Boolean(id) && tab !== "NOTIFICATION_ACTIVITY",
  });

  const notificationQuery = useQuery<NotificationAuditResponse>({
    queryKey: ["organizationAuditNotification", id, search, fromDate, toDate, page],
    queryFn: async () => {
      const res = await api.get(`/organizations/${id}/audit-logs/notification-activity`, {
        params: {
          search: search.trim() || undefined,
          fromDate: fromDate || undefined,
          toDate: toDate || undefined,
          page,
          size: 20,
        },
      });
      return res.data;
    },
    enabled: Boolean(id) && tab === "NOTIFICATION_ACTIVITY",
  });

  const pageMeta = tab === "NOTIFICATION_ACTIVITY" ? notificationQuery.data?.page : memberActivityQuery.data;

  const tabItems = useMemo(
    () => [
      { key: "MEMBER_ACTIVITY" as const, label: "Member Activity" },
      { key: "MEMBER_STATUS_ACTIVITY" as const, label: "Member Status Activity" },
      { key: "NOTIFICATION_ACTIVITY" as const, label: "Notification Activity" },
      { key: "ADMIN_ACTIVITY" as const, label: "Admin Activity" },
    ],
    []
  );

  return (
    <div className="mx-auto max-w-7xl space-y-5 p-4 sm:p-6 lg:p-8">
      <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
        <Link to={`/app/organizations/${id}`} className="inline-flex items-center text-sm font-bold text-slate-600 hover:text-slate-900">
          <ArrowLeft className="mr-2 h-4 w-4" />
          Back to Organization
        </Link>
        <h1 className="mt-3 text-2xl font-black text-slate-950">Organization Audit Logs</h1>
        <p className="mt-1 text-sm font-semibold text-slate-500">
          {organizationQuery.data?.name || "Organization"} activity is isolated and categorized by tab.
        </p>
      </section>

      <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex flex-wrap gap-2">
          {tabItems.map((item) => (
            <button
              key={item.key}
              type="button"
              onClick={() => {
                setTab(item.key);
                setPage(0);
              }}
              className={`rounded-full px-4 py-2 text-sm font-black ${
                tab === item.key ? "bg-slate-950 text-white" : "bg-slate-100 text-slate-700"
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>
        <div className="mt-3 grid gap-3 md:grid-cols-4">
          <div className="relative md:col-span-2">
            <Search className="pointer-events-none absolute left-3 top-3.5 h-4 w-4 text-slate-400" />
            <Input
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                setPage(0);
              }}
              className="pl-9"
              placeholder="Search action, admin, member, description"
            />
          </div>
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
      </section>

      {tab === "NOTIFICATION_ACTIVITY" && (
        <section className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
          <MetricCard label="WhatsApp sent" value={notificationQuery.data?.summary.whatsappSent ?? 0} />
          <MetricCard label="Email sent" value={notificationQuery.data?.summary.emailSent ?? 0} />
          <MetricCard label="Failed count" value={notificationQuery.data?.summary.failedCount ?? 0} />
          <MetricCard label="Today's sent" value={notificationQuery.data?.summary.todaySentCount ?? 0} />
          <MetricCard label="Monthly sent" value={notificationQuery.data?.summary.monthlySentCount ?? 0} />
        </section>
      )}

      <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          {tab === "NOTIFICATION_ACTIVITY" ? (
            <table className="min-w-full text-left">
              <thead className="bg-slate-50">
                <tr className="text-xs uppercase tracking-wide text-slate-500">
                  <th className="px-4 py-3">Time</th>
                  <th className="px-4 py-3">Channel</th>
                  <th className="px-4 py-3">Receiver</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Description</th>
                </tr>
              </thead>
              <tbody>
                {(notificationQuery.data?.page.items ?? []).map((row, index) => (
                  <tr key={`${row.time}-${index}`} className="border-t border-slate-100 text-sm">
                    <td className="px-4 py-3">{formatDateTime(row.time)}</td>
                    <td className="px-4 py-3">{row.channel}</td>
                    <td className="px-4 py-3">{row.receiver || "-"}</td>
                    <td className="px-4 py-3">{row.status}</td>
                    <td className="px-4 py-3">{row.description}</td>
                  </tr>
                ))}
                {(notificationQuery.data?.page.items ?? []).length === 0 && (
                  <tr>
                    <td className="px-4 py-8 text-center text-sm font-semibold text-slate-500" colSpan={5}>
                      No notification activity found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          ) : tab === "MEMBER_STATUS_ACTIVITY" ? (
            <table className="min-w-full text-left">
              <thead className="bg-slate-50">
                <tr className="text-xs uppercase tracking-wide text-slate-500">
                  <th className="px-4 py-3">Time</th>
                  <th className="px-4 py-3">Admin</th>
                  <th className="px-4 py-3">Member</th>
                  <th className="px-4 py-3">Action</th>
                  <th className="px-4 py-3">Description</th>
                </tr>
              </thead>
              <tbody>
                {(memberActivityQuery.data?.items as MemberStatusRow[] | undefined)?.map((row, index) => (
                  <tr key={`${row.time}-${index}`} className="border-t border-slate-100 text-sm">
                    <td className="px-4 py-3">{formatDateTime(row.time)}</td>
                    <td className="px-4 py-3">{row.admin}</td>
                    <td className="px-4 py-3">{row.member || "-"}</td>
                    <td className="px-4 py-3">{row.action}</td>
                    <td className="px-4 py-3">{row.description}</td>
                  </tr>
                ))}
                {(memberActivityQuery.data?.items ?? []).length === 0 && (
                  <tr>
                    <td className="px-4 py-8 text-center text-sm font-semibold text-slate-500" colSpan={5}>
                      No member status activity found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          ) : tab === "ADMIN_ACTIVITY" ? (
            <table className="min-w-full text-left">
              <thead className="bg-slate-50">
                <tr className="text-xs uppercase tracking-wide text-slate-500">
                  <th className="px-4 py-3">Time</th>
                  <th className="px-4 py-3">Admin</th>
                  <th className="px-4 py-3">Action</th>
                  <th className="px-4 py-3">Description</th>
                </tr>
              </thead>
              <tbody>
                {(memberActivityQuery.data?.items as AdminActivityRow[] | undefined)?.map((row, index) => (
                  <tr key={`${row.time}-${index}`} className="border-t border-slate-100 text-sm">
                    <td className="px-4 py-3">{formatDateTime(row.time)}</td>
                    <td className="px-4 py-3">{row.admin}</td>
                    <td className="px-4 py-3">{row.action}</td>
                    <td className="px-4 py-3">{row.description}</td>
                  </tr>
                ))}
                {(memberActivityQuery.data?.items ?? []).length === 0 && (
                  <tr>
                    <td className="px-4 py-8 text-center text-sm font-semibold text-slate-500" colSpan={4}>
                      No admin activity found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          ) : (
            <table className="min-w-full text-left">
              <thead className="bg-slate-50">
                <tr className="text-xs uppercase tracking-wide text-slate-500">
                  <th className="px-4 py-3">Time</th>
                  <th className="px-4 py-3">Admin</th>
                  <th className="px-4 py-3">Action</th>
                  <th className="px-4 py-3">Member</th>
                  <th className="px-4 py-3">Description</th>
                </tr>
              </thead>
              <tbody>
                {(memberActivityQuery.data?.items as MemberActivityRow[] | undefined)?.map((row, index) => (
                  <tr key={`${row.time}-${index}`} className="border-t border-slate-100 text-sm">
                    <td className="px-4 py-3">{formatDateTime(row.time)}</td>
                    <td className="px-4 py-3">{row.admin}</td>
                    <td className="px-4 py-3">{row.action}</td>
                    <td className="px-4 py-3">{row.member || "-"}</td>
                    <td className="px-4 py-3">{row.description}</td>
                  </tr>
                ))}
                {(memberActivityQuery.data?.items ?? []).length === 0 && (
                  <tr>
                    <td className="px-4 py-8 text-center text-sm font-semibold text-slate-500" colSpan={5}>
                      No member activity found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          )}
        </div>
        <div className="flex items-center justify-between border-t border-slate-100 px-4 py-3 text-sm">
          <div className="font-semibold text-slate-500">
            Showing {(pageMeta?.items ?? []).length} of {pageMeta?.totalElements ?? 0}
          </div>
          <div className="flex items-center gap-2">
            <Button type="button" variant="outline" onClick={() => setPage((current) => Math.max(current - 1, 0))} disabled={(pageMeta?.page ?? 0) <= 0}>
              Previous
            </Button>
            <div className="font-semibold text-slate-600">
              Page {(pageMeta?.page ?? 0) + 1} / {Math.max(pageMeta?.totalPages ?? 1, 1)}
            </div>
            <Button type="button" variant="outline" onClick={() => setPage((current) => current + 1)} disabled={!pageMeta?.hasNext}>
              Next
            </Button>
          </div>
        </div>
      </section>
    </div>
  );
}

function MetricCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">{label}</div>
      <div className="mt-2 text-2xl font-black text-slate-950">{value}</div>
    </div>
  );
}

