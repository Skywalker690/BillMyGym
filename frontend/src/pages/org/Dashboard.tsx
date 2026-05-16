import { useQuery } from "@tanstack/react-query";
import { AlertCircle, AlertTriangle, Clock, Plus, TrendingUp, Users, Wallet } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import api from "@/lib/api";

const money = (value: number | string | undefined) =>
  `Rs ${Number(value ?? 0).toLocaleString("en-IN", { maximumFractionDigits: 0 })}`;

const date = (value?: string) => value ? new Date(value).toLocaleDateString() : "-";

export default function Dashboard() {
  const { data: report, isLoading } = useQuery({
    queryKey: ["dashboardReport"],
    queryFn: async () => {
      const res = await api.get("/reports/dashboard");
      return res.data;
    },
  });

  if (isLoading) {
    return <div className="p-8">Loading dashboard...</div>;
  }

  const revenue = report?.revenue ?? {};
  const summary = report?.memberSummary ?? {};
  const stats = [
    { title: "Current Revenue", value: money(revenue.currentRevenue), icon: Wallet, color: "text-green-600", bg: "bg-green-100" },
    { title: "Pending Revenue", value: money(revenue.pendingRevenue), icon: AlertTriangle, color: "text-amber-600", bg: "bg-amber-100" },
    { title: "Expected Revenue", value: money(revenue.expectedRevenue), icon: TrendingUp, color: "text-indigo-600", bg: "bg-indigo-100" },
    { title: "Overdue Revenue", value: money(revenue.overdueRevenue), icon: AlertCircle, color: "text-red-600", bg: "bg-red-100" },
    { title: "Active Members", value: revenue.activeMembers ?? 0, icon: Users, color: "text-blue-600", bg: "bg-blue-100" },
    { title: "Prepaid Members", value: revenue.prepaidMembers ?? 0, icon: Clock, color: "text-teal-600", bg: "bg-teal-100" },
  ];

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8">
      <div className="flex flex-col gap-4 md:flex-row md:justify-between md:items-center bg-white/40 dark:bg-slate-900/40 p-6 rounded-2xl backdrop-blur-xl border border-white/60 dark:border-white/10 shadow-sm">
        <div>
          <h1 className="text-3xl font-bold text-slate-900 dark:text-white tracking-tight">Overview</h1>
          <p className="text-slate-500 dark:text-slate-400 mt-1 font-medium">Server-computed operations and revenue snapshot.</p>
        </div>
        <div className="flex gap-3">
          <Link to="/app/members/new">
            <Button className="rounded-full px-5 bg-indigo-600 hover:bg-indigo-700 text-white font-medium">
              <Plus className="w-4 h-4 mr-2" /> Add Member
            </Button>
          </Link>
          <Link to="/app/reminders">
            <Button variant="outline" className="rounded-full px-5 bg-white/60 dark:bg-slate-800/60 font-medium">
              <Clock className="w-4 h-4 mr-2" /> Reminders
            </Button>
          </Link>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
        {stats.map((stat) => (
          <div key={stat.title} className="glass-panel p-6 flex flex-col justify-between min-h-36">
            <div className={`p-3 rounded-xl ${stat.bg} ${stat.color} w-fit`}>
              <stat.icon className="h-5 w-5" />
            </div>
            <div>
              <p className="text-xs font-semibold tracking-wider uppercase text-slate-500 dark:text-slate-400 mb-1">{stat.title}</p>
              <h3 className="text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight">{stat.value}</h3>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <div className="glass-panel p-6">
          <p className="text-xs font-semibold tracking-wider uppercase text-slate-500">Directory Summary</p>
          <div className="mt-5 grid grid-cols-2 gap-4 text-sm">
            <Metric label="Total" value={summary.totalMembers} />
            <Metric label="Active" value={summary.activeMembers} />
            <Metric label="Expiring Soon" value={summary.expiringSoonMemberships} />
            <Metric label="Overdue" value={summary.overdueMemberships} />
          </div>
        </div>
        <MembershipList title="Expiring Soon" items={report?.expiringSoon ?? []} tone="amber" />
        <MembershipList title="Pending Renewals" items={report?.overdue ?? []} tone="red" />
      </div>

      <div className="glass-panel overflow-hidden">
        <div className="p-6 border-b border-black/5 dark:border-white/5">
          <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white">Recent Reminder Activity</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-black/5 dark:divide-white/5">
            <thead>
              <tr>
                <th className="px-6 py-4 text-left text-xs font-bold text-slate-500 uppercase tracking-wider">Date</th>
                <th className="px-6 py-4 text-left text-xs font-bold text-slate-500 uppercase tracking-wider">Channel</th>
                <th className="px-6 py-4 text-left text-xs font-bold text-slate-500 uppercase tracking-wider">Members</th>
                <th className="px-6 py-4 text-left text-xs font-bold text-slate-500 uppercase tracking-wider">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-black/5 dark:divide-white/5">
              {(report?.recentReminderLogs ?? []).map((log: any) => (
                <tr key={log.id}>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-700 dark:text-slate-300">{date(log.sentAt ?? log.createdAt)}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">{log.channel}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">{log.memberCount}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">{log.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value?: number }) {
  return (
    <div className="rounded-xl border border-black/5 dark:border-white/10 bg-white/50 dark:bg-slate-900/40 p-4">
      <div className="text-2xl font-bold text-slate-900 dark:text-white">{value ?? 0}</div>
      <div className="text-xs font-semibold uppercase tracking-wider text-slate-500 mt-1">{label}</div>
    </div>
  );
}

function MembershipList({ title, items, tone }: { title: string; items: any[]; tone: "amber" | "red" }) {
  const color = tone === "amber" ? "text-amber-600" : "text-red-600";
  return (
    <div className="glass-panel overflow-hidden">
      <div className="p-6 border-b border-black/5 dark:border-white/5">
        <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white">{title}</h2>
      </div>
      <div className="p-5">
        {items.length === 0 ? (
          <p className="text-sm text-slate-500 font-medium">No memberships in this report.</p>
        ) : (
          <ul className="space-y-3">
            {items.map((member) => (
              <li key={member.id} className="flex justify-between gap-4 rounded-xl border border-black/5 dark:border-white/10 bg-white/50 dark:bg-slate-900/40 p-4">
                <div>
                  <p className="font-bold text-sm text-slate-900 dark:text-white">{member.fullName}</p>
                  <p className={`text-xs font-medium mt-1 ${color}`}>
                    {member.currentMembership?.planName ?? "Membership"} ends {date(member.currentMembership?.endDate)}
                  </p>
                </div>
                <Link to={`/app/members/${member.id}/edit`}>
                  <Button variant="ghost" size="sm" className="rounded-full">View</Button>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
