import { useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, RefreshCcw, Search } from "lucide-react";
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
  const [appliedTab, setAppliedTab] = useState<TabKey>("MEMBER_ACTIVITY");
  const [appliedSearch, setAppliedSearch] = useState("");
  const [appliedFromDate, setAppliedFromDate] = useState("");
  const [appliedToDate, setAppliedToDate] = useState("");
  const [page, setPage] = useState(0);
  const [refreshToken, setRefreshToken] = useState(0);

  const organizationQuery = useQuery({
    queryKey: ["organizationBrief", id],
    queryFn: async () => {
      const res = await api.get(`/organizations/${id}`);
      return res.data;
    },
    enabled: Boolean(id),
  });

  const memberActivityQuery = useQuery<AuditPage<MemberActivityRow>>({
    queryKey: ["organizationAuditMember", id, appliedTab, appliedSearch, appliedFromDate, appliedToDate, page, refreshToken],
    queryFn: async () => {
      const endpoint =
        appliedTab === "MEMBER_ACTIVITY"
          ? `/organizations/${id}/audit-logs/member-activity`
          : appliedTab === "MEMBER_STATUS_ACTIVITY"
            ? `/organizations/${id}/audit-logs/member-status`
            : `/organizations/${id}/audit-logs/admin-activity`;
      const res = await api.get(endpoint, {
        params: {
          search: appliedSearch.trim() || undefined,
          fromDate: appliedFromDate || undefined,
          toDate: appliedToDate || undefined,
          page,
          size: 20,
        },
      });
      return res.data;
    },
    enabled: Boolean(id) && appliedTab !== "NOTIFICATION_ACTIVITY",
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });

  const notificationQuery = useQuery<NotificationAuditResponse>({
    queryKey: ["organizationAuditNotification", id, appliedSearch, appliedFromDate, appliedToDate, page, refreshToken],
    queryFn: async () => {
      const res = await api.get(`/organizations/${id}/audit-logs/notification-activity`, {
        params: {
          search: appliedSearch.trim() || undefined,
          fromDate: appliedFromDate || undefined,
          toDate: appliedToDate || undefined,
          page,
          size: 20,
        },
      });
      return res.data;
    },
    enabled: Boolean(id) && appliedTab === "NOTIFICATION_ACTIVITY",
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });

  const pageMeta = appliedTab === "NOTIFICATION_ACTIVITY" ? notificationQuery.data?.page : memberActivityQuery.data;

  const tabItems = useMemo(
    () => [
      { key: "MEMBER_ACTIVITY" as const, label: "Member Activity" },
      { key: "MEMBER_STATUS_ACTIVITY" as const, label: "Member Status Activity" },
      { key: "NOTIFICATION_ACTIVITY" as const, label: "Notification Activity" },
      { key: "ADMIN_ACTIVITY" as const, label: "Admin Activity" },
    ],
    []
  );

  function applyAuditFilters(nextPage = 0) {
    setPage(nextPage);
    setAppliedTab(tab);
    setAppliedSearch(search);
    setAppliedFromDate(fromDate);
    setAppliedToDate(toDate);
    if (
      nextPage === page &&
      appliedTab === tab &&
      appliedSearch === search &&
      appliedFromDate === fromDate &&
      appliedToDate === toDate
    ) {
      setRefreshToken((current) => current + 1);
    }
  }

  return (
    <div className="mx-auto max-w-7xl space-y-3 p-2.5 sm:space-y-5 sm:p-6 lg:p-8">
      <section className="rounded-[1.35rem] border border-slate-200 bg-white p-3.5 shadow-sm sm:rounded-3xl sm:p-5">
        <Link to={`/app/organizations/${id}`} className="inline-flex items-center text-xs font-bold text-slate-600 hover:text-slate-900 sm:text-sm">
          <ArrowLeft className="mr-2 h-4 w-4" />
          Back to Organization
        </Link>
        <h1 className="mt-2.5 text-xl font-black text-slate-950 sm:mt-3 sm:text-2xl">Organization Audit Logs</h1>
        <p className="mt-1 text-xs font-semibold leading-5 text-slate-500 sm:text-sm">
          {organizationQuery.data?.name || "Organization"} activity is isolated and categorized by tab.
        </p>
      </section>

      <section className="rounded-[1.35rem] border border-slate-200 bg-white p-3.5 shadow-sm sm:rounded-3xl sm:p-5">
        <div className="flex flex-wrap gap-2">
          {tabItems.map((item) => (
            <button
              key={item.key}
              type="button"
              onClick={() => {
                setTab(item.key);
              }}
              className={`rounded-full px-3 py-1.5 text-[11px] font-black sm:px-4 sm:py-2 sm:text-sm ${
                tab === item.key ? "bg-slate-950 text-white" : "bg-slate-100 text-slate-700"
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>
        <div className="mt-3 grid gap-2.5 md:grid-cols-4">
          <div className="relative md:col-span-2">
            <Search className="pointer-events-none absolute left-3 top-3.5 h-4 w-4 text-slate-400" />
            <Input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              className="h-9 pl-9 text-xs sm:h-10 sm:text-sm"
              placeholder="Search action, admin, member, description"
            />
          </div>
          <Input
            type="date"
            value={fromDate}
            onChange={(event) => setFromDate(event.target.value)}
            className="h-9 text-xs sm:h-10 sm:text-sm"
          />
          <Input
            type="date"
            value={toDate}
            onChange={(event) => setToDate(event.target.value)}
            className="h-9 text-xs sm:h-10 sm:text-sm"
          />
        </div>
        <div className="mt-3 flex justify-end sm:mt-4">
          <Button type="button" variant="outline" onClick={() => applyAuditFilters(0)} disabled={memberActivityQuery.isFetching || notificationQuery.isFetching} className="h-9 rounded-full text-xs font-bold sm:h-10 sm:text-sm">
            <RefreshCcw className={`mr-2 h-4 w-4 ${memberActivityQuery.isFetching || notificationQuery.isFetching ? "animate-spin" : ""}`} />
            Refresh Audit Logs
          </Button>
        </div>
      </section>

      {appliedTab === "NOTIFICATION_ACTIVITY" && (
        <section className="grid grid-cols-2 gap-2 sm:gap-3 md:grid-cols-2 xl:grid-cols-5">
          <MetricCard label="WhatsApp sent" value={notificationQuery.data?.summary.whatsappSent ?? 0} />
          <MetricCard label="Email sent" value={notificationQuery.data?.summary.emailSent ?? 0} />
          <MetricCard label="Failed count" value={notificationQuery.data?.summary.failedCount ?? 0} />
          <MetricCard label="Today's sent" value={notificationQuery.data?.summary.todaySentCount ?? 0} />
          <MetricCard label="Monthly sent" value={notificationQuery.data?.summary.monthlySentCount ?? 0} />
        </section>
      )}

      <section className="overflow-hidden rounded-[1.35rem] border border-slate-200 bg-white shadow-sm sm:rounded-3xl">
        <div className="overflow-x-auto">
          {appliedTab === "NOTIFICATION_ACTIVITY" ? (
            <table className="min-w-[760px] text-left sm:min-w-full">
              <thead className="bg-slate-50">
                <tr className="text-[10px] uppercase tracking-wide text-slate-500 sm:text-xs">
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Time</th>
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Channel</th>
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Receiver</th>
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Status</th>
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Description</th>
                </tr>
              </thead>
              <tbody>
                {(notificationQuery.data?.page.items ?? []).map((row, index) => (
                  <tr key={`${row.time}-${index}`} className="border-t border-slate-100 text-xs sm:text-sm">
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{formatDateTime(row.time)}</td>
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{row.channel}</td>
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{row.receiver || "-"}</td>
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{row.status}</td>
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{row.description}</td>
                  </tr>
                ))}
                {(notificationQuery.data?.page.items ?? []).length === 0 && (
                  <tr>
                    <td className="px-3 py-6 text-center text-xs font-semibold text-slate-500 sm:px-4 sm:py-8 sm:text-sm" colSpan={5}>
                      No notification activity found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          ) : appliedTab === "MEMBER_STATUS_ACTIVITY" ? (
            <table className="min-w-[760px] text-left sm:min-w-full">
              <thead className="bg-slate-50">
                <tr className="text-[10px] uppercase tracking-wide text-slate-500 sm:text-xs">
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Time</th>
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Admin</th>
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Member</th>
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Action</th>
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Description</th>
                </tr>
              </thead>
              <tbody>
                {(memberActivityQuery.data?.items as MemberStatusRow[] | undefined)?.map((row, index) => (
                  <tr key={`${row.time}-${index}`} className="border-t border-slate-100 text-xs sm:text-sm">
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{formatDateTime(row.time)}</td>
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{row.admin}</td>
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{row.member || "-"}</td>
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{row.action}</td>
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{row.description}</td>
                  </tr>
                ))}
                {(memberActivityQuery.data?.items ?? []).length === 0 && (
                  <tr>
                    <td className="px-3 py-6 text-center text-xs font-semibold text-slate-500 sm:px-4 sm:py-8 sm:text-sm" colSpan={5}>
                      No member status activity found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          ) : appliedTab === "ADMIN_ACTIVITY" ? (
            <table className="min-w-[700px] text-left sm:min-w-full">
              <thead className="bg-slate-50">
                <tr className="text-[10px] uppercase tracking-wide text-slate-500 sm:text-xs">
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Time</th>
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Admin</th>
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Action</th>
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Description</th>
                </tr>
              </thead>
              <tbody>
                {(memberActivityQuery.data?.items as AdminActivityRow[] | undefined)?.map((row, index) => (
                  <tr key={`${row.time}-${index}`} className="border-t border-slate-100 text-xs sm:text-sm">
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{formatDateTime(row.time)}</td>
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{row.admin}</td>
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{row.action}</td>
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{row.description}</td>
                  </tr>
                ))}
                {(memberActivityQuery.data?.items ?? []).length === 0 && (
                  <tr>
                    <td className="px-3 py-6 text-center text-xs font-semibold text-slate-500 sm:px-4 sm:py-8 sm:text-sm" colSpan={4}>
                      No admin activity found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          ) : (
            <table className="min-w-[760px] text-left sm:min-w-full">
              <thead className="bg-slate-50">
                <tr className="text-[10px] uppercase tracking-wide text-slate-500 sm:text-xs">
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Time</th>
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Admin</th>
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Action</th>
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Member</th>
                  <th className="px-3 py-2.5 sm:px-4 sm:py-3">Description</th>
                </tr>
              </thead>
              <tbody>
                {(memberActivityQuery.data?.items as MemberActivityRow[] | undefined)?.map((row, index) => (
                  <tr key={`${row.time}-${index}`} className="border-t border-slate-100 text-xs sm:text-sm">
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{formatDateTime(row.time)}</td>
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{row.admin}</td>
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{row.action}</td>
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{row.member || "-"}</td>
                    <td className="px-3 py-2.5 sm:px-4 sm:py-3">{row.description}</td>
                  </tr>
                ))}
                {(memberActivityQuery.data?.items ?? []).length === 0 && (
                  <tr>
                    <td className="px-3 py-6 text-center text-xs font-semibold text-slate-500 sm:px-4 sm:py-8 sm:text-sm" colSpan={5}>
                      No member activity found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          )}
        </div>
        <div className="flex flex-col gap-3 border-t border-slate-100 px-3 py-3 text-xs sm:flex-row sm:items-center sm:justify-between sm:px-4 sm:text-sm">
          <div className="font-semibold text-slate-500">
            Showing {(pageMeta?.items ?? []).length} of {pageMeta?.totalElements ?? 0}
          </div>
          <div className="flex items-center justify-between gap-2 sm:justify-end">
            <Button
              type="button"
              variant="outline"
              onClick={() => applyAuditFilters(Math.max((pageMeta?.page ?? 0) - 1, 0))}
              disabled={(pageMeta?.page ?? 0) <= 0}
            >
              Previous
            </Button>
            <div className="text-center font-semibold text-slate-600">
              Page {(pageMeta?.page ?? 0) + 1} / {Math.max(pageMeta?.totalPages ?? 1, 1)}
            </div>
            <Button
              type="button"
              variant="outline"
              onClick={() => applyAuditFilters((pageMeta?.page ?? 0) + 1)}
              disabled={!pageMeta?.hasNext}
            >
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
    <div className="rounded-[1rem] border border-slate-200 bg-white p-2.5 shadow-sm sm:rounded-2xl sm:p-4">
      <div className="text-[9px] font-black uppercase tracking-[0.14em] text-slate-500 sm:text-[10px] sm:tracking-[0.2em]">{label}</div>
      <div className="mt-1.5 text-lg font-black text-slate-950 sm:mt-2 sm:text-2xl">{value}</div>
    </div>
  );
}
