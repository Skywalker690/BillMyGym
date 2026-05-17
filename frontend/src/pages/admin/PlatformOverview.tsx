import { useQuery } from "@tanstack/react-query";
import { Building2, Activity as ActivityIcon, Plus, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Link } from "react-router-dom";
import api from "@/lib/api";

export default function PlatformOverview() {
  const { data: analytics, isLoading } = useQuery({
    queryKey: ["platformAnalytics"],
    queryFn: async () => {
      const res = await api.get("/analytics/platform");
      return res.data;
    }
  });

  if (isLoading) {
    return <div className="p-8">Loading overview...</div>;
  }

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8">
      {/* Hero Section */}
      <div className="bg-gradient-to-r from-slate-900 to-indigo-900 rounded-[2rem] p-10 text-white shadow-xl flex flex-col justify-between overflow-hidden relative">
        <div className="relative z-10">
          <h1 className="text-4xl font-bold tracking-tight mb-4">GymFlow Platform</h1>
          <p className="text-indigo-200 max-w-xl text-lg mb-8 font-medium">
            Manage all onboarded organizations, monitor global activity, and provision new gym instances from the command center.
          </p>
          <div className="flex gap-4">
            <Link to="/app/organizations/new">
              <Button size="lg" className="bg-white text-indigo-900 hover:bg-slate-100 font-bold gap-3 rounded-full border-0 px-8">
                <Plus className="w-5 h-5"/> Provision New Organization
              </Button>
            </Link>
          </div>
        </div>
        
        {/* Decorative background elements */}
        <div className="absolute right-0 bottom-0 opacity-10 pointer-events-none translate-x-1/4 translate-y-1/4">
          <Building2 className="w-96 h-96" />
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="glass-panel overflow-hidden hover:bg-white/60 dark:hover:bg-slate-800/80 transition-colors">
          <div className="p-8 flex items-center justify-between">
            <div>
              <p className="text-sm font-bold text-slate-500 dark:text-slate-400 mb-1 uppercase tracking-widest">Total Onboarded Orgs</p>
              <h2 className="text-5xl font-extrabold text-slate-900 dark:text-white tracking-tight">{analytics?.totalOrganizations || 0}</h2>
            </div>
            <div className="p-5 bg-indigo-50/50 dark:bg-indigo-900/20 border border-indigo-100/50 dark:border-indigo-800/30 text-indigo-600 dark:text-indigo-400 rounded-3xl shadow-sm">
              <Building2 className="w-10 h-10" />
            </div>
          </div>
        </div>
        
        <div className="glass-panel overflow-hidden hover:bg-white/60 dark:hover:bg-slate-800/80 transition-colors">
          <div className="p-8 flex items-center justify-between">
            <div>
              <p className="text-sm font-bold text-slate-500 dark:text-slate-400 mb-1 uppercase tracking-widest">Active Subscriptions</p>
              <h2 className="text-5xl font-extrabold text-green-600 dark:text-green-400 tracking-tight">{analytics?.activeSubscriptions || 0}</h2>
            </div>
            <div className="p-5 bg-green-50/50 dark:bg-green-900/20 border border-green-100/50 dark:border-green-800/30 text-green-600 dark:text-green-400 rounded-3xl shadow-sm">
              <ActivityIcon className="w-10 h-10" />
            </div>
          </div>
        </div>
      </div>
      
      <div className="mt-8">
         <Link to="/app/organizations">
           <div className="glass-panel overflow-hidden border-indigo-100/50 dark:border-indigo-900/30 hover:border-indigo-300 dark:hover:border-indigo-700 hover:bg-indigo-50/30 dark:hover:bg-indigo-900/20 transition-all cursor-pointer group shadow-sm">
              <div className="p-8 flex justify-between items-center">
                 <div className="flex items-center">
                    <div className="bg-white/80 dark:bg-slate-800/80 p-4 rounded-2xl shadow-sm border border-black/5 dark:border-white/5 mr-5 group-hover:scale-105 transition-transform">
                       <Building2 className="w-8 h-8 text-indigo-600 dark:text-indigo-400" />
                    </div>
                    <div>
                      <h3 className="font-bold text-slate-900 dark:text-white text-xl tracking-tight">Manage Organizations Directory</h3>
                      <p className="text-sm font-medium text-slate-500 dark:text-slate-400 mt-1">View, edit or deactivate registered gym clients.</p>
                    </div>
                 </div>
                 <div className="bg-white/80 dark:bg-slate-800/80 rounded-full p-3 text-indigo-600 dark:text-indigo-400 shadow-sm border border-black/5 dark:border-white/5 group-hover:scale-110 group-hover:bg-indigo-600 group-hover:text-white transition-all text-white">
                   <ChevronRight className="w-6 h-6"/>
                 </div>
              </div>
           </div>
         </Link>
      </div>

    </div>
  );
}
