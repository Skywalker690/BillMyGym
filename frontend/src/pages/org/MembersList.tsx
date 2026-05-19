import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Clock3, Pencil, Plus, RefreshCw, Search, ShieldAlert, Trash2, XCircle } from "lucide-react";
import { Link } from "react-router-dom";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { toast } from "sonner";
import api from "@/lib/api";

const PAGE_SIZE = 40;

const formatDate = (value?: string) =>
  value
    ? new Intl.DateTimeFormat("en-IN", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(value))
    : "-";

export default function MembersList() {
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["members", page, searchTerm, statusFilter],
    queryFn: async () => {
      const res = await api.get("/members", {
        params: {
          page,
          size: PAGE_SIZE,
          search: searchTerm || undefined,
          membershipStatus: statusFilter === "ALL" ? undefined : statusFilter,
        },
      });
      return res.data;
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/members/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["members"] });
      queryClient.invalidateQueries({ queryKey: ["dashboardReport"] });
      toast.success("Member deactivated successfully");
    },
    onError: () => toast.error("Failed to deactivate member"),
  });

  const members = data?.members ?? [];
  const summary = data?.summary ?? {};
  const memberLimit = summary.memberLimit;

  if (isLoading) {
    return <div className="p-8">Loading members...</div>;
  }

  return (
    <div className="mx-auto min-w-0 max-w-7xl space-y-8 px-4 py-8 sm:px-6 lg:px-8">
      <section className="billmygym-shell rounded-[2rem] p-8">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-[10px] font-black uppercase tracking-[0.26em] text-emerald-700">Member Operations</div>
            <h1 className="mt-2 text-4xl font-black tracking-tight text-slate-950">Members Directory</h1>
            <p className="mt-2 max-w-2xl text-sm font-medium leading-6 text-slate-600">
              Search your member base, spot payment risk, and move directly from directory management into renewal action.
            </p>
          </div>
          <Link to="/app/members/new">
            <Button className="h-12 rounded-full bg-slate-950 px-6 font-bold text-white hover:bg-slate-800">
              <Plus className="mr-2 h-4 w-4" />
              Add Member
            </Button>
          </Link>
        </div>
      </section>

      <section className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">
        <SummaryTile label="Total Members" value={summary.totalMembers} tone="slate" />
        <SummaryTile label="Active Members" value={summary.activeMembers} tone="emerald" />
        <SummaryTile label="Expiring Soon" value={summary.expiringSoonMemberships} tone="amber" />
        <SummaryTile label="Overdue" value={summary.overdueMemberships} tone="rose" />
      </section>

      <section className="glass-panel rounded-[1.8rem] p-4">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div className="relative w-full flex-1 md:max-w-md">
            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              placeholder="Search by name, phone, or email..."
              className="h-11 rounded-xl border-black/5 bg-white/60 pl-11"
              value={searchTerm}
              onChange={(event) => {
                setPage(0);
                setSearchTerm(event.target.value);
              }}
            />
          </div>
          <div className="w-full md:w-auto md:min-w-[220px]">
            <select
              className="h-11 w-full rounded-xl border border-black/5 bg-white/60 px-4 text-sm font-semibold text-slate-700"
              value={statusFilter}
              onChange={(event) => {
                setPage(0);
                setStatusFilter(event.target.value);
              }}
            >
              <option value="ALL">All Memberships</option>
              <option value="ACTIVE">Active</option>
              <option value="EXPIRING_SOON">Expiring Soon</option>
              <option value="EXPIRED">Expired</option>
              <option value="OVERDUE">Overdue</option>
            </select>
          </div>
        </div>
      </section>

      <section className="glass-panel overflow-hidden rounded-[1.9rem]">
        {members.length === 0 ? (
          <div className="p-14 text-center text-sm font-semibold text-slate-500">No members found matching your filters.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-[980px] w-full text-left text-sm">
              <thead className="border-b border-black/5 bg-white/30 text-slate-500">
                <tr>
                  <th className="px-6 py-5 text-xs font-black uppercase tracking-[0.2em]">Member</th>
                  <th className="px-6 py-5 text-xs font-black uppercase tracking-[0.2em]">Plan</th>
                  <th className="px-6 py-5 text-xs font-black uppercase tracking-[0.2em]">Period</th>
                  <th className="px-6 py-5 text-xs font-black uppercase tracking-[0.2em]">Membership</th>
                  <th className="px-6 py-5 text-xs font-black uppercase tracking-[0.2em]">Payment</th>
                  <th className="px-6 py-5 text-right text-xs font-black uppercase tracking-[0.2em]">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-black/5">
                {members.map((member: any) => {
                  const membership = member.currentMembership;
                  const status = membership?.expiringSoon ? "EXPIRING_SOON" : membership?.status ?? "NO_MEMBERSHIP";
                  return (
                    <tr key={member.id} className="transition-colors hover:bg-white/50">
                      <td className="min-w-[240px] px-6 py-5">
                        <div className="flex items-start justify-between gap-3">
                          <div className="min-w-0">
                            <div className="font-black text-slate-950">{member.fullName}</div>
                            <div className="mt-1 text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">{member.phone}</div>
                          </div>
                          <Link
                            to={`/app/members/${member.id}/edit`}
                            className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-black/5 bg-white/70 text-slate-500 shadow-sm transition-colors hover:bg-white hover:text-emerald-700"
                            aria-label={`Edit ${member.fullName}`}
                            title="Edit member"
                          >
                            <Pencil className="h-4 w-4" />
                          </Link>
                        </div>
                      </td>
                      <td className="px-6 py-5 font-semibold text-slate-700">
                        {membership ? `${membership.durationMonths} month${membership.durationMonths > 1 ? "s" : ""}` : "No membership"}
                      </td>
                      <td className="px-6 py-5 text-xs font-semibold text-slate-500">
                        <div>{formatDate(membership?.startDate)}</div>
                        <div className="mt-1">to {formatDate(membership?.endDate)}</div>
                      </td>
                      <td className="px-6 py-5">
                        <StatusBadge status={status} />
                      </td>
                      <td className="px-6 py-5">
                        <Badge variant="outline" className="rounded-full border-transparent bg-slate-100 px-2.5 py-1 font-black tracking-[0.14em] text-slate-700">
                          {membership?.paymentStatus ?? "N/A"}
                        </Badge>
                        {membership && Number(membership.pendingAmount) > 0 && (
                          <div className="mt-2 text-xs font-bold text-amber-700">INR {Number(membership.pendingAmount).toLocaleString("en-IN")} pending</div>
                        )}
                      </td>
                      <td className="min-w-[240px] px-6 py-5">
                        <div className="flex flex-wrap justify-end gap-2">
                          <Link to={`/app/members/${member.id}/renew`}>
                            <Button
                              variant="outline"
                              className="h-10 rounded-full border-emerald-200 bg-emerald-50 px-4 text-sm font-bold text-emerald-700 shadow-sm transition-colors hover:bg-emerald-100 hover:text-emerald-800"
                            >
                              <RefreshCw className="mr-2 h-4 w-4" />
                              Renew
                            </Button>
                          </Link>
                          <Button
                            type="button"
                            variant="outline"
                            className="h-10 rounded-full border-rose-200 bg-rose-50 px-4 text-sm font-bold text-rose-700 shadow-sm transition-colors hover:bg-rose-100 hover:text-rose-800"
                            onClick={() => {
                              if (confirm("Deactivate this member?")) {
                                deleteMutation.mutate(member.id);
                              }
                            }}
                          >
                            <Trash2 className="mr-2 h-4 w-4" />
                            Deactivate
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm font-semibold text-slate-500">
          Page {(data?.page ?? 0) + 1} of {Math.max(data?.totalPages ?? 1, 1)} · {data?.totalElements ?? 0} members
        </p>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" disabled={data?.first} onClick={() => setPage((value) => Math.max(value - 1, 0))}>
            Previous
          </Button>
          <Button variant="outline" disabled={data?.last} onClick={() => setPage((value) => value + 1)}>
            Next
          </Button>
        </div>
      </div>

      <section className="flex justify-end">
        <MemberLimitCard memberLimit={memberLimit} activeMembers={summary.activeMembers ?? 0} />
      </section>
    </div>
  );
}

function MemberLimitCard({
  memberLimit,
  activeMembers,
}: {
  memberLimit?: {
    plan?: string;
    usedMembers?: number;
    memberLimit?: number | null;
    unlimited?: boolean;
    remainingSlots?: number;
    usagePercent?: number;
  };
  activeMembers: number;
}) {
  const usedMembers = Number(memberLimit?.usedMembers ?? activeMembers ?? 0);
  const limit = memberLimit?.memberLimit ?? null;
  const unlimited = Boolean(memberLimit?.unlimited);
  const usagePercent = unlimited ? 0 : Number(memberLimit?.usagePercent ?? 0);
  const remainingSlots = Number(memberLimit?.remainingSlots ?? 0);

  return (
    <div className="glass-panel w-full max-w-sm rounded-[1.7rem] p-5">
      <div className="flex items-center justify-between gap-4">
        <div>
          <div className="text-[10px] font-black uppercase tracking-[0.22em] text-emerald-700">Members Used</div>
          <div className="mt-2 text-3xl font-black tracking-tight text-slate-950">
            {unlimited ? `${usedMembers} / Unlimited` : `${usedMembers} / ${limit ?? 0}`}
          </div>
        </div>
        <Badge className="border-slate-200 bg-white/80 text-slate-700">{memberLimit?.plan ?? "STARTER"}</Badge>
      </div>
      {!unlimited ? (
        <>
          <div className="mt-5 h-3 overflow-hidden rounded-full bg-slate-200">
            <div
              className="h-full rounded-full bg-emerald-600 transition-all duration-300"
              style={{ width: `${Math.min(Math.max(usagePercent, 0), 100)}%` }}
            />
          </div>
          <div className="mt-3 flex items-center justify-between gap-3 text-sm font-bold text-slate-600">
            <span>{usagePercent}%</span>
            <span>{remainingSlots} slots remaining</span>
          </div>
        </>
      ) : (
        <div className="mt-4 rounded-2xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm font-bold text-emerald-800">
          Unlimited member capacity
        </div>
      )}
    </div>
  );
}

function SummaryTile({ label, value, tone }: { label: string; value?: number; tone: "slate" | "emerald" | "amber" | "rose" }) {
  const toneClasses = {
    slate: "from-slate-900/10 via-slate-400/5 to-transparent",
    emerald: "from-emerald-500/18 via-emerald-300/8 to-transparent",
    amber: "from-amber-500/18 via-amber-300/8 to-transparent",
    rose: "from-rose-500/18 via-rose-300/8 to-transparent",
  }[tone];

  return (
    <div className="glass-panel relative overflow-hidden rounded-[1.7rem] p-5">
      <div className={cn("absolute inset-0 bg-gradient-to-br", toneClasses)} />
      <div className="relative z-10">
        <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">{label}</div>
        <h3 className="mt-2 text-3xl font-black tracking-tight text-slate-950">{value ?? 0}</h3>
      </div>
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const active = status === "ACTIVE";
  const expiring = status === "EXPIRING_SOON";
  const overdue = status === "EXPIRED" || status === "OVERDUE";
  return (
    <Badge
      variant="outline"
      className={cn(
        "flex w-fit items-center gap-1.5 rounded-full border-transparent px-3 py-1 font-black tracking-[0.14em]",
        active && "bg-emerald-100 text-emerald-800",
        expiring && "bg-amber-100 text-amber-800",
        overdue && "bg-rose-100 text-rose-800",
        status === "NO_MEMBERSHIP" && "bg-slate-100 text-slate-600",
      )}
    >
      {active && <CheckCircle2 className="h-3.5 w-3.5" />}
      {expiring && <Clock3 className="h-3.5 w-3.5" />}
      {overdue && <ShieldAlert className="h-3.5 w-3.5" />}
      {!active && !expiring && !overdue && status === "NO_MEMBERSHIP" && <XCircle className="h-3.5 w-3.5" />}
      {status.replace("_", " ")}
    </Badge>
  );
}
