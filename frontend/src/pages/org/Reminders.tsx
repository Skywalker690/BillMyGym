import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertCircle, BellRing, Clock3, Play, Settings as SettingsIcon } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";
import api from "@/lib/api";
import { Badge } from "@/components/ui/badge";

export default function Reminders() {
  const queryClient = useQueryClient();

  const { data: reminderReport, isLoading } = useQuery({
    queryKey: ["reminderReport"],
    queryFn: async () => {
      const res = await api.get("/reports/reminders");
      return res.data;
    },
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
    <div className="mx-auto max-w-7xl space-y-8 p-8">
      <section className="billmygym-shell rounded-[2rem] p-8">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-[10px] font-black uppercase tracking-[0.26em] text-emerald-700">Renewal Monitoring</div>
            <h1 className="mt-2 text-4xl font-black tracking-tight text-slate-950">Reminders Command</h1>
            <p className="mt-2 max-w-2xl text-sm font-medium leading-6 text-slate-600">
              Control the reminder engine, audit delivery runs, and keep renewals moving before they convert into revenue leakage.
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <Link to="/app/settings">
              <Button variant="outline" className="h-11 rounded-full border-white/80 bg-white/75 px-5 font-bold">
                <SettingsIcon className="mr-2 h-4 w-4" />
                Configure
              </Button>
            </Link>
            <Button
              className="h-11 rounded-full bg-slate-950 px-5 font-bold text-white hover:bg-slate-800"
              onClick={() => {
                if (confirm("Run reminders manually right now?")) {
                  runMutation.mutate();
                }
              }}
              disabled={runMutation.isPending}
            >
              <Play className="mr-2 h-4 w-4" />
              {runMutation.isPending ? "Processing..." : "Run Now"}
            </Button>
          </div>
        </div>
      </section>

      <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
        <div className="space-y-6">
          <div className="glass-panel overflow-hidden rounded-[1.9rem]">
            <div className="border-b border-white/70 p-6">
              <h2 className="flex items-center gap-3 text-2xl font-black tracking-tight text-slate-950">
                <Clock3 className="h-5 w-5 text-emerald-700" />
                Control Panel
              </h2>
              <p className="mt-2 text-sm font-medium text-slate-500">Current reminder timing and delivery channel state.</p>
            </div>
            <div className="space-y-4 p-6">
              <SignalRow label="Reminder lead time" value={`${settings?.reminderDaysBeforeExpiry} days`} />
              <SignalRow label="WhatsApp digest" value={settings?.whatsappEnabled ? "Enabled" : "Disabled"} />
              <SignalRow label="Email digest" value={settings?.emailEnabled ? "Enabled" : "Disabled"} />
              <div className="rounded-[1.4rem] border border-amber-100 bg-amber-50/70 p-4 text-sm font-semibold text-amber-900">
                Manual execution uses the same backend workflow as scheduled runs, so this is safe for local ops checks.
              </div>
            </div>
          </div>

          {lastRun && (
            <div className="glass-panel overflow-hidden rounded-[1.9rem]">
              <div className="border-b border-white/70 p-6">
                <h2 className="text-2xl font-black tracking-tight text-slate-950">Latest Digest</h2>
              </div>
              <div className="grid gap-4 p-6 sm:grid-cols-2">
                <MiniStat label="Members Covered" value={lastRun.memberCount ?? 0} />
                <MiniStat label="Last Status" value={lastRun.status} />
                <div className="sm:col-span-2 rounded-[1.4rem] border border-white/80 bg-white/72 p-4 text-sm font-semibold text-slate-600">
                  Last activity at {new Date(lastRun.sentAt || lastRun.createdAt).toLocaleString()}
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="glass-panel overflow-hidden rounded-[1.9rem]">
          <div className="border-b border-white/70 p-6">
            <div className="flex items-center justify-between gap-4">
              <div>
                <h2 className="text-2xl font-black tracking-tight text-slate-950">Reminder Workflow Logs</h2>
                <p className="mt-2 text-sm font-medium text-slate-500">History of digest notifications sent by the scheduler.</p>
              </div>
              <div className="rounded-2xl border border-white/80 bg-white/75 p-3 shadow-sm">
                <BellRing className="h-5 w-5 text-emerald-700" />
              </div>
            </div>
          </div>
          <div className="p-6">
            {logs.length === 0 ? (
              <div className="py-16 text-center text-slate-500">
                <Clock3 className="mx-auto mb-4 h-14 w-14 opacity-40" />
                <p className="text-sm font-semibold">No reminder logs found yet.</p>
              </div>
            ) : (
              <div className="space-y-3">
                {logs.map((log: any) => (
                  <div key={log.id} className="rounded-[1.4rem] border border-white/80 bg-white/72 p-4 shadow-sm">
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                      <div>
                        <div className="text-sm font-black text-slate-950">{new Date(log.sentAt || log.createdAt).toLocaleString()}</div>
                        <div className="mt-1 text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">{log.channel}</div>
                      </div>
                      <div className="flex items-center gap-2">
                        <Badge className="border-slate-200 bg-white text-slate-700">{log.memberCount ?? 0} members</Badge>
                        <Badge className={log.status === "SENT" ? "border-emerald-200 bg-emerald-100 text-emerald-800" : "border-amber-200 bg-amber-100 text-amber-800"}>
                          {log.status}
                        </Badge>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}

function SignalRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[1.35rem] border border-white/80 bg-white/72 px-4 py-3 shadow-sm">
      <div className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">{label}</div>
      <div className="mt-1 text-lg font-black text-slate-950">{value}</div>
    </div>
  );
}

function MiniStat({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-[1.4rem] border border-white/80 bg-white/72 p-4 shadow-sm">
      <div className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">{label}</div>
      <div className="mt-2 text-2xl font-black text-slate-950">{value}</div>
    </div>
  );
}
