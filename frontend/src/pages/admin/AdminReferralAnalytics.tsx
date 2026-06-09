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
    <div className="mx-auto max-w-5xl space-y-4 p-3 sm:space-y-6 sm:p-6 lg:p-8">
      {/* Header */}
      <div className="glass-panel relative overflow-hidden rounded-[1.5rem] bg-gradient-to-br from-indigo-950 to-indigo-900 text-white shadow-xl sm:rounded-[2rem]">
        <div className="absolute top-0 right-0 p-8 opacity-20">
          <Gift className="w-48 h-48" />
        </div>
        <div className="relative z-10 flex flex-col items-start justify-between gap-4 p-4 sm:flex-row sm:items-center sm:gap-6 sm:p-10">
          <div>
            <h2 className="mb-2 text-2xl font-black tracking-tight sm:text-3xl">Referral Program Analytics</h2>
            <p className="max-w-2xl text-sm font-medium leading-6 text-indigo-100/90 sm:text-lg sm:leading-relaxed">
              Monitor the platform-wide impact of the GymFlow referral program.
            </p>
          </div>
          <div className="flex w-full items-center gap-3 rounded-2xl border border-white/20 bg-white/10 px-4 py-3 backdrop-blur-md sm:w-auto sm:px-5">
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
      <div className="grid grid-cols-2 gap-2 sm:gap-4 md:grid-cols-2 lg:grid-cols-4">
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
    <div className="glass-panel flex items-center justify-between rounded-[1.15rem] border border-slate-200 bg-white p-3 shadow-sm sm:rounded-[1.5rem] sm:p-5">
      <div>
        <p className="mb-1 text-[9px] font-black uppercase tracking-[0.1em] text-slate-500 sm:text-xs sm:tracking-wider">{title}</p>
        <p className="text-xl font-black text-slate-900 sm:text-2xl">
          {Number(value).toLocaleString()}
        </p>
      </div>
      <div className={`rounded-xl bg-slate-50 p-2 sm:rounded-2xl sm:p-3 ${color}`}>
        <Icon className="h-5 w-5 sm:h-6 sm:w-6" />
      </div>
    </div>
  );
}
