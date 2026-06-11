import { useMutation, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, BadgeCheck, DatabaseZap, RotateCcw } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import api from "@/lib/api";

type DemoResetResponse = {
  success: boolean;
  message: string;
};

export default function DemoManagement() {
  const queryClient = useQueryClient();
  const resetMutation = useMutation({
    mutationFn: async () => {
      const response = await api.post<DemoResetResponse>("/admin/demo/reset");
      return response.data;
    },
    onSuccess: async (response) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["organizations"] }),
        queryClient.invalidateQueries({ queryKey: ["organizationDirectory"] }),
        queryClient.invalidateQueries({ queryKey: ["dashboardReport"] }),
        queryClient.invalidateQueries({ queryKey: ["financialReport"] }),
        queryClient.invalidateQueries({ queryKey: ["reminderReport"] }),
      ]);
      toast.success(response.message || "Demo workspace reset successfully");
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || "Unable to reset demo workspace");
    },
  });

  function resetDemoWorkspace() {
    const confirmed = window.confirm(
      "Reset the demo workspace? This deletes demo members, memberships, payments, notifications, reports, and analytics before reseeding the workspace."
    );
    if (!confirmed) return;
    resetMutation.mutate();
  }

  return (
    <div className="mx-auto max-w-6xl space-y-5 px-3 pb-8 pt-3 sm:space-y-6 sm:p-6 lg:p-8">
      <section className="relative overflow-hidden rounded-[1.45rem] border border-slate-200 bg-white p-4 shadow-sm sm:rounded-[2rem] sm:p-7 lg:p-10">
        <div className="absolute inset-y-0 right-0 w-1/2 bg-[radial-gradient(ellipse_at_top_right,rgba(16,185,129,0.1),transparent_50%),radial-gradient(ellipse_at_bottom_right,rgba(14,165,233,0.08),transparent_52%)]" />
        <div className="relative z-10 grid gap-5 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <Badge className="border-sky-200 bg-sky-100 text-sky-800">
                <DatabaseZap className="mr-1.5 h-3.5 w-3.5" />
                Demo Management
              </Badge>
              <Badge className="border-emerald-200 bg-emerald-100 text-emerald-800">Internal only</Badge>
            </div>
            <h1 className="mt-4 max-w-3xl text-3xl font-black tracking-tight text-slate-950 sm:text-4xl lg:text-5xl">
              Reset the sales demo workspace in one click.
            </h1>
            <p className="mt-3 max-w-2xl text-sm font-semibold leading-6 text-slate-600 sm:text-base sm:leading-7">
              Rebuild the demo organization with deterministic members, memberships, payments, reports, and notification history before the next walkthrough.
            </p>
          </div>

          <div className="rounded-[1.35rem] border border-amber-200 bg-amber-50/85 p-4 shadow-sm sm:rounded-[1.8rem] sm:p-5">
            <div className="flex items-start gap-3">
              <div className="rounded-2xl bg-amber-100 p-2 text-amber-700">
                <AlertTriangle className="h-5 w-5" />
              </div>
              <div>
                <div className="text-sm font-black text-amber-950">Demo data will be replaced</div>
                <p className="mt-1 text-xs font-semibold leading-5 text-amber-800 sm:text-sm">
                  This action is restricted to demo organizations. Regular organizations are blocked server-side.
                </p>
              </div>
            </div>
            <Button
              type="button"
              className="mt-4 h-11 w-full rounded-xl bg-slate-950 text-sm font-black text-white hover:bg-slate-800"
              onClick={resetDemoWorkspace}
              disabled={resetMutation.isPending}
            >
              <RotateCcw className="mr-2 h-4 w-4" />
              {resetMutation.isPending ? "Resetting..." : "Reset Demo Workspace"}
            </Button>
          </div>
        </div>
      </section>

      <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <DemoSpecCard label="Members" value="30" detail="Seeded demo profiles" />
        <DemoSpecCard label="Active" value="20" detail="Paid memberships" />
        <DemoSpecCard label="Expiring" value="5" detail="Due today" />
        <DemoSpecCard label="Risk" value="5" detail="Overdue and expired" />
      </section>

      <section className="grid gap-4 lg:grid-cols-[0.95fr_1.05fr]">
        <div className="rounded-[1.45rem] border border-slate-200 bg-white p-5 shadow-sm sm:rounded-[1.8rem] sm:p-6">
          <div className="flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.22em] text-emerald-700">
            <BadgeCheck className="h-4 w-4" />
            Seeded Revenue
          </div>
          <div className="mt-4 grid grid-cols-2 gap-3">
            <div className="rounded-2xl border border-emerald-100 bg-emerald-50 p-4">
              <div className="text-[10px] font-black uppercase tracking-[0.18em] text-emerald-700">Collected</div>
              <div className="mt-2 text-2xl font-black text-slate-950">₹25,000</div>
            </div>
            <div className="rounded-2xl border border-rose-100 bg-rose-50 p-4">
              <div className="text-[10px] font-black uppercase tracking-[0.18em] text-rose-700">Pending</div>
              <div className="mt-2 text-2xl font-black text-slate-950">₹4,000</div>
            </div>
          </div>
        </div>

        <div className="rounded-[1.45rem] border border-slate-200 bg-white p-5 shadow-sm sm:rounded-[1.8rem] sm:p-6">
          <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">Reset Workflow</div>
          <div className="mt-4 grid gap-2 sm:grid-cols-2">
            {["Delete demo members", "Delete demo memberships", "Delete demo payments", "Delete demo notifications", "Clear analytics", "Run demo seeder"].map((item) => (
              <div key={item} className="rounded-xl border border-slate-100 bg-slate-50 px-3 py-2 text-xs font-black text-slate-700">
                {item}
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}

function DemoSpecCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <div className="rounded-[1.25rem] border border-slate-200 bg-white p-4 shadow-sm sm:rounded-[1.5rem] sm:p-5">
      <div className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">{label}</div>
      <div className="mt-2 text-3xl font-black tracking-tight text-slate-950">{value}</div>
      <div className="mt-1 text-xs font-semibold text-slate-500">{detail}</div>
    </div>
  );
}
