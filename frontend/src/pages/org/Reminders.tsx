import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertCircle, BellRing, Clock3, Play, Settings as SettingsIcon } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";
import api from "@/lib/api";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { useDemoPlanPreview } from "@/context/DemoPlanPreviewContext";

export default function Reminders() {
  const { isDemoWorkspace } = useDemoPlanPreview();
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
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || "Failed to run reminders");
    },
  });

  if (isLoading) {
    return <div className="p-8">Loading reminders panel...</div>;
  }

  const settings = reminderReport?.settings;
  const logs = reminderReport?.recentReminderLogs ?? [];
  const manualQuota = reminderReport?.manualQuota;
  const lastRun = logs[0];
  const manualRunsUsed = Number(manualQuota?.usedThisMonth ?? 0);
  const manualRunLimit = Number(manualQuota?.monthlyLimit ?? 20);
  const unlimitedManualRuns = Boolean(manualQuota?.unlimited);
  const canRunManual = Boolean(manualQuota?.canRun ?? true);

  return (
    <div className="mx-auto max-w-7xl space-y-5 sm:space-y-8 px-3 sm:p-8">
      <section className="billmygym-shell rounded-[1.8rem] sm:rounded-[2rem] p-4 sm:p-8">
        <div className="flex flex-col gap-4 sm:gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="text-[9px] sm:text-[10px] font-black uppercase tracking-[0.22em] sm:tracking-[0.26em] text-emerald-700">Renewal Monitoring</div>
            <h1 className="mt-1.5 sm:mt-2 text-2xl sm:text-4xl font-black tracking-tight text-slate-950">Reminders Command</h1>
            <p className="mt-1.5 sm:mt-2 max-w-2xl text-xs sm:text-sm font-medium leading-5 sm:leading-6 text-slate-600">
              Control reminders, audit runs, and protect revenue before leakage.
            </p>
          </div>
          <div className="flex flex-wrap gap-2 sm:gap-3">
            <Link to="/app/settings">
              <Button variant="outline" className="h-9 sm:h-11 rounded-full border-white/80 bg-white/75 px-3 sm:px-5 text-xs sm:text-sm font-bold">
                <SettingsIcon className="mr-1.5 sm:mr-2 h-3.5 sm:h-4 w-3.5 sm:w-4" />
                <span className="hidden sm:inline">Configure</span>
                <span className="sm:hidden">Setup</span>
              </Button>
            </Link>
            <Button
              className="h-9 sm:h-11 rounded-full bg-slate-950 px-3 sm:px-5 text-xs sm:text-sm font-bold text-white hover:bg-slate-800"
              onClick={() => {
                if (confirm("Run reminders manually right now?")) {
                  runMutation.mutate();
                }
              }}
              disabled={runMutation.isPending || !canRunManual}
            >
              <Play className="mr-1.5 sm:mr-2 h-3.5 sm:h-4 w-3.5 sm:w-4" />
              <span className="hidden sm:inline">{runMutation.isPending ? "Processing..." : canRunManual ? "Run Now" : "Limit Reached"}</span>
              <span className="sm:hidden">{runMutation.isPending ? "..." : "Run"}</span>
            </Button>
          </div>
        </div>
      </section>

      <section className="grid gap-4 sm:gap-6 lg:grid-cols-[0.9fr_1.1fr]">
        <div className="space-y-4 sm:space-y-6">
          <div className="glass-panel overflow-hidden rounded-[1.5rem] sm:rounded-[1.9rem]">
            <div className="border-b border-white/70 p-3 sm:p-6">
              <h2 className="flex items-center gap-2 sm:gap-3 text-lg sm:text-2xl font-black tracking-tight text-slate-950">
                <Clock3 className="h-4 sm:h-5 w-4 sm:w-5 text-emerald-700" />
                Control Panel
              </h2>
              <p className="mt-1 sm:mt-2 text-[10px] sm:text-sm font-medium text-slate-500">Delivery state and status.</p>
            </div>
            <div className="space-y-2 sm:space-y-4 p-3 sm:p-6">
              <SignalRow label="Manual runs used" value={unlimitedManualRuns ? `${manualRunsUsed} this month` : `${manualRunsUsed} / ${manualRunLimit}`} compact />
              <SignalRow label="WhatsApp digest" value={settings?.whatsappEnabled ? "Enabled" : "Disabled"} compact />
              <SignalRow label="Email digest" value={settings?.emailEnabled ? "Enabled" : "Disabled"} compact />
              {unlimitedManualRuns ? (
                <div className="rounded-[1rem] sm:rounded-[1.4rem] border border-sky-100 bg-sky-50/80 p-2.5 sm:p-4 text-[10px] sm:text-sm font-semibold text-sky-900">
                  Unlimited manual runs for demo use only.
                </div>
              ) : (
                <div className="rounded-[1rem] sm:rounded-[1.4rem] border border-amber-100 bg-amber-50/70 p-2.5 sm:p-4 text-[10px] sm:text-sm font-semibold text-amber-900">
                  20 runs per month limit. Scheduled jobs are separate.
                </div>
              )}
              {isDemoWorkspace && (
                <Badge className="w-fit text-[8px] sm:text-xs border-sky-200 bg-sky-100 text-sky-800 px-2 sm:px-3 py-1">Demo only</Badge>
              )}
              {!canRunManual && (
                <div className="rounded-[1rem] sm:rounded-[1.4rem] border border-rose-100 bg-rose-50/80 p-2.5 sm:p-4 text-[10px] sm:text-sm font-semibold text-rose-900">
                  Monthly limit reached. Resets next month.
                </div>
              )}
            </div>
          </div>

          {lastRun && (
            <div className="glass-panel overflow-hidden rounded-[1.5rem] sm:rounded-[1.9rem]">
              <div className="border-b border-white/70 p-3 sm:p-6">
                <h2 className="text-base sm:text-2xl font-black tracking-tight text-slate-950">Latest Digest</h2>
              </div>
              <div className="grid gap-2 sm:gap-4 p-3 sm:p-6 grid-cols-2 sm:grid-cols-2">
                <MiniStat label="Members Covered" value={lastRun.memberCount ?? 0} compact />
                <MiniStat label="Last Status" value={lastRun.status} compact />
                <div className="col-span-2 rounded-[1rem] sm:rounded-[1.4rem] border border-white/80 bg-white/72 p-2 sm:p-4 text-[10px] sm:text-sm font-semibold text-slate-600">
                  {new Date(lastRun.sentAt || lastRun.createdAt).toLocaleString()}
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="glass-panel overflow-hidden rounded-[1.5rem] sm:rounded-[1.9rem]">
          <div className="border-b border-white/70 p-3 sm:p-6">
            <div className="flex items-center justify-between gap-3 sm:gap-4">
              <div>
                <h2 className="text-base sm:text-2xl font-black tracking-tight text-slate-950">Reminder Logs</h2>
                <p className="mt-1 sm:mt-2 text-[10px] sm:text-sm font-medium text-slate-500">Digest history.</p>
              </div>
              <div className="rounded-lg sm:rounded-2xl border border-white/80 bg-white/75 p-2 sm:p-3 shadow-sm">
                <BellRing className="h-4 sm:h-5 w-4 sm:w-5 text-emerald-700" />
              </div>
            </div>
          </div>
          <div className="p-3 sm:p-6">
            {logs.length === 0 ? (
              <div className="py-8 sm:py-16 text-center text-slate-500">
                <Clock3 className="mx-auto mb-2 sm:mb-4 h-10 sm:h-14 w-10 sm:w-14 opacity-40" />
                <p className="text-xs sm:text-sm font-semibold">No logs yet.</p>
              </div>
            ) : (
              <div className="space-y-2 sm:space-y-3">
                {logs.map((log: any) => (
                  <div key={log.id} className="rounded-[1rem] sm:rounded-[1.4rem] border border-white/80 bg-white/72 p-2.5 sm:p-4 shadow-sm">
                    <div className="flex flex-col gap-2 sm:gap-3 sm:flex-row sm:items-center sm:justify-between">
                      <div>
                        <div className="text-xs sm:text-sm font-black text-slate-950">{new Date(log.sentAt || log.createdAt).toLocaleString()}</div>
                        <div className="mt-0.5 text-[8px] sm:text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">{log.channel}</div>
                      </div>
                      <div className="flex items-center gap-1.5 sm:gap-2">
                        <Badge className="border-slate-200 bg-white text-slate-700 text-[8px] sm:text-xs px-2 sm:px-3 py-0.5 sm:py-1">{log.memberCount ?? 0}</Badge>
                        <Badge className={log.status === "SENT" ? "border-emerald-200 bg-emerald-100 text-emerald-800" : "border-amber-200 bg-amber-100 text-amber-800"} style={{fontSize: '0.625rem', padding: '0.25rem 0.5rem'}}>
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

function SignalRow({ label, value, compact = false }: { label: string; value: string; compact?: boolean }) {
  return (
    <div className={cn("rounded-[1rem] sm:rounded-[1.35rem] border border-white/80 bg-white/72 shadow-sm", compact ? "px-2.5 sm:px-4 py-2 sm:py-3" : "px-4 py-3")}>
      <div className={cn("font-black uppercase tracking-[0.18em] text-slate-500", compact ? "text-[8px] sm:text-[10px]" : "text-[10px]")}>{label}</div>
      <div className={cn("font-black text-slate-950", compact ? "mt-0.5 sm:mt-1 text-base sm:text-lg" : "mt-1 text-lg")}>{value}</div>
    </div>
  );
}

function MiniStat({ label, value, compact = false }: { label: string; value: string | number; compact?: boolean }) {
  return (
    <div className={cn("rounded-[1rem] sm:rounded-[1.4rem] border border-white/80 bg-white/72 shadow-sm", compact ? "p-2 sm:p-4" : "p-4")}>
      <div className={cn("font-black uppercase tracking-[0.18em] text-slate-500", compact ? "text-[8px] sm:text-[10px]" : "text-[10px]")}>{label}</div>
      <div className={cn("font-black text-slate-950", compact ? "mt-0.5 sm:mt-2 text-lg sm:text-2xl" : "mt-2 text-2xl")}>{value}</div>
    </div>
  );
}
