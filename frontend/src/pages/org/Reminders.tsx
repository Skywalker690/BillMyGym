import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Clock, Play, AlertCircle, Settings as SettingsIcon } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";
import api from "@/lib/api";

export default function Reminders() {
  const queryClient = useQueryClient();

  const { data: reminderReport, isLoading } = useQuery({
    queryKey: ["reminderReport"],
    queryFn: async () => {
      const res = await api.get("/reports/reminders");
      return res.data;
    }
  });

  const runMutation = useMutation({
    mutationFn: () => api.post("/reminders/run"),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ["reminderReport"] });
      queryClient.invalidateQueries({ queryKey: ["dashboardReport"] });
      const result = response.data;
      toast.success(`Reminder run complete: ${result.digestsSent ?? 0} digest(s) sent`);
    },
    onError: () => toast.error("Failed to run reminders"),
  });

  if (isLoading) {
    return <div className="p-8">Loading reminders panel...</div>;
  }

  const settings = reminderReport?.settings;
  const logs = reminderReport?.recentReminderLogs ?? [];
  const lastRun = logs[0];

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8">
      <div className="flex justify-between items-center bg-white/40 dark:bg-slate-900/40 p-6 rounded-[2rem] backdrop-blur-xl border border-white/60 dark:border-white/10 shadow-sm">
        <div>
          <h1 className="text-4xl font-bold text-slate-900 dark:text-white tracking-tight">Reminders</h1>
          <p className="text-slate-500 dark:text-slate-400 mt-1 font-medium">Manage and track automated membership reminders.</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="md:col-span-1 space-y-6">
          <div className="glass-panel overflow-hidden">
            <div className="p-6 border-b border-black/5 dark:border-white/5 bg-indigo-50/50 dark:bg-indigo-900/10">
              <h2 className="text-xl font-bold tracking-tight text-indigo-900 dark:text-indigo-300 flex items-center">
                <Clock className="w-5 h-5 mr-3 text-indigo-600 dark:text-indigo-400" />
                Control Panel
              </h2>
            </div>
            <div className="p-6 space-y-6">
              <div>
                <h3 className="text-sm font-bold text-slate-900 dark:text-white uppercase tracking-wider mb-4">Current Configuration</h3>
                <ul className="space-y-4 text-sm text-slate-700 dark:text-slate-300 font-medium">
                  <li className="flex justify-between border-b border-black/5 dark:border-white/5 pb-2"><span>Remind before expiry:</span> <span className="font-bold text-slate-900 dark:text-white">{settings?.reminderDaysBeforeExpiry} days</span></li>
                  <li className="flex justify-between border-b border-black/5 dark:border-white/5 pb-2"><span>WhatsApp:</span> <span className={settings?.whatsappEnabled ? "text-green-600 dark:text-green-400 font-bold" : "text-slate-400 dark:text-slate-500"}>{settings?.whatsappEnabled ? "Enabled" : "Disabled"}</span></li>
                  <li className="flex justify-between border-b border-black/5 dark:border-white/5 pb-2"><span>Email:</span> <span className={settings?.emailEnabled ? "text-green-600 dark:text-green-400 font-bold" : "text-slate-400 dark:text-slate-500"}>{settings?.emailEnabled ? "Enabled" : "Disabled"}</span></li>
                </ul>
                <div className="mt-6">
                  <Link to="/app/settings">
                    <Button variant="outline" className="w-full rounded-xl bg-white/50 dark:bg-slate-800/50 backdrop-blur-sm border-black/5 dark:border-white/20 hover:bg-white/80 dark:hover:bg-slate-700/80 font-semibold h-11">
                      <SettingsIcon className="w-4 h-4 mr-2" /> Modify Settings
                    </Button>
                  </Link>
                </div>
              </div>

              <div className="pt-6 border-t border-black/5 dark:border-white/5">
                <Button
                  className="w-full rounded-xl h-12 shadow-lg shadow-indigo-500/20 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-base"
                  onClick={() => {
                    if (confirm("Are you sure you want to run reminders manually? This will process notifications for eligible members immediately.")) {
                      runMutation.mutate();
                    }
                  }}
                  disabled={runMutation.isPending}
                >
                  <Play className="w-5 h-5 mr-3 fill-current" />
                  {runMutation.isPending ? "Processing..." : "Run Reminders Now"}
                </Button>
                <p className="text-xs text-slate-500 dark:text-slate-400 text-center mt-4 font-medium flex items-center justify-center">
                  <AlertCircle className="w-3.5 h-3.5 mr-1.5" /> Automated processes normally handle this.
                </p>
              </div>
            </div>
          </div>

          {lastRun && (
            <div className="glass-panel overflow-hidden">
              <div className="p-6 border-b border-black/5 dark:border-white/5">
                <h2 className="text-lg font-bold tracking-tight text-slate-900 dark:text-white">Latest Digest</h2>
              </div>
              <div className="p-6">
                <div className="grid grid-cols-2 gap-4 text-center">
                  <div className="p-5 bg-white/50 dark:bg-slate-800/50 border border-black/5 dark:border-white/5 rounded-2xl">
                    <div className="text-3xl font-extrabold text-slate-900 dark:text-white">{lastRun.memberCount ?? 0}</div>
                    <div className="text-[10px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-widest mt-2">Members Covered</div>
                  </div>
                  <div className="p-5 bg-white/50 dark:bg-slate-800/50 border border-indigo-100 dark:border-indigo-900/50 rounded-2xl shadow-sm shadow-indigo-500/10">
                    <div className="text-3xl font-extrabold text-indigo-600 dark:text-indigo-400">{lastRun.status}</div>
                    <div className="text-[10px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-widest mt-2">Status</div>
                  </div>
                </div>
                <div className="text-xs font-medium text-center text-slate-500 dark:text-slate-400 mt-6 bg-slate-50 dark:bg-slate-900 px-4 py-2 rounded-lg">
                  Last digest sent at {new Date(lastRun.sentAt || lastRun.createdAt).toLocaleString()}
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="md:col-span-2">
          <div className="glass-panel overflow-hidden h-full flex flex-col">
            <div className="p-6 border-b border-black/5 dark:border-white/5">
              <h2 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">Reminder Workflow Logs</h2>
              <p className="text-sm text-slate-500 dark:text-slate-400 mt-1 font-medium">History of all admin digest notifications sent by the scheduler.</p>
            </div>
            <div className="flex-1 p-6">
              {logs.length === 0 ? (
                <div className="py-16 text-center text-slate-500 dark:text-slate-400 flex flex-col items-center justify-center h-full">
                  <Clock className="w-16 h-16 mx-auto text-slate-300 dark:text-slate-600 mb-6 opacity-50" />
                  <p className="font-medium text-lg">No reminder logs found yet.</p>
                </div>
              ) : (
                <div className="overflow-x-auto rounded-xl border border-black/5 dark:border-white/5">
                  <table className="w-full text-sm text-left">
                    <thead className="bg-slate-50/80 dark:bg-slate-800/80 text-slate-500 dark:text-slate-400">
                      <tr>
                        <th className="px-6 py-4 font-bold tracking-wider uppercase text-xs">Date & Time</th>
                        <th className="px-6 py-4 font-bold tracking-wider uppercase text-xs text-right">Channel</th>
                        <th className="px-6 py-4 font-bold tracking-wider uppercase text-xs text-right">Members</th>
                        <th className="px-6 py-4 font-bold tracking-wider uppercase text-xs text-right">Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-black/5 dark:divide-white/5 bg-white/30 dark:bg-slate-900/30">
                      {logs.map((log: any) => (
                        <tr key={log.id} className="hover:bg-white/60 dark:hover:bg-slate-800 transition-colors">
                          <td className="px-6 py-4 whitespace-nowrap text-slate-900 dark:text-white font-semibold">
                            {new Date(log.sentAt || log.createdAt).toLocaleString()}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-slate-600 dark:text-slate-300 font-medium text-right">
                            {log.channel}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-slate-600 dark:text-slate-300 font-medium text-right">
                            {log.memberCount ?? 0}
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap text-indigo-600 dark:text-indigo-400 text-right font-bold tracking-wide">
                            {log.status}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
