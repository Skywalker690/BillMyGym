import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Building2, CalendarDays, Edit, MapPin, Plus, Search, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import api from "@/lib/api";

type Organization = {
  id: number;
  name: string;
  district?: string | null;
  adminFullName?: string | null;
  adminEmail?: string | null;
  adminPhoneNumber?: string | null;
  status: "TRIAL" | "ACTIVE" | "PAUSED" | "DEACTIVATED";
  organizationType?: "REGULAR" | "PILOT";
  subscriptionDueDate?: string | null;
  trialEndDate?: string | null;
  pilotEndDate?: string | null;
};

const lifecycleStatuses = ["TRIAL", "ACTIVE", "PAUSED", "DEACTIVATED"] as const;
const organizationTypes = ["REGULAR", "PILOT"] as const;

const badgeClasses: Record<string, string> = {
  ACTIVE: "bg-emerald-100 text-emerald-800 border-emerald-200",
  TRIAL: "bg-amber-100 text-amber-800 border-amber-200",
  PAUSED: "bg-orange-100 text-orange-800 border-orange-200",
  DEACTIVATED: "bg-rose-100 text-rose-800 border-rose-200",
  PILOT: "bg-sky-100 text-sky-800 border-sky-200",
  REGULAR: "bg-slate-100 text-slate-800 border-slate-200",
};

const dateText = (value?: string | null) => {
  if (!value) return "Not scheduled";
  return new Intl.DateTimeFormat("en-IN", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(value));
};

