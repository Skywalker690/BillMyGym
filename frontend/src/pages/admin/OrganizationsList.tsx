import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Search, Building2, MapPin, Trash2, Edit } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { toast } from "sonner";
import api from "@/lib/api";

export default function OrganizationsList() {
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const queryClient = useQueryClient();

  const { data: organizations, isLoading } = useQuery({
    queryKey: ["organizations"],
    queryFn: async () => {
      const res = await api.get("/organizations");
      return res.data;
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/organizations/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["organizations"] });
      toast.success("Organization successfully soft deleted");
    },
    onError: () => toast.error("Failed to delete organization"),
  });

  if (isLoading) {
    return <div className="p-8">Loading organizations...</div>;
  }

  const filteredOrgs = organizations?.filter((org: any) => {
    const matchesSearch = 
      org.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
      org.district?.toLowerCase().includes(searchTerm.toLowerCase());
    
    const matchesStatus = statusFilter === "ALL" || org.status === statusFilter;
    
    return matchesSearch && matchesStatus;
  }) || [];

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-6">
      <div className="flex justify-between items-center bg-white/40 dark:bg-slate-900/40 p-6 rounded-[2rem] backdrop-blur-xl border border-white/60 dark:border-white/10 shadow-sm">
        <div>
          <h1 className="text-4xl font-bold text-slate-900 dark:text-white tracking-tight">Organizations Directory</h1>
          <p className="text-slate-500 dark:text-slate-400 mt-1 font-medium">Manage all multitenant instances on the platform.</p>
        </div>
        <Link to="/app/organizations/new">
          <Button className="rounded-full px-6 shadow-md shadow-indigo-500/20 bg-indigo-600 hover:bg-indigo-700 text-white font-medium h-11"><Plus className="w-4 h-4 mr-2" /> New Organization</Button>
        </Link>
      </div>

      <div className="glass-panel p-4 flex flex-col md:flex-row justify-between items-center gap-4">
        <div className="relative flex-1 w-full md:max-w-md">
          <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 text-slate-400 h-4 w-4" />
          <Input 
            placeholder="Search by name or district..." 
            className="pl-11 bg-white/50 dark:bg-slate-900/50 border-black/5 dark:border-white/10 rounded-xl focus-visible:ring-indigo-500/50 h-11 backdrop-blur-sm"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        <div className="flex gap-4 w-full md:w-auto">
          <select 
            className="flex-1 md:flex-none h-11 rounded-xl border border-black/5 dark:border-white/10 bg-white/50 dark:bg-slate-900/50 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 backdrop-blur-sm text-slate-700 dark:text-slate-300"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="ALL">All Statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
            <option value="SUSPENDED">Suspended</option>
          </select>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredOrgs.length === 0 ? (
          <div className="col-span-full py-16 text-center text-slate-500 font-medium glass-panel flex flex-col items-center justify-center">
            <p>No organizations found matching your criteria.</p>
            {searchTerm && <Button variant="link" onClick={() => setSearchTerm("")} className="mt-2 text-indigo-600">Clear search</Button>}
          </div>
        ) : (
          filteredOrgs.map((org: any) => (
            <div key={org.id} className="glass-panel overflow-hidden flex flex-col hover:-translate-y-1 transition-all duration-300 group">
              <div className="p-6 flex-1 space-y-5">
                <div className="flex justify-between items-start">
                  <div className="bg-white/80 dark:bg-slate-800/80 p-4 border border-black/5 dark:border-white/5 rounded-2xl text-indigo-600 dark:text-indigo-400 shadow-sm group-hover:scale-105 transition-transform">
                    <Building2 className="w-6 h-6" />
                  </div>
                  <Badge variant={org.status === "ACTIVE" ? "default" : "secondary"} className="rounded-full px-3 py-1 font-semibold tracking-wide uppercase text-[10px]">
                    {org.status}
                  </Badge>
                </div>
                
                <div>
                  <h3 className="text-2xl font-bold text-slate-900 dark:text-white tracking-tight truncate" title={org.name}>{org.name}</h3>
                  <div className="flex items-center text-slate-500 dark:text-slate-400 font-medium text-sm mt-1.5">
                     <MapPin className="w-3.5 h-3.5 mr-1.5" />
                     <span className="truncate">{org.district || "No district"}</span>
                  </div>
                </div>
                
                <div className="pt-5 border-t border-black/5 dark:border-white/5 space-y-2.5 text-sm text-slate-600 dark:text-slate-300 font-medium bg-white/30 dark:bg-slate-900/30 p-4 rounded-xl">
                  <p><span className="font-bold text-slate-900 dark:text-white uppercase tracking-widest text-[10px] mr-2">Admin:</span> {org.adminFullName}</p>
                  <p className="truncate" title={org.adminEmail}><span className="font-bold text-slate-900 dark:text-white uppercase tracking-widest text-[10px] mr-2">Email:</span> {org.adminEmail}</p>
                  <p><span className="font-bold text-slate-900 dark:text-white uppercase tracking-widest text-[10px] mr-2">Phone:</span> {org.adminPhoneNumber}</p>
                </div>
              </div>
              
              <div className="bg-white/40 dark:bg-slate-900/40 p-4 border-t border-black/5 dark:border-white/5 flex justify-end space-x-3">
                <Link to={`/app/organizations/${org.id}`}>
                   <Button variant="outline" size="sm" className="bg-white/50 dark:bg-slate-800/50 backdrop-blur-sm border-black/5 dark:border-white/10 hover:bg-white/80 dark:hover:bg-slate-700/80 rounded-full px-5 font-semibold">
                     <Edit className="w-4 h-4 mr-2" /> Manage
                   </Button>
                </Link>
                <Button 
                   variant="ghost" 
                   size="sm" 
                   className="text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-full px-3"
                   onClick={() => {
                     if(confirm("Are you sure you want to delete this organization? This is a soft delete.")) {
                       deleteMutation.mutate(org.id);
                     }
                   }}
                >
                  <Trash2 className="w-4 h-4" />
                </Button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
