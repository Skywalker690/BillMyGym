import { useQuery } from "@tanstack/react-query";
import { Users, CheckCircle2, Gift, Crown } from "lucide-react";
import api from "@/lib/api";

export default function AdminReferralAnalytics() {
  const { data: analyticsData, isLoading } = useQuery({
    queryKey: ["adminReferralAnalytics"],
    queryFn: () => api.get("/referrals/analytics").then((res) => res.data.data),
  });

  if (isLoading) {
    return <div className="p-6">Loading analytics...</div>;
  }

  const data = analyticsData || {};

  return (
    <div className="space-y-6 max-w-5xl mx-auto p-4 sm:p-6 lg:p-8">
      {/* Header */}
      <div className="glass-panel overflow-hidden rounded-[2rem] bg-gradient-to-br from-indigo-950 to-indigo-900 text-white shadow-xl relative">
        <div className="absolute top-0 right-0 p-8 opacity-20">
          <Gift className="w-48 h-48" />
        </div>
        <div className="p-8 sm:p-10 relative z-10">
          <h2 className="text-3xl font-black tracking-tight mb-2">Referral Program Analytics</h2>
          <p className="text-indigo-100/90 text-lg max-w-2xl font-medium leading-relaxed">
            Monitor the platform-wide impact of the GymFlow referral program.
          </p>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Total Referral Signups" value={data.totalReferralSignups || 0} icon={Users} color="text-indigo-500" />
        <StatCard title="Successful Referrals" value={data.totalSuccessfulReferrals || 0} icon={CheckCircle2} color="text-emerald-500" />
        <StatCard title="Total GymCoins Distributed" value={data.totalCoinsRewarded || 0} icon={Gift} color="text-amber-500" />
        <StatCard title="6-Month Pro Unlocks" value={data.totalSixMonthRewards || 0} icon={Crown} color="text-purple-500" />
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