export default function OrganizationsList() {
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [typeFilter, setTypeFilter] = useState("ALL");
  const queryClient = useQueryClient();

  const { data: organizations, isLoading } = useQuery<Organization[]>({
    queryKey: ["organizations"],
    queryFn: async () => {
      const res = await api.get("/organizations");
      return res.data;
    },
    staleTime: 60_000,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.delete(`/organizations/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["organizations"] });
      queryClient.invalidateQueries({ queryKey: ["platformAnalytics"] });
      toast.success("Organization successfully soft deleted");
    },
    onError: () => toast.error("Failed to delete organization"),
  });

  if (isLoading) {
    return <div className="p-8">Loading organizations...</div>;
  }

  const filteredOrgs = organizations?.filter((org) => {
    const text = `${org.name} ${org.district ?? ""} ${org.adminEmail ?? ""}`.toLowerCase();
    const matchesSearch = text.includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === "ALL" || org.status === statusFilter;
    const matchesType = typeFilter === "ALL" || org.organizationType === typeFilter;
    return matchesSearch && matchesStatus && matchesType;
  }) ?? [];

  return (
    <div className="mx-auto max-w-7xl space-y-6 p-8">
      <div className="billmygym-shell flex flex-col justify-between gap-5 rounded-[2rem] p-6 shadow-sm md:flex-row md:items-center">
        <div>
          <h1 className="text-4xl font-black tracking-tight text-slate-950">Organizations Directory</h1>
          <p className="mt-1 font-medium text-slate-500">Manage tenant lifecycle, pilot access, billing readiness, and admin ownership.</p>
        </div>
        <Link to="/app/organizations/new">
          <Button className="h-11 rounded-full bg-emerald-600 px-6 font-bold text-white shadow-md shadow-emerald-500/20 hover:bg-emerald-700">
            <Plus className="mr-2 h-4 w-4" /> New Organization
          </Button>
        </Link>
      </div>

      <div className="glass-panel flex flex-col items-center justify-between gap-4 rounded-[1.5rem] p-4 md:flex-row">
        <div className="relative w-full flex-1 md:max-w-md">
          <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <Input
            placeholder="Search by name, district, or admin email..."
            className="h-11 rounded-xl border-black/5 bg-white/65 pl-11 backdrop-blur-sm focus-visible:ring-emerald-500/50"
            value={searchTerm}
            onChange={(event) => setSearchTerm(event.target.value)}
          />
        </div>
        <div className="flex w-full flex-col gap-3 sm:flex-row md:w-auto">
          <select
            className="h-11 rounded-xl border border-black/5 bg-white/65 px-4 py-2 text-sm font-semibold text-slate-700 backdrop-blur-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
          >
            <option value="ALL">All Statuses</option>
            {lifecycleStatuses.map((status) => (
              <option key={status} value={status}>{status}</option>
            ))}
          </select>
          <select
            className="h-11 rounded-xl border border-black/5 bg-white/65 px-4 py-2 text-sm font-semibold text-slate-700 backdrop-blur-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
            value={typeFilter}
            onChange={(event) => setTypeFilter(event.target.value)}
          >
            <option value="ALL">All Types</option>
            {organizationTypes.map((type) => (
              <option key={type} value={type}>{type}</option>
            ))}
          </select>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
        {filteredOrgs.length === 0 ? (
          <div className="glass-panel col-span-full flex flex-col items-center justify-center rounded-[1.5rem] py-16 text-center font-medium text-slate-500">
            <p>No organizations found matching your criteria.</p>
            {(searchTerm || statusFilter !== "ALL" || typeFilter !== "ALL") && (
              <Button
                variant="link"
                onClick={() => {
                  setSearchTerm("");
                  setStatusFilter("ALL");
                  setTypeFilter("ALL");
                }}
                className="mt-2 text-emerald-700"
              >
                Clear filters
              </Button>
            )}
          </div>
        ) : (
          filteredOrgs.map((org) => (
            <div key={org.id} className="glass-panel group flex flex-col overflow-hidden rounded-[1.75rem] transition-all duration-300 hover:-translate-y-1">
              <div className="flex-1 space-y-5 p-6">
                <div className="flex items-start justify-between gap-3">
                  <div className="rounded-2xl border border-white/80 bg-white/80 p-4 text-emerald-700 shadow-sm transition-transform group-hover:scale-105">
                    <Building2 className="h-6 w-6" />
                  </div>
                  <div className="flex flex-wrap justify-end gap-2">
                    <Badge className={badgeClasses[org.status]}>{org.status}</Badge>
                    <Badge className={badgeClasses[org.organizationType ?? "REGULAR"]}>{org.organizationType ?? "REGULAR"}</Badge>
                  </div>
                </div>

                <div>
                  <h3 className="truncate text-2xl font-black tracking-tight text-slate-950" title={org.name}>{org.name}</h3>
                  <div className="mt-1.5 flex items-center text-sm font-medium text-slate-500">
                    <MapPin className="mr-1.5 h-3.5 w-3.5" />
                    <span className="truncate">{org.district || "No district"}</span>
                  </div>
                </div>

                <div className="rounded-xl border border-white/70 bg-white/50 p-4 text-sm font-medium text-slate-600">
                  <p><span className="mr-2 text-[10px] font-black uppercase tracking-widest text-slate-950">Admin:</span> {org.adminFullName || "-"}</p>
                  <p className="truncate" title={org.adminEmail ?? undefined}><span className="mr-2 text-[10px] font-black uppercase tracking-widest text-slate-950">Email:</span> {org.adminEmail || "-"}</p>
                  <p><span className="mr-2 text-[10px] font-black uppercase tracking-widest text-slate-950">Phone:</span> {org.adminPhoneNumber || "-"}</p>
                </div>

                <div className="rounded-xl border border-emerald-100 bg-emerald-50/60 p-4">
                  <div className="flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.22em] text-emerald-700">
                    <CalendarDays className="h-4 w-4" />
                    Lifecycle Date
                  </div>
                  <p className="mt-2 text-sm font-bold text-slate-900">
                    {org.organizationType === "PILOT" ? `Pilot ends ${dateText(org.pilotEndDate)}` : `Subscription due ${dateText(org.subscriptionDueDate ?? org.trialEndDate)}`}
                  </p>
                </div>
              </div>

              <div className="flex justify-end gap-3 border-t border-white/70 bg-white/45 p-4">
                <Link to={`/app/organizations/${org.id}`}>
                  <Button variant="outline" size="sm" className="rounded-full border-white/80 bg-white/70 px-5 font-semibold hover:bg-white">
                    <Edit className="mr-2 h-4 w-4" /> Manage
                  </Button>
                </Link>
                <Button
                  variant="ghost"
                  size="sm"
                  className="rounded-full px-3 text-red-500 hover:bg-red-50 hover:text-red-600"
                  onClick={() => {
                    if (confirm("Soft delete this organization? History, members, and payments will be preserved.")) {
                      deleteMutation.mutate(org.id);
                    }
                  }}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
