import { useEffect, useState, useRef } from "react";
import type { ReactNode } from "react";
import { useForm } from "react-hook-form";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { BellRing, MessageSquare } from "lucide-react";
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
    },
  });

  const watchConfig = watch();

  useEffect(() => {
    if (settings) {
      reset({
        whatsappEnabled: settings.whatsappEnabled || false,
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
            {watchConfig.whatsappEnabled ? (
              <div className="grid gap-5 min-w-0">
                <WhatsAppDigestCarousel />
              </div>
            ) : (
              <div className="flex min-h-[280px] flex-col items-center justify-center rounded-[1.6rem] border border-dashed border-slate-200 bg-white/50 text-center text-slate-400">
                <BellRing className="mb-4 h-10 w-10 opacity-50" />
                <p className="text-sm font-semibold">Enable WhatsApp to preview the admin digest.</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function WhatsAppExpiryPreview() {
  const members = [
    { name: "Rahul", meta: "overdue by 2 days" },
    { name: "Akhil", meta: "ends today" },
    { name: "Meera", meta: "ends today" },
  ];

  return (
    <div className="w-full shrink-0 snap-center overflow-hidden rounded-[1.6rem] border border-emerald-100 bg-[#e8fff3] shadow-sm">
      <div className="bg-[#075e54] px-5 py-4 text-white">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-white/15">
            <MessageSquare className="h-5 w-5" />
          </div>
          <div>
            <div className="text-sm font-black">BillMyGym Alert</div>
            <div className="text-[11px] font-semibold text-white/75">Expiry Digest Preview</div>
          </div>
        </div>
      </div>
      <div className="space-y-3 p-5">
        <div className="ml-auto max-w-[88%] rounded-2xl rounded-tr-sm bg-white px-4 py-3 text-sm font-semibold leading-6 text-slate-700 shadow-sm">
          <div className="font-black text-slate-950">Expiry Alert</div>
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

function WhatsAppMonthlyReportPreview() {
  return (
    <div className="w-full shrink-0 snap-center overflow-hidden rounded-[1.6rem] border border-sky-100 bg-[#e8f4ff] shadow-sm">
      <div className="bg-[#075e54] px-5 py-4 text-white">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-white/15">
            <MessageSquare className="h-5 w-5" />
          </div>
          <div>
            <div className="text-sm font-black">BillMyGym Report</div>
            <div className="text-[11px] font-semibold text-white/75">Monthly Digest Preview</div>
          </div>
        </div>
      </div>
      <div className="space-y-3 p-5">
        <div className="ml-auto max-w-[88%] rounded-2xl rounded-tr-sm bg-white px-4 py-3 text-sm font-semibold leading-6 text-slate-700 shadow-sm">
          <div className="font-black text-slate-950">BillMyGym Monthly Report | October 2023</div>
          <div className="mt-3 space-y-2">
            <div className="rounded-xl bg-sky-50 px-3 py-2">New members: <span className="font-bold">12</span></div>
            <div className="rounded-xl bg-sky-50 px-3 py-2">Active members: <span className="font-bold">45</span></div>
            <div className="rounded-xl bg-sky-50 px-3 py-2">Collected revenue: <span className="font-bold">₹ 1,84,000</span></div>
          </div>
        </div>
        <div className="text-right text-[10px] font-bold uppercase tracking-[0.18em] text-sky-800/70">Delivered to admin WhatsApp</div>
      </div>
    </div>
  );
}

function WhatsAppDigestCarousel() {
  const [activeIndex, setActiveIndex] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);

  const scrollTo = (index: number) => {
    if (!containerRef.current) return;
    const container = containerRef.current;
    const scrollAmount = index * container.clientWidth;
    container.scrollTo({ left: scrollAmount, behavior: "smooth" });
    setActiveIndex(index);
  };

  return (
    <div className="relative w-full">
      <div 
        ref={containerRef}
        className="flex w-full snap-x snap-mandatory overflow-x-auto pb-4 [&::-webkit-scrollbar]:hidden"
        onScroll={(e) => {
          const container = e.currentTarget;
          const slideIndex = Math.round(container.scrollLeft / container.clientWidth);
          if (slideIndex !== activeIndex) setActiveIndex(slideIndex);
        }}
        style={{ scrollbarWidth: "none", msOverflowStyle: "none" }}
      >
        <WhatsAppExpiryPreview />
        <WhatsAppMonthlyReportPreview />
      </div>
      
      <div className="mt-2 flex items-center justify-center gap-2">
        <button 
          type="button"
          onClick={() => scrollTo(0)} 
          className={`h-2 w-2 rounded-full transition-all ${activeIndex === 0 ? "bg-emerald-600 w-4" : "bg-slate-300"}`} 
          aria-label="View Expiry Digest"
        />
        <button 
          type="button"
          onClick={() => scrollTo(1)} 
          className={`h-2 w-2 rounded-full transition-all ${activeIndex === 1 ? "bg-emerald-600 w-4" : "bg-slate-300"}`}
          aria-label="View Monthly Report Digest"
        />
      </div>
    </div>
  );
}

function ChannelRow({
  icon,
  title,
  description,
  register,
}: {
  icon: ReactNode;
  title: string;
  description: string;
  register: any;
}) {
  return (
    <div className="flex items-center justify-between rounded-[1.5rem] border border-white/80 bg-white/72 p-5 shadow-sm">
      <div className="flex items-center gap-4">
        <div className="rounded-xl bg-emerald-100/80 p-3 text-emerald-700">{icon}</div>
        <div>
          <h4 className="font-black text-slate-950">{title}</h4>
          <p className="mt-1 text-sm font-medium text-slate-500">{description}</p>
        </div>
      </div>
      <label className="relative inline-flex cursor-pointer items-center">
        <input type="checkbox" className="peer sr-only" {...register} />
        <div
          className="relative h-8 w-16 rounded-full border border-slate-200 bg-slate-200/80 transition-colors after:absolute after:left-[3px] after:top-1/2 after:h-6 after:w-6 after:-translate-y-1/2 after:rounded-full after:bg-white after:shadow-sm after:transition-transform after:content-[''] peer-checked:border-emerald-500 peer-checked:bg-emerald-500 peer-checked:after:translate-x-8"
        />
      </label>
    </div>
  );
}
