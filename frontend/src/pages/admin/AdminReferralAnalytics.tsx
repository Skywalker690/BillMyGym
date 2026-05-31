import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Users, CheckCircle2, Gift, Crown, Settings2 } from "lucide-react";
import api from "@/lib/api";
import { toast } from "sonner";

export default function AdminReferralAnalytics() {
  const queryClient = useQueryClient();

  const { data: analyticsData, isLoading } = useQuery({
    queryKey: ["adminReferralAnalytics"],
    queryFn: () => api.get("/referrals/analytics").then((res) => res.data),
  });

  const { data: settingsData } = useQuery({
    queryKey: ["referralSettings"],
    queryFn: () => api.get("/referrals/settings").then((res) => Boolean(res.data)),
  });

  const toggleMutation = useMutation({
    mutationFn: (enabled: boolean) => api.post(`/referrals/settings/toggle?enabled=${enabled}`),
    onSuccess: (_, variables) => {
      toast.success(variables ? "Referral program enabled" : "Referral program disabled");
      queryClient.invalidateQueries({ queryKey: ["referralSettings"] });
    },
    onError: () => toast.error("Failed to update referral settings"),
  });

  if (isLoading) {
    return <div className="p-6">Loading analytics...</div>;
  }

  const data = analyticsData || {};
  const isReferralsEnabled = settingsData === true;

  return (
    <div className="space-y-6 max-w-5xl mx-auto p-4 sm:p-6 lg:p-8">
      {/* Header */}
      <div className="glass-panel overflow-hidden rounded-[2rem] bg-gradient-to-br from-indigo-950 to-indigo-900 text-white shadow-xl relative">
        <div className="absolute top-0 right-0 p-8 opacity-20">
          <Gift className="w-48 h-48" />
        </div>
        <div className="p-8 sm:p-10 relative z-10 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-6">
          <div>
            <h2 className="text-3xl font-black tracking-tight mb-2">Referral Program Analytics</h2>
            <p className="text-indigo-100/90 text-lg max-w-2xl font-medium leading-relaxed">
              Monitor the platform-wide impact of the GymFlow referral program.
            </p>
          </div>
          <div className="flex items-center gap-3 bg-white/10 backdrop-blur-md px-5 py-3 rounded-2xl border border-white/20">
            <Settings2 className="w-5 h-5 text-indigo-200" />
            <div className="flex flex-col">
              <span className="text-sm font-bold">Referral Program</span>
              <span className="text-xs text-indigo-200">{isReferralsEnabled ? 'Active' : 'Disabled'}</span>
            </div>
            <button
              role="switch"
              aria-checked={isReferralsEnabled}
              onClick={() => toggleMutation.mutate(!isReferralsEnabled)}
              disabled={toggleMutation.isPending}
              className={`ml-2 relative inline-flex h-6 w-11 shrink-0 cursor-pointer items-center rounded-full border-2 border-transparent transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 focus-visible:ring-offset-2 focus-visible:ring-offset-white disabled:cursor-not-allowed disabled:opacity-50 ${isReferralsEnabled ? 'bg-emerald-500' : 'bg-slate-300'}`}
            >
              <span
                className={`pointer-events-none block h-5 w-5 rounded-full bg-white shadow-lg ring-0 transition-transform ${isReferralsEnabled ? 'translate-x-5' : 'translate-x-0'}`}
              />
            </button>
          </div>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Total Referral Signups" value={data.totalReferralSignups || 0} icon={Users} color="text-indigo-500" />
        <StatCard title="Successful Referrals" value={data.totalSuccessfulReferrals || 0} icon={CheckCircle2} color="text-emerald-500" />
        <StatCard title="Total GymCoins Distributed" value={data.totalCoinsRewarded || 0} icon={Gift} color="text-amber-500" />
        <StatCard title="6-Month BillMyGym Unlocks" value={data.totalSixMonthRewards || 0} icon={Crown} color="text-purple-500" />
      </div>
    </div>
  );
}

function StatCard({ title, value, icon: Icon, color }: any) {
  return (
    <div className="glass-panel rounded-[1.5rem] p-5 shadow-sm border border-slate-200 bg-white flex items-center justify-between">
      <div>
        <p className="text-xs font-black uppercase tracking-wider text-slate-500 mb-1">{title}</p>
        <p className="text-2xl font-black text-slate-900">
          {Number(value).toLocaleString()}
        </p>
      </div>
      <div className={`p-3 rounded-2xl bg-slate-50 ${color}`}>
        <Icon className="w-6 h-6" />
      </div>
    </div>
  );
}
