import { useQuery } from "@tanstack/react-query";
import { Gift, CheckCircle2, Clock, Copy, Plus, Award, ShieldOff } from "lucide-react";
import { toast } from "sonner";
import { format } from "date-fns";
import api from "@/lib/api";

export default function ReferralDashboard() {
  const { data: referralsEnabled, isLoading: isLoadingSettings } = useQuery({
    queryKey: ["referralSettings"],
    queryFn: () => api.get("/referrals/settings").then((res) => Boolean(res.data)),
  });

  const { data: dashboardData, isLoading } = useQuery({
    queryKey: ["referralDashboard"],
    queryFn: () => api.get("/referrals/dashboard").then((res) => res.data),
    enabled: referralsEnabled === true,
  });

  const copyLink = () => {
    if (!dashboardData?.referralCode) return;
    const url = `${window.location.origin}/signup?ref=${dashboardData.referralCode}`;
    navigator.clipboard.writeText(url);
    toast.success("Referral link copied!");
  };

  const copyCode = () => {
    if (!dashboardData?.referralCode) return;
    navigator.clipboard.writeText(dashboardData.referralCode);
    toast.success("Referral code copied!");
  };

  if (isLoadingSettings || (referralsEnabled === true && isLoading)) {
    return <div className="p-6">Loading dashboard...</div>;
  }

  if (referralsEnabled === false) {
    return (
      <div className="mx-auto max-w-3xl p-4 sm:p-6 lg:p-8">
        <div className="glass-panel overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-xl">
          <div className="bg-gradient-to-br from-slate-950 via-slate-900 to-slate-800 p-8 text-white sm:p-10">
            <div className="mb-6 inline-flex rounded-2xl border border-white/10 bg-white/10 p-4">
              <ShieldOff className="h-8 w-8 text-amber-300" />
            </div>
            <h2 className="text-3xl font-black tracking-tight">Referral program is currently disabled</h2>
            <p className="mt-3 max-w-2xl text-base font-medium leading-relaxed text-slate-300">
              Referral links and codes are temporarily inactive. New organizations can still sign up, but no referral will be accepted until the super admin turns the program back on.
            </p>
          </div>
        </div>
      </div>
    );
  }

  const data = dashboardData || {};

  return (
    <div className="space-y-6 max-w-5xl mx-auto p-4 sm:p-6 lg:p-8">
      {/* Hero Section */}
      <div className="glass-panel overflow-hidden rounded-[2rem] bg-gradient-to-br from-emerald-950 to-emerald-900 text-white shadow-xl relative">
        <div className="absolute top-0 right-0 p-8 opacity-20">
          <Gift className="w-48 h-48" />
        </div>
        <div className="p-8 sm:p-10 relative z-10">
          <h2 className="text-3xl font-black tracking-tight mb-4">Invite & Earn GymCoins</h2>
          <p className="text-emerald-100/90 text-lg max-w-2xl font-medium leading-relaxed mb-8">
            Get <span className="font-bold text-white">300 GymCoins</span> for every organization you refer that completes their first subscription payment. Reach 6 successful referrals and unlock a massive <span className="font-bold text-white">6 Months Free Pro Subscription</span>!
          </p>
          
          <div className="flex flex-col sm:flex-row gap-4 max-w-md">
            <div className="bg-white/10 backdrop-blur-sm border border-white/20 rounded-[1.2rem] flex items-center justify-between p-2 flex-1">
              <span className="font-mono font-bold text-lg px-4 truncate">{data.referralCode || "Generating..."}</span>
              <button 
                onClick={copyCode}
                className="bg-white text-emerald-950 hover:bg-emerald-50 px-4 py-2 rounded-xl font-bold transition flex items-center"
              >
                <Copy className="w-4 h-4 mr-2" /> Code
              </button>
            </div>
            <button 
              onClick={copyLink}
              className="bg-emerald-500 hover:bg-emerald-400 text-white px-6 py-3 rounded-[1.2rem] font-bold transition flex items-center justify-center whitespace-nowrap shadow-lg shadow-emerald-500/30"
            >
              <Plus className="w-5 h-5 mr-2" /> Share Link
            </button>
          </div>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="GymCoin Balance" value={data.gymCoinBalance || 0} icon={Award} suffix="Coins" color="text-amber-500" />
        <StatCard title="Successful Referrals" value={data.successfulReferrals || 0} icon={CheckCircle2} color="text-emerald-500" />
        <StatCard title="Pending Signups" value={data.pendingReferrals || 0} icon={Clock} color="text-slate-500" />
        <StatCard title="Total Earnings" value={data.totalCoinsEarned || 0} icon={Gift} suffix="Coins" color="text-indigo-500" />
      </div>

      {/* Progress to Next Milestone */}
      <div className="glass-panel rounded-[1.5rem] p-6 shadow-sm border border-slate-200 bg-white">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-black text-lg text-slate-900">Milestone Progress</h3>
          <span className="text-sm font-bold text-emerald-600 bg-emerald-50 px-3 py-1 rounded-full">
            {data.sixMonthRewardClaimed ? "Milestone Achieved!" : `${data.nextMilestoneRemaining} referrals left`}
          </span>
        </div>
        <div className="h-4 bg-slate-100 rounded-full overflow-hidden">
          <div 
            className="h-full bg-gradient-to-r from-emerald-400 to-emerald-600 transition-all duration-1000 ease-out"
            style={{ width: `${Math.min(100, (6 - (data.nextMilestoneRemaining || 6)) * (100/6))}%` }}
          />
        </div>
        <p className="mt-3 text-sm font-medium text-slate-500">
          Unlock 6 months of Pro for free on your 6th successful referral!
        </p>
      </div>

      {/* Recent Activity */}
      <div className="grid lg:grid-cols-2 gap-6">
        <ActivityList title="Recent Referrals" items={data.recentReferrals} emptyMsg="No referrals yet. Share your code!" />
        <ActivityList title="Recent Rewards" items={data.recentRewards} emptyMsg="Rewards will appear here once referrals subscribe." isReward />
      </div>
    </div>
  );
}

