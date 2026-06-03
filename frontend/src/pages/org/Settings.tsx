import { useEffect } from "react";
import type { ReactNode } from "react";
import { useForm } from "react-hook-form";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { BellRing, Mail, MessageSquare } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import api from "@/lib/api";

export default function Settings() {
  const queryClient = useQueryClient();

  const { data: settings, isLoading } = useQuery({
    queryKey: ["settings"],
    queryFn: async () => {
      const res = await api.get("/settings");
      return res.data;
    },
  });

  const { register, handleSubmit, reset, watch, formState: { isSubmitting } } = useForm({
    defaultValues: {
      whatsappEnabled: false,
      emailEnabled: false,
    },
  });

  const watchConfig = watch();

  useEffect(() => {
    if (settings) {
      reset({
        whatsappEnabled: settings.whatsappEnabled || false,
        emailEnabled: settings.emailEnabled || false,
      });
    }
  }, [settings, reset]);

  const mutation = useMutation({
    mutationFn: (data: any) => api.patch("/settings", data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["settings"] });
      toast.success("Settings saved successfully.");
    },
    onError: () => {
      toast.error("Failed to save settings.");
    },
  });

  if (isLoading) {
    return <div className="p-8">Loading settings...</div>;
  }

  return (
    <div className="mx-auto max-w-6xl space-y-8 p-8">
      <div className="grid gap-8 lg:grid-cols-[1.15fr_0.85fr]">
        <form id="settings-form" onSubmit={handleSubmit((data) => mutation.mutate(data))} className="space-y-6">
          <div className="glass-panel overflow-hidden rounded-[1.9rem]">
            <div className="border-b border-white/70 p-6">
              <h2 className="flex items-center gap-3 text-2xl font-black tracking-tight text-slate-950">
                <MessageSquare className="h-5 w-5 text-emerald-700" />
                Delivery Channels
              </h2>
            </div>
            <div className="space-y-4 p-6">
              <ChannelRow
                icon={<MessageSquare className="h-5 w-5" />}
                title="WhatsApp Digest"
                description="Send the admin summary through WhatsApp."
                register={register("whatsappEnabled")}
                tone="emerald"
              />
              <ChannelRow
                icon={<Mail className="h-5 w-5" />}
                title="Email Digest"
                description="Send the admin summary over email."
                register={register("emailEnabled")}
                tone="blue"
              />
            </div>
          </div>

          <div className="flex justify-end">
            <Button type="submit" form="settings-form" disabled={isSubmitting} className="h-12 rounded-full bg-slate-950 px-8 font-bold text-white hover:bg-slate-800">
              {isSubmitting ? "Saving..." : "Save Preferences"}
            </Button>
          </div>
        </form>

        <div className="glass-panel sticky top-24 overflow-hidden rounded-[1.9rem]">
          <div className="border-b border-white/70 p-6 text-center">
            <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">Live Preview</div>
            <h2 className="mt-2 text-2xl font-black tracking-tight text-slate-950">Digest Experience</h2>
          </div>
          <div className="p-6">
            {watchConfig.whatsappEnabled || watchConfig.emailEnabled ? (
              <div className="grid gap-5">
                {watchConfig.whatsappEnabled && (
                  <WhatsAppDigestPreview />
                )}
                {watchConfig.emailEnabled && (
                  <EmailDigestPreview />
                )}
              </div>
            ) : (
              <div className="flex min-h-[280px] flex-col items-center justify-center rounded-[1.6rem] border border-dashed border-slate-200 bg-white/50 text-center text-slate-400">
                <BellRing className="mb-4 h-10 w-10 opacity-50" />
                <p className="text-sm font-semibold">Enable a channel to preview the admin digest.</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function WhatsAppDigestPreview() {
  const members = [
    { name: "Rahul", meta: "overdue by 2 days" },
    { name: "Akhil", meta: "ends today" },
    { name: "Meera", meta: "ends today" },
  ];

  return (
    <div className="overflow-hidden rounded-[1.6rem] border border-emerald-100 bg-[#e8fff3] shadow-sm">
      <div className="bg-[#075e54] px-5 py-4 text-white">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-white/15">
            <MessageSquare className="h-5 w-5" />
          </div>
          <div>
            <div className="text-sm font-black">BillMyGym Digest</div>
            <div className="text-[11px] font-semibold text-white/75">WhatsApp preview</div>
          </div>
        </div>
      </div>
      <div className="space-y-3 p-5">
        <div className="ml-auto max-w-[88%] rounded-2xl rounded-tr-sm bg-white px-4 py-3 text-sm font-semibold leading-6 text-slate-700 shadow-sm">
          <div className="font-black text-slate-950">BillMyGym Alert</div>
          <div className="mt-2">Overdue memberships:</div>
          <div className="mt-3 space-y-2">
            {members.map((member, index) => (
              <div key={member.name} className="rounded-xl bg-emerald-50 px-3 py-2">
                {index + 1}. {member.name} | {member.meta}
              </div>
            ))}
          </div>
          <div className="mt-3 font-black text-emerald-800">Total needing follow-up: {members.length}</div>
        </div>
        <div className="text-right text-[10px] font-bold uppercase tracking-[0.18em] text-emerald-800/70">Delivered to admin WhatsApp</div>
      </div>
    </div>
  );
}

function EmailDigestPreview() {
  const rows = [
    { name: "Rahul", status: "Overdue", detail: "2 days overdue" },
    { name: "Akhil", status: "Due Today", detail: "Renewal closes today" },
    { name: "Meera", status: "Due Today", detail: "Renewal closes today" },
  ];

  return (
    <div className="overflow-hidden rounded-[1.6rem] border border-sky-100 bg-white shadow-sm">
      <div className="border-b border-slate-100 bg-[linear-gradient(135deg,rgba(14,165,233,0.12),rgba(255,255,255,0.96))] px-5 py-4">
        <div className="mb-3 inline-flex rounded-full bg-sky-100 px-3 py-1 text-[10px] font-black uppercase tracking-[0.2em] text-sky-700">
          Email
        </div>
        <div className="text-lg font-black tracking-tight text-slate-950">Membership Renewal Summary</div>
        <div className="mt-1 text-xs font-semibold text-slate-500">From: BillMyGym System</div>
      </div>
      <div className="p-5">
        <div className="rounded-2xl border border-slate-100 bg-slate-50/80 p-4">
          <div className="text-[10px] font-black uppercase tracking-[0.22em] text-slate-500">Members needing attention</div>
          <div className="mt-4 divide-y divide-slate-200">
            {rows.map((row) => (
              <div key={row.name} className="grid grid-cols-[1fr_auto] gap-3 py-3">
                <div>
                  <div className="text-sm font-black text-slate-950">{row.name}</div>
                  <div className="mt-1 text-xs font-semibold text-slate-500">{row.detail}</div>
                </div>
                <div className="self-center rounded-full border border-sky-100 bg-white px-3 py-1 text-[10px] font-black uppercase tracking-[0.16em] text-sky-700">
                  {row.status}
                </div>
              </div>
            ))}
          </div>
        </div>
        <div className="mt-4 rounded-2xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm font-bold text-emerald-900">
          Total follow-ups: {rows.length}
        </div>
      </div>
    </div>
  );
}

function ChannelRow({
  icon,
  title,
  description,
  register,
  tone,
}: {
  icon: ReactNode;
  title: string;
  description: string;
  register: any;
  tone: "emerald" | "blue";
}) {
  const checkedClass =
    tone === "emerald"
      ? "peer-checked:bg-emerald-500 peer-checked:border-emerald-500"
      : "peer-checked:bg-sky-500 peer-checked:border-sky-500";

  return (
    <div className="flex items-center justify-between rounded-[1.5rem] border border-white/80 bg-white/72 p-5 shadow-sm">
      <div className="flex items-center gap-4">
        <div className={`rounded-xl p-3 ${tone === "emerald" ? "bg-emerald-100/80 text-emerald-700" : "bg-sky-100/80 text-sky-700"}`}>{icon}</div>
        <div>
          <h4 className="font-black text-slate-950">{title}</h4>
          <p className="mt-1 text-sm font-medium text-slate-500">{description}</p>
        </div>
      </div>
      <label className="relative inline-flex cursor-pointer items-center">
        <input type="checkbox" className="peer sr-only" {...register} />
        <div
          className={`relative h-8 w-16 rounded-full border border-slate-200 bg-slate-200/80 transition-colors after:absolute after:left-[3px] after:top-1/2 after:h-6 after:w-6 after:-translate-y-1/2 after:rounded-full after:bg-white after:shadow-sm after:transition-transform after:content-[''] peer-checked:after:translate-x-8 ${checkedClass}`}
        />
      </label>
    </div>
  );
}
