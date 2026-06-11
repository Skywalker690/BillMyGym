import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BellRing, Clock3, LogOut, Play, QrCode, Save, Settings as SettingsIcon, Smartphone } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { toast } from "sonner";
import api from "@/lib/api";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { useDemoPlanPreview } from "@/context/DemoPlanPreviewContext";
import { useAuth } from "@/context/AuthContext";
import { demoMonthlyReportDigestMessage, demoReminderReport, demoWorkflowDigestMessage, isDemoOrganizationContext } from "@/lib/demoWorkspaceData";
import {
  DEFAULT_DEMO_BAILEYS_RECIPIENT,
  getDemoBaileysStatus,
  logoutDemoBaileysSession,
  persistDemoBaileysRecipient,
  persistDemoBaileysUrl,
  readDemoBaileysRecipient,
  readDemoBaileysUrl,
  runDemoBaileysReminders,
  startDemoBaileysSession,
  type DemoBaileysStatus,
} from "@/lib/demoBaileysClient";
import { readCachedSubscriptionStatus } from "@/lib/subscriptionStatusCache";

type DemoTriggerMode = "expiry_workflow" | "monthly_report";

export default function Reminders() {
  const { isDemoWorkspace } = useDemoPlanPreview();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const cachedSubscriptionStatus = readCachedSubscriptionStatus();
  const isDemoReminders = isDemoOrganizationContext({
    isDemoWorkspace,
    organizationId: user?.organizationId ?? cachedSubscriptionStatus?.organizationId,
    organizationType: cachedSubscriptionStatus?.organizationType,
  });
  const [gatewayUrl, setGatewayUrl] = useState(() => readDemoBaileysUrl());
  const [gatewayUrlInput, setGatewayUrlInput] = useState(() => readDemoBaileysUrl());
  const [pairingPhone, setPairingPhone] = useState("");
  const [recipientPhone, setRecipientPhone] = useState(() => readDemoBaileysRecipient());
  const [demoTriggerMode, setDemoTriggerMode] = useState<DemoTriggerMode>("expiry_workflow");
  const [demoLogs, setDemoLogs] = useState(() => demoReminderReport().recentReminderLogs);

  const { data: reminderReport, isLoading } = useQuery({
    queryKey: ["reminderReport"],
    queryFn: async () => {
      const res = await api.get("/reports/reminders");
      return res.data;
    },
    enabled: !isDemoReminders,
  });

  const baileysStatusQuery = useQuery({
    queryKey: ["demoBaileysStatus", gatewayUrl],
    queryFn: () => getDemoBaileysStatus(gatewayUrl),
    enabled: isDemoReminders,
    refetchInterval: isDemoReminders ? 3000 : false,
    retry: false,
  });

  const startBaileysMutation = useMutation({
    mutationFn: (phoneNumber?: string) => startDemoBaileysSession(gatewayUrl, phoneNumber),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["demoBaileysStatus", gatewayUrl] });
      toast.success("Demo WhatsApp session started");
    },
    onError: (error: any) => {
      toast.error(error?.message || "Unable to start demo WhatsApp session");
    },
  });

  const logoutBaileysMutation = useMutation({
    mutationFn: () => logoutDemoBaileysSession(gatewayUrl),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["demoBaileysStatus", gatewayUrl] });
      toast.success("Demo WhatsApp session logged out");
    },
    onError: (error: any) => {
      toast.error(error?.message || "Unable to logout demo WhatsApp session");
    },
  });

  const runMutation = useMutation({
    mutationFn: () => isDemoReminders
      ? runDemoBaileysReminders(
          demoTriggerMode === "monthly_report"
            ? demoMonthlyReportDigestMessage(new Date(), recipientPhone)
            : demoWorkflowDigestMessage(new Date(), recipientPhone),
          gatewayUrl
        ).then((data) => ({ data } as any))
      : api.post("/reminders/run"),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ["reminderReport"] });
      queryClient.invalidateQueries({ queryKey: ["dashboardReport"] });
      const result = response.data;
      if (isDemoReminders) {
        setDemoLogs((logs) => [
          {
            id: Date.now(),
            channel: "WHATSAPP",
            type: demoTriggerMode === "monthly_report" ? "MONTHLY_REPORT" : "EXPIRY_DIGEST",
            status: result.failedMessages > 0 ? "PARTIAL" : "SENT",
            memberCount: result.processedMembers ?? 0,
            sentAt: new Date().toISOString(),
          },
          ...logs,
        ].slice(0, 25));
        toast.success(`${demoTriggerMode === "monthly_report" ? "Monthly report" : "Expiry workflow"} sent: ${result.digestsSent ?? 0} WhatsApp digest(s)`);
        return;
      }
      toast.success(`Reminder run complete: ${result.digestsSent ?? 0} WhatsApp digest(s) sent`);
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || error?.message || "Failed to run reminders");
    },
  });

  const visibleReminderReport = useMemo(() => {
    if (!isDemoReminders) {
      return reminderReport;
    }
    return {
      ...demoReminderReport(),
      recentReminderLogs: demoLogs,
    };
  }, [demoLogs, isDemoReminders, reminderReport]);

  if (isLoading && !isDemoReminders) {
    return <div className="p-8">Loading reminders panel...</div>;
  }

  const settings = visibleReminderReport?.settings;
  const logs = visibleReminderReport?.recentReminderLogs ?? [];
  const manualQuota = visibleReminderReport?.manualQuota;
  const lastRun = logs[0];
  const manualRunsUsed = Number(manualQuota?.usedThisMonth ?? 0);
  const manualRunLimit = Number(manualQuota?.monthlyLimit ?? 20);
  const unlimitedManualRuns = Boolean(manualQuota?.unlimited);
  const demoBaileysStatus = baileysStatusQuery.data;
  const demoRecipientReady = !isDemoReminders || recipientPhone.trim().length >= 8;
  const manualQuotaBlocked = manualQuota?.canRun === false;
  const canRunManual = Boolean(manualQuota?.canRun ?? true) && (!isDemoReminders || demoRecipientReady);
  const runButtonLabel = runMutation.isPending
    ? "Processing..."
    : canRunManual
      ? "Run Now"
      : isDemoReminders && !demoRecipientReady
        ? "Add Phone"
        : "Limit Reached";
  const runButtonMobileLabel = runMutation.isPending
    ? "..."
    : canRunManual
      ? "Run"
      : isDemoReminders && !demoRecipientReady
        ? "Phone"
        : "Run";

  function updateRecipientPhone(value: string) {
    setRecipientPhone(value);
    persistDemoBaileysRecipient(value);
  }

  function saveDemoBaileysConfig() {
    persistDemoBaileysUrl(gatewayUrlInput);
    persistDemoBaileysRecipient(recipientPhone);
    const savedUrl = readDemoBaileysUrl();
    setGatewayUrl(savedUrl);
    setGatewayUrlInput(savedUrl);
    queryClient.invalidateQueries({ queryKey: ["demoBaileysStatus"] });
    toast.success("Demo WhatsApp configuration saved");
  }

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
              onClick={() => runMutation.mutate()}
              disabled={runMutation.isPending || !canRunManual}
            >
              <Play className="mr-1.5 sm:mr-2 h-3.5 sm:h-4 w-3.5 sm:w-4" />
              <span className="hidden sm:inline">{runButtonLabel}</span>
              <span className="sm:hidden">{runButtonMobileLabel}</span>
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
              {unlimitedManualRuns ? (
                <div className="rounded-[1rem] sm:rounded-[1.4rem] border border-sky-100 bg-sky-50/80 p-2.5 sm:p-4 text-[10px] sm:text-sm font-semibold text-sky-900">
                  Unlimited manual runs for demo use only.
                </div>
              ) : (
                <div className="rounded-[1rem] sm:rounded-[1.4rem] border border-amber-100 bg-amber-50/70 p-2.5 sm:p-4 text-[10px] sm:text-sm font-semibold text-amber-900">
                  20 runs per month limit. Scheduled jobs are separate.
                </div>
              )}
              {isDemoReminders && (
                <Badge className="w-fit text-[8px] sm:text-xs border-sky-200 bg-sky-100 text-sky-800 px-2 sm:px-3 py-1">Demo only</Badge>
              )}
              {isDemoReminders && (
                <DemoRecipientOverride
                  value={recipientPhone}
                  onChange={updateRecipientPhone}
                />
              )}
              {isDemoReminders && (
                <DemoTriggerModeSelector
                  value={demoTriggerMode}
                  onChange={setDemoTriggerMode}
                />
              )}
              {isDemoReminders && (
                <DemoBaileysPanel
                  gatewayUrlInput={gatewayUrlInput}
                  pairingPhone={pairingPhone}
                  status={demoBaileysStatus}
                  isStatusError={baileysStatusQuery.isError}
                  isStatusLoading={baileysStatusQuery.isLoading}
                  isStarting={startBaileysMutation.isPending}
                  isLoggingOut={logoutBaileysMutation.isPending}
                  onGatewayUrlChange={setGatewayUrlInput}
                  onPairingPhoneChange={setPairingPhone}
                  onSave={saveDemoBaileysConfig}
                  onStartQr={() => startBaileysMutation.mutate(undefined)}
                  onStartPairing={() => startBaileysMutation.mutate(pairingPhone)}
                  onLogout={() => logoutBaileysMutation.mutate()}
                />
              )}
              {manualQuotaBlocked && (
                <div className="rounded-[1rem] sm:rounded-[1.4rem] border border-rose-100 bg-rose-50/80 p-2.5 sm:p-4 text-[10px] sm:text-sm font-semibold text-rose-900">
                  Monthly limit reached. Resets next month.
                </div>
              )}
            </div>
          </div>

          {lastRun && (
            <div className="glass-panel overflow-hidden rounded-[1.5rem] sm:rounded-[1.9rem]">
              <div className="border-b border-white/70 p-3 sm:p-6">
                <h2 className="text-base sm:text-2xl font-black tracking-tight text-slate-950">Latest WhatsApp Digest</h2>
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
                <p className="mt-1 sm:mt-2 text-[10px] sm:text-sm font-medium text-slate-500">WhatsApp digest history.</p>
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
                        <div className="mt-0.5 text-[8px] sm:text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">
                          {log.type === "MONTHLY_REPORT" ? "Monthly report" : log.type === "EXPIRY_DIGEST" ? "Expiry workflow" : log.channel}
                        </div>
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

function DemoRecipientOverride({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div className="rounded-[1rem] sm:rounded-[1.4rem] border border-emerald-100 bg-emerald-50/85 p-3 sm:p-4 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <div className="text-[8px] sm:text-[10px] font-black uppercase tracking-[0.2em] text-emerald-700">Demo Digest Receiver</div>
          <div className="mt-1 text-sm sm:text-lg font-black text-slate-950">Admin phone override</div>
        </div>
        <Smartphone className="h-4 w-4 text-emerald-700" />
      </div>
      <Input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-3 h-10 rounded-xl bg-white/90 text-xs font-bold"
        placeholder={DEFAULT_DEMO_BAILEYS_RECIPIENT}
        inputMode="tel"
      />
    </div>
  );
}

function DemoTriggerModeSelector({
  value,
  onChange,
}: {
  value: DemoTriggerMode;
  onChange: (value: DemoTriggerMode) => void;
}) {
  const options: Array<{
    id: DemoTriggerMode;
    title: string;
    detail: string;
    tone: "emerald" | "sky";
  }> = [
    {
      id: "expiry_workflow",
      title: "Expiry Workflow",
      detail: "Send the expired-members admin digest.",
      tone: "emerald",
    },
    {
      id: "monthly_report",
      title: "Monthly Report",
      detail: "Send the gym performance report digest.",
      tone: "sky",
    },
  ];

  return (
    <div className="rounded-[1rem] sm:rounded-[1.4rem] border border-white/80 bg-white/70 p-3 sm:p-4 shadow-sm">
      <div className="flex items-center justify-between gap-2">
        <div>
          <div className="text-[8px] sm:text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Demo Trigger Mode</div>
          <div className="mt-1 text-sm sm:text-lg font-black text-slate-950">Choose what Run Now sends</div>
        </div>
        <Play className="h-4 w-4 text-slate-600" />
      </div>
      <div className="mt-3 grid gap-2 sm:grid-cols-2">
        {options.map((option) => {
          const active = value === option.id;
          return (
            <button
              key={option.id}
              type="button"
              onClick={() => onChange(option.id)}
              className={cn(
                "rounded-xl border p-3 text-left transition-all",
                active
                  ? option.tone === "emerald"
                    ? "border-emerald-200 bg-emerald-50 text-emerald-950 shadow-sm ring-2 ring-emerald-100"
                    : "border-sky-200 bg-sky-50 text-sky-950 shadow-sm ring-2 ring-sky-100"
                  : "border-slate-200 bg-white/80 text-slate-700 hover:bg-white"
              )}
            >
              <div className="text-xs font-black">{option.title}</div>
              <div className="mt-1 text-[10px] font-semibold leading-4 opacity-75">{option.detail}</div>
            </button>
          );
        })}
      </div>
    </div>
  );
}

function DemoBaileysPanel({
  gatewayUrlInput,
  pairingPhone,
  status,
  isStatusError,
  isStatusLoading,
  isStarting,
  isLoggingOut,
  onGatewayUrlChange,
  onPairingPhoneChange,
  onSave,
  onStartQr,
  onStartPairing,
  onLogout,
}: {
  gatewayUrlInput: string;
  pairingPhone: string;
  status?: DemoBaileysStatus;
  isStatusError: boolean;
  isStatusLoading: boolean;
  isStarting: boolean;
  isLoggingOut: boolean;
  onGatewayUrlChange: (value: string) => void;
  onPairingPhoneChange: (value: string) => void;
  onSave: () => void;
  onStartQr: () => void;
  onStartPairing: () => void;
  onLogout: () => void;
}) {
  const statusLabel = isStatusError ? "offline" : status?.status ?? (isStatusLoading ? "checking" : "idle");
  const connected = status?.status === "open";

  return (
    <div className="rounded-[1rem] sm:rounded-[1.4rem] border border-sky-100 bg-sky-50/80 p-3 sm:p-4 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <div className="text-[8px] sm:text-[10px] font-black uppercase tracking-[0.2em] text-sky-700">Demo WhatsApp</div>
          <div className="mt-1 text-sm sm:text-lg font-black text-slate-950">Baileys Gateway</div>
        </div>
        <Badge className={connected ? "border-emerald-200 bg-emerald-100 text-emerald-800" : "border-amber-200 bg-amber-100 text-amber-800"}>
          {statusLabel}
        </Badge>
      </div>

      <div className="mt-3 grid gap-2 sm:grid-cols-[minmax(0,1fr)_auto]">
        <Input
          value={gatewayUrlInput}
          onChange={(event) => onGatewayUrlChange(event.target.value)}
          className="h-10 rounded-xl bg-white/90 text-xs font-bold"
          placeholder="http://localhost:3217"
        />
        <Button type="button" variant="outline" className="h-10 rounded-xl bg-white/85 text-xs font-black" onClick={onSave}>
          <Save className="mr-2 h-3.5 w-3.5" />
          Save
        </Button>
      </div>

      <div className="mt-3 grid gap-2 sm:grid-cols-[minmax(0,1fr)_auto_auto]">
        <Input
          value={pairingPhone}
          onChange={(event) => onPairingPhoneChange(event.target.value)}
          className="h-10 rounded-xl bg-white/90 text-xs font-bold"
          placeholder="Pairing phone with country code"
        />
        <Button type="button" variant="outline" className="h-10 rounded-xl bg-white/85 text-xs font-black" onClick={onStartPairing} disabled={isStarting}>
          <Smartphone className="mr-2 h-3.5 w-3.5" />
          Pair
        </Button>
        <Button type="button" variant="outline" className="h-10 rounded-xl bg-white/85 text-xs font-black" onClick={onStartQr} disabled={isStarting}>
          <QrCode className="mr-2 h-3.5 w-3.5" />
          QR
        </Button>
      </div>

      <div className="mt-3 flex justify-end">
        <Button type="button" variant="outline" className="h-10 rounded-xl bg-white/85 text-xs font-black text-rose-700" onClick={onLogout} disabled={isLoggingOut}>
          <LogOut className="mr-2 h-3.5 w-3.5" />
          Logout
        </Button>
      </div>

      {status?.pairingCode && (
        <div className="mt-3 rounded-xl border border-white/80 bg-white/85 px-3 py-2 text-sm font-black text-slate-950">
          Pairing code: {status.pairingCode}
        </div>
      )}

      {status?.qrDataUrl && (
        <div className="mt-3 flex justify-center rounded-xl border border-white/80 bg-white/85 p-3">
          <img src={status.qrDataUrl} alt="Demo WhatsApp QR code" className="h-44 w-44 rounded-lg" />
        </div>
      )}

      {isStatusError && (
        <div className="mt-3 rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-[10px] sm:text-xs font-bold text-amber-900">
          Start the local gateway from <span className="font-black">demo-whatsapp-gateway</span> before connecting.
        </div>
      )}

      {status?.lastError && (
        <div className="mt-3 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-[10px] sm:text-xs font-bold text-rose-900">
          {status.lastError}
        </div>
      )}
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
