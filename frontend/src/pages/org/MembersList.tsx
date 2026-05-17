import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Clock, Edit, MoreHorizontal, Plus, Search, Trash2, XCircle } from "lucide-react";
import { Link } from "react-router-dom";
import { Badge } from "@/components/ui/badge";
import { Button, buttonVariants } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { toast } from "sonner";
import api from "@/lib/api";

const PAGE_SIZE = 40;

const formatDate = (value?: string) => value ? new Date(value).toLocaleDateString() : "-";

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

  if (isLoading) {
    return <div className="p-8">Loading members...</div>;
  }

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8">
      <div className="flex flex-col gap-4 md:flex-row md:justify-between md:items-center bg-white/40 dark:bg-slate-900/40 p-6 rounded-2xl backdrop-blur-xl border border-white/60 dark:border-white/10 shadow-sm">
        <div>
          <h1 className="text-3xl font-bold text-slate-900 dark:text-white tracking-tight">Members Directory</h1>
          <p className="text-slate-500 dark:text-slate-400 mt-1 font-medium">Paged from the backend, 40 members at a time.</p>
        </div>
        <Link to="/app/members/new">
          <Button className="rounded-full px-5 bg-indigo-600 hover:bg-indigo-700 text-white font-medium">
            <Plus className="w-4 h-4 mr-2" /> Add Member
          </Button>
        </Link>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-5">
        <SummaryTile label="Total" value={summary.totalMembers} />
        <SummaryTile label="Active Members" value={summary.activeMembers} />
        <SummaryTile label="Expiring Soon" value={summary.expiringSoonMemberships} />
        <SummaryTile label="Overdue" value={summary.overdueMemberships} />
      </div>

      <div className="glass-panel p-4 flex flex-col md:flex-row justify-between items-center gap-4">
        <div className="relative flex-1 w-full md:max-w-md">
          <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 text-slate-400 h-4 w-4" />
          <Input
            placeholder="Search by name, phone, or email..."
            className="pl-11 bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm"
            value={searchTerm}
            onChange={(event) => {
              setPage(0);
              setSearchTerm(event.target.value);
            }}
          />
        </div>
        <select
          className="w-full md:w-auto h-11 rounded-xl border border-black/5 dark:border-white/10 bg-white/50 dark:bg-slate-900/50 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 backdrop-blur-sm text-slate-700 dark:text-slate-300"
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

      <div className="glass-panel overflow-hidden">
        {members.length === 0 ? (
          <div className="p-12 text-center text-slate-500 font-medium">No members found matching your criteria.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="bg-white/30 dark:bg-white/5 text-slate-500 dark:text-slate-400 border-b border-black/5 dark:border-white/5">
                <tr>
                  <th className="px-6 py-5 font-bold tracking-wider uppercase text-xs">Member</th>
                  <th className="px-6 py-5 font-bold tracking-wider uppercase text-xs">Plan</th>
                  <th className="px-6 py-5 font-bold tracking-wider uppercase text-xs">Period</th>
                  <th className="px-6 py-5 font-bold tracking-wider uppercase text-xs">Status</th>
                  <th className="px-6 py-5 font-bold tracking-wider uppercase text-xs">Payment</th>
                  <th className="px-6 py-5 font-bold tracking-wider uppercase text-xs text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-black/5 dark:divide-white/5">
                {members.map((member: any) => {
                  const membership = member.currentMembership;
                  const status = membership?.expiringSoon ? "EXPIRING_SOON" : membership?.status ?? "NO_MEMBERSHIP";
                  return (
                    <tr key={member.id} className="hover:bg-white/50 dark:hover:bg-white/5 transition-colors group">
                      <td className="px-6 py-5">
                        <div className="font-bold text-slate-900 dark:text-white text-[15px]">{member.fullName}</div>
                        <div className="text-xs font-medium text-slate-500 mt-1">{member.phone}</div>
                      </td>
                      <td className="px-6 py-5 font-medium text-slate-700 dark:text-slate-300">
                        {membership ? `${membership.durationMonths} month${membership.durationMonths > 1 ? "s" : ""}` : "No membership"}
                      </td>
                      <td className="px-6 py-5 text-xs font-medium text-slate-500 dark:text-slate-400">
                        <div>{formatDate(membership?.startDate)}</div>
                        <div className="mt-0.5">to {formatDate(membership?.endDate)}</div>
                      </td>
                      <td className="px-6 py-5">
                        <StatusBadge status={status} />
                      </td>
                      <td className="px-6 py-5">
                        <Badge variant="outline" className="rounded-full px-2.5 py-1 font-bold tracking-wide border-transparent bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                          {membership?.paymentStatus ?? "N/A"}
                        </Badge>
                        {membership && Number(membership.pendingAmount) > 0 && (
                          <div className="text-[11px] text-amber-600 font-semibold mt-1">₹{Number(membership.pendingAmount).toLocaleString("en-IN")} pending</div>
                        )}
                      </td>
                      <td className="px-6 py-5 text-right">
                        <DropdownMenu>
                          <DropdownMenuTrigger className={cn(buttonVariants({ variant: "ghost" }), "h-10 w-10 p-0 rounded-full bg-white/50 dark:bg-slate-800/50 border border-black/5 shadow-sm")}>
                            <MoreHorizontal className="h-4 w-4 text-slate-600 dark:text-slate-300" />
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end" className="glass-panel p-2 min-w-[190px]">
                            <Link to={`/app/members/${member.id}/edit`}>
                              <DropdownMenuItem className="cursor-pointer rounded-lg font-medium p-2 mb-1">
                                <Edit className="w-4 h-4 mr-3 text-slate-500" /> Edit Member
                              </DropdownMenuItem>
                            </Link>
                            <DropdownMenuItem
                              className="text-red-600 dark:text-red-500 cursor-pointer rounded-lg font-medium p-2"
                              onClick={() => {
                                if (confirm("Deactivate this member?")) {
                                  deleteMutation.mutate(member.id);
                                }
                              }}
                            >
                              <Trash2 className="w-4 h-4 mr-3" /> Deactivate
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="flex items-center justify-between">
        <p className="text-sm text-slate-500">
          Page {(data?.page ?? 0) + 1} of {Math.max(data?.totalPages ?? 1, 1)} · {data?.totalElements ?? 0} members
        </p>
        <div className="flex gap-2">
          <Button variant="outline" disabled={data?.first} onClick={() => setPage((value) => Math.max(value - 1, 0))}>Previous</Button>
          <Button variant="outline" disabled={data?.last} onClick={() => setPage((value) => value + 1)}>Next</Button>
        </div>
      </div>
    </div>
  );
}

function SummaryTile({ label, value }: { label: string; value?: number }) {
  return (
    <div className="glass-panel p-5">
      <p className="text-xs font-semibold tracking-wider uppercase text-slate-500 dark:text-slate-400">{label}</p>
      <h3 className="text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight mt-2">{value ?? 0}</h3>
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
        "rounded-full px-2.5 py-1 font-bold tracking-wide flex items-center gap-1.5 border-transparent w-fit",
        active && "bg-green-100/80 text-green-700 dark:bg-green-500/10 dark:text-green-400",
        expiring && "bg-amber-100/80 text-amber-700 dark:bg-amber-500/10 dark:text-amber-400",
        overdue && "bg-red-100/80 text-red-700 dark:bg-red-500/10 dark:text-red-400",
        status === "NO_MEMBERSHIP" && "bg-slate-100/80 text-slate-600 dark:bg-slate-800 dark:text-slate-300"
      )}
    >
      {active && <CheckCircle2 className="w-3.5 h-3.5" />}
      {expiring && <Clock className="w-3.5 h-3.5" />}
      {overdue && <XCircle className="w-3.5 h-3.5" />}
      {status.replace("_", " ")}
    </Badge>
  );
}