function StatCard({ title, value, icon: Icon, suffix, color }: any) {
  return (
    <div className="glass-panel rounded-[1.5rem] p-5 shadow-sm border border-slate-200 bg-white flex items-center justify-between">
      <div>
        <p className="text-xs font-black uppercase tracking-wider text-slate-500 mb-1">{title}</p>
        <p className="text-2xl font-black text-slate-900">
          {Number(value).toLocaleString()} {suffix && <span className="text-sm font-semibold text-slate-400">{suffix}</span>}
        </p>
      </div>
      <div className={`p-3 rounded-2xl bg-slate-50 ${color}`}>
        <Icon className="w-6 h-6" />
      </div>
    </div>
  );
}

function ActivityList({ title, items, emptyMsg, isReward }: any) {
  return (
    <div className="glass-panel rounded-[1.5rem] p-6 shadow-sm border border-slate-200 bg-white">
      <h3 className="font-black text-lg text-slate-900 mb-4">{title}</h3>
      {(!items || items.length === 0) ? (
        <div className="text-sm font-medium text-slate-500 py-8 text-center bg-slate-50 rounded-xl">{emptyMsg}</div>
      ) : (
        <div className="space-y-3">
          {items.map((item: any, i: number) => (
            <div key={i} className="flex items-center justify-between p-3 rounded-xl border border-slate-100 hover:bg-slate-50 transition">
              <div>
                <p className="font-bold text-slate-900 text-sm">{isReward ? item.rewardType?.replace(/_/g, " ") || 'REWARD' : item.organizationName}</p>
                <p className="text-xs font-semibold text-slate-500">{item.date ? format(new Date(item.date), "MMM d, yyyy") : 'N/A'}</p>
              </div>
              <div className="text-right">
                {isReward ? (
                  <span className="font-black text-emerald-600 text-sm">+{item.amount || `${item.monthsRewarded} Mo`}</span>
                ) : (
                  <span className={`text-xs font-bold px-2.5 py-1 rounded-full ${item.status === 'REWARDED' ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-700'}`}>
                    {item.status}
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
